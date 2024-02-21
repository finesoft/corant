/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.modules.mongodb;

import static org.corant.context.Beans.find;
import static org.corant.context.Beans.findNamed;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.getMapInstant;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Services.resolve;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.corant.config.Configs;
import org.corant.context.naming.NamingReference;
import org.corant.context.qualifier.Preference;
import org.corant.context.qualifier.Qualifiers.DefaultNamedQualifierObjectManager;
import org.corant.context.qualifier.Qualifiers.NamedQualifierObjectManager;
import org.corant.modules.mongodb.MongoClientConfig.MongodbConfig;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.normal.Priorities;
import org.corant.shared.ubiquity.Sortable;
import org.eclipse.microprofile.config.ConfigProvider;
import com.mongodb.MongoClientSettings.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;

/**
 * corant-modules-mongodb
 *
 * Initialize the named qualifier Mongo Client bean/Mongo Database bean for injection, use Unnamed
 * qualifier for injection while the configurations do not assign a name.
 *
 * @author bingo 下午4:55:16
 */
public class MongoClientExtension implements Extension {

  final Logger logger = Logger.getLogger(this.getClass().getName());
  volatile InitialContext jndi;
  volatile NamedQualifierObjectManager<MongoClientConfig> clientConfigManager =
      NamedQualifierObjectManager.empty();
  volatile NamedQualifierObjectManager<MongodbConfig> databaseConfigManager =
      NamedQualifierObjectManager.empty();

  public static BsonValue bsonId(Serializable id) {
    if (id == null) {
      return null;
    }
    if (id instanceof Long || Long.TYPE.equals(id.getClass())) {
      return new BsonInt64((Long) id);
    } else if (id instanceof Integer || Integer.TYPE.equals(id.getClass())) {
      return new BsonInt32((Integer) id);
    } else {
      return new BsonString(asString(id));
    }
  }

  public static MongoClient getClient(String name) {
    if (isNotBlank(name) && name.startsWith(MongoClientConfig.JNDI_SUBCTX_NAME)) {
      try {
        return forceCast(new InitialContext().lookup(name));
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    } else {
      return findNamed(MongoClient.class, name).orElseThrow(
          () -> new CorantRuntimeException("Can not find any mongo client named %s.", name));
    }
  }

  public static MongoDatabase getDatabase(String namespace) {
    return findNamed(MongoDatabase.class, namespace).orElseThrow(
        () -> new CorantRuntimeException("Can not find any mongo database named %s.", namespace));
  }

  public static GridFSBucket getGridFSBucket(String namespace) {
    return resolve(MongoClientExtension.class).produceGridFSBucket(namespace);
  }

  public NamedQualifierObjectManager<MongoClientConfig> getClientConfigManager() {
    return clientConfigManager;
  }

  public NamedQualifierObjectManager<MongodbConfig> getDatabaseConfigManager() {
    return databaseConfigManager;
  }

  public Instant getDatabaseLocalTime(MongoDatabase db) {
    return getMapInstant(
        db.runCommand(new Document(mapOf("serverStatus", 1, "repl", 0, "metrics", 0, "locks", 0))),
        "localTime");
  }

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      clientConfigManager.getAllWithQualifiers().forEach((c, n) -> {
        event.<MongoClient>addBean().addQualifiers(n).addTransitiveTypeClosure(MongoClient.class)
            .beanClass(MongoClient.class).scope(ApplicationScoped.class)
            .produceWith(beans -> produceClient(beans, c, n))
            .disposeWith((mc, beans) -> mc.close());
        if (c.isBindToJndi()) {
          resolveJndi(c.getName(), n);
        }
      });

      databaseConfigManager.getAllWithQualifiers().forEach((c, n) -> {
        event.<MongoDatabase>addBean().addQualifiers(n)
            .addTransitiveTypeClosure(MongoDatabase.class).beanClass(MongoDatabase.class)
            .scope(ApplicationScoped.class).produceWith(beans -> produceDatabase(beans, c));
      });
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    clientConfigManager = MongoClientConfig.from(ConfigProvider.getConfig());
    Set<MongodbConfig> dbCfgs = new HashSet<>();
    clientConfigManager.getAllWithNames().values().stream()
        .flatMap(mc -> mc.getDatabases().values().stream())
        .forEach(dbc -> shouldBeTrue(dbCfgs.add(dbc), "The mongodb data base named %s dup!",
            dbc.getName()));
    databaseConfigManager = new DefaultNamedQualifierObjectManager<>(dbCfgs);
    if (databaseConfigManager.isEmpty()) {
      logger.info(() -> "Can not find any mongodb databases!");
    } else {
      logger.info(() -> String.format("Found %s mongodb databases named [%s].",
          databaseConfigManager.size(),
          String.join(",", databaseConfigManager.getAllDisplayNames())));
    }
  }

  protected void onBeforeShutdown(
      @Observes @Priority(Priorities.FRAMEWORK_LOWER) BeforeShutdown bs) {
    clientConfigManager.destroy();
    databaseConfigManager.destroy();
  }

  protected MongoClient produceClient(Instance<Object> beans, MongoClientConfig cfg,
      Annotation[] qualifiers) {
    List<ServerAddress> seeds = cfg.getHostAndPorts().stream()
        .map(hnp -> new ServerAddress(hnp.getLeft(), hnp.getRight())).collect(Collectors.toList());
    Builder builder = cfg.produceBuiler();
    if (isNotEmpty(seeds)) {
      builder.applyToClusterSettings(b -> b.hosts(seeds));
    } else {
      builder.applyConnectionString(cfg.getUri());
    }
    MongoCredential credential = cfg.produceCredential();
    if (credential != null) {
      builder.credential(credential);
    }
    if (!beans.select(MongoClientConfigurator.class).isUnsatisfied()) {
      beans.select(MongoClientConfigurator.class, qualifiers).stream()
          .sorted(Sortable::reverseCompare).forEach(h -> h.configure(cfg, builder));
    }
    return MongoClients.create(builder.build());
  }

  protected MongoDatabase produceDatabase(Instance<Object> beans, MongodbConfig cfg) {
    MongoClient mc =
        find(MongoClient.class, clientConfigManager.getQualifiers(cfg.getClientName())).orElseThrow(
            () -> new CorantRuntimeException("Can not find mongo data base with client name %s.",
                cfg.getClientName()));
    return mc.getDatabase(cfg.getDatabaseName());
  }

  protected GridFSBucket produceGridFSBucket(String bucketNamespace) {
    String[] names = split(bucketNamespace, Names.NAME_SPACE_SEPARATORS, true, true);
    shouldBeTrue(!isEmpty(names) && names.length > 1);
    if (names.length == 2) {
      return GridFSBuckets.create(getDatabase(names[0]), names[1]);
    } else {
      return GridFSBuckets.create(getDatabase(names[0] + Names.NAME_SPACE_SEPARATORS + names[1]),
          names[2]);
    }
  }

  protected void validate(@Observes AfterDeploymentValidation adv, BeanManager bm) {
    clientConfigManager.getAllWithQualifiers().forEach((cfg, quas) -> {
      if (cfg.isVerifyDeployment()) {
        logger.info(() -> String.format("Check mongo client %s", cfg.getName()));
        resolve(MongoClient.class, quas).startSession().close();// FIXME use another ways.
      }
    });
  }

  void resolveJndi(String name, Annotation[] qualifiers) {
    if (isNotBlank(name)) {
      synchronized (this) {
        try {
          if (jndi == null) {
            jndi = new InitialContext();
            jndi.createSubcontext(MongoClientConfig.JNDI_SUBCTX_NAME);
          }
          String jndiName = MongoClientConfig.JNDI_SUBCTX_NAME + "/" + name;
          jndi.bind(jndiName, new NamingReference(MongoClient.class, qualifiers));
          logger.info(() -> String.format("Bind mongo client %s to jndi.", jndiName));
        } catch (NamingException e) {
          throw new CorantRuntimeException(e);
        }
      }
    }
  }

  @ApplicationScoped
  public static class GridFSBucketProducer {

    @Produces
    @Dependent
    @Preference
    protected GridFSBucket produce(InjectionPoint ip) {
      Preference preference = null;
      if (ip.getAnnotated() != null) {
        preference = ip.getAnnotated().getAnnotation(Preference.class);
      }
      if (preference == null) {
        Optional<Annotation> annop = ip.getQualifiers().stream()
            .filter(p -> p.annotationType().equals(Preference.class)).findFirst();
        if (annop.isPresent()) {
          preference = (Preference) annop.get();
        }
      }
      if (preference != null) {
        return MongoClientExtension
            .getGridFSBucket(Configs.assemblyStringConfigProperty(preference.value()));
      }
      return null;
    }
  }
}

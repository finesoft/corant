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
package org.corant.suites.mongodb;

import static org.corant.kernel.util.Instances.resolveNamed;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getMapInstant;
import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.corant.Corant;
import org.corant.kernel.normal.Names;
import org.corant.kernel.util.Instances.NamingReference;
import org.corant.kernel.util.Qualifiers.DefaultNamedQualifierObjectManager;
import org.corant.kernel.util.Qualifiers.NamedQualifierObjectManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.mongodb.MongoClientConfig.MongodbConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

/**
 * corant-suites-mongodb
 *
 * Initialize the named qualifier Mongo Client bean/Mongo Database bean for injection, use Unnamed
 * qualifier for injection while the configurations do not assign a name.
 *
 * @author bingo 下午4:55:16
 *
 */
public class MongoClientExtension implements Extension {

  final Logger logger = Logger.getLogger(this.getClass().getName());
  volatile InitialContext jndi;
  volatile NamedQualifierObjectManager<MongoClientConfig> clientConfigManager =
      NamedQualifierObjectManager.empty();
  volatile NamedQualifierObjectManager<MongodbConfig> databaseConfigManager =
      NamedQualifierObjectManager.empty();
  Set<String> gridFSBucketNames = new HashSet<>();

  public static BsonValue bsonId(Serializable id) {
    if (id == null) {
      return null;
    }
    if (id instanceof Long || Long.TYPE.equals(id.getClass())) {
      return new BsonInt64(Long.class.cast(id));
    } else if (id instanceof Integer || Integer.TYPE.equals(id.getClass())) {
      return new BsonInt32(Integer.class.cast(id));
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
      return resolveNamed(MongoClient.class, name).orElseThrow(
          () -> new CorantRuntimeException("Can not find any mongo client named %s", name));
    }
  }

  public static MongoDatabase getDatabase(String namespace) {
    return resolveNamed(MongoDatabase.class, namespace).orElseThrow(
        () -> new CorantRuntimeException("Can not find any mongo database named %s", namespace));
  }

  /**
   *
   * @return the clientConfigManager
   */
  public NamedQualifierObjectManager<MongoClientConfig> getClientConfigManager() {
    return clientConfigManager;
  }

  /**
   *
   * @return the databaseConfigManager
   */
  public NamedQualifierObjectManager<MongodbConfig> getDatabaseConfigManager() {
    return databaseConfigManager;
  }

  public Instant getDatabaseLocalTime(MongoDatabase db) {
    return getMapInstant(
        db.runCommand(new Document(mapOf("serverStatus", 1, "repl", 0, "metrics", 0, "locks", 0))),
        "localTime");
  }

  public GridFSBucket getGridFSBucket(String namespace) {
    if (Corant.me() == null) {
      return null;
    }
    Instance<GridFSBucket> inst =
        Corant.instance().select(GridFSBucket.class, NamedLiteral.of(namespace));
    return inst.isResolvable() ? inst.get() : null;
  }

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      clientConfigManager.getAllWithQualifiers().forEach((c, n) -> {
        event.<MongoClient>addBean().addQualifiers(n).addTransitiveTypeClosure(MongoClient.class)
            .beanClass(MongoClient.class).scope(ApplicationScoped.class).produceWith(beans -> {
              return produceClient(beans, c, n);
            }).disposeWith((mc, beans) -> mc.close());
        resolveJndi(c.getName(), n);
      });

      databaseConfigManager.getAllWithQualifiers().forEach((c, n) -> {
        event.<MongoDatabase>addBean().addQualifiers(n)
            .addTransitiveTypeClosure(MongoDatabase.class).beanClass(MongoDatabase.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              return produceDatabase(beans, c);
            });
      });

      for (final String gfn : gridFSBucketNames) {
        event.<GridFSBucket>addBean().addQualifier(NamedLiteral.of(gfn))
            .addTransitiveTypeClosure(GridFSBucket.class).beanClass(GridFSBucket.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              return produceGridFSBucket(gfn);
            });
      }
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
      logger.info(
          () -> String.format("Find %s mongodb databases named [%s].", databaseConfigManager.size(),
              String.join(",", databaseConfigManager.getAllDisplayNames())));
    }
  }

  protected MongoClient produceClient(Instance<Object> beans, MongoClientConfig cfg,
      Annotation[] qualifiers) {
    List<ServerAddress> seeds = cfg.getHostAndPorts().stream()
        .map(hnp -> new ServerAddress(hnp.getLeft(), hnp.getRight())).collect(Collectors.toList());
    MongoCredential credential = cfg.produceCredential();
    Builder builder = cfg.produceBuiler();
    if (!beans.select(MongoClientConfigurator.class).isUnsatisfied()) {
      beans.select(MongoClientConfigurator.class, qualifiers).forEach(h -> h.configure(builder));
    }
    MongoClientOptions clientOptions = builder.build();
    MongoClient mc = credential == null ? new MongoClient(seeds, clientOptions)
        : new MongoClient(seeds, credential, clientOptions);
    return mc;
  }

  protected MongoDatabase produceDatabase(Instance<Object> beans, MongodbConfig cfg) {
    MongoClient mc = beans.select(MongoClient.class, NamedLiteral.of(cfg.getClientName())).get();
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

  void onProcessInjectionPoint(@Observes ProcessInjectionPoint<?, ?> pip, BeanManager beanManager) {
    final InjectionPoint ip = pip.getInjectionPoint();
    if (GridFSBucket.class.equals(ip.getType())) {
      Named named = ip.getAnnotated().getAnnotation(Named.class);
      String[] names = split(named.value(), Names.NAME_SPACE_SEPARATORS, true, true);
      if (!isEmpty(names) && names.length > 1) {
        gridFSBucketNames.add(named.value());
      }
    }
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
}

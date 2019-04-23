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

import static org.corant.shared.util.Assertions.shouldBeNull;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.asMap;
import static org.corant.shared.util.MapUtils.getMapInstant;
import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
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
import org.corant.kernel.util.Cdis;
import org.corant.kernel.util.Manageables.NamingReference;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
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

  protected final Set<String> databaseNames = new LinkedHashSet<>();
  protected final Set<String> clientNames = new LinkedHashSet<>();
  protected final Set<String> gridFSBucketNames = new LinkedHashSet<>();
  protected final Map<String, MongoClientConfig> clientConfigs = new HashMap<>();
  protected final Map<String, MongodbConfig> databaseConfigs = new HashMap<>();
  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  volatile boolean initedJndiSubCtx = false;
  volatile InitialContext jndi;

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

  public MongoClient getClient(String clientName) {
    if (Corant.me() == null) {
      return null;
    }
    Instance<MongoClient> inst =
        Corant.instance().select(MongoClient.class, NamedLiteral.of(clientName));
    return inst.isResolvable() ? inst.get() : null;
  }

  /**
   * @return the clientConfigs
   */
  public Map<String, MongoClientConfig> getClientConfigs() {
    return Collections.unmodifiableMap(clientConfigs);
  }

  /**
   * @return the clientNames
   */
  public Set<String> getClientNames() {
    return Collections.unmodifiableSet(clientNames);
  }

  /**
   * @return the clients
   */
  public Map<String, MongoClient> getClients() {
    Map<String, MongoClient> map = new HashMap<>();
    for (String clientName : clientNames) {
      MongoClient mc = getClient(clientName);
      if (mc != null) {
        map.put(clientName, mc);
      }
    }
    return Collections.unmodifiableMap(map);
  }

  public MongoDatabase getDatabase(String namespace) {
    if (Corant.me() == null) {
      return null;
    }
    Instance<MongoDatabase> inst =
        Corant.instance().select(MongoDatabase.class, NamedLiteral.of(namespace));
    return inst.isResolvable() ? inst.get() : null;
  }

  /**
   *
   * @return the databaseConfigs
   */
  public Map<String, MongodbConfig> getDatabaseConfigs() {
    return Collections.unmodifiableMap(databaseConfigs);
  }

  public Instant getDatabaseLocalTime(MongoDatabase db) {
    return getMapInstant(
        db.runCommand(new Document(asMap("serverStatus", 1, "repl", 0, "metrics", 0, "locks", 0))),
        "localTime");
  }

  /**
   *
   * @return the databaseNames
   */
  public Set<String> getDatabaseNames() {
    return Collections.unmodifiableSet(databaseNames);
  }

  /**
   *
   * @return the databases
   */
  public Map<String, MongoDatabase> getDatabases() {
    Map<String, MongoDatabase> map = new HashMap<>();
    for (String dbName : databaseNames) {
      MongoDatabase mc = getDatabase(dbName);
      if (mc != null) {
        map.put(dbName, mc);
      }
    }
    return Collections.unmodifiableMap(map);
  }

  public GridFSBucket getGridFSBucket(String namespace) {
    if (Corant.me() == null) {
      return null;
    }
    Instance<GridFSBucket> inst =
        Corant.instance().select(GridFSBucket.class, NamedLiteral.of(namespace));
    return inst.isResolvable() ? inst.get() : null;
  }

  /**
   *
   * @return the gridFSBucketNames
   */
  public Set<String> getGridFSBucketNames() {
    return Collections.unmodifiableSet(gridFSBucketNames);
  }

  /**
   *
   * @return the gridFSBuckets
   */
  public Map<String, GridFSBucket> getGridFSBuckets() {
    Map<String, GridFSBucket> map = new HashMap<>();
    for (String dbName : gridFSBucketNames) {
      GridFSBucket mc = getGridFSBucket(dbName);
      if (mc != null) {
        map.put(dbName, mc);
      }
    }
    return Collections.unmodifiableMap(map);
  }

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      for (final String cn : getClientNames()) {
        final Annotation[] qualifiers = Cdis.resolveNameds(cn);
        event.<MongoClient>addBean().addQualifiers(qualifiers)
            .addQualifier(Default.Literal.INSTANCE).addTransitiveTypeClosure(MongoClient.class)
            .beanClass(MongoClient.class).scope(ApplicationScoped.class).produceWith(beans -> {
              return produceClient(beans, getClientConfigs().get(cn));
            }).disposeWith((mc, beans) -> mc.close());
        if (isNotBlank(cn)) {
          synchronized (this) {
            try {
              if (jndi == null) {
                jndi = new InitialContext();
              }
              if (!initedJndiSubCtx) {
                jndi.createSubcontext(MongoClientConfig.JNDI_SUBCTX_NAME);
                initedJndiSubCtx = true;
              }
              String jndiName = MongoClientConfig.JNDI_SUBCTX_NAME + "/" + cn;
              jndi.bind(jndiName, new NamingReference(MongoClient.class, qualifiers));
              logger.info(() -> String.format("Bind mongo client %s to jndi.", jndiName));
            } catch (NamingException e) {
              throw new CorantRuntimeException(e);
            }
          }
        }
      }

      for (final String dn : getDatabaseNames()) {
        event.<MongoDatabase>addBean().addQualifier(NamedLiteral.of(dn))
            .addTransitiveTypeClosure(MongoDatabase.class).beanClass(MongoDatabase.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              return produceDatabase(beans, getDatabaseConfigs().get(dn));
            });
      }

      for (final String gfn : getGridFSBucketNames()) {
        event.<GridFSBucket>addBean().addQualifier(NamedLiteral.of(gfn))
            .addTransitiveTypeClosure(GridFSBucket.class).beanClass(GridFSBucket.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              return produceGridFSBucket(beans, gfn);
            });
      }
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    gridFSBucketNames.clear();
    databaseNames.clear();
    databaseConfigs.clear();
    clientNames.clear();
    clientConfigs.clear();
    clientConfigs.putAll(MongoClientConfig.from(ConfigProvider.getConfig()));
    clientNames.addAll(clientConfigs.keySet());
    clientConfigs.values().stream().flatMap(mc -> mc.getDatabases().values().stream())
        .forEach(db -> shouldBeNull(databaseConfigs.put(db.getNameSpace(), db),
            "The mongodb data base named %s dup!", db.getNameSpace()));
    databaseNames.addAll(databaseConfigs.keySet());
    if (databaseNames.isEmpty()) {
      logger.info(() -> "Can not find any mongodb databases!");
    } else {
      logger.info(() -> String.format("Find mongodb databases named [%s].",
          String.join(",", databaseNames)));
    }
  }

  protected MongoClient produceClient(Instance<Object> beans, MongoClientConfig cfg) {
    List<ServerAddress> seeds = cfg.getHostAndPorts().stream()
        .map(hnp -> new ServerAddress(hnp.getLeft(), hnp.getRight())).collect(Collectors.toList());
    MongoCredential credential = cfg.produceCredential();
    Builder builder = cfg.produceBuiler();
    if (!beans.select(MongoClientConfigurator.class).isUnsatisfied()) {
      beans.select(MongoClientConfigurator.class).forEach(h -> h.configure(builder));
    }
    MongoClientOptions clientOptions = builder.build();
    MongoClient mc = new MongoClient(seeds, credential, clientOptions);
    return mc;
  }

  protected MongoDatabase produceDatabase(Instance<Object> beans, MongodbConfig cfg) {
    MongoClient mc = beans.select(MongoClient.class, NamedLiteral.of(cfg.getClientName())).get();
    return mc.getDatabase(cfg.getName());
  }

  protected GridFSBucket produceGridFSBucket(Instance<Object> beans, String bucketNamespace) {
    String[] names = split(bucketNamespace, Names.NAME_SPACE_SEPARATORS, true, true);
    shouldBeTrue(!isEmpty(names) && names.length > 1);
    if (names.length == 2) {
      MongoDatabase md = beans.select(MongoDatabase.class, NamedLiteral.of(names[0])).get();
      return GridFSBuckets.create(md, names[1]);
    } else {
      MongoDatabase md = beans.select(MongoDatabase.class,
          NamedLiteral.of(names[0] + Names.NAME_SPACE_SEPARATORS + names[1])).get();
      return GridFSBuckets.create(md, names[2]);
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
}

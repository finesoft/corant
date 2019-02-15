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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.corant.kernel.util.ConfigUtils;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names.JndiNames;
import org.corant.suites.mongodb.MongoClientConfig.MongodbConfig;
import org.eclipse.microprofile.config.ConfigProvider;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

/**
 * corant-suites-mongodb
 *
 * @author bingo 下午4:55:16
 *
 */
public class MongoClientExtension implements Extension {

  protected final Set<String> databaseNames = new LinkedHashSet<>();
  protected final Set<String> clientNames = new LinkedHashSet<>();
  protected final Map<String, MongoClient> clients = new ConcurrentHashMap<>();
  protected final Map<String, MongoDatabase> databases = new ConcurrentHashMap<>();
  protected final Map<String, MongoClientConfig> clientConfigs = new HashMap<>();
  protected final Map<String, MongodbConfig> databaseConfigs = new HashMap<>();
  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  public MongoClient getClient(String clientName) {
    return clients.get(clientName);
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
    return Collections.unmodifiableMap(clients);
  }

  /**
   *
   * @return the databaseConfigs
   */
  public Map<String, MongodbConfig> getDatabaseConfigs() {
    return Collections.unmodifiableMap(databaseConfigs);
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
    return Collections.unmodifiableMap(databases);
  }

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      for (final String cn : getClientNames()) {
        event.<MongoClient>addBean().addQualifier(NamedLiteral.of(cn))
            .addTransitiveTypeClosure(MongoClient.class).beanClass(MongoClient.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              return produceClient(beans, getClientConfigs().get(cn));
            }).disposeWith((mc, beans) -> mc.close());
      }

      for (final String dn : getDatabaseNames()) {
        event.<MongoDatabase>addBean().addQualifier(NamedLiteral.of(dn))
            .addTransitiveTypeClosure(MongoDatabase.class).beanClass(MongoDatabase.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              return produceDatabase(beans, getDatabaseConfigs().get(dn));
            });
      }
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    databases.clear();
    databaseNames.clear();
    databaseConfigs.clear();
    clients.clear();
    clientNames.clear();
    clientConfigs.clear();
    clientConfigs.putAll(MongoClientConfig.from(ConfigProvider.getConfig()));
    clientNames.addAll(clientConfigs.keySet());
    clientConfigs.forEach((cn, dbs) -> {
      dbs.getDatabases().forEach((dn, db) -> {
        databaseConfigs.put(String.join(ConfigUtils.SEPARATOR, cn, dn), db);
      });
    });
    databaseNames.addAll(databaseConfigs.keySet());
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
    InitialContext jndi =
        beans.select(InitialContext.class).isResolvable() ? beans.select(InitialContext.class).get()
            : null;
    if (jndi != null) {
      try {
        jndi.bind(JndiNames.JNDI_COMP_NME + "/mongodb/" + cfg.getClient(), mc);
      } catch (NamingException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return mc;
  }

  protected MongoDatabase produceDatabase(Instance<Object> beans, MongodbConfig cfg) {
    MongoClient mc = beans.select(MongoClient.class, NamedLiteral.of(cfg.getClientName())).get();
    return mc.getDatabase(cfg.getDatabase());
  }
}

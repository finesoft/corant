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
import org.eclipse.microprofile.config.ConfigProvider;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * corant-suites-mongodb
 *
 * @author bingo 下午4:55:16
 *
 */
public class MongoClientExtension implements Extension {

  protected final Set<String> databaseNames = new LinkedHashSet<>();
  protected final Map<String, MongoClient> clients = new ConcurrentHashMap<>();
  protected final Map<String, MongoClientConfig> clientConfigs = new HashMap<>();
  protected final Logger logger = Logger.getLogger(this.getClass().getName());

  public MongoClient getClient(String databaseName) {
    return clients.get(databaseName);
  }

  /**
   * @return the clientConfigs
   */
  public Map<String, MongoClientConfig> getClientConfigs() {
    return Collections.unmodifiableMap(clientConfigs);
  }

  /**
   * @return the clients
   */
  public Map<String, MongoClient> getClients() {
    return Collections.unmodifiableMap(clients);
  }

  /**
   * @return the mongoAppNames
   */
  public Set<String> getDatabaseNames() {
    return Collections.unmodifiableSet(databaseNames);
  }

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      for (final String dbn : getDatabaseNames()) {
        event.<MongoClient>addBean().addQualifier(NamedLiteral.of(dbn))
            .addTransitiveTypeClosure(MongoClient.class).beanClass(MongoClient.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              return produce(beans, getClientConfigs().get(dbn));
            }).disposeWith((mc, beans) -> mc.close());
      }
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    databaseNames.clear();
    clients.clear();
    clientConfigs.clear();
    clientConfigs.putAll(MongoClientConfig.from(ConfigProvider.getConfig()));
    databaseNames.addAll(clientConfigs.keySet());
  }

  protected MongoClient produce(Instance<Object> beans, MongoClientConfig cfg) {
    List<ServerAddress> seeds = cfg.getHostAndPorts().stream()
        .map(hnp -> new ServerAddress(hnp.getLeft(), hnp.getRight())).collect(Collectors.toList());
    MongoCredential credential = cfg.produceCredential();
    Builder builder = cfg.produceBuiler();
    if (!beans.select(MongoClientConfigurator.class).isUnsatisfied()) {
      beans.select(MongoClientConfigurator.class).forEach(h -> h.configure(builder));
    }
    MongoClientOptions clientOptions = builder.build();
    return new MongoClient(seeds, credential, clientOptions);
  }
}

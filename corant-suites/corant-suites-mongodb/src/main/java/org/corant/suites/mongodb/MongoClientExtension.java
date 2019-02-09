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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import org.eclipse.microprofile.config.ConfigProvider;
import com.mongodb.MongoClient;

/**
 * corant-suites-mongodb
 *
 * @author bingo 下午4:55:16
 *
 */
public class MongoClientExtension implements Extension {

  protected final Set<String> mongoAppNames = new LinkedHashSet<>();
  protected final Map<String, MongoClient> clients = new ConcurrentHashMap<>();
  protected final Map<String, MongoClientConfig> clientConfigs = new HashMap<>();
  protected final Logger logger = Logger.getLogger(this.getClass().getName());

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
  public Set<String> getMongoAppNames() {
    return Collections.unmodifiableSet(mongoAppNames);
  }

  protected void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      for (final String appName : getMongoAppNames()) {
        event.<MongoClient>addBean().addQualifier(NamedLiteral.of(appName))
            .addTransitiveTypeClosure(MongoClient.class).beanClass(MongoClient.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              return null;
            }).disposeWith((mc, beans) -> mc.close());
      }
    }
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    mongoAppNames.clear();
    clients.clear();
    clientConfigs.clear();
    clientConfigs.putAll(MongoClientConfig.from(ConfigProvider.getConfig()));
    mongoAppNames.addAll(clientConfigs.keySet());
  }

  protected MongoClient produce(MongoClientConfig cfg) {
    MongoClient mc = new MongoClient();
    return mc;
  }
}

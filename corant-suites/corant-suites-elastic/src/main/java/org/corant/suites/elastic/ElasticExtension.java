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
package org.corant.suites.elastic;

import static org.corant.shared.util.ObjectUtils.shouldBeTrue;
import static org.corant.shared.util.StringUtils.split;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.ConfigProvider;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 * corant-suites-elastic
 *
 * @author bingo 上午11:56:19
 *
 */
public class ElasticExtension implements Extension {

  protected final Logger logger = Logger.getLogger(this.getClass().getName());
  protected final Map<String, ElasticConfig> configs = new LinkedHashMap<>();

  /**
   *
   * @return the configs
   */
  public Map<String, ElasticConfig> getConfigs() {
    return Collections.unmodifiableMap(configs);
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    configs.clear();
    ElasticConfig.from(ConfigProvider.getConfig()).forEach(configs::put);
    if (configs.isEmpty()) {
      logger.info(() -> "Can not find any elastic cluster configurations.");
    } else {
      logger.config(() -> String.format("Find elastic cluster names %s",
          String.join(", ", configs.keySet())));
    }
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      for (final String clusterName : configs.keySet()) {
        event.<TransportClient>addBean().addQualifier(NamedLiteral.of(clusterName))
            .addTransitiveTypeClosure(TransportClient.class).beanClass(TransportClient.class)
            .scope(ApplicationScoped.class).produceWith(beans -> {
              try {
                return produce(beans, clusterName);
              } catch (UnknownHostException e) {
                throw new CorantRuntimeException(e);
              }
            }).disposeWith((tc, beans) -> tc.close());
      }
    }
  }

  TransportClient produce(Instance<Object> beans, String clusterName) throws UnknownHostException {
    ElasticConfig cfg = configs.get(clusterName);
    Builder builder = Settings.builder();
    cfg.getProperties().forEach(builder::put);
    builder.put("cluster.name", cfg.getClusterName());
    TransportClient tc = new PreBuiltTransportClient(builder.build());
    for (String clusterNode : split(cfg.getClusterNodes(), ",")) {
      String[] hostPort = split(clusterNode, ":");
      shouldBeTrue(hostPort != null && hostPort.length == 2, "Cluster %s node property error",
          clusterName);
      tc.addTransportAddress(
          new TransportAddress(InetAddress.getByName(hostPort[0]), Integer.valueOf(hostPort[1])));
    }
    logger.info(() -> String.format("Built elastic transport client with cluster name is %s.",
        clusterName));
    return tc;
  }
}

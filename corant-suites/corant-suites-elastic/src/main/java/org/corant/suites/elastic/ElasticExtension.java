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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.StringUtils.split;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import org.corant.kernel.event.PreContainerStopEvent;
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
  protected final Map<String, TransportClient> clients = new ConcurrentHashMap<>();

  public ElasticConfig getConfig(String clusterName) {
    return configs.get(clusterName);
  }

  public Map<String, ElasticConfig> getConfigs() {
    return Collections.unmodifiableMap(configs);
  }

  public TransportClient getTransportClient(String clusterName) {
    return clients.computeIfAbsent(clusterName, this::produce);
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    configs.clear();
    ElasticConfig.from(ConfigProvider.getConfig()).forEach(configs::put);
    if (configs.isEmpty()) {
      logger.info(() -> "Can not find any elastic cluster configurations.");
    } else {
      logger.info(() -> String.format("Find elastic cluster names %s",
          String.join(", ", configs.keySet())));
    }
  }

  protected void onPreContainerStopEvent(@Observes PreContainerStopEvent e) {
    clients.values().forEach(TransportClient::close);
  }

  @SuppressWarnings("resource")
  TransportClient produce(String clusterName) {
    ElasticConfig cfg = shouldNotNull(configs.get(clusterName));
    Builder builder = Settings.builder();
    cfg.getProperties().forEach(builder::put);
    builder.put("cluster.name", cfg.getClusterName());
    TransportClient tc = new PreBuiltTransportClient(builder.build());
    for (String clusterNode : split(cfg.getClusterNodes(), ",", true, true)) {
      final String[] hostPort = split(clusterNode, ":", true, true);
      shouldBeTrue(hostPort.length == 2, "Cluster %s node property error", clusterName);
      try {
        tc.addTransportAddress(new TransportAddress(InetAddress.getByName(hostPort[0]),
            Integer.parseInt(hostPort[1])));
        tc.connectedNodes();
      } catch (NumberFormatException | UnknownHostException e) {
        throw new CorantRuntimeException(e, "Can not build transport client from %s:%s",
            (Object[]) hostPort);
      }
    }
    logger.info(() -> String.format("Built elastic transport client with cluster name is %s.",
        clusterName));
    return tc;
  }
}

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

import static org.corant.kernel.util.Instances.resolveNamed;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.split;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Singleton;
import org.corant.config.declarative.DeclarativeConfigResolver;
import org.corant.kernel.event.PreContainerStopEvent;
import org.corant.kernel.util.Qualifiers.DefaultNamedQualifierObjectManager;
import org.corant.kernel.util.Qualifiers.NamedQualifierObjectManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.elastic.metadata.resolver.AbstractElasticIndexingResolver;
import org.corant.suites.elastic.metadata.resolver.ElasticIndexingResolver;
import org.corant.suites.elastic.service.AbstractElasticDocumentService;
import org.corant.suites.elastic.service.AbstractElasticIndicesService;
import org.corant.suites.elastic.service.ElasticDocumentService;
import org.corant.suites.elastic.service.ElasticIndicesService;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 * corant-suites-elastic
 *
 * TODO Support unnamed elastic config
 *
 * @author bingo 上午11:56:19
 *
 */
public class ElasticExtension implements Extension, Function<String, TransportClient> {

  static {
    System.setProperty("es.set.netty.runtime.available.processors", "false");
  }

  protected final Logger logger = Logger.getLogger(this.getClass().getName());
  protected final Map<String, PreBuiltTransportClient> clients = new ConcurrentHashMap<>();
  private volatile NamedQualifierObjectManager<ElasticConfig> configManager =
      NamedQualifierObjectManager.empty();

  @Override
  public TransportClient apply(String t) {
    return getTransportClient(t);
  }

  public NamedQualifierObjectManager<ElasticConfig> getConfigManager() {
    return configManager;
  }

  public PreBuiltTransportClient getTransportClient(String clusterName) {
    if (isBlank(clusterName) && clients.size() == 1) {
      return clients.values().iterator().next();
    }
    return clients.computeIfAbsent(clusterName, this::produce);
  }

  protected void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
    Map<String, ElasticConfig> configs =
        DeclarativeConfigResolver.resolveMulti(ElasticConfig.class);
    configManager = new DefaultNamedQualifierObjectManager<>(configs.values());
    if (configManager.isEmpty()) {
      logger.info(() -> "Can not find any elastic cluster configurations.");
    } else {
      logger.info(() -> String.format("Find %s elastic clusters named [%s]", configManager.size(),
          String.join(", ", configManager.getAllDisplayNames())));
    }
  }

  protected void onPreContainerStopEvent(@Observes PreContainerStopEvent e) {
    clients.values().forEach(TransportClient::close);
  }

  void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery event) {
    if (event != null) {
      getConfigManager().getAllWithQualifiers().forEach((c, q) -> {
        event.<PreBuiltTransportClient>addBean().addQualifiers(q)
            .addTransitiveTypeClosure(TransportClient.class)
            .beanClass(PreBuiltTransportClient.class).scope(ApplicationScoped.class)
            .produceWith(beans -> {
              return getTransportClient(c.getClusterName());
            }).disposeWith((tc, beans) -> tc.close());// FIXME proxy error on TransportClient
        event.<ElasticDocumentService>addBean().addQualifiers(q)
            .addTransitiveTypeClosure(ElasticDocumentService.class)
            .beanClass(ElasticDocumentService.class).scope(Singleton.class).produceWith(beans -> {
              return new DefaultElasticDocumentService(beans, c);
            }).disposeWith((tc, beans) -> {
            });
        event.<ElasticIndicesService>addBean().addQualifiers(q)
            .addTransitiveTypeClosure(ElasticIndicesService.class)
            .beanClass(ElasticIndicesService.class).scope(Singleton.class).produceWith(beans -> {
              return new DefaultElasticIndicesService(beans, c);
            }).disposeWith((tc, beans) -> {
            });
        event.<ElasticIndexingResolver>addBean().addQualifiers(q)
            .addTransitiveTypeClosure(ElasticIndexingResolver.class)
            .beanClass(ElasticIndexingResolver.class).scope(Singleton.class).produceWith(beans -> {
              return new DefaultElasticIndexingResolver(c);
            }).disposeWith((tc, beans) -> {
            });
      });
    }
  }

  @SuppressWarnings("resource")
  PreBuiltTransportClient produce(String clusterName) {
    ElasticConfig cfg = shouldNotNull(configManager.get(clusterName));
    Builder builder = Settings.builder();
    cfg.getProperties().forEach(builder::put);
    builder.put("cluster.name", cfg.getClusterName());
    PreBuiltTransportClient tc = new PreBuiltTransportClient(builder.build());
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

  /**
   * corant-suites-elastic
   *
   * @author bingo 下午4:56:09
   *
   */
  public static class DefaultElasticDocumentService extends AbstractElasticDocumentService {

    final TransportClient transportClient;

    /**
     * @param instance
     * @param config
     */
    public DefaultElasticDocumentService(Instance<Object> instance, ElasticConfig ec) {
      transportClient =
          instance.select(ElasticExtension.class).get().getTransportClient(ec.getClusterName());
      indexingResolver = resolveNamed(ElasticIndexingResolver.class, ec.getClusterName()).get();
    }

    @Override
    public TransportClient getTransportClient() {
      return transportClient;
    }
  }

  /**
   * corant-suites-elastic
   *
   * @author bingo 下午4:58:18
   *
   */
  public static class DefaultElasticIndexingResolver extends AbstractElasticIndexingResolver {
    final ElasticConfig config;
    final ElasticIndicesService indicesService;

    /**
     * @param ec
     */
    public DefaultElasticIndexingResolver(ElasticConfig ec) {
      config = ec;
      indicesService = resolveNamed(ElasticIndicesService.class, ec.getClusterName()).get();
      logger = Logger.getLogger(DefaultElasticIndexingResolver.class.getName());
      onPostConstruct();
    }

    @Override
    protected void createIndex() {
      namedIndices.values().forEach(indicesService::create);
    }

    @Override
    protected ElasticConfig getConfig() {
      return config;
    }
  }

  /**
   * corant-suites-elastic
   *
   * @author bingo 下午4:59:59
   *
   */
  public static class DefaultElasticIndicesService extends AbstractElasticIndicesService {
    final TransportClient transportClient;

    /**
     * @param instance
     * @param ec
     */
    public DefaultElasticIndicesService(Instance<Object> instance, ElasticConfig ec) {
      transportClient =
          instance.select(ElasticExtension.class).get().getTransportClient(ec.getClusterName());
    }

    @Override
    public TransportClient getTransportClient() {
      return transportClient;
    }
  }
}

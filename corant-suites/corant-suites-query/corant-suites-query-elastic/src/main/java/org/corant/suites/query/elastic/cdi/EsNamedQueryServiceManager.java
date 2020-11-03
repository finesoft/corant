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
package org.corant.suites.query.elastic.cdi;

import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.isBlank;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.config.Configs;
import org.corant.suites.query.elastic.AbstractEsNamedQueryService;
import org.corant.suites.query.elastic.DefaultEsQueryExecutor;
import org.corant.suites.query.elastic.EsNamedQuerier;
import org.corant.suites.query.elastic.EsNamedQueryService;
import org.corant.suites.query.elastic.EsQueryExecutor;
import org.corant.suites.query.shared.AbstractNamedQuerierResolver;
import org.corant.suites.query.shared.NamedQueryServiceManager;
import org.corant.suites.query.shared.mapping.Query.QueryType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.elasticsearch.client.transport.TransportClient;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
// @Priority(1)
@ApplicationScoped
// @Alternative
public class EsNamedQueryServiceManager implements NamedQueryServiceManager {

  protected final Map<String, EsNamedQueryService> services = new ConcurrentHashMap<>();// FIXME
                                                                                        // scope

  @Inject
  protected Logger logger;

  @Inject
  protected AbstractNamedQuerierResolver<EsNamedQuerier> resolver;

  @Inject
  protected Function<String, TransportClient> transportClientManager;

  @Inject
  @ConfigProperty(name = "query.elastic.max-select-size", defaultValue = "128")
  protected Integer maxSelectSize;

  @Inject
  @ConfigProperty(name = "query.elastic.limit", defaultValue = "16")
  protected Integer limit;

  @Inject
  @ConfigProperty(name = "query.elastic.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Override
  public EsNamedQueryService get(Object qualifier) {
    String key = resolveQualifier(qualifier);
    return services.computeIfAbsent(key, k -> {
      final String clusterName = isBlank(k) ? defaultQualifierValue.orElse(EMPTY) : k;
      logger.fine(() -> String.format(
          "Create default elastic named query service, the data center is [%s]. ", clusterName));
      return new DefaultEsNamedQueryService(transportClientManager.apply(clusterName), this);
    });
  }

  @Override
  public QueryType getType() {
    return QueryType.ES;
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    services.clear();
    logger.fine(() -> "Clear cached named query services.");
  }

  @Produces
  @EsQuery
  protected EsNamedQueryService produce(InjectionPoint ip) {
    Annotation qualifier = null;
    for (Annotation a : ip.getQualifiers()) {
      if (a.annotationType().equals(EsQuery.class)) {
        qualifier = a;
        break;
      }
    }
    return get(qualifier);
  }

  protected String resolveQualifier(Object qualifier) {
    return Configs.assemblyStringConfigProperty(
        qualifier instanceof EsQuery ? ((EsQuery) qualifier).value() : asDefaultString(qualifier));
  }

  /**
   * corant-suites-query-elastic
   *
   * @author bingo 上午10:50:50
   *
   */
  public static class DefaultEsNamedQueryService extends AbstractEsNamedQueryService {

    protected final EsQueryExecutor executor;
    protected final int defaultMaxSelectSize;
    protected final int defaultLimit;
    protected final AbstractNamedQuerierResolver<EsNamedQuerier> resolver;

    /**
     * @param transportClient
     * @param manager
     */
    public DefaultEsNamedQueryService(TransportClient transportClient,
        EsNamedQueryServiceManager manager) {
      executor = new DefaultEsQueryExecutor(transportClient);
      resolver = manager.resolver;
      defaultMaxSelectSize = manager.maxSelectSize;
      defaultLimit = manager.limit < 1 ? DEFAULT_LIMIT : manager.limit;
    }

    /**
     * @param executor
     * @param defaultMaxSelectSize
     * @param defaultLimit
     * @param resolver
     */
    protected DefaultEsNamedQueryService(EsQueryExecutor executor, int defaultMaxSelectSize,
        int defaultLimit, AbstractNamedQuerierResolver<EsNamedQuerier> resolver) {
      super();
      this.executor = executor;
      this.defaultMaxSelectSize = defaultMaxSelectSize;
      this.defaultLimit = defaultLimit;
      this.resolver = resolver;
    }

    @Override
    protected int getDefaultLimit() {
      return defaultLimit;
    }

    @Override
    protected int getDefaultMaxSelectSize() {
      return defaultMaxSelectSize;
    }

    @Override
    protected EsQueryExecutor getExecutor() {
      return executor;
    }

    @Override
    protected AbstractNamedQuerierResolver<EsNamedQuerier> getQuerierResolver() {
      return resolver;
    }

  }
}

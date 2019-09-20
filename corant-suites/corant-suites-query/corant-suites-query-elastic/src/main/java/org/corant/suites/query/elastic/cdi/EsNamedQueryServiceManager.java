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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.suites.query.elastic.AbstractEsNamedQueryService;
import org.corant.suites.query.elastic.DefaultEsQueryExecutor;
import org.corant.suites.query.elastic.EsInLineNamedQueryResolver;
import org.corant.suites.query.elastic.EsNamedQueryService;
import org.corant.suites.query.elastic.EsQueryExecutor;
import org.corant.suites.query.shared.Querier;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.elasticsearch.client.transport.TransportClient;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
@Priority(1)
@ApplicationScoped
@Alternative
public class EsNamedQueryServiceManager {

  static final Map<String, EsNamedQueryService> services = new ConcurrentHashMap<>();// FIXME scope

  @Inject
  protected Logger logger;

  @Inject
  protected EsInLineNamedQueryResolver<String, Object> resolver;

  @Inject
  protected TransportClientManager transportClientManager;

  @Inject
  @ConfigProperty(name = "query.elastic.max-select-size", defaultValue = "128")
  protected Integer maxSelectSize;

  @Inject
  @ConfigProperty(name = "query.elastic.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Produces
  @EsQuery
  EsNamedQueryService produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final EsQuery sc = shouldNotNull(annotated.getAnnotation(EsQuery.class));
    String dataCenterName = defaultString(sc.value());
    if (isBlank(dataCenterName) && defaultQualifierValue.isPresent()) {
      dataCenterName = defaultQualifierValue.get();
    }
    final String dataCenter = dataCenterName;
    return services.computeIfAbsent(dataCenter, (dc) -> {
      logger.info(() -> String
          .format("Create default elastic named query service, the data center is [%s]. ", dc));
      return new DefaultEsNamedQueryService(transportClientManager.get(dc), resolver,
          maxSelectSize);
    });
  }

  /**
   * corant-suites-query-elastic
   *
   * @author bingo 上午10:50:50
   *
   */
  public static final class DefaultEsNamedQueryService extends AbstractEsNamedQueryService {

    private final EsQueryExecutor executor;
    final int defaultMaxSelectSize;

    /**
     * @param transportClient
     * @param resolver
     * @param defaultMaxSelectSize
     */
    public DefaultEsNamedQueryService(TransportClient transportClient,
        EsInLineNamedQueryResolver<String, Object> resolver, int defaultMaxSelectSize) {
      executor = new DefaultEsQueryExecutor(transportClient);
      logger = Logger.getLogger(this.getClass().getName());
      this.resolver = resolver;
      this.defaultMaxSelectSize = defaultMaxSelectSize;
    }

    @Override
    protected EsQueryExecutor getExecutor() {
      return executor;
    }

    @Override
    protected int resolveMaxSelectSize(Querier querier) {
      return querier.getQuery().getProperty(PRO_KEY_MAX_SELECT_SIZE, Integer.class,
          defaultMaxSelectSize);
    }
  }
}

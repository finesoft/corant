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
package org.corant.suites.query.cassandra.cdi;

import static org.corant.shared.util.StringUtils.EMPTY;
import static org.corant.shared.util.StringUtils.asDefaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.suites.cdi.Instances.findNamed;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.cassandra.AbstractCasNamedQueryService;
import org.corant.suites.query.cassandra.CasNamedQuerier;
import org.corant.suites.query.cassandra.CasQueryExecutor;
import org.corant.suites.query.cassandra.DefaultCasQueryExecutor;
import org.corant.suites.query.shared.AbstractNamedQuerierResolver;
import org.corant.suites.query.shared.NamedQueryService;
import org.corant.suites.query.shared.NamedQueryServiceManager;
import org.corant.suites.query.shared.mapping.Query.QueryType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.datastax.driver.core.Cluster;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
// @Priority(1)
@ApplicationScoped
// @Alternative
public class CasNamedQueryServiceManager implements NamedQueryServiceManager {

  protected final Map<String, NamedQueryService> services = new ConcurrentHashMap<>();

  @Inject
  protected Logger logger;

  @Inject
  protected AbstractNamedQuerierResolver<CasNamedQuerier> resolver;

  @Inject
  @ConfigProperty(name = "query.cassandra.max-select-size", defaultValue = "128")
  protected Integer maxSelectSize;

  @Inject
  @ConfigProperty(name = "query.cassandra.limit", defaultValue = "16")
  protected Integer limit;

  @Inject
  @ConfigProperty(name = "query.cassandra.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Override
  public NamedQueryService get(Object qualifier) {
    String key = resolveQualifier(qualifier);
    return services.computeIfAbsent(key, k -> {
      final String clusterName = isBlank(k) ? defaultQualifierValue.orElse(EMPTY) : k;
      logger.fine(() -> String.format(
          "Create default cassandra named query service, the cluster is [%s].", clusterName));
      return new DefaultCasNamedQueryService(clusterName, this);
    });
  }

  @Override
  public QueryType getType() {
    return QueryType.MG;
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    services.clear();
    logger.fine(() -> "Clear cached named query services.");
  }

  @Produces
  @CasQuery
  protected NamedQueryService produce(InjectionPoint ip) {
    Annotation qualifier = null;
    for (Annotation a : ip.getQualifiers()) {
      if (a.annotationType().equals(CasQuery.class)) {
        qualifier = a;
        break;
      }
    }
    return get(qualifier);
  }

  protected String resolveQualifier(Object qualifier) {
    return qualifier instanceof CasQuery ? ((CasQuery) qualifier).value()
        : asDefaultString(qualifier);
  }

  /**
   * corant-suites-query-cassandra
   *
   * @author bingo 下午3:41:34
   *
   */
  public static class DefaultCasNamedQueryService extends AbstractCasNamedQueryService {

    protected final int defaultMaxSelectSize;
    protected final int defaultLimit;
    protected final AbstractNamedQuerierResolver<CasNamedQuerier> resolver;
    protected final CasQueryExecutor executor;

    /**
     * @param clusterName
     * @param manager
     */
    public DefaultCasNamedQueryService(String clusterName, CasNamedQueryServiceManager manager) {
      resolver = manager.resolver;
      defaultMaxSelectSize = manager.maxSelectSize;
      defaultLimit = manager.limit < 1 ? DEFAULT_LIMIT : manager.limit;
      executor = new DefaultCasQueryExecutor(
          findNamed(Cluster.class, clusterName).orElseThrow(() -> new CorantRuntimeException(
              "Can't build default cassandra named query, the cluster named %s not found.",
              clusterName)),
          defaultMaxSelectSize);
    }

    /**
     * @param cluster
     * @param defaultMaxSelectSize
     * @param defaultLimit
     * @param resolver
     */
    protected DefaultCasNamedQueryService(Cluster cluster, int defaultMaxSelectSize,
        int defaultLimit, AbstractNamedQuerierResolver<CasNamedQuerier> resolver) {
      super();
      this.defaultMaxSelectSize = defaultMaxSelectSize;
      this.defaultLimit = defaultLimit;
      this.resolver = resolver;
      executor = new DefaultCasQueryExecutor(cluster, defaultMaxSelectSize);
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
    protected CasQueryExecutor getExecutor() {
      return executor;
    }

    @Override
    protected AbstractNamedQuerierResolver<CasNamedQuerier> getQuerierResolver() {
      return resolver;
    }

  }
}

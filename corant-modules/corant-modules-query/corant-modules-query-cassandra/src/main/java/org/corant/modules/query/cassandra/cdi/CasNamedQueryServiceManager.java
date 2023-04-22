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
package org.corant.modules.query.cassandra.cdi;

import static org.corant.context.Beans.findNamed;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.isBlank;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.corant.config.Configs;
import org.corant.context.qualifier.Qualifiers;
import org.corant.modules.query.NamedQueryService;
import org.corant.modules.query.cassandra.AbstractCasNamedQueryService;
import org.corant.modules.query.cassandra.CasNamedQuerier;
import org.corant.modules.query.cassandra.CasQueryExecutor;
import org.corant.modules.query.cassandra.DefaultCasQueryExecutor;
import org.corant.modules.query.mapping.Query.QueryType;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.NamedQueryServiceManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import com.datastax.driver.core.Cluster;

/**
 * corant-modules-query-cassandra
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
  @ConfigProperty(name = "corant.query.cassandra.fetch-size", defaultValue = "16")
  protected Integer fetchSize;

  @Inject
  @ConfigProperty(name = "corant.query.cassandra.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Override
  public NamedQueryService get(Object qualifier) {
    String key = resolveQualifier(qualifier);
    return services.computeIfAbsent(key, k -> {
      final String clusterName =
          isBlank(k) ? defaultQualifierValue.orElse(Qualifiers.EMPTY_NAME) : k;
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
    return Configs
        .assemblyStringConfigProperty(qualifier instanceof CasQuery ? ((CasQuery) qualifier).value()
            : asDefaultString(qualifier));
  }

  /**
   * corant-modules-query-cassandra
   *
   * @author bingo 下午3:41:34
   *
   */
  public static class DefaultCasNamedQueryService extends AbstractCasNamedQueryService {

    protected final AbstractNamedQuerierResolver<CasNamedQuerier> resolver;
    protected final CasQueryExecutor executor;

    /**
     * @param clusterName
     * @param manager
     */
    public DefaultCasNamedQueryService(String clusterName, CasNamedQueryServiceManager manager) {
      resolver = manager.resolver;
      executor = new DefaultCasQueryExecutor(
          findNamed(Cluster.class, clusterName).orElseThrow(() -> new CorantRuntimeException(
              "Can't build default cassandra named query, the cluster named %s not found.",
              clusterName)),
          manager.fetchSize);
    }

    /**
     * @param cluster
     * @param fetchSize
     * @param resolver
     */
    protected DefaultCasNamedQueryService(Cluster cluster, int fetchSize,
        AbstractNamedQuerierResolver<CasNamedQuerier> resolver) {
      this.resolver = resolver;
      executor = new DefaultCasQueryExecutor(cluster, fetchSize);
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

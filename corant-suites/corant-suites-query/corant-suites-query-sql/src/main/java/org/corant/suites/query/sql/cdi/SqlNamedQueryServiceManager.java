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
package org.corant.suites.query.sql.cdi;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ConversionUtils.toObject;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.StringUtils.EMPTY;
import static org.corant.shared.util.StringUtils.asDefaultString;
import static org.corant.shared.util.StringUtils.split;
import static org.corant.suites.cdi.Instances.findNamed;
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
import javax.sql.DataSource;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.shared.AbstractNamedQuerierResolver;
import org.corant.suites.query.shared.NamedQueryService;
import org.corant.suites.query.shared.NamedQueryServiceManager;
import org.corant.suites.query.shared.mapping.Query.QueryType;
import org.corant.suites.query.sql.AbstractSqlNamedQueryService;
import org.corant.suites.query.sql.DefaultSqlQueryExecutor;
import org.corant.suites.query.sql.SqlNamedQuerier;
import org.corant.suites.query.sql.SqlQueryConfiguration;
import org.corant.suites.query.sql.SqlQueryConfiguration.Builder;
import org.corant.suites.query.sql.SqlQueryExecutor;
import org.corant.suites.query.sql.dialect.Dialect.DBMS;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
@Priority(1)
@ApplicationScoped
@Alternative
public class SqlNamedQueryServiceManager implements NamedQueryServiceManager {

  static final Map<String, NamedQueryService> services = new ConcurrentHashMap<>();// FIXME scope

  @Inject
  protected Logger logger;

  @Inject
  protected AbstractNamedQuerierResolver<SqlNamedQuerier> resolver;

  @Inject
  @ConfigProperty(name = "query.sql.max-select-size", defaultValue = "128")
  protected Integer maxSelectSize;

  @Inject
  @ConfigProperty(name = "query.sql.limit", defaultValue = "16")
  protected Integer limit;

  @Inject
  @ConfigProperty(name = "query.sql.fetch-size", defaultValue = "16")
  protected Integer fetchSize;

  @Inject
  @ConfigProperty(name = "query.sql.fetch-direction")
  protected Optional<Integer> fetchDirection;

  @Inject
  @ConfigProperty(name = "query.sql.timeout", defaultValue = "0")
  protected Integer timeout;

  @Inject
  @ConfigProperty(name = "query.sql.max-field-size", defaultValue = "0")
  protected Integer maxFieldSize;

  @Inject
  @ConfigProperty(name = "query.sql.max-rows", defaultValue = "0")
  protected Integer maxRows;

  @Inject
  @ConfigProperty(name = "query.sql.default-qualifier-value")
  protected Optional<String> defaultQualifierValue;

  @Inject
  @ConfigProperty(name = "query.sql.default-qualifier-dialect", defaultValue = "MYSQL")
  protected DBMS defaultQualifierDialect;

  @Override
  public NamedQueryService get(Object qualifier) {
    String key = resolveQualifer(qualifier);
    return services.computeIfAbsent(key, k -> {
      String dataSourceName = defaultQualifierValue.orElse(EMPTY);
      DBMS dialect = defaultQualifierDialect;
      String[] qs = split(asDefaultString(qualifier), ":", true, true);
      if (qs.length > 0) {
        dataSourceName = qs[0];
        if (qs.length > 1) {
          dialect = toObject(qs[1], DBMS.class);
        }
      }
      logger.fine(String.format(
          "Create default sql named query service, the data source is [%s] and dialect is [%s].",
          dataSourceName, dialect));
      return new DefaultSqlNamedQueryService(dataSourceName, dialect, this);
    });
  }

  @Override
  public QueryType getType() {
    return QueryType.SQL;
  }

  @Produces
  @SqlQuery
  NamedQueryService produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final SqlQuery sc = shouldNotNull(annotated.getAnnotation(SqlQuery.class));
    return get(sc);
  }

  String resolveQualifer(Object qualifier) {
    if (qualifier instanceof SqlQuery) {
      SqlQuery q = forceCast(qualifier);
      return q.value().concat(":").concat(q.dialect());
    } else {
      return asDefaultString(qualifier);
    }
  }

  /**
   * corant-suites-query-sql
   *
   * @author bingo 下午6:54:23
   *
   */
  public static class DefaultSqlNamedQueryService extends AbstractSqlNamedQueryService {

    protected final SqlQueryExecutor executor;
    protected final int defaultMaxSelectSize;
    protected final int defaultLimit;
    protected final AbstractNamedQuerierResolver<SqlNamedQuerier> resolver;

    /**
     * @param executor
     * @param defaultMaxSelectSize
     * @param defaultLimit
     * @param resolver
     */
    protected DefaultSqlNamedQueryService(SqlQueryExecutor executor, int defaultMaxSelectSize,
        int defaultLimit, AbstractNamedQuerierResolver<SqlNamedQuerier> resolver) {
      super();
      this.executor = executor;
      this.defaultMaxSelectSize = defaultMaxSelectSize;
      this.defaultLimit = defaultLimit;
      this.resolver = resolver;
    }

    /**
     * @param dataSourceName
     * @param dbms
     * @param manager
     */
    protected DefaultSqlNamedQueryService(String dataSourceName, DBMS dbms,
        SqlNamedQueryServiceManager manager) {
      resolver = manager.resolver;
      Builder builder = SqlQueryConfiguration.defaultBuilder()
          .dataSource(findNamed(DataSource.class, dataSourceName)
              .orElseThrow(() -> new CorantRuntimeException(
                  "Can't build default sql named query, the data source named %s not found.",
                  dataSourceName)))
          .dialect(dbms.instance()).fetchSize(manager.fetchSize).maxFieldSize(manager.maxFieldSize)
          .maxRows(manager.maxRows).queryTimeout(manager.timeout);
      manager.fetchDirection.ifPresent(builder::fetchDirection);
      executor = new DefaultSqlQueryExecutor(builder.build());
      defaultMaxSelectSize = manager.maxSelectSize;
      defaultLimit = manager.limit < 1 ? DEFAULT_LIMIT : manager.limit;
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
    protected SqlQueryExecutor getExecutor() {
      return executor;
    }

    @Override
    protected AbstractNamedQuerierResolver<SqlNamedQuerier> getQuerierResolver() {
      return resolver;
    }

  }
}

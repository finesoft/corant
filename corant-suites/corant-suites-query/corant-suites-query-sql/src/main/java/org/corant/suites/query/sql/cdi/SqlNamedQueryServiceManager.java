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

import static org.corant.kernel.util.Instances.resolveNamed;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
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
import org.corant.suites.query.shared.NamedQueryService;
import org.corant.suites.query.sql.AbstractSqlNamedQueryService;
import org.corant.suites.query.sql.DefaultSqlQueryExecutor;
import org.corant.suites.query.sql.SqlNamedQueryResolver;
import org.corant.suites.query.sql.SqlQueryConfiguration;
import org.corant.suites.query.sql.SqlQueryExecutor;
import org.corant.suites.query.sql.dialect.Dialect.DBMS;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
@Priority(1)
@ApplicationScoped
@Alternative
public class SqlNamedQueryServiceManager {

  static final Map<String, NamedQueryService> services = new ConcurrentHashMap<>();

  @Inject
  protected SqlNamedQueryResolver<String, Object> resolver;

  @Produces
  @SqlQuery
  NamedQueryService produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final SqlQuery sc = shouldNotNull(annotated.getAnnotation(SqlQuery.class));
    final String dataSource = shouldNotBlank(sc.value());
    final DBMS dbms = sc.dialect();
    return services.computeIfAbsent(dataSource, (ds) -> {
      return new DefaultSqlNamedQueryService(ds, dbms, resolver);
    });
  }

  /**
   * corant-suites-query-sql
   *
   * @author bingo 下午6:54:23
   *
   */
  public static final class DefaultSqlNamedQueryService extends AbstractSqlNamedQueryService {

    private final SqlQueryExecutor executor;

    /**
     * @param dataSource
     * @param dbms
     */
    protected DefaultSqlNamedQueryService(String dataSource, DBMS dbms,
        SqlNamedQueryResolver<String, Object> resolver) {
      this.resolver = resolver;
      logger = Logger.getLogger(this.getClass().getName());
      executor = new DefaultSqlQueryExecutor(SqlQueryConfiguration.defaultBuilder()
          .dataSource(resolveNamed(DataSource.class, dataSource).get()).dialect(dbms.instance())
          .build());
    }

    @Override
    protected SqlQueryExecutor getExecutor() {
      return executor;
    }
  }
}

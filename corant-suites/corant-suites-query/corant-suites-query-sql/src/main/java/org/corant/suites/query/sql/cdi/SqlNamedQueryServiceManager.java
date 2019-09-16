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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import org.corant.kernel.api.DataSourceService;
import org.corant.suites.query.shared.NamedQueryService;
import org.corant.suites.query.sql.AbstractSqlNamedQueryService;
import org.corant.suites.query.sql.DefaultSqlQueryExecutor;
import org.corant.suites.query.sql.SqlQueryConfiguration;
import org.corant.suites.query.sql.SqlQueryExecutor;
import org.corant.suites.query.sql.dialect.Dialect.DBMS;

/**
 * corant-suites-query-sql
 *
 * @author bingo 下午6:04:28
 *
 */
@ApplicationScoped
public class SqlNamedQueryServiceManager {

  static final Map<String, NamedQueryService> services = new ConcurrentHashMap<>();

  @Inject
  DataSourceService dataSourceService;

  @Produces
  @ApplicationScoped
  @SqlQuery("")
  NamedQueryService produce(InjectionPoint ip) {
    final Annotated annotated = ip.getAnnotated();
    final SqlQuery sc = shouldNotNull(annotated.getAnnotation(SqlQuery.class));
    final String dataSource = sc.value();
    final DBMS dbms = sc.dialect();
    return services.computeIfAbsent(dataSource, (ds) -> {
      return new AbstractSqlNamedQueryService() {
        final SqlQueryExecutor executor =
            new DefaultSqlQueryExecutor(SqlQueryConfiguration.defaultBuilder()
                .dataSource(dataSourceService.get(dataSource)).dialect(dbms.instance()).build());

        @Override
        protected SqlQueryExecutor getExecutor() {
          return executor;
        }
      };
    });
  }
}

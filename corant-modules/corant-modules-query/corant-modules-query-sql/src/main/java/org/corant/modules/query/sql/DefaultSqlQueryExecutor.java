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
package org.corant.modules.query.sql;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.forceCast;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.dbutils.StatementConfiguration;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.sql.dialect.Dialect;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午5:31:55
 *
 */
public class DefaultSqlQueryExecutor implements SqlQueryExecutor {

  public static final MapHandler MAP_HANDLER = new MapHandler();
  public static final MapListHandler MAP_LIST_HANDLER = new MapListHandler();

  protected final SqlQueryConfiguration confiuration;
  protected final DefaultQueryRunner runner;
  protected final Dialect dialect;

  @SuppressWarnings("deprecation")
  public DefaultSqlQueryExecutor(SqlQueryConfiguration confiuration) {
    this.confiuration = confiuration;
    runner = new DefaultQueryRunner(confiuration.getDataSource(),
        new StatementConfiguration(confiuration.getFetchDirection(), confiuration.getFetchSize(),
            confiuration.getMaxFieldSize(), confiuration.getMaxRows(),
            confiuration.getQueryTimeout()));
    dialect = confiuration.getDialect();
  }

  public static DefaultSqlQueryExecutor of(DataSource ds) {
    return of(new SqlQueryConfiguration.Builder().dataSource(shouldNotNull(ds)));
  }

  public static DefaultSqlQueryExecutor of(SqlQueryConfiguration.Builder builder) {
    return new DefaultSqlQueryExecutor(builder.build());
  }

  public static DefaultSqlQueryExecutor of(String jndiDsName) {
    String useJndiDsName = shouldNotNull(jndiDsName);
    try {
      InitialContext jndi = new InitialContext();
      DataSource ds = shouldNotNull(forceCast(jndi.lookup(useJndiDsName)));
      return of(ds);
    } catch (NamingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  public Map<String, Object> get(String sql, Duration timeout, Object... args) throws SQLException {
    Object result;
    if (args.length > 0) {
      result = getRunner().select(sql, MAP_HANDLER, 1, timeout, args);
    } else {
      result = getRunner().select(sql, MAP_HANDLER, 1, timeout);
    }
    return forceCast(result);
  }

  @Override
  public Dialect getDialect() {
    return dialect;
  }

  @Override
  public List<Map<String, Object>> select(String sql, int expectRows, Duration timeout,
      Object... args) throws SQLException {
    Object result;
    if (args.length > 0) {
      result = getRunner().select(sql, MAP_LIST_HANDLER, expectRows, timeout, args);
    } else {
      result = getRunner().select(sql, MAP_LIST_HANDLER, expectRows, timeout);
    }
    return forceCast(result);
  }

  @Override
  public Stream<Map<String, Object>> stream(String sql, Duration timeout, Object... args) {
    try {
      return new StreamableQueryRunner(confiuration)
          .streamQuery(confiuration.getDataSource().getConnection(), true, sql, MAP_HANDLER, args);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected DefaultQueryRunner getRunner() {
    return runner;
  }

}

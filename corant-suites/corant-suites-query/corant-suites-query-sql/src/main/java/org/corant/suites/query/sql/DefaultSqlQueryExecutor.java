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
package org.corant.suites.query.sql;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.StatementConfiguration;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.sql.dialect.Dialect;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:31:55
 *
 */
public class DefaultSqlQueryExecutor implements SqlQueryExecutor {

  public final static MapHandler MAP_HANDLER = new MapHandler();
  public final static MapListHandler MAP_LIST_HANDLER = new MapListHandler();

  protected final SqlQueryConfiguration confiuration;
  protected final QueryRunner runner;
  protected final Dialect dialect;

  public DefaultSqlQueryExecutor(SqlQueryConfiguration confiuration) {
    this.confiuration = confiuration;
    runner = new QueryRunner(confiuration.getDataSource(),
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
  public Map<String, Object> get(String sql, Object... args) throws SQLException {
    Object result = null;
    if (args.length > 0) {
      result = getRunner().query(sql, MAP_HANDLER, args);
    } else {
      result = getRunner().query(sql, MAP_HANDLER);
    }
    return forceCast(result);
  }

  @Override
  public Dialect getDialect() {
    return dialect;
  }

  @Override
  public List<Map<String, Object>> select(String sql, Object... args) throws SQLException {
    Object result = null;
    if (args.length > 0) {
      result = getRunner().query(sql, MAP_LIST_HANDLER, args);
    } else {
      result = getRunner().query(sql, MAP_LIST_HANDLER);
    }
    return forceCast(result);
  }

  @Override
  public Stream<Map<String, Object>> stream(String sql, Object... args) {
    try {
      return new StreamableQueryRunner(confiuration)
          .streamQuery(confiuration.getDataSource().getConnection(), true, sql, MAP_HANDLER, args);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected QueryRunner getRunner() {
    return runner;
  }

}

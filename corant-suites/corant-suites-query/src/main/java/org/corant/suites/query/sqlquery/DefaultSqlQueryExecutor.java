/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.suites.query.sqlquery;

import static org.corant.shared.normal.Names.JndiNames.JNDI_DATS_NME;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.StatementConfiguration;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.corant.Corant;
import org.corant.suites.query.QueryRuntimeException;

/**
 * asosat-query
 *
 * @author bingo 下午5:31:55
 *
 */
public class DefaultSqlQueryExecutor implements SqlQueryExecutor {

  protected final QueryRunner runner;

  public DefaultSqlQueryExecutor(SqlQueryConfiguration confiuration) {
    runner = new QueryRunner(confiuration.getDataSource(),
        new StatementConfiguration(confiuration.getFetchDirection(), confiuration.getFetchSize(),
            confiuration.getMaxFieldSize(), confiuration.getMaxRows(),
            confiuration.getQueryTimeout()));
  }

  public static DefaultSqlQueryExecutor of(SqlQueryConfiguration.Builder builder) {
    return new DefaultSqlQueryExecutor(builder.build());
  }

  public static DefaultSqlQueryExecutor of(String jndiDsName) {
    String useJndiDsName = shouldNotNull(jndiDsName).startsWith(JNDI_DATS_NME) ? jndiDsName
        : JNDI_DATS_NME + "/" + jndiDsName;
    try {
      return of(new SqlQueryConfiguration.Builder().dataSource(
          forceCast(Corant.cdi().select(InitialContext.class).get().lookup(useJndiDsName))));
    } catch (NamingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  public Map<String, Object> get(String sql) throws SQLException {
    return getRunner().query(sql, new MapHandler());
  }

  @Override
  public <T> T get(String sql, Class<T> resultClass, Object... args) throws SQLException {
    ResultSetHandler<?> handler = resolveResultSetHandler(resultClass, false);
    Object result = null;
    if (args.length > 0) {
      result = getRunner().query(sql, handler, args);
    } else {
      result = getRunner().query(sql, handler);
    }
    return forceCast(result);
  }

  @Override
  public List<Map<String, Object>> select(String sql) throws SQLException {
    List<Map<String, Object>> tmp = getRunner().query(sql, new MapListHandler());
    return tmp == null ? new ArrayList<>() : tmp;
  }

  @Override
  public <T> List<T> select(String sql, Class<T> resultClass, Object... args) throws SQLException {
    ResultSetHandler<?> handler = resolveResultSetHandler(resultClass, true);
    Object result = null;
    if (args.length > 0) {
      result = getRunner().query(sql, handler, args);
    } else {
      result = getRunner().query(sql, handler);
    }
    return forceCast(result);
  }

  @Override
  public <T> Stream<T> stream(String sql, Map<String, Object> param) {
    // TODO
    return null;
  }

  protected QueryRunner getRunner() {
    return runner;
  }

  protected ResultSetHandler<?> resolveResultSetHandler(Class<?> resultClass, boolean isList) {
    if (isList) {
      return Map.class.isAssignableFrom(resultClass) ? new MapListHandler()
          : new BeanListHandler<>(resultClass);
    } else {
      return Map.class.isAssignableFrom(resultClass) ? new MapHandler()
          : new BeanHandler<>(resultClass);
    }
  }
}

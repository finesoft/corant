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
package org.corant.suites.datasource.shared;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.StatementConfiguration;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

/**
 * corant-suites-datasource-shared
 *
 * @author bingo 下午6:52:27
 *
 */
public class JDBCTemplate {

  public final static QueryRunner SIMPLE_RUNNER = new QueryRunner();
  public final static MapHandler MAP_HANDLER = new MapHandler();
  public final static MapListHandler MAP_LIST_HANDLER = new MapListHandler();

  protected final QueryRunner runner;

  public JDBCTemplate(DataSource ds) {
    this(ds, false, null, 32, null, null, null);
  }

  /**
   *
   * @param ds the data source
   * @param pmdKnownBroken
   * @param fetchDirection The direction for fetching rows from database tables.
   * @param fetchSize The number of rows that should be fetched from the database when more rows are
   *        needed.
   * @param maxFieldSize The maximum number of bytes that can be returned for character and binary
   *        column values.
   * @param maxRows The maximum number of rows that a <code>ResultSet</code> can produce.
   * @param queryTimeout The number of seconds the driver will wait for execution.
   */
  protected JDBCTemplate(DataSource ds, boolean pmdKnownBroken, Integer fetchDirection,
      Integer fetchSize, Integer maxFieldSize, Integer maxRows, Integer queryTimeout) {
    super();
    runner = new QueryRunner(shouldNotNull(ds), pmdKnownBroken,
        new StatementConfiguration(fetchDirection, fetchSize, maxFieldSize, maxRows, queryTimeout));
  }

  public static int[] batch(Connection conn, String sql, Object[][] params) throws SQLException {
    return SIMPLE_RUNNER.batch(conn, sql, params);
  }

  public static int execute(Connection conn, String sql, Object... params) throws SQLException {
    return SIMPLE_RUNNER.execute(conn, sql, params);
  }

  public static <T> List<T> execute(Connection conn, String sql, ResultSetHandler<T> rsh,
      Object... params) throws SQLException {
    return SIMPLE_RUNNER.execute(conn, sql, rsh, params);
  }

  public static List<Map<String, Object>> executeOf(Connection conn, String sql, Object... params)
      throws SQLException {
    return SIMPLE_RUNNER.execute(conn, sql, MAP_HANDLER, params);
  }

  public static JDBCTemplate from(DataSource ds) {
    return new JDBCTemplate(ds);
  }

  public static Map<String, Object> insert(Connection conn, String sql, Object... params)
      throws SQLException {
    return insert(conn, sql, MAP_HANDLER, params);
  }

  public static <T> T insert(Connection conn, String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    return SIMPLE_RUNNER.insert(conn, sql, rsh, params);
  }

  public static List<Map<String, Object>> insertBatch(Connection conn, String sql,
      Object[][] params) throws SQLException {
    return SIMPLE_RUNNER.insertBatch(conn, sql, MAP_LIST_HANDLER, params);
  }

  public static <T> T insertBatch(Connection conn, String sql, ResultSetHandler<T> rsh,
      Object[][] params) throws SQLException {
    return SIMPLE_RUNNER.insertBatch(conn, sql, rsh, params);
  }

  public static List<Map<String, Object>> query(Connection conn, String sql, Object... params)
      throws SQLException {
    return query(conn, sql, MAP_LIST_HANDLER);
  }

  public static <T> T query(Connection conn, String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    return SIMPLE_RUNNER.query(conn, sql, rsh, params);
  }

  public static int update(Connection conn, String sql, Object... params) throws SQLException {
    return SIMPLE_RUNNER.update(conn, sql, params);
  }

  public int[] batch(String sql, Object[][] params) throws SQLException {
    return runner.batch(sql, params);
  }

  public int execute(String sql, Object... params) throws SQLException {
    return runner.execute(sql, params);
  }

  public <T> List<T> execute(String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    return execute(sql, rsh, params);
  }

  public List<Map<String, Object>> executeOf(String sql, Object... params) throws SQLException {
    return execute(sql, MAP_HANDLER, params);
  }

  public Map<String, Object> insert(String sql, Object... params) throws SQLException {
    return runner.insert(sql, MAP_HANDLER, params);
  }

  public <T> T insert(String sql, ResultSetHandler<T> rsh, Object... params) throws SQLException {
    return runner.insert(sql, rsh, params);
  }

  public List<Map<String, Object>> insertBatch(String sql, Object[][] params) throws SQLException {
    return runner.insertBatch(sql, MAP_LIST_HANDLER, params);
  }

  public <T> T insertBatch(String sql, ResultSetHandler<T> rsh, Object[][] params)
      throws SQLException {
    return runner.insertBatch(sql, rsh, params);
  }

  public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
    return runner.query(sql, MAP_LIST_HANDLER, params);
  }

  public <T> T query(String sql, ResultSetHandler<T> rsh, Object... params) throws SQLException {
    return runner.query(sql, rsh, params);
  }

  public int update(String sql, Object... params) throws SQLException {
    return runner.update(sql, params);
  }
}

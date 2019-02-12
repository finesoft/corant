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
import static org.corant.shared.util.ObjectUtils.max;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.StatementConfiguration;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.corant.shared.exception.CorantRuntimeException;

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
  public final static int DFLT_FETCH_SIZE = 32;

  protected final DataSource dataSource;
  protected final QueryRunner runner;

  public JDBCTemplate(DataSource ds) {
    this(ds, false, null, DFLT_FETCH_SIZE, null, null, null);
  }

  /**
   *
   * @param dataSource the data source
   * @param pmdKnownBroken
   * @param fetchDirection The direction for fetching rows from database tables.
   * @param fetchSize The number of rows that should be fetched from the database when more rows are
   *        needed.
   * @param maxFieldSize The maximum number of bytes that can be returned for character and binary
   *        column values.
   * @param maxRows The maximum number of rows that a <code>ResultSet</code> can produce.
   * @param queryTimeout The number of seconds the driver will wait for execution.
   */
  public JDBCTemplate(DataSource dataSource, boolean pmdKnownBroken, Integer fetchDirection,
      Integer fetchSize, Integer maxFieldSize, Integer maxRows, Integer queryTimeout) {
    super();
    this.dataSource = shouldNotNull(dataSource);
    runner = new QueryRunner(dataSource, pmdKnownBroken,
        new StatementConfiguration(fetchDirection, fetchSize, maxFieldSize, maxRows, queryTimeout));
  }

  public static int[] batch(Connection conn, String sql, Object[][] params) throws SQLException {
    return SIMPLE_RUNNER.batch(conn, sql, params);
  }

  public static JDBCTemplate build(DataSource ds) {
    return new JDBCTemplate(ds);
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

  public static void release(ResultSet rs, PreparedStatement stmt, Connection conn,
      boolean closeConn) {
    try {
      DbUtils.close(rs);
    } catch (SQLException e) {
    } finally {
      try {
        DbUtils.close(stmt);
      } catch (SQLException e) {
      }
      if (closeConn) {
        try {
          DbUtils.close(conn);
        } catch (SQLException e) {
        }
      }
    }
  }

  public static Stream<Map<String, Object>> stream(Connection conn, String sql, int fetchSize,
      Object... params) throws SQLException {
    return stream(conn, sql, fetchSize, MAP_HANDLER, params);
  }

  public static <T> Stream<T> stream(Connection conn, String sql, int fetchSize,
      ResultSetHandler<T> rsh, Object... params) throws SQLException {
    return new StreamableQueryRunner(
        new StatementConfiguration(null, max(fetchSize, DFLT_FETCH_SIZE), null, null, null))
            .stream(conn, false, sql, rsh, params);
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
    return runner.execute(sql, rsh, params);
  }

  public List<Map<String, Object>> executeOf(String sql, Object... params) throws SQLException {
    return execute(sql, MAP_HANDLER, params);
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public QueryRunner getRunner() {
    return runner;
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

  public Stream<Map<String, Object>> stream(String sql, int fetchSize, Object... params)
      throws SQLException {
    return stream(sql, fetchSize, MAP_HANDLER, params);
  }

  public <T> Stream<T> stream(String sql, int fetchSize, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    return new StreamableQueryRunner(
        new StatementConfiguration(null, max(fetchSize, DFLT_FETCH_SIZE), null, null, null))
            .stream(dataSource.getConnection(), true, sql, rsh, params);
  }

  public int update(String sql, Object... params) throws SQLException {
    return runner.update(sql, params);
  }

  public static class ResultSetSpliterator<T> extends AbstractSpliterator<T> {
    final static int CHARACTERISTICS = Spliterator.NONNULL | Spliterator.IMMUTABLE;
    private final boolean closeConn;
    private final Connection conn;
    private final PreparedStatement stmt;
    private final ResultSet rs;
    private final ResultSetHandler<T> rsh;

    public ResultSetSpliterator(Connection conn, PreparedStatement stmt, ResultSet rs,
        boolean closeConn, ResultSetHandler<T> rsh) {
      super(Long.MAX_VALUE, CHARACTERISTICS);
      this.conn = conn;
      this.stmt = stmt;
      this.closeConn = closeConn;
      this.rs = rs;
      this.rsh = rsh;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
      try {
        T obj = rsh.handle(rs);
        boolean hasMore = obj != null;
        if (hasMore) {
          action.accept(obj);
        } else {
          release(rs, stmt, conn, closeConn);
        }
        return hasMore;
      } catch (SQLException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  public static class StreamableQueryRunner extends QueryRunner {

    public StreamableQueryRunner() {
      super();
    }

    public StreamableQueryRunner(StatementConfiguration stmtConfig) {
      super(stmtConfig);
    }

    @SuppressWarnings("restriction")
    public <T> Stream<T> stream(Connection conn, boolean closeConn, String sql,
        ResultSetHandler<T> rsh, Object... params) throws SQLException {
      if (conn == null) {
        throw new SQLException("Null connection");
      }

      if (sql == null) {
        if (closeConn) {
          DbUtils.close(conn);
        }
        throw new SQLException("Null SQL statement");
      }

      if (rsh == null) {
        if (closeConn) {
          DbUtils.close(conn);
        }
        throw new SQLException("Null ResultSetHandler");
      }

      PreparedStatement stmt = null;
      ResultSet rs = null;
      try {
        final PreparedStatement stmtx = stmt = this.prepareStatement(conn, sql);
        fillStatement(stmt, params);
        final ResultSet rsx = rs = wrap(stmt.executeQuery());
        Stream<T> stream = StreamSupport
            .stream(new ResultSetSpliterator<>(conn, stmtx, rsx, closeConn, rsh), false)
            .onClose(() -> release(rsx, stmtx, conn, closeConn));
        // FIXME Last Line of Defense, use jdk.internal.ref.Cleaner when using JDK9
        sun.misc.Cleaner.create(stream, () -> release(rsx, stmtx, conn, closeConn));
        return stream;
      } catch (Exception e) {
        release(rs, stmt, conn, closeConn);
        if (e instanceof SQLException) {
          rethrow(SQLException.class.cast(e), sql, params);
        }
      }
      return Stream.empty();
    }
  }
}

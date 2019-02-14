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
import static org.corant.shared.util.CollectionUtils.asList;
import static org.corant.shared.util.ObjectUtils.max;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicInteger;
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
 * TODO insertBatch/batch support stream parameters.
 *
 * @author bingo 下午6:52:27
 *
 */
public class JDBCTemplate {

  public static final QueryRunner SIMPLE_RUNNER = new QueryRunner();
  public static final MapHandler MAP_HANDLER = new MapHandler();
  public static final MapListHandler MAP_LIST_HANDLER = new MapListHandler();
  public static final int DFLT_FETCH_SIZE = 32;
  protected static final StreamableQueryRunner SIMPLE_STREAM_RUNNER = new StreamableQueryRunner();

  protected final DataSource dataSource;
  protected final StatementConfiguration stmtConfig;
  protected final QueryRunner runner;
  protected final StreamableQueryRunner streamRunner;

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
    stmtConfig =
        new StatementConfiguration(fetchDirection, fetchSize, maxFieldSize, maxRows, queryTimeout);
    runner = new QueryRunner(dataSource, pmdKnownBroken, stmtConfig);
    streamRunner = new StreamableQueryRunner(stmtConfig);
  }

  public static void batch(Connection conn, String sql, int batchSubmitSize,
      Stream<Iterable<?>> params, Consumer<int[]> consumer) throws SQLException {
    SIMPLE_STREAM_RUNNER.streamBatch(conn, false, sql, batchSubmitSize, params, consumer);
  }

  public static int[] batch(Connection conn, String sql, Object[][] params) throws SQLException {
    return SIMPLE_RUNNER.batch(conn, sql, params);
  }

  public static int[] batch(Connection conn, String sql, Stream<Iterable<?>> params)
      throws SQLException {
    return SIMPLE_STREAM_RUNNER.streamBatch(conn, false, sql, params);
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

  public static List<Map<String, Object>> executes(Connection conn, String sql, Object... params)
      throws SQLException {
    return SIMPLE_RUNNER.execute(conn, sql, MAP_HANDLER, params);
  }

  public static Map<String, Object> get(Connection conn, String sql, Object... params)
      throws SQLException {
    return query(conn, sql, MAP_HANDLER);
  }

  public static Map<String, Object> insert(Connection conn, String sql, Object... params)
      throws SQLException {
    return insert(conn, sql, MAP_HANDLER, params);
  }

  public static <T> T insert(Connection conn, String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    return SIMPLE_RUNNER.insert(conn, sql, rsh, params);
  }

  public static void insertBatch(Connection conn, String sql, int batchSubmitSize,
      Stream<Iterable<?>> params, Consumer<List<Map<String, Object>>> consumer)
      throws SQLException {
    SIMPLE_STREAM_RUNNER.streamInsertBatch(conn, false, sql, batchSubmitSize, MAP_LIST_HANDLER,
        params, consumer);
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
            .streamQuery(conn, false, sql, rsh, params);
  }

  public static int update(Connection conn, String sql, Object... params) throws SQLException {
    return SIMPLE_RUNNER.update(conn, sql, params);
  }

  public int[] batch(String sql, Object[][] params) throws SQLException {
    return runner.batch(sql, params);
  }

  public int[] batch(String sql, Stream<Iterable<?>> params) throws SQLException {
    return streamRunner.streamBatch(dataSource.getConnection(), true, sql, params);
  }

  public int execute(String sql, Object... params) throws SQLException {
    return runner.execute(sql, params);
  }

  public <T> List<T> execute(String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    return runner.execute(sql, rsh, params);
  }

  public List<Map<String, Object>> executes(String sql, Object... params) throws SQLException {
    return execute(sql, MAP_HANDLER, params);
  }

  public Map<String, Object> get(String sql, Object... params) throws SQLException {
    return runner.query(sql, MAP_HANDLER, params);
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public QueryRunner getRunner() {
    return runner;
  }

  public StatementConfiguration getStmtConfig() {
    return stmtConfig;
  }

  public Map<String, Object> insert(String sql, Object... params) throws SQLException {
    return runner.insert(sql, MAP_HANDLER, params);
  }

  public <T> T insert(String sql, ResultSetHandler<T> rsh, Object... params) throws SQLException {
    return runner.insert(sql, rsh, params);
  }

  public void insertBatch(String sql, int batchSubmitSize, Stream<Iterable<?>> params,
      Consumer<List<Map<String, Object>>> consumer) throws SQLException {
    streamRunner.streamInsertBatch(dataSource.getConnection(), true, sql, batchSubmitSize,
        MAP_LIST_HANDLER, params, consumer);
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
            .streamQuery(dataSource.getConnection(), true, sql, rsh, params);
  }

  public int update(String sql, Object... params) throws SQLException {
    return runner.update(sql, params);
  }

  public static class ResultSetSpliterator<T> extends AbstractSpliterator<T> {
    final static int CHARACTERISTICS = Spliterator.NONNULL | Spliterator.IMMUTABLE;
    private final Runnable releaser;
    private final ResultSet rs;
    private final ResultSetHandler<T> rsh;

    public ResultSetSpliterator(ResultSet rs, ResultSetHandler<T> rsh, Runnable releaser) {
      super(Long.MAX_VALUE, CHARACTERISTICS);
      this.releaser = releaser;
      this.rs = rs;
      this.rsh = rsh;
    }

    ResultSetSpliterator(Gadget obj, ResultSetHandler<T> rsh) {
      this(obj.rs, rsh, obj);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
      try {
        T obj = rsh.handle(rs);
        boolean hasMore = obj != null;
        if (hasMore) {
          action.accept(obj);
        } else {
          releaser.run();
        }
        return hasMore;
      } catch (Exception e) {
        releaser.run();
        throw new CorantRuntimeException(e);
      }
    }
  }

  static class Gadget implements Runnable {
    final Connection conn;
    final PreparedStatement stmt;
    final ResultSet rs;
    final boolean closeConn;

    Gadget(Connection conn, PreparedStatement stmt, ResultSet rs, boolean closeConn) {
      super();
      this.conn = conn;
      this.stmt = stmt;
      this.rs = rs;
      this.closeConn = closeConn;
    }

    @Override
    public void run() {
      release(rs, stmt, conn, closeConn);
    }

  }

  static class StreamableQueryRunner extends QueryRunner {

    public StreamableQueryRunner() {
      super();
    }

    public StreamableQueryRunner(StatementConfiguration stmtConfig) {
      super(stmtConfig);
    }

    void streamBatch(Connection conn, boolean closeConn, String sql, int batchSubmitSize,
        Stream<Iterable<?>> params, Consumer<int[]> consumer) throws SQLException {
      preCondition(conn, closeConn, sql);
      if (params == null) {
        if (closeConn) {
          close(conn);
        }
        throw new SQLException("Null parameters. If parameters aren't need, pass an empty stream.");
      }
      PreparedStatement stmt = null;
      final Consumer<int[]> useConsumer = consumer != null ? consumer : (ia) -> {
      };
      try {
        final PreparedStatement stmtx = stmt = this.prepareStatement(conn, sql);
        final AtomicInteger counter = new AtomicInteger();
        final AtomicInteger batchCounter = new AtomicInteger();
        final int submitSize = max(batchSubmitSize, DFLT_FETCH_SIZE);
        params.forEach(it -> {
          try {
            completeStatement(stmtx, it).addBatch();
            if (counter.incrementAndGet() % submitSize == 0) {
              useConsumer.accept(stmtx.executeBatch());
              batchCounter.set(counter.get());
            }
          } catch (SQLException e) {
            throw new CorantRuntimeException(e);
          }
        });
        if (counter.get() > batchCounter.get()) {
          useConsumer.accept(stmt.executeBatch());
        }
      } catch (SQLException e) {
        rethrow(e, sql, params);
      } finally {
        release(null, stmt, conn, closeConn);
      }
    }

    int[] streamBatch(Connection conn, boolean closeConn, String sql, Stream<Iterable<?>> params)
        throws SQLException {
      preCondition(conn, closeConn, sql);
      if (params == null) {
        if (closeConn) {
          close(conn);
        }
        throw new SQLException("Null parameters. If parameters aren't need, pass an empty stream.");
      }
      PreparedStatement stmt = null;
      int[] rows = null;
      try {
        final PreparedStatement stmtx = stmt = this.prepareStatement(conn, sql);
        params.forEach(it -> {
          try {
            completeStatement(stmtx, it).addBatch();
          } catch (SQLException e) {
            throw new CorantRuntimeException(e);
          }
        });
        rows = stmt.executeBatch();
      } catch (SQLException e) {
        rethrow(e, sql, params);
      } finally {
        release(null, stmt, conn, closeConn);
      }
      return rows;
    }

    <T> void streamInsertBatch(Connection conn, boolean closeConn, String sql, int batchSubmitSize,
        ResultSetHandler<T> rsh, Stream<Iterable<?>> params, Consumer<T> consumer)
        throws SQLException {
      preCondition(conn, closeConn, sql);
      if (params == null) {
        if (closeConn) {
          close(conn);
        }
        throw new SQLException("Null parameters. If parameters aren't need, pass an empty array.");
      }
      PreparedStatement stmt = null;
      final Consumer<T> useConsumer = consumer == null ? (t) -> {
      } : consumer;
      try {
        final PreparedStatement stmtx =
            stmt = this.prepareStatement(conn, sql, Statement.RETURN_GENERATED_KEYS);
        final AtomicInteger counter = new AtomicInteger();
        final AtomicInteger batchCounter = new AtomicInteger();
        final int submitSize = max(batchSubmitSize, DFLT_FETCH_SIZE);
        params.forEach(it -> {
          try {
            completeStatement(stmtx, it).addBatch();
            if (counter.incrementAndGet() % submitSize == 0) {
              stmtx.executeBatch();
              ResultSet rs = stmtx.getGeneratedKeys();
              useConsumer.accept(rsh.handle(rs));
              batchCounter.set(counter.get());
            }
          } catch (SQLException e) {
            throw new CorantRuntimeException(e);
          }
        });
        if (counter.get() > batchCounter.get()) {
          stmt.executeBatch();
          ResultSet rs = stmt.getGeneratedKeys();
          useConsumer.accept(rsh.handle(rs));
        }
      } catch (Exception e) {
        rethrow(e, sql);
      } finally {
        release(null, stmt, conn, closeConn);
      }
    }

    @SuppressWarnings("restriction")
    <T> Stream<T> streamQuery(Connection conn, boolean closeConn, String sql,
        ResultSetHandler<T> rsh, Object... params) throws SQLException {
      preCondition(conn, closeConn, sql);
      if (rsh == null) {
        if (closeConn) {
          DbUtils.close(conn);
        }
        throw new SQLException("Null ResultSetHandler");
      }
      Gadget g = null;
      try {
        PreparedStatement stmt = completeStatement(prepareStatement(conn, sql), params);
        ResultSet rs = wrap(stmt.executeQuery());
        g = new Gadget(conn, stmt, rs, closeConn);
        Stream<T> s = StreamSupport.stream(new ResultSetSpliterator<>(g, rsh), false).onClose(g);
        // FIXME Last line of defense for release, use jdk.internal.ref.Cleaner when using JDK9
        sun.misc.Cleaner.create(s, g);
        return s;
      } catch (Exception e) {
        if (g != null) {
          g.run();
        }
        rethrow(e, sql, params);
      }
      return Stream.empty();
    }

    private PreparedStatement completeStatement(PreparedStatement stmt, Iterable<?> params)
        throws SQLException {
      List<?> list = asList(params);
      fillStatement(stmt, list.toArray(new Object[list.size()]));
      return stmt;
    }

    private PreparedStatement completeStatement(PreparedStatement stmt, Object... params)
        throws SQLException {
      fillStatement(stmt, params);
      return stmt;
    }

    private void preCondition(Connection conn, boolean closeConn, String sql) throws SQLException {
      if (conn == null) {
        throw new SQLException("Null connection");
      }
      if (sql == null) {
        if (closeConn) {
          DbUtils.close(conn);
        }
        throw new SQLException("Null SQL statement");
      }
    }

    private void rethrow(Exception e, String sql, Object... params) throws SQLException {
      if (e instanceof SQLException) {
        super.rethrow(SQLException.class.cast(e), sql, params);
      } else {
        super.rethrow(new SQLException(e), sql, params);
      }
    }
  }
}

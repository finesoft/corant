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
package org.corant.modules.datasource.shared;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Functions.emptyConsumer;
import static org.corant.shared.util.Lists.linkedListOf;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
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
import org.corant.modules.datasource.shared.util.DbUtilBasicRowProcessor;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午6:52:27
 *
 */
public class JDBCTemplate {

  public static final String SQL_PARAM_PH = "?";
  public static final char SQL_PARAM_PH_C = SQL_PARAM_PH.charAt(0);
  public static final char SQL_PARAM_ESC_C = '\'';
  public static final String SQL_PARAM_SP = ",";
  public static final QueryRunner SIMPLE_RUNNER = new QueryRunner();
  public static final MapHandler MAP_HANDLER = new MapHandler(DbUtilBasicRowProcessor.INST);
  public static final MapListHandler MAP_LIST_HANDLER =
      new MapListHandler(DbUtilBasicRowProcessor.INST);
  public static final int DFLT_FETCH_SIZE = 32;
  protected static final StreamableQueryRunner SIMPLE_STREAM_RUNNER = new StreamableQueryRunner();

  protected final DataSource dataSource;
  protected final StatementConfiguration stmtConfig;
  protected final QueryRunner runner;
  protected final StreamableQueryRunner streamRunner;

  /**
   * Create a JDNC Template that binded given data source
   *
   * @param ds the data source use to bind
   */
  public JDBCTemplate(DataSource ds) {
    this(ds, false, null, DFLT_FETCH_SIZE, null, null, null);
  }

  /**
   * Create a JDBC Template
   *
   * @param dataSource the data source
   * @param pmdKnownBroken
   * @param fetchDirection The direction for fetching rows from database tables.
   * @param fetchSize The number of rows that should be fetched from the database when more rows are
   *        needed.
   * @param maxFieldSize The maximum number of bytes that can be returned for character and binary
   *        column values.
   * @param maxRows The maximum number of rows that a {@code ResultSet} can produce.
   * @param queryTimeout The number of seconds the driver will wait for execution.
   */
  public JDBCTemplate(DataSource dataSource, boolean pmdKnownBroken, Integer fetchDirection,
      Integer fetchSize, Integer maxFieldSize, Integer maxRows, Integer queryTimeout) {
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
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return SIMPLE_RUNNER.execute(conn, processeds.getKey(), processeds.getValue());
  }

  public static <T> List<T> execute(Connection conn, String sql, ResultSetHandler<T> rsh,
      Object... params) throws SQLException {
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return SIMPLE_RUNNER.execute(conn, processeds.getKey(), rsh, processeds.getValue());
  }

  public static List<List<Map<String, Object>>> executes(Connection conn, String sql,
      Object... params) throws SQLException {
    return execute(conn, sql, MAP_LIST_HANDLER, params);
  }

  public static Map<String, Object> get(Connection conn, String sql, Object... params)
      throws SQLException {
    return query(conn, sql, MAP_HANDLER, params);
  }

  public static Map<String, Object> insert(Connection conn, String sql, Object... params)
      throws SQLException {
    return insert(conn, sql, MAP_HANDLER, params);
  }

  public static <T> T insert(Connection conn, String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return SIMPLE_RUNNER.insert(conn, processeds.getKey(), rsh, processeds.getValue());
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
    return query(conn, sql, MAP_LIST_HANDLER, params);
  }

  public static <T> T query(Connection conn, String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return SIMPLE_RUNNER.query(conn, processeds.getKey(), rsh, processeds.getValue());
  }

  public static void release(ResultSet rs, PreparedStatement stmt, Connection conn,
      boolean closeConn) {
    try {
      DbUtils.close(rs);
    } catch (SQLException e) {
      // Noop
    } finally {
      try {
        DbUtils.close(stmt);
      } catch (SQLException e) {
        // Noop
      }
      if (closeConn) {
        try {
          DbUtils.close(conn);
        } catch (SQLException e) {
          // Noop
        }
      }
    }
  }

  public static Stream<Map<String, Object>> stream(Connection conn, String sql, Integer fetchSize,
      Object... params) throws SQLException {
    return stream(conn, sql, fetchSize, MAP_HANDLER, params);
  }

  public static <T> Stream<T> stream(Connection conn, String sql, Integer fetchSize,
      ResultSetHandler<T> rsh, Object... params) throws SQLException {
    return new StreamableQueryRunner(new StatementConfiguration(null, fetchSize, null, null, null))
        .streamQuery(conn, false, sql, rsh, params);
  }

  public static void tryBatch(Connection conn, String sql, int batchSubmitSize,
      Stream<Iterable<?>> params, Consumer<int[]> consumer) {
    try {
      batch(conn, sql, batchSubmitSize, params, consumer);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static int[] tryBatch(Connection conn, String sql, Object[][] params) {
    try {
      return batch(conn, sql, params);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static int[] tryBatch(Connection conn, String sql, Stream<Iterable<?>> params) {
    try {
      return batch(conn, sql, params);
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static int tryExecute(Connection conn, String sql, Object... params) {
    try {
      return execute(conn, sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T> List<T> tryExecute(Connection conn, String sql, ResultSetHandler<T> rsh,
      Object... params) {
    try {
      return execute(conn, sql, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static List<List<Map<String, Object>>> tryExecutes(Connection conn, String sql,
      Object... params) {
    try {
      return executes(conn, sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static Map<String, Object> tryGet(Connection conn, String sql, Object... params) {
    try {
      return get(conn, sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static Map<String, Object> tryInsert(Connection conn, String sql, Object... params) {
    try {
      return insert(conn, sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T> T tryInsert(Connection conn, String sql, ResultSetHandler<T> rsh,
      Object... params) {
    try {
      return insert(conn, sql, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void tryInsertBatch(Connection conn, String sql, int batchSubmitSize,
      Stream<Iterable<?>> params, Consumer<List<Map<String, Object>>> consumer) {
    try {
      insertBatch(conn, sql, batchSubmitSize, params, consumer);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static List<Map<String, Object>> tryInsertBatch(Connection conn, String sql,
      Object[][] params) {
    try {
      return insertBatch(conn, sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T> T tryInsertBatch(Connection conn, String sql, ResultSetHandler<T> rsh,
      Object[][] params) {
    try {
      return insertBatch(conn, sql, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static List<Map<String, Object>> tryQuery(Connection conn, String sql, Object... params) {
    try {
      return query(conn, sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T> T tryQuery(Connection conn, String sql, ResultSetHandler<T> rsh,
      Object... params) {
    try {
      return query(conn, sql, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static Stream<Map<String, Object>> tryStream(Connection conn, String sql, int fetchSize,
      Object... params) {
    try {
      return stream(conn, sql, fetchSize, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T> Stream<T> tryStream(Connection conn, String sql, int fetchSize,
      ResultSetHandler<T> rsh, Object... params) {
    try {
      return stream(conn, sql, fetchSize, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static int tryUpdate(Connection conn, String sql, Object... params) {
    try {
      return update(conn, sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static int update(Connection conn, String sql, Object... params) throws SQLException {
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return SIMPLE_RUNNER.update(conn, processeds.getKey(), processeds.getValue());
  }

  static Pair<String, Object[]> processSqlAndParams(String sql, Object... params) {
    if (isEmpty(params) || isBlank(sql)
        || streamOf(params).noneMatch(p -> p instanceof Collection || p.getClass().isArray())) {
      return Pair.of(sql, params);
    }
    LinkedList<Object> orginalParams = linkedListOf(params);
    List<Object> fixedParams = new ArrayList<>();
    StringBuilder fixedSql = new StringBuilder();
    int escapes = 0;
    int sqlLen = sql.length();
    for (int i = 0; i < sqlLen; i++) {
      char c = sql.charAt(i);
      if (c == SQL_PARAM_ESC_C) {
        fixedSql.append(c);
        escapes++;
      } else if (c == SQL_PARAM_PH_C && escapes % 2 == 0) {
        Object param = orginalParams.remove();
        if (param instanceof Iterable) {
          Iterable<?> iterableParam = (Iterable<?>) param;
          if (isNotEmpty(iterableParam)) {
            for (Object p : iterableParam) {
              fixedParams.add(p);
              fixedSql.append(SQL_PARAM_PH).append(SQL_PARAM_SP);
            }
            fixedSql.deleteCharAt(fixedSql.length() - 1);
          }
        } else if (param != null && param.getClass().isArray()) {
          Object[] arrayParam = (Object[]) param;
          if (isNotEmpty(arrayParam)) {
            for (Object p : arrayParam) {
              fixedParams.add(p);
              fixedSql.append(SQL_PARAM_PH).append(SQL_PARAM_SP);
            }
            fixedSql.deleteCharAt(fixedSql.length() - 1);
          }
        } else {
          fixedParams.add(param);
          fixedSql.append(c);
        }
      } else {
        fixedSql.append(c);
      }
    }
    if (escapes % 2 != 0 || !orginalParams.isEmpty()) {
      throw new CorantRuntimeException("Parameters and sql statements not match!");
    }
    return Pair.of(fixedSql.toString(), fixedParams.toArray());
  }

  public int[] batch(String sql, Object[][] params) throws SQLException {
    return runner.batch(sql, params);
  }

  public int[] batch(String sql, Stream<Iterable<?>> params) throws SQLException {
    return streamRunner.streamBatch(dataSource.getConnection(), true, sql, params);
  }

  public int execute(String sql, Object... params) throws SQLException {
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return runner.execute(processeds.getKey(), processeds.getValue());
  }

  public <T> List<T> execute(String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return runner.execute(processeds.getKey(), rsh, processeds.getValue());
  }

  public List<Map<String, Object>> executes(String sql, Object... params) throws SQLException {
    return execute(sql, MAP_HANDLER, params);
  }

  public Map<String, Object> get(String sql, Object... params) throws SQLException {
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return runner.query(processeds.getKey(), MAP_HANDLER, processeds.getValue());
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
    return insert(sql, MAP_HANDLER, params);
  }

  public <T> T insert(String sql, ResultSetHandler<T> rsh, Object... params) throws SQLException {
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return runner.insert(processeds.getKey(), rsh, processeds.getValue());
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
    return query(sql, MAP_LIST_HANDLER, params);
  }

  public <T> T query(String sql, ResultSetHandler<T> rsh, Object... params) throws SQLException {
    Pair<String, Object[]> processeds = processSqlAndParams(sql, params);
    return runner.query(processeds.getKey(), rsh, processeds.getValue());
  }

  /**
   * Query stream results, use for mass data query.
   *
   * <p>
   * NOTE: In order to release related resources, please remember to close after using the stream.
   *
   * <pre>
   * Example: try(Stream stream = stream(s,f,p)){
   *    stream.forEach(row->{
   *        //do somthing
   *    })
   * }
   * </pre>
   *
   * @param sql
   * @param fetchSize
   * @param params
   * @return
   * @throws SQLException stream
   */
  public Stream<Map<String, Object>> stream(String sql, Integer fetchSize, Object... params)
      throws SQLException {
    return stream(sql, fetchSize, MAP_HANDLER, params);
  }

  /**
   * Query stream results, use for mass data query.
   *
   * <p>
   * NOTE: In order to release related resources, please remember to close after using the stream.
   *
   * <pre>
   * Example: try(Stream stream = stream(s,f,r,p)){
   *    stream.forEach(row->{
   *        //do somthing
   *    })
   * }
   * </pre>
   *
   * @param <T>
   * @param sql
   * @param fetchSize
   * @param rsh
   * @param params
   * @return
   * @throws SQLException stream
   */
  public <T> Stream<T> stream(String sql, Integer fetchSize, ResultSetHandler<T> rsh,
      Object... params) throws SQLException {
    return new StreamableQueryRunner(new StatementConfiguration(null, fetchSize, null, null, null))
        .streamQuery(dataSource.getConnection(), true, sql, rsh, params);
  }

  public int[] tryBatch(String sql, Object[][] params) {
    try {
      return batch(sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public int[] tryBatch(String sql, Stream<Iterable<?>> params) {
    try {
      return batch(sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public int tryExecute(String sql, Object... params) {
    try {
      return execute(sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public <T> List<T> tryExecute(String sql, ResultSetHandler<T> rsh, Object... params) {
    try {
      return execute(sql, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public List<Map<String, Object>> tryExecutes(String sql, Object... params) {
    try {
      return executes(sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public Map<String, Object> tryGet(String sql, Object... params) {
    try {
      return get(sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public Map<String, Object> tryInsert(String sql, Object... params) {
    try {
      return insert(sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public <T> T tryInsert(String sql, ResultSetHandler<T> rsh, Object... params) {
    try {
      return insert(sql, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }

  }

  public void tryInsertBatch(String sql, int batchSubmitSize, Stream<Iterable<?>> params,
      Consumer<List<Map<String, Object>>> consumer) {
    try {
      insertBatch(sql, batchSubmitSize, params, consumer);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public List<Map<String, Object>> tryInsertBatch(String sql, Object[][] params) {
    try {
      return insertBatch(sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public <T> T tryInsertBatch(String sql, ResultSetHandler<T> rsh, Object[][] params) {
    try {
      return insertBatch(sql, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public List<Map<String, Object>> tryQuery(String sql, Object... params) {
    try {
      return query(sql, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public <T> T tryQuery(String sql, ResultSetHandler<T> rsh, Object... params) {
    try {
      return query(sql, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public Stream<Map<String, Object>> tryStream(String sql, Integer fetchSize, Object... params) {
    try {
      return stream(sql, fetchSize, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public <T> Stream<T> tryStream(String sql, Integer fetchSize, ResultSetHandler<T> rsh,
      Object... params) {
    try {
      return stream(sql, fetchSize, rsh, params);
    } catch (SQLException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public int update(String sql, Object... params) throws SQLException {
    Pair<String, Object[]> processed = processSqlAndParams(sql, params);
    return runner.update(processed.getKey(), processed.getValue());
  }

  public static class ResultSetSpliterator<T> extends AbstractSpliterator<T> {
    static final int CHARACTERISTICS = Spliterator.NONNULL | Spliterator.IMMUTABLE;
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

    public StreamableQueryRunner() {}

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
      final Consumer<int[]> useConsumer = consumer != null ? consumer : ia -> {
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
        throw new SQLException("Null parameters. If parameters aren't need, pass an empty stream.");
      }
      PreparedStatement stmt = null;
      final Consumer<T> useConsumer = consumer == null ? emptyConsumer() : consumer;
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
        final ResultSetSpliterator<T> spliterator = new ResultSetSpliterator<>(g, rsh);
        return StreamSupport.stream(spliterator, false).onClose(g);
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
      List<?> list = listOf(params);
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
        super.rethrow((SQLException) e, sql, params);
      } else {
        super.rethrow(new SQLException(e), sql, params);
      }
    }
  }
}

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

import static org.corant.shared.util.Functions.emptyConsumer;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.ObjectUtils.max;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.StatementConfiguration;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-query-sql
 *
 * @author bingo 上午10:00:38
 *
 */
public class StreamableQueryRunner extends QueryRunner {

  final int defaultBatchSize;

  public StreamableQueryRunner() {
    super();
    defaultBatchSize = 16;
  }

  public StreamableQueryRunner(SqlQueryConfiguration confiuration) {
    super(new StatementConfiguration(confiuration.getFetchDirection(), confiuration.getFetchSize(),
        confiuration.getMaxFieldSize(), confiuration.getMaxRows(), confiuration.getQueryTimeout()));
    defaultBatchSize = max(confiuration.getFetchSize(), 16);
  }

  static void release(ResultSet rs, PreparedStatement stmt, Connection conn, boolean closeConn) {
    try {
      DbUtils.close(rs);
    } catch (SQLException e) {
      // Noop!
    } finally {
      try {
        DbUtils.close(stmt);
      } catch (SQLException e) {
        // Noop!
      }
      if (closeConn) {
        try {
          DbUtils.close(conn);
        } catch (SQLException e) {
          // Noop!
        }
      }
    }
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
      final int submitSize = max(batchSubmitSize, defaultBatchSize);
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
      final int submitSize = max(batchSubmitSize, defaultBatchSize);
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
  <T> Stream<T> streamQuery(Connection conn, boolean closeConn, String sql, ResultSetHandler<T> rsh,
      Object... params) throws SQLException {
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

  static class ResultSetSpliterator<T> extends AbstractSpliterator<T> {
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
}

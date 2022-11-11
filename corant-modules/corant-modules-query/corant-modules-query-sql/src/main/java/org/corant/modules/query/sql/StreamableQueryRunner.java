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

import static org.corant.shared.util.Objects.max;
import java.lang.ref.Cleaner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.StatementConfiguration;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Mutable.MutableInteger;

/**
 * corant-modules-query-sql
 *
 * @author bingo 上午10:00:38
 *
 */
public class StreamableQueryRunner extends QueryRunner {

  public StreamableQueryRunner() {}

  public StreamableQueryRunner(Integer fetchDirection, Integer batchSize, Integer maxFieldSize,
      Integer maxRows, Integer queryTimeout) {
    super(new StatementConfiguration(fetchDirection, max(batchSize, 1), maxFieldSize, maxRows,
        queryTimeout));
  }

  public StreamableQueryRunner(SqlQueryConfiguration confiuration, Duration timeout) {
    super(new StatementConfiguration(confiuration.getFetchDirection(),
        max(confiuration.getFetchSize(), 1), confiuration.getMaxFieldSize(), null,
        timeout == null ? null : (int) timeout.getSeconds()));
  }

  <T> Stream<T> streamQuery(Connection conn, boolean closeConn, String sql,
      ResultSetHandler<T> resultSetHandler, BiPredicate<Integer, Object> terminater,
      boolean autoClose, Object... params) throws SQLException {
    preCondition(conn, closeConn, sql, resultSetHandler);
    Releaser releaser = null;
    try {
      PreparedStatement statement = completeStatement(prepareStatement(conn, sql), params);
      ResultSet resultSet = wrap(statement.executeQuery());
      releaser = new Releaser(conn, statement, resultSet, closeConn);
      Stream<T> stream = StreamSupport
          .stream(new ResultSetSpliterator<>(releaser, resultSetHandler, terminater), false)
          .onClose(releaser);
      if (autoClose) {
        Cleaner.create().register(resultSet, releaser);// JDK9+
      }
      return stream;
    } catch (Exception e) {
      if (releaser != null) {
        releaser.run();
      }
      rethrowAny(e, sql, params);
    }
    return Stream.empty();
  }

  private PreparedStatement completeStatement(PreparedStatement stmt, Object... params)
      throws SQLException {
    fillStatement(stmt, params);
    return stmt;
  }

  private void preCondition(Connection conn, boolean closeConn, String sql,
      ResultSetHandler<?> resultSetHandler) throws SQLException {
    if (conn == null) {
      throw new SQLException("Null connection");
    }
    if (sql == null) {
      if (closeConn) {
        DbUtils.close(conn);
      }
      throw new SQLException("Null SQL statement");
    }
    if (resultSetHandler == null) {
      if (closeConn) {
        DbUtils.close(conn);
      }
      throw new SQLException("Null ResultSetHandler");
    }
  }

  private void rethrowAny(Exception e, String sql, Object... params) throws SQLException {
    if (e instanceof SQLException) {
      rethrow((SQLException) e, sql, params);
    } else {
      rethrow(new SQLException(e), sql, params);
    }
  }

  static class Releaser implements Runnable {
    final Connection connection;
    final PreparedStatement statement;
    final ResultSet resultSet;
    final boolean closeConn;

    Releaser(Connection conn, PreparedStatement stmt, ResultSet rs, boolean closeConn) {
      connection = conn;
      statement = stmt;
      resultSet = rs;
      this.closeConn = closeConn;
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

    @Override
    public void run() {
      release(resultSet, statement, connection, closeConn);
    }
  }

  static class ResultSetSpliterator<T> extends AbstractSpliterator<T> {
    static final int CHARACTERISTICS = Spliterator.NONNULL | Spliterator.IMMUTABLE;
    private final Runnable releaser;
    private final ResultSet resultSet;
    private final ResultSetHandler<T> resultSetHandler;
    private final BiPredicate<Integer, Object> terminater;
    private final MutableInteger counter = MutableInteger.of(0);

    public ResultSetSpliterator(ResultSet resultSet, ResultSetHandler<T> resultSetHandler,
        BiPredicate<Integer, Object> terminater, Runnable releaser) {
      super(Long.MAX_VALUE, CHARACTERISTICS);
      this.terminater = terminater;
      this.releaser = releaser;
      this.resultSet = resultSet;
      this.resultSetHandler = resultSetHandler;
    }

    ResultSetSpliterator(Releaser obj, ResultSetHandler<T> resultSetHandler,
        BiPredicate<Integer, Object> terminater) {
      this(obj.resultSet, resultSetHandler, terminater, obj);
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
      try {
        T object = resultSetHandler.handle(resultSet);
        boolean hasMore = object != null;
        if (hasMore) {
          hasMore = terminater == null || terminater.test(counter.incrementAndGet(), object);
        }
        if (hasMore) {
          action.accept(object);
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

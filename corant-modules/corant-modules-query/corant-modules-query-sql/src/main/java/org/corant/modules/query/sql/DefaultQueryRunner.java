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

import static org.corant.shared.util.Objects.containsNotNull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import javax.sql.DataSource;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.StatementConfiguration;
import org.corant.modules.datasource.shared.util.DbUtilQueryRunner;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午1:21:08
 */
public class DefaultQueryRunner extends DbUtilQueryRunner {

  public DefaultQueryRunner() {}

  public DefaultQueryRunner(boolean pmdKnownBroken) {
    super(pmdKnownBroken);
  }

  public DefaultQueryRunner(DataSource ds) {
    super(ds);
  }

  public DefaultQueryRunner(DataSource ds, boolean pmdKnownBroken) {
    super(ds, pmdKnownBroken);
  }

  public DefaultQueryRunner(DataSource ds, boolean pmdKnownBroken,
      StatementConfiguration stmtConfig) {
    super(ds, pmdKnownBroken, stmtConfig);
  }

  public DefaultQueryRunner(DataSource ds, boolean pmdKnownBroken,
      StatementConfiguration stmtConfig, ResultSetConfiguration resultSetConfig) {
    super(ds, pmdKnownBroken, stmtConfig, resultSetConfig);
  }

  public DefaultQueryRunner(DataSource ds, StatementConfiguration stmtConfig) {
    super(ds, stmtConfig);
  }

  public DefaultQueryRunner(DataSource ds, StatementConfiguration stmtConfig,
      ResultSetConfiguration resultSetConfig) {
    super(ds, stmtConfig, resultSetConfig);
  }

  public DefaultQueryRunner(SqlQueryConfiguration configuration) {
    this(configuration, null);
  }

  public DefaultQueryRunner(SqlQueryConfiguration configuration, Duration timeout) {
    this(configuration.getDataSource(),
        new StatementConfiguration(configuration.getFetchDirection(), configuration.getFetchSize(),
            configuration.getMaxFieldSize(), null, timeout),
        containsNotNull(configuration.getResultSetType(), configuration.getResultSetConcurrency(),
            configuration.getResultSetHoldability())
                ? new ResultSetConfiguration(configuration.getResultSetType(),
                    configuration.getResultSetConcurrency(),
                    configuration.getResultSetHoldability())
                : null);
  }

  public DefaultQueryRunner(StatementConfiguration stmtConfig) {
    super(stmtConfig);
  }

  public DefaultQueryRunner(StatementConfiguration stmtConfig,
      ResultSetConfiguration resultSetConfig) {
    super(stmtConfig, resultSetConfig);
  }

  public <T> T select(String sql, ResultSetHandler<T> rsh, int expectRows, Duration timeout)
      throws SQLException {
    Connection conn = prepareConnection();
    return this.<T>select(conn, true, sql, rsh, expectRows, timeout, (Object[]) null);
  }

  public <T> T select(String sql, ResultSetHandler<T> rsh, int expectRows, Duration timeout,
      Object... params) throws SQLException {
    Connection conn = prepareConnection();
    return this.<T>select(conn, true, sql, rsh, expectRows, timeout, params);
  }

  <T> T select(Connection conn, boolean closeConn, String sql, ResultSetHandler<T> rsh,
      int expectRows, Duration timeout, Object... params) throws SQLException {
    if (conn == null) {
      throw new SQLException("Null connection");
    }

    if (sql == null) {
      if (closeConn) {
        close(conn);
      }
      throw new SQLException("Null SQL statement");
    }

    if (rsh == null) {
      if (closeConn) {
        close(conn);
      }
      throw new SQLException("Null ResultSetHandler");
    }

    PreparedStatement stmt = null;
    ResultSet rs = null;
    T result = null;

    try {
      stmt = this.prepareStatement(conn, sql);
      if (expectRows > 0) {
        stmt.setMaxRows(expectRows);// force max rows
      }
      if (timeout != null) {
        stmt.setQueryTimeout((int) timeout.toSeconds());// set the time-out
      }
      fillStatement(stmt, params);
      rs = wrap(stmt.executeQuery());
      result = rsh.handle(rs);

    } catch (SQLException e) {
      rethrow(e, sql, params);

    } finally {
      try {
        close(rs);
      } finally {
        close(stmt);
        if (closeConn) {
          close(conn);
        }
      }
    }

    return result;
  }
}

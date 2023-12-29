/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.datasource.shared.util;

import static org.corant.shared.util.Objects.defaultObject;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.StatementConfiguration;

/**
 * corant-modules-datasource-shared
 *
 * @author bingo 下午2:34:58
 */
public class DbUtilQueryRunner extends QueryRunner {

  protected final StatementConfiguration myStmtConfig;
  protected final ResultSetConfiguration resultSetConfig;

  /**
   * @see QueryRunner#QueryRunner()
   */
  public DbUtilQueryRunner() {
    myStmtConfig = null;
    resultSetConfig = null;
  }

  /**
   * @see QueryRunner#QueryRunner(boolean)
   */
  public DbUtilQueryRunner(boolean pmdKnownBroken) {
    super(pmdKnownBroken);
    myStmtConfig = null;
    resultSetConfig = null;
  }

  /**
   * @see QueryRunner#QueryRunner(DataSource)
   */
  public DbUtilQueryRunner(DataSource ds) {
    super(ds);
    myStmtConfig = null;
    resultSetConfig = null;
  }

  /**
   * @see QueryRunner#QueryRunner(DataSource, boolean)
   */
  public DbUtilQueryRunner(DataSource ds, boolean pmdKnownBroken) {
    super(ds, pmdKnownBroken);
    myStmtConfig = null;
    resultSetConfig = null;
  }

  /**
   * @see QueryRunner#QueryRunner(DataSource, boolean, StatementConfiguration)
   */
  public DbUtilQueryRunner(DataSource ds, boolean pmdKnownBroken,
      StatementConfiguration stmtConfig) {
    super(ds, pmdKnownBroken, stmtConfig);
    myStmtConfig = stmtConfig;
    resultSetConfig = null;
  }

  /**
   * @see QueryRunner#QueryRunner(DataSource, boolean, StatementConfiguration)
   */
  public DbUtilQueryRunner(DataSource ds, boolean pmdKnownBroken, StatementConfiguration stmtConfig,
      ResultSetConfiguration resultSetConfig) {
    super(ds, pmdKnownBroken, stmtConfig);
    myStmtConfig = stmtConfig;
    this.resultSetConfig = resultSetConfig;
  }

  /**
   * @see QueryRunner#QueryRunner(DataSource, StatementConfiguration)
   */
  public DbUtilQueryRunner(DataSource ds, StatementConfiguration stmtConfig) {
    super(ds, stmtConfig);
    myStmtConfig = stmtConfig;
    resultSetConfig = null;
  }

  /**
   * @see QueryRunner#QueryRunner(DataSource, StatementConfiguration)
   */
  public DbUtilQueryRunner(DataSource ds, StatementConfiguration stmtConfig,
      ResultSetConfiguration resultSetConfig) {
    super(ds, stmtConfig);
    myStmtConfig = stmtConfig;
    this.resultSetConfig = resultSetConfig;
  }

  /**
   * @see QueryRunner#QueryRunner(StatementConfiguration)
   */
  public DbUtilQueryRunner(StatementConfiguration stmtConfig) {
    super(stmtConfig);
    myStmtConfig = stmtConfig;
    resultSetConfig = null;
  }

  /**
   * @see QueryRunner#QueryRunner(StatementConfiguration)
   */
  public DbUtilQueryRunner(StatementConfiguration stmtConfig,
      ResultSetConfiguration resultSetConfig) {
    super(stmtConfig);
    myStmtConfig = stmtConfig;
    this.resultSetConfig = resultSetConfig;
  }

  protected void configureStatement(Statement stmt) throws SQLException {

    if (myStmtConfig != null) {
      if (myStmtConfig.isFetchDirectionSet()) {
        stmt.setFetchDirection(myStmtConfig.getFetchDirection());
      }

      if (myStmtConfig.isFetchSizeSet()) {
        stmt.setFetchSize(myStmtConfig.getFetchSize());
      }

      if (myStmtConfig.isMaxFieldSizeSet()) {
        stmt.setMaxFieldSize(myStmtConfig.getMaxFieldSize());
      }

      if (myStmtConfig.isMaxRowsSet()) {
        stmt.setMaxRows(myStmtConfig.getMaxRows());
      }

      if (myStmtConfig.isQueryTimeoutSet()) {
        stmt.setQueryTimeout(myStmtConfig.getQueryTimeout());
      }
    }
  }

  @Override
  protected CallableStatement prepareCall(Connection conn, String sql) throws SQLException {
    if (resultSetConfig == null) {
      return conn.prepareCall(sql);
    } else if (resultSetConfig.resultSetHoldability != null) {
      return conn.prepareCall(sql, resultSetConfig.resultSetType,
          resultSetConfig.resultSetConcurrency, resultSetConfig.resultSetHoldability);
    } else {
      return conn.prepareCall(sql, resultSetConfig.resultSetType,
          resultSetConfig.resultSetConcurrency);
    }
    // FIXME should we need to config the return CallableStatement like PreparedStatement.
  }

  @Override
  protected PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
    PreparedStatement ps;
    if (resultSetConfig == null) {
      ps = conn.prepareStatement(sql);
    } else if (resultSetConfig.resultSetHoldability != null) {
      ps = conn.prepareStatement(sql, resultSetConfig.resultSetType,
          resultSetConfig.resultSetConcurrency, resultSetConfig.resultSetHoldability);
    } else {
      ps = conn.prepareStatement(sql, resultSetConfig.resultSetType,
          resultSetConfig.resultSetConcurrency);
    }
    try {
      configureStatement(ps);
    } catch (SQLException e) {
      ps.close();
      throw e;
    }
    return ps;
  }

  @Override
  protected PreparedStatement prepareStatement(Connection conn, String sql, int returnedKeys)
      throws SQLException {
    PreparedStatement ps = conn.prepareStatement(sql, returnedKeys);
    try {
      configureStatement(ps);
    } catch (SQLException e) {
      ps.close();
      throw e;
    }
    return ps;
  }

  /**
   * corant-modules-datasource-shared
   *
   * @author bingo 下午2:47:14
   *
   */
  public static class ResultSetConfiguration {
    public static final ResultSetConfiguration DEFAULT = new ResultSetConfiguration(null, null);
    final Integer resultSetType;
    final Integer resultSetConcurrency;
    final Integer resultSetHoldability;

    public ResultSetConfiguration(Integer resultSetType, Integer resultSetConcurrency) {
      this(resultSetType, resultSetConcurrency, null);
    }

    public ResultSetConfiguration(Integer resultSetType, Integer resultSetConcurrency,
        Integer resultSetHoldability) {
      this.resultSetType = defaultObject(resultSetType, ResultSet.TYPE_FORWARD_ONLY);
      this.resultSetConcurrency = defaultObject(resultSetConcurrency, ResultSet.CONCUR_READ_ONLY);
      this.resultSetHoldability = resultSetHoldability;
    }
  }
}

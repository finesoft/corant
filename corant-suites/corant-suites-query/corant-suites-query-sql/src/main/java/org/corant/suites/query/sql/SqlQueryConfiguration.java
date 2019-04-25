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

import javax.sql.DataSource;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.sql.dialect.Dialect;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:56:03
 *
 */
public interface SqlQueryConfiguration {

  static final Integer DFLT_FETCH_SIZE = 16;

  static Builder defaultBuilder() {
    return new Builder();
  }

  DataSource getDataSource();

  Dialect getDialect();

  /**
   * @see java.sql.ResultSet#getFetchDirection()
   * @return the fetchDirection
   */
  default Integer getFetchDirection() {
    return null;
  }

  /**
   * @see java.sql.ResultSet#getFetchSize()
   * @return the fetchSize
   */
  default Integer getFetchSize() {
    return DFLT_FETCH_SIZE;
  }

  /**
   * @see java.sql.Statement#getMaxRows()
   * @return the maxFieldSize
   */
  default Integer getMaxFieldSize() {
    return 0;
  }

  /**
   * @see java.sql.Statement#getMaxFieldSize()
   * @return the maxFieldSize
   */
  default Integer getMaxRows() {
    return 0;
  }

  /**
   * @see java.sql.Statement#getQueryTimeout()
   * @return the maxFieldSize
   */
  default Integer getQueryTimeout() {
    return 0;
  }

  static class Builder {

    final DefaultSqlQueryConfiguration cfg = new DefaultSqlQueryConfiguration();

    public SqlQueryConfiguration build() {
      if (cfg.dataSource == null || cfg.dialect == null) {
        throw new QueryRuntimeException();
      }
      return cfg;
    }

    public Builder dataSource(DataSource dataSource) {
      cfg.dataSource = dataSource;
      return this;
    }

    public Builder dialect(Dialect dialect) {
      cfg.dialect = dialect;
      return this;
    }

    public Builder fetchDirection(Integer fetchDirection) {
      cfg.fetchDirection = fetchDirection;
      return this;
    }

    public Builder fetchSize(Integer fetchSize) {
      cfg.fetchSize = fetchSize;
      return this;
    }

    public Builder maxFieldSize(Integer maxFieldSize) {
      cfg.maxFieldSize = maxFieldSize;
      return this;
    }

    public Builder queryTimeout(Integer queryTimeout) {
      cfg.queryTimeout = queryTimeout;
      return this;
    }
  }

  static class DefaultSqlQueryConfiguration implements SqlQueryConfiguration {

    protected DataSource dataSource;
    protected Dialect dialect;
    protected Integer fetchDirection;
    protected Integer fetchSize = DFLT_FETCH_SIZE;
    protected Integer maxFieldSize = 0;
    protected Integer queryTimeout = 0;

    protected Integer maxRows = 32;

    @Override
    public DataSource getDataSource() {
      return dataSource;
    }

    @Override
    public Dialect getDialect() {
      return dialect;
    }

    @Override
    public Integer getFetchDirection() {
      return fetchDirection;
    }

    @Override
    public Integer getFetchSize() {
      return fetchSize;
    }

    @Override
    public Integer getMaxFieldSize() {
      return maxFieldSize;
    }

    @Override
    public Integer getMaxRows() {
      return maxRows;
    }

    @Override
    public Integer getQueryTimeout() {
      return queryTimeout;
    }

  }
}

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
package org.corant.suites.query.jpql;

import javax.persistence.EntityManagerFactory;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:56:03
 *
 */
public interface JpqlQueryConfiguration {

  Integer DFLT_FETCH_SIZE = 16;

  static Builder defaultBuilder() {
    return new Builder();
  }

  EntityManagerFactory getEntityManagerFactory();

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
   * @see java.sql.Statement#getMaxFieldSize()
   * @return the maxFieldSize
   */
  default Integer getMaxFieldSize() {
    return 0;
  }

  /**
   * @see java.sql.Statement#getMaxRows()
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

    final DefaultJpqlQueryConfiguration cfg = new DefaultJpqlQueryConfiguration();

    public JpqlQueryConfiguration build() {
      return cfg;
    }

    public Builder entityManagerFactory(EntityManagerFactory entityManagerFactory) {
      cfg.entityManagerFactory = entityManagerFactory;
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

    public Builder maxRows(Integer maxRows) {
      cfg.maxRows = maxRows;
      return this;
    }

    public Builder queryTimeout(Integer queryTimeout) {
      cfg.queryTimeout = queryTimeout;
      return this;
    }
  }

  static class DefaultJpqlQueryConfiguration implements JpqlQueryConfiguration {

    protected EntityManagerFactory entityManagerFactory;
    protected Integer fetchDirection;
    protected Integer fetchSize = DFLT_FETCH_SIZE;
    protected Integer maxFieldSize = 0;
    protected Integer queryTimeout = 0;
    protected Integer maxRows = 0;

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
      return entityManagerFactory;
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

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
package org.corant.suites.query.sql.dialect;

import org.corant.suites.query.shared.dynamic.SqlHelper;

/**
 * corant-suites-query
 *
 * @author bingo 上午10:58:26
 *
 */
public interface Dialect {

  String COUNT_FIELD_NAME = "_total";

  static String getNonOrderByPart(String sql) {
    return SqlHelper.removeOrderBy(sql);
  }

  /**
   * Convert SQL statement to Count SQL statement
   *
   * @param sql to convert SQL
   * @return Count SQL statement
   */
  default String getCountSql(String sql) {
    return new StringBuilder(sql.length() + 40).append("SELECT COUNT(1) ").append(COUNT_FIELD_NAME)
        .append(" FROM ( ").append(Dialect.getNonOrderByPart(sql)).append(" ) AS tmp_count_")
        .toString();
  }

  /**
   * Convert SQL statement to Limit SQL default offset is 0
   *
   * @param sql
   * @param limit
   * @return getLimitSql
   */
  default String getLimitSql(String sql, int limit) {
    return getLimitSql(sql, 0, limit);
  }

  /**
   * Convert SQL statement to Paging SQL
   *
   * @param sql to convert SQL
   * @param offset begin offset
   * @param limit page size
   * @return Paging SQL statement
   */
  String getLimitSql(String sql, int offset, int limit);

  /**
   *
   * @return supportsLimit
   */
  boolean supportsLimit();

  public enum DBMS {
    MYSQL() {
      @Override
      public Dialect instance() {
        return MySQLDialect.INSTANCE;
      }
    },
    ORACLE() {
      @Override
      public Dialect instance() {
        return OracleDialect.INSTANCE;
      }
    },
    DB2() {

      @Override
      public Dialect instance() {
        return DB2Dialect.INSTANCE;
      }
    },
    H2() {

      @Override
      public Dialect instance() {
        return H2Dialect.INSTANCE;
      }
    },
    HSQL() {

      @Override
      public Dialect instance() {
        return HSQLDialect.INSTANCE;
      }
    },
    POSTGRE() {

      @Override
      public Dialect instance() {
        return PostgreSQLDialect.INSTANCE;
      }
    },
    SQLSERVER2005() {

      @Override
      public Dialect instance() {
        return SQLServer2005Dialect.INSTANCE;
      }
    },
    SQLSERVER2008() {

      @Override
      public Dialect instance() {
        return SQLServer2008Dialect.INSTANCE;
      }
    },
    SQLSERVER2012() {

      @Override
      public Dialect instance() {
        return SQLServer2012Dialect.INSTANCE;
      }
    },
    SYBASE() {

      @Override
      public Dialect instance() {
        return SybaseDialect.INSTANCE;
      }
    };

    public Dialect instance() {
      return null;
    }
  }
}

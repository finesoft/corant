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
package org.corant.suites.query.sqlquery.dialect;

import org.corant.suites.query.sqlquery.SqlHelper;

/**
 * asosat-query
 *
 * @author bingo 上午10:58:26
 *
 */
public interface Dialect {

  public static final String COUNT_FIELD_NAME = "_total";

  public static String getNonOrderByPart(String sql) {
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
    MYSQL, ORACLE, DB2, H2, HSQL, POSTGRE, SQLSERVER, SQLSERVER2005, SYBASE,
  }
}

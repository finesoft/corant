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
package org.corant.modules.query.sql.dialect;

import java.util.Map;
import org.corant.modules.query.shared.dynamic.SqlHelper;

/**
 * corant-modules-query-sql
 *
 * When OFFSET is 0, then no rows are skipped.
 *
 * @author bingo 上午11:44:38
 *
 */
public class MySQLDialect implements Dialect {

  public static final Dialect INSTANCE = new MySQLDialect();

  @Override
  public String getLimitSql(String sql, int offset, int limit, Map<String, ?> hints) {
    return getLimitString(sql, offset, limit, hints);
  }

  /**
   * <pre>
   * dialect.getLimitString("select * from user", 12, 14) will return
   * select * from user limit 12,14
   * </pre>
   *
   * <p>
   *
   * <pre>
   * SELECT
   *    [ALL | DISTINCT | DISTINCTROW ]
   *    [HIGH_PRIORITY]
   *    [STRAIGHT_JOIN]
   *    [SQL_SMALL_RESULT] [SQL_BIG_RESULT] [SQL_BUFFER_RESULT]
   *    [SQL_CACHE | SQL_NO_CACHE] [SQL_CALC_FOUND_ROWS]
   *    select_expr [, select_expr] ...
   *    [into_option]
   *    [FROM table_references
   *      [PARTITION partition_list]]
   *    [WHERE where_condition]
   *    [GROUP BY {col_name | expr | position}
   *      [ASC | DESC], ... [WITH ROLLUP]]
   *    [HAVING where_condition]
   *    [ORDER BY {col_name | expr | position}
   *      [ASC | DESC], ...]
   *    [LIMIT {[offset,] row_count | row_count OFFSET offset}]
   *    [PROCEDURE procedure_name(argument_list)]
   *    [into_option]
   *    [FOR UPDATE | LOCK IN SHARE MODE]

   * into_option: {
   *    INTO OUTFILE 'file_name'
   *    * [CHARACTER SET charset_name]
   *    * export_options
   *   | INTO DUMPFILE 'file_name'
   *   | INTO var_name [, var_name] ...
   * }
   * </pre>
   */
  public String getLimitString(String sql, int offset, int limit, Map<String, ?> hints) {
    if (offset > 0) {
      return new StringBuilder(sql.length() + 32).append(sql).append(" LIMIT ").append(offset)
          .append(",").append(limit).toString();
    } else {
      return new StringBuilder(sql.length() + 16).append(sql).append(" LIMIT ").append(limit)
          .toString();
    }
  }

  @Override
  public String getNonOrderByPart(String sql) {
    if (sql != null) {
      int pos = SqlHelper.shallowIndexOfPattern(sql, SqlHelper.ORDER_BY_PATTERN, 0);
      if (pos > 0 && sql.indexOf('?', pos) == -1
          && SqlHelper.shallowIndexOfPattern(sql, SqlHelper.LIMIT_PATTERN, pos) < 0) {
        return sql.substring(0, pos);
      }
    }
    return sql;
  }

  @Override
  public boolean supportsLimit() {
    return true;
  }
}

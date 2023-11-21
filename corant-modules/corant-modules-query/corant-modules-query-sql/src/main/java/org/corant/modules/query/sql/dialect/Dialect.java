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

import static org.corant.shared.util.Lists.immutableListOf;
import static org.corant.shared.util.Maps.getMapBoolean;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.corant.modules.datasource.shared.SqlStatements;
import org.corant.modules.query.shared.dynamic.SqlHelper;

/**
 * corant-modules-query-sql
 *
 * @author bingo 上午10:58:26
 *
 */
public interface Dialect {

  List<String> AGGREGATE_FUNCTIONS = immutableListOf("AVG", "COUNT", "MAX", "MIN", "SUM");

  String COUNT_FIELD_NAME = "total_";
  String COUNT_TEMP_TABLE_NAME = "tmp_count_";
  String USE_DEFAULT_COUNT_SQL_HINT_KEY = "_use_default_count_sql";

  default Collection<String> getAggregationFunctionNames() {
    return AGGREGATE_FUNCTIONS;
  }

  /**
   * Convert SQL statement to Count SQL statement, optimized statements can be reorganized by
   * CCJSqlParserUtil parsing.
   *
   * @param sql to convert SQL
   * @param hints the hints use to improve the execution process
   * @return Count SQL statement
   */
  default String getCountSql(String sql, Map<String, ?> hints) {
    if (!getMapBoolean(hints, USE_DEFAULT_COUNT_SQL_HINT_KEY, false)) {
      try {
        return SqlStatements.resolveCountSql(sql, "1", COUNT_FIELD_NAME, COUNT_TEMP_TABLE_NAME,
            getAggregationFunctionNames());
      } catch (Exception ex) {
        return getDefaultCountSql(sql, hints);
      }
    } else {
      return getDefaultCountSql(sql, hints);
    }
  }

  /**
   * Convert SQL statement to Count SQL statement
   *
   * @param sql to convert SQL
   * @param hints the hints use to improve the execution process
   * @return Count SQL statement
   */
  default String getDefaultCountSql(String sql, Map<String, ?> hints) {
    return new StringBuilder(sql.length() + 64).append("SELECT COUNT(1) ").append(COUNT_FIELD_NAME)
        .append(" FROM ( ").append(getNonOrderByPart(sql)).append(" ) ")
        .append(COUNT_TEMP_TABLE_NAME).toString();
  }

  /**
   * Convert SQL statement to Paging SQL
   *
   * @param sql to convert SQL
   * @param offset begin offset
   * @param limit the fetched size
   * @param hints the hints use to improve the execution process
   * @return Paging SQL statement
   */
  String getLimitSql(String sql, int offset, int limit, Map<String, ?> hints);

  /**
   * Convert SQL statement to Limit SQL default offset is 0
   *
   * @param sql to convert SQL
   * @param limit the fetched size
   * @param hints the hints use to improve the execution process
   * @return getLimitSql
   */
  default String getLimitSql(String sql, int limit, Map<String, ?> hints) {
    return getLimitSql(sql, 0, limit, hints);
  }

  default String getNonOrderByPart(String sql) {
    return SqlHelper.removeOrderBy(sql);
  }

  /**
   * Return whether the underling database supports limitation sql.
   *
   * @return supportsLimit
   */
  boolean supportsLimit();

}

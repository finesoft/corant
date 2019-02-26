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

import static org.corant.shared.util.StringUtils.isBlank;
import java.util.Locale;
import org.corant.suites.query.sqlquery.SqlHelper;

/**
 * corant-suites-query
 *
 * @author bingo 上午11:03:33
 *
 */
public class SQLServer2005Dialect extends SQLServerDialect {

  @Override
  public String getLimitSql(String sql, int offset, int limit) {
    return getLimitString(sql, offset, limit);
  }

  @Override
  public boolean supportsLimit() {
    return true;
  }

  /**
   * Add a LIMIT clause to the given SQL SELECT
   * <p/>
   * The LIMIT SQL will look like:
   * <p/>
   * WITH script AS (SELECT TOP 100 percent ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as
   * __row_number__, * from table_name) SELECT * FROM script WHERE __row_number__ BETWEEN :offset
   * and :lastRows ORDER BY __row_number__
   *
   * @param sql The SQL statement to base the limit script off of.
   * @param offset Offset of the first row to be returned by the script (zero-based)
   * @param limit Maximum number of rows to be returned by the script
   * @return A new SQL statement with the LIMIT clause applied.
   */
  protected String getLimitString(String sql, int offset, int limit) {
    String orderby = SqlHelper.getOrderBy(sql), distinct = "",
        lowered = sql.toLowerCase(Locale.ROOT), sqlPart = sql;
    if (lowered.trim().startsWith(SqlHelper.SELECT_SPACE)) {
      int index = SqlHelper.SELECT_SPACE.length();
      if (lowered.startsWith(SqlHelper.SELECT_DISTINCT_SPACE)) {
        distinct = "DISTINCT ";
        index = SqlHelper.SELECT_DISTINCT_SPACE.length();
      }
      sqlPart = sqlPart.substring(index);
    }
    // if no ORDER BY is specified use fake ORDER BY field to avoid errors
    if (isBlank(orderby)) {
      orderby = SQL_DFLT_ORDERBY;
    }

    return new StringBuilder(sql.length() + 128).append("WITH query_ AS (SELECT ").append(distinct)
        .append("TOP 100 PERCENT ROW_NUMBER() OVER (").append(orderby).append(") as row_number_, ")
        .append(sqlPart).append(") SELECT * FROM query_ WHERE row_number_ BETWEEN ").append(offset)
        .append(" AND ").append(offset + limit - 1).append(" ORDER BY row_number_").toString();
  }
}

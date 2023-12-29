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

import static org.corant.shared.util.Maps.getMapBoolean;
import java.util.Map;
import org.corant.modules.query.shared.dynamic.SqlHelper;

/**
 * corant-modules-query-sql
 *
 * When OFFSET is 0, then no rows are skipped.
 *
 * @author bingo 上午11:03:33
 */
public class SQLServer2012Dialect extends SQLServer2005Dialect {

  public static final Dialect INSTANCE = new SQLServer2012Dialect();

  @Override
  public String getNonOrderByPart(String sql) {
    if (sql != null) {
      int pos = SqlHelper.shallowIndexOfPattern(sql, SqlHelper.ORDER_BY_PATTERN, 0);
      if (pos > 0 && sql.indexOf('?', pos) == -1
          && SqlHelper.shallowIndexOfPattern(sql, SqlHelper.OFFSET_PATTERN, pos) < 0) {
        return sql.substring(0, pos);
      }
    }
    return sql;
  }

  /**
   * Add a LIMIT clause to the given SQL SELECT
   * <p>
   * The LIMIT SQL will look like:
   * <p>
   * SELECT XXX FROM T OFFSET offset FETCH NEXT limit ROWS ONLY;
   * <p>
   *
   * <pre>
   * -- Syntax for SQL Server and Azure SQL Database
   *
   * $SELECT statement$ ::=
   *    [ WITH { [ XMLNAMESPACES ,] [ $common_table_expression$ [,...n] ] } ]
   *    $query_expression$
   *    [ ORDER BY $order_by_expression$ ]
   *    [ $FOR Clause$]
   *    [ OPTION ( $query_hint$ [ ,...n ] ) ]
   * $query_expression$ ::=
   *    { $query_specification$ | ( $query_expression$ ) }
   *    [  { UNION [ ALL ] | EXCEPT | INTERSECT }
   *    * $query_specification$ | ( $query_expression$ ) [...n ] ]
   * $query_specification$ ::=
   * SELECT [ ALL | DISTINCT ]
   *    [TOP ( expression ) [PERCENT] [ WITH TIES ] ]
   *    $ select_list $
   *    [ INTO new_table ]
   *    [ FROM { $table_source$ } [ ,...n ] ]
   *    [ WHERE $search_condition$ ]
   *    [ $GROUP BY$ ]
   *    [ HAVING $ search_condition $ ]
   * </pre>
   *
   * <p>
   *
   * <pre>
   * <b>Note:
   * To achieve stable results between query requests using OFFSET and FETCH, the
   * following conditions must be met:
   *
   * 1.The underlying data that is used by the query must not change. That is, either the rows
   * touched by the query are not updated or all requests for pages from the query are executed in a
   * single transaction using either snapshot or serializable transaction isolation. For more
   * information about these transaction isolation levels, see SET TRANSACTION ISOLATION LEVEL
   * (Transact-SQL).
   *
   * 2.The ORDER BY clause contains a column or combination of columns that are guaranteed to be
   * unique.
   *
   * 3.If the query SQL statement contains ORDER BYclause, this method use OFFSET FETCH NEXT
   * ways to execute the query by default, so caller must care whether the ORDER BY clause contains
   * a column or combination of columns that are guaranteed to be unique. If caller want to use
   * ROW_NUMBER ways (like SQLServer2005), the caller must specify the value of key
   * {@code useRowNumber} in the hint to be {@code true}.
   * </b>
   * </pre>
   *
   * @param sql The SQL statement to base the limit script off of.
   * @param offset Offset of the first row to be returned by the script (zero-based)
   * @param limit Maximum number of rows to be returned by the script
   * @param hints the hints use to improve the execution process
   * @return A new SQL statement with the LIMIT clause applied.
   */
  @Override
  protected String getLimitString(String sql, int offset, int limit, Map<String, ?> hints) {
    if (getMapBoolean(hints, "useRowNumber") || !SqlHelper.containOrderBy(sql)) {
      return super.getLimitString(sql, offset, limit, hints);
    }
    int pos = getInsertPosition(sql);
    StringBuilder tsql = new StringBuilder(sql.length() + 50).append(sql, 0, pos);
    tsql.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(limit)
        .append(" ROWS ONLY");
    if (pos > sql.length()) {
      tsql.append(sql.substring(pos - 1));
    }
    return tsql.toString();

  }

}

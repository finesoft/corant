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

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.corant.modules.query.shared.dynamic.SqlHelper;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-query-sql
 *
 * <p>
 * NOTE: Some code come from hibernate, if there is infringement, please inform
 * me(finesoft@gmail.com).
 *
 * @author bingo 上午11:03:33
 *
 */
public class SQLServer2005Dialect extends SQLServerDialect {

  public static final Dialect INSTANCE = new SQLServer2005Dialect();

  private static final String SPACE_NEWLINE_LINEFEED = "[\\s\\t\\n\\r]*";
  private static final Pattern WITH_CTE =
      Pattern.compile("(^" + SPACE_NEWLINE_LINEFEED + "WITH" + SPACE_NEWLINE_LINEFEED + ")",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern WITH_EXPRESSION_NAME =
      Pattern.compile("(^" + SPACE_NEWLINE_LINEFEED + "[a-zA-Z0-9]*" + SPACE_NEWLINE_LINEFEED + ")",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern WITH_COLUMN_NAMES_START =
      Pattern.compile("(^" + SPACE_NEWLINE_LINEFEED + "\\()", Pattern.CASE_INSENSITIVE);
  private static final Pattern WITH_COLUMN_NAMES_END =
      Pattern.compile("(\\))", Pattern.CASE_INSENSITIVE);
  private static final Pattern WITH_AS =
      Pattern.compile("(^" + SPACE_NEWLINE_LINEFEED + "AS" + SPACE_NEWLINE_LINEFEED + ")",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern WITH_COMMA =
      Pattern.compile("(^" + SPACE_NEWLINE_LINEFEED + ",)", Pattern.CASE_INSENSITIVE);

  @Override
  public String getLimitSql(String sql, int offset, int limit, Map<String, ?> hints) {
    return getLimitString(sql, offset, limit, hints);
  }

  @Override
  public boolean supportsLimit() {
    return true;
  }

  /**
   * Adds {@code TOP} expression
   *
   * @param sb SQL query.
   * @param offset the offset where top expression pattern matching should begin.
   * @param top the top numbers
   */
  protected void addTopExpression(StringBuilder sb, int offset, int top) {
    // We should use either of these which come first (SELECT or SELECT DISTINCT).
    String sql = sb.toString();
    final int selectPos = SqlHelper.shallowIndexOfPattern(sql, SqlHelper.SELECT_PATTERN, offset);
    final int selectDistinctPos =
        SqlHelper.shallowIndexOfPattern(sql, SqlHelper.SELECT_DISTINCT_PATTERN, offset);
    if (selectPos == selectDistinctPos) {
      // Place TOP after SELECT DISTINCT
      sb.insert(selectDistinctPos + SqlHelper.SELECT_DISTINCT.length(), " TOP(" + top + ")");
    } else {
      // Place TOP after SELECT
      sb.insert(selectPos + SqlHelper.SELECT.length(), " TOP(" + top + ")");
    }
  }

  /**
   * Advances over the CTE inner query that is contained inside matching '(' and ')'.
   *
   * @param sql The sql buffer.
   * @param offset The offset where to begin advancing the position from.
   * @return the position immediately after the CTE inner query plus 1.
   *
   * @throws IllegalArgumentException if the matching parenthesis aren't detected at the end of the
   *         parse.
   */
  protected int advanceOverCTEInnerQuery(StringBuilder sql, int offset) {
    int brackets = 0;
    int index = offset;
    int length = sql.length();
    boolean inString = false;
    for (; index < length; ++index) {
      final char ch = sql.charAt(index);
      if (ch == '\'') {
        inString = true;
      } else if (ch == '\'' && inString) {
        inString = false;
      } else if (ch == '(' && !inString) {
        brackets++;
      } else if (ch == ')' && !inString) {
        brackets--;
        if (brackets == 0) {
          break;
        }
      }
    }

    if (brackets > 0) {
      throw new IllegalArgumentException(
          "Failed to parse the CTE query inner query because closing ')' was not found.");
    }

    return index - offset + 1;
  }

  protected void encloseWithOuterQuery(StringBuilder sql, int offset) {
    sql.insert(offset,
        "SELECT inner_query.*, ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __row_nr__ FROM ( ");
    sql.append(" ) inner_query ");
  }

  protected int getInsertPosition(String sql) {
    int position = sql.length() - 1;
    for (; position > 0; --position) {
      char ch = sql.charAt(position);
      if (ch != ';' && ch != ' ' && ch != '\r' && ch != '\n') {
        break;
      }
    }
    return position + 1;
  }

  /**
   * Add a LIMIT clause to the given SQL SELECT (ROW_NUMBER for Paging)
   *
   * The LIMIT SQL will look like:
   *
   * <pre>
   * WITH query AS (
   *   SELECT inner_query.*
   *        , ROW_NUMBER() OVER (ORDER BY CURRENT_TIMESTAMP) as __hibernate_row_nr__
   *     FROM ( original_query_with_top_if_order_by_present_and_all_aliased_columns ) inner_query
   * )
   * SELECT alias_list FROM query WHERE __row_nr__ >= offset AND __row_nr__ < offset + last
   * </pre>
   *
   * When offset equals {@literal 0}, only <code>TOP(?)</code> expression is added to the original
   * query.
   *
   * @param sql The SQL statement to base the limit script off of.
   * @param offset Offset of the first row to be returned by the script (zero-based)
   * @param limit Maximum number of rows to be returned by the script
   * @param hints the hints use to improve the execution process
   * @return A new SQL statement with the LIMIT clause applied.
   */
  protected String getLimitString(String sql, int offset, int limit, Map<String, ?> hints) {
    final StringBuilder sb = new StringBuilder(sql);
    if (sb.charAt(sb.length() - 1) == ';') {
      sb.setLength(sb.length() - 1);
    }
    Pair<Integer, Boolean> ctes = getStatementIndex(sb);
    int top = offset + limit;
    if (offset == 0) {
      addTopExpression(sb, ctes.key(), top);
    } else {
      if (SqlHelper.containOrderBy(sql)) {
        addTopExpression(sb, ctes.key(), top);
      }
      encloseWithOuterQuery(sb, ctes.key());
      sb.insert(ctes.key(), !ctes.value() ? "WITH query AS (" : ", query AS (");
      sb.append(") SELECT * FROM query ");
      sb.append("WHERE __row_nr__ > ").append(offset).append(" AND __row_nr__ <= ").append(top);
    }
    return sb.toString();
  }

  /**
   * Get the starting point for the limit handler to begin injecting and transforming the SQL. For
   * non-CTE queries, this is offset 0. For CTE queries, this will be where the CTE's SELECT clause
   * begins (skipping all query definitions, column definitions and expressions).
   *
   * This method also sets {@code isCTE} if the query is parsed as a CTE query.
   *
   * @param sql The sql buffer.
   * @return the index where to begin parsing.
   */
  protected Pair<Integer, Boolean> getStatementIndex(StringBuilder sql) {
    final Matcher matcher = WITH_CTE.matcher(sql.toString());
    if (matcher.find() && matcher.groupCount() > 0) {
      return Pair.of(locateQueryInCTEStatement(sql, matcher.end()), true);
    }
    return Pair.of(0, false);
  }

  /**
   * Steps through the SQL buffer from the specified offset and performs a series of pattern
   * matches. The method locates where the CTE SELECT clause begins and returns that offset from the
   * SQL buffer.
   *
   * @param sql The sql buffer.
   * @param offset The offset to begin pattern matching.
   *
   * @return the offset where the CTE SELECT clause begins.
   * @throws IllegalArgumentException if the parse of the CTE query fails.
   */
  protected int locateQueryInCTEStatement(StringBuilder sql, int offset) {
    while (true) {
      Matcher matcher = WITH_EXPRESSION_NAME.matcher(sql.substring(offset));
      if (matcher.find() && matcher.groupCount() > 0) {
        offset += matcher.end();
        matcher = WITH_COLUMN_NAMES_START.matcher(sql.substring(offset));
        if (matcher.find() && matcher.groupCount() > 0) {
          offset += matcher.end();
          matcher = WITH_COLUMN_NAMES_END.matcher(sql.substring(offset));
          if (matcher.find() && matcher.groupCount() > 0) {
            offset += matcher.end();
            offset += advanceOverCTEInnerQuery(sql, offset);
            matcher = WITH_COMMA.matcher(sql.substring(offset));
            if (matcher.find() && matcher.groupCount() > 0) {
              // another CTE fragment exists, re-start parse of CTE
              offset += matcher.end();
            } else {
              // last CTE fragment, we're at the start of the SQL.
              return offset;
            }
          } else {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                "Failed to parse CTE expression columns at offset %d, SQL [%s]", offset,
                sql.toString()));
          }
        } else {
          matcher = WITH_AS.matcher(sql.substring(offset));
          if (matcher.find() && matcher.groupCount() > 0) {
            offset += matcher.end();
            offset += advanceOverCTEInnerQuery(sql, offset);
            matcher = WITH_COMMA.matcher(sql.substring(offset));
            if (matcher.find() && matcher.groupCount() > 0) {
              // another CTE fragment exists, re-start parse of CTE
              offset += matcher.end();
            } else {
              // last CTE fragment, we're at the start of the SQL.
              return offset;
            }
          } else {
            throw new IllegalArgumentException(String.format(Locale.ROOT,
                "Failed to locate AS keyword in CTE query at offset %d, SQL [%s]", offset,
                sql.toString()));
          }
        }
      } else {
        throw new IllegalArgumentException(String.format(Locale.ROOT,
            "Failed to locate CTE expression name at offset %d, SQL [%s]", offset, sql.toString()));
      }
    }
  }
}

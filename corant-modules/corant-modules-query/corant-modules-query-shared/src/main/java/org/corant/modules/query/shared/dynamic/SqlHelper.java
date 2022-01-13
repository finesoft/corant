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
package org.corant.modules.query.shared.dynamic;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.linkedListOf;
import static org.corant.shared.util.Primitives.wrapArray;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-query-shared
 *
 * NOTE: Some code come from hibernate, if there is infringement, please inform
 * me(finesoft@gmail.com).
 *
 * @author bingo 上午11:04:34
 *
 */
public class SqlHelper {

  public static final String SELECT = "select";
  public static final String SELECT_SPACE = "select ";
  public static final String FROM = "from";
  public static final String WHERE = "where";
  public static final String DISTINCT = "distinct";
  public static final String ORDER_BY = "order\\s+by";
  public static final String OFFSET = "offset\\s";
  public static final String LIMIT = "limit\\s";
  public static final String FETCH = "fetch\\s";
  public static final String SELECT_DISTINCT = SELECT + " " + DISTINCT;
  public static final String SELECT_DISTINCT_SPACE = SELECT_DISTINCT + " ";
  public static final Pattern SELECT_DISTINCT_PATTERN =
      buildShallowIndexPattern(SELECT_DISTINCT_SPACE, true);

  public static final Pattern SELECT_PATTERN = buildShallowIndexPattern(SELECT + "(.*)", true);
  public static final Pattern FROM_PATTERN = buildShallowIndexPattern(FROM, true);
  public static final Pattern WHERE_PATTERN = buildShallowIndexPattern(WHERE, true);
  public static final Pattern DISTINCT_PATTERN = buildShallowIndexPattern(DISTINCT, true);
  public static final Pattern ORDER_BY_PATTERN = buildShallowIndexPattern(ORDER_BY, true);
  public static final Pattern OFFSET_PATTERN = SqlHelper.buildShallowIndexPattern(OFFSET, true);
  public static final Pattern LIMIT_PATTERN = SqlHelper.buildShallowIndexPattern(LIMIT, true);
  public static final Pattern FETCH_PATTERN = SqlHelper.buildShallowIndexPattern(FETCH, true);

  public static final String SQL_PARAM_PH = "?";
  public static final char SQL_PARAM_PH_C = SQL_PARAM_PH.charAt(0);
  public static final char SQL_PARAM_ESC_C = '\'';
  public static final String SQL_PARAM_SP = ",";

  private SqlHelper() {}

  /**
   * Builds a pattern that can be used to find matches of case-insensitive matches based on the
   * search pattern that is not enclosed in parenthesis.
   *
   * @param pattern String search pattern.
   * @param wordBoundary whether to apply a word boundary restriction.
   * @return Compiled {@link Pattern}.
   */
  public static Pattern buildShallowIndexPattern(String pattern, boolean wordBoundary) {
    return Pattern.compile("(" + (wordBoundary ? "\\b" : "") + pattern + (wordBoundary ? "\\b" : "")
        + ")(?![^\\(|\\[]*(\\)|\\]))", Pattern.CASE_INSENSITIVE);
  }

  public static boolean containDistinct(String sql) {
    return sql != null && shallowIndexOfPattern(sql, DISTINCT_PATTERN, 0) > 0;
  }

  public static boolean containOrderBy(String sql) {
    return sql != null && shallowIndexOfPattern(sql, ORDER_BY_PATTERN, 0) > 0;
  }

  public static boolean containRegex(String sql, String regex) {
    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(sql);
    return matcher.find();
  }

  public static boolean containSelectDistinct(String sql) {
    return sql != null && shallowIndexOfPattern(sql, SELECT_DISTINCT_PATTERN, 0) > 0;
  }

  public static boolean containWhere(String sql) {
    return sql != null && shallowIndexOfPattern(sql, WHERE_PATTERN, 0) > 0;
  }

  public static String getOrderBy(String sql) {
    if (sql != null) {
      int pos = shallowIndexOfPattern(sql, ORDER_BY_PATTERN, 0);
      if (pos > 0) {
        return sql.substring(pos);
      }
    }
    return null;
  }

  /**
   * Return a pre-processed SQL statement containing place holders according to the given SQL and
   * parameter array. If the parameter is an array or list, it will be automatically expanded into
   * multiple place holders.
   *
   * FIXME: Need another implementation
   *
   * @param sql the SQL statement may containing place holders
   * @param parameters the parameters
   */
  public static Pair<String, Object[]> getPrepared(String sql, Object... parameters) {
    if (isEmpty(parameters) || isBlank(sql) || streamOf(parameters)
        .noneMatch(p -> p instanceof Collection || p != null && p.getClass().isArray())) {
      return Pair.of(sql, parameters);
    }
    LinkedList<Object> orginalParams = linkedListOf(parameters);
    List<Object> fixedParams = new ArrayList<>();
    StringBuilder fixedSql = new StringBuilder();
    int escapes = 0;
    int sqlLen = sql.length();
    for (int i = 0; i < sqlLen; i++) {
      char c = sql.charAt(i);
      if (c == SQL_PARAM_ESC_C) {
        fixedSql.append(c);
        escapes++;
      } else if (c == SQL_PARAM_PH_C && escapes % 2 == 0) {
        Object param = orginalParams.remove();
        if (param instanceof Iterable) {
          Iterable<?> iterableParam = (Iterable<?>) param;
          if (isNotEmpty(iterableParam)) {
            for (Object p : iterableParam) {
              fixedParams.add(p);
              fixedSql.append(SQL_PARAM_PH).append(SQL_PARAM_SP);
            }
            fixedSql.deleteCharAt(fixedSql.length() - 1);
          }
        } else if (param != null && param.getClass().isArray()) {
          Object[] arrayParam = wrapArray(param);
          if (isNotEmpty(arrayParam)) {
            for (Object p : arrayParam) {
              fixedParams.add(p);
              fixedSql.append(SQL_PARAM_PH).append(SQL_PARAM_SP);
            }
            fixedSql.deleteCharAt(fixedSql.length() - 1);
          }
        } else {
          fixedParams.add(param);
          fixedSql.append(c);
        }
      } else {
        fixedSql.append(c);
      }
    }
    if (escapes % 2 != 0 || !orginalParams.isEmpty()) {
      throw new QueryRuntimeException("Parameters and sql statements not match!");
    }
    return Pair.of(fixedSql.toString(), fixedParams.toArray());
  }

  public static String getSelectColumns(String sql) {
    return sql.substring(getSelectColumnsStartPosition(sql),
        shallowIndexOfPattern(sql, FROM_PATTERN, 0));
  }

  public static void main(String... regex) {
    String sql = "SELECT distinct a.*,basd,asd as aa ,asdasd,"
        + "(selectr top 1 sss from xxx where sss.sdd = aass order by aa asc) " + "FROM TABLE "
        + "WHERE " + "XXXX IN (SELECT TOP 1 X FROM XX ORDER BY XX) "
        + "order \t \n by ss asc,sss desc,assss asc";
    System.out.println("ORIGINAL:\n" + sql);
    System.out.println("REMOVE_ORDER_BY:\n" + removeOrderBy(sql));
    System.out.println("REMOVE_SELECT:\n" + removeSelect(sql));
    System.out.println("SELECT_COLUMNS:\n" + getSelectColumns(sql));
    System.out.println("ORDER_BY:\n" + getOrderBy(sql));
  }

  public static String removeOrderBy(String sql) {
    if (sql != null) {
      int pos = shallowIndexOfPattern(sql, ORDER_BY_PATTERN, 0);
      if (pos > 0 && sql.indexOf('?', pos) == -1) {
        // Some database allow prepare statement use place holder FIXME
        return sql.substring(0, pos);
      }
    }
    return sql;
  }

  public static String removeSelect(String sql) {
    int pos = shallowIndexOfPattern(sql, FROM_PATTERN, 0);
    if (pos != -1) {
      return sql.substring(pos);
    } else {
      return sql;
    }
  }

  /**
   *
   * Returns index of the first case-insensitive match of search pattern that is not enclosed in
   * parenthesis.
   *
   * @param sb String to search.
   * @param pattern Compiled search pattern.
   * @param fromIndex The index from which to start the search.
   *
   * @return Position of the first match, or {@literal -1} if not found.
   */
  public static int shallowIndexOfPattern(final String sb, final Pattern pattern, int fromIndex) {
    int index = -1;

    // quick exit, save performance and avoid exceptions
    if (sb.length() < fromIndex || fromIndex < 0) {
      return -1;
    }

    List<IgnoreRange> ignoreRangeList = generateIgnoreRanges(sb);

    Matcher matcher = pattern.matcher(sb);
    matcher.region(fromIndex, sb.length());

    if (ignoreRangeList.isEmpty()) {
      // old behavior where the first match is used if no ignorable ranges
      // were deduced from the matchString.
      if (matcher.find() && matcher.groupCount() > 0) {
        index = matcher.start();
      }
    } else {
      // rather than taking the first match, we now iterate all matches
      // until we determine a match that isn't considered "ignorable'.
      while (matcher.find() && matcher.groupCount() > 0) {
        final int position = matcher.start();
        if (!isPositionIgnorable(ignoreRangeList, position)) {
          index = position;
          break;
        }
      }
    }
    return index;
  }

  /**
   * Geneartes a list of {@code IgnoreRange} objects that represent nested sections of the provided
   * SQL buffer that should be ignored when performing regular expression matches.
   *
   * @param sql The SQL buffer.
   * @return list of {@code IgnoreRange} objects, never {@code null}.
   */
  static List<IgnoreRange> generateIgnoreRanges(String sql) {
    List<IgnoreRange> ignoreRangeList = new ArrayList<>();

    int depth = 0;
    int start = -1;
    int sqlLen = sql.length();
    boolean insideAStringValue = false;
    for (int i = 0; i < sqlLen; ++i) {
      final char ch = sql.charAt(i);
      if (ch == '\'') {
        insideAStringValue = !insideAStringValue;
      } else if (ch == '(' && !insideAStringValue) {
        depth++;
        if (depth == 1) {
          start = i;
        }
      } else if (ch == ')' && !insideAStringValue) {
        if (depth > 0) {
          if (depth == 1) {
            ignoreRangeList.add(new IgnoreRange(start, i));
            start = -1;
          }
          depth--;
        } else {
          throw new IllegalStateException("Found an unmatched ')' at position " + i + ": " + sql);
        }
      }
    }

    if (depth != 0) {
      throw new IllegalStateException(
          "Unmatched parenthesis in rendered SQL (" + depth + " depth): " + sql);
    }

    return ignoreRangeList;
  }

  static int getSelectColumnsStartPosition(String sb) {
    final int startPos = shallowIndexOfPattern(sb, SELECT_PATTERN, 0);
    // adjustment for 'select distinct ' and 'select '.
    final String sql = sb.substring(startPos).toLowerCase(Locale.getDefault());
    if (sql.startsWith(SELECT_DISTINCT_SPACE)) {
      return startPos + SELECT_DISTINCT_SPACE.length();
    } else if (sql.startsWith(SELECT_SPACE)) {
      return startPos + SELECT_SPACE.length();
    }
    return startPos;
  }

  /**
   * Returns whether the specified {@code position} is within the ranges of the
   * {@code ignoreRangeList}.
   *
   * @param ignoreRangeList list of {@code IgnoreRange} objects deduced from the SQL buffer.
   * @param position the position to determine whether is ignorable.
   * @return {@code true} if the position is to ignored/skipped, {@code false} otherwise.
   */
  static boolean isPositionIgnorable(List<IgnoreRange> ignoreRangeList, int position) {
    for (IgnoreRange ignoreRange : ignoreRangeList) {
      if (ignoreRange.isWithinRange(position)) {
        return true;
      }
    }
    return false;
  }

  static int shallowIndexOf(String sb, String search, int fromIndex) {
    final String lowercase = sb.toLowerCase(Locale.getDefault()); // case-insensitive match
    final int len = lowercase.length();
    final int searchlen = search.length();
    int pos = -1;
    int depth = 0;
    int cur = fromIndex;
    do {
      pos = lowercase.indexOf(search, cur);
      if (pos != -1) {
        for (int iter = cur; iter < pos; iter++) {
          char c = sb.charAt(iter);
          if (c == '(') {
            depth = depth + 1;
          } else if (c == ')') {
            depth = depth - 1;
          }
        }
        cur = pos + searchlen;
      }
    } while (cur < len && depth != 0 && pos != -1);
    return depth == 0 ? pos : -1;
  }

  static class IgnoreRange {
    private final int start;
    private final int end;

    IgnoreRange(int start, int end) {
      this.start = start;
      this.end = end;
    }

    boolean isWithinRange(int position) {
      return position >= start && position <= end;
    }
  }
}

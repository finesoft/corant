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
package org.corant.modules.jpa.shared;

import static org.corant.context.Beans.resolve;
import java.util.Locale;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * corant-modules-jpa-shared
 *
 * @author bingo 下午3:01:05
 */
public class JPQLHelper {

  public static String getTotalQL(String ql) {
    String queryString = ql;
    queryString =
        "select count(*) as n " + queryString.substring(shallowIndexOfWord(queryString, "from", 0));
    queryString = queryString.replaceAll("(?i)\\sorder(\\s)+by.+", " "); // 若存在order by则去除
    queryString = queryString.replaceAll("(?i)\\sjoin(\\s)+fetch\\s", " join "); // 若存在fetch则去除
    return queryString;
  }

  public static Query getTotalQuery(Query noTotalQuery, EntityManager em) {
    return resolve(JPQLResolver.class).resolveTotalQuery(noTotalQuery, em);
  }

  /**
   * Returns index of the first case-insensitive match of search term that is not enclosed in
   * parentheses.
   *
   * @param sb String to search.
   * @param search Search term.
   * @param fromIndex The index from which to start the search.
   *
   * @return Position of the first match, or {@literal -1} if not found.
   */
  static int shallowIndexOf(String sb, String search, int fromIndex) {
    final String lowercase = sb.toLowerCase(Locale.ROOT); // case-insensitive match
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

  /**
   * Returns index of the first case-insensitive match of search term surrounded by spaces that is
   * not enclosed in parentheses.
   *
   * @param sb String to search.
   * @param search Search term.
   * @param fromIndex The index from which to start the search.
   *
   * @return Position of the first match, or {@literal -1} if not found.
   */
  static int shallowIndexOfWord(final String sb, final String search, int fromIndex) {
    final int index = shallowIndexOf(sb, ' ' + search + ' ', fromIndex);
    return index != -1 ? index + 1 : -1; // In case of match adding one
    // because of space placed in
    // front of search term.
  }

  @FunctionalInterface
  public interface JPQLResolver {
    Query resolveTotalQuery(Query noTotalQuery, EntityManager em);
  }
}

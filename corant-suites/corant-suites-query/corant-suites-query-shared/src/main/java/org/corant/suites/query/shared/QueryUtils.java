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
package org.corant.suites.query.shared;

import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StringUtils.split;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * corant-suites-query
 *
 * @author bingo 上午10:54:15
 *
 */
public class QueryUtils {

  public static void extractResult(Object result, String paths, boolean flatList,
      List<Object> list) {
    extractResult(result, split(paths, ".", true, false), flatList, list);
  }

  public static void extractResult(Object result, String[] paths, boolean flatList,
      List<Object> list) {
    if (!interruptExtract(result, paths, flatList, list)) {
      if (result instanceof Map) {
        String path = paths[0];
        Object next = Map.class.cast(result).get(path);
        if (next != null) {
          extractResult(next, Arrays.copyOfRange(paths, 1, paths.length), flatList, list);
        }
      } else if (result instanceof Iterable) {
        for (Object next : Iterable.class.cast(result)) {
          if (next != null) {
            extractResult(next, paths, flatList, list);
          }
        }
      } else if (result != null) {
        extractResult(listOf((Object[]) result), paths, flatList, list);// may be array
      }
    }
  }

  static boolean interruptExtract(Object result, String[] paths, boolean flatList,
      List<Object> list) {
    if (isEmpty(paths)) {
      if (result instanceof Iterable && flatList) {
        listOf((Iterable<?>) result).forEach(list::add);
      } else {
        list.add(result);
      }
      return true;
    }
    return false;
  }
}

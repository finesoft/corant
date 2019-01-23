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
package org.corant.suites.query.esquery;

import static org.corant.shared.util.CollectionUtils.asList;
import static org.corant.shared.util.CollectionUtils.isEmpty;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * corant-suites-query
 *
 * @author bingo 下午3:40:59
 *
 */
public class ResultExtractor {

  public static void extractResult(Iterable<Object> result, String[] paths, boolean flatList,
      List<Object> list) {
    if (!interruptExtract(result, paths, flatList, list)) {
      for (Object next : result) {
        if (next != null) {
          extractResult(next, paths, flatList, list);
        }
      }
    }
  }

  public static void extractResult(Map<Object, Object> result, String[] paths, boolean flatList,
      List<Object> list) {
    if (!interruptExtract(result, paths, flatList, list)) {
      String path = paths[0];
      Object next = result.get(path);
      if (next != null) {
        extractResult(next, Arrays.copyOfRange(paths, 1, paths.length), flatList, list);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static void extractResult(Object result, String[] paths, boolean flatList,
      List<Object> list) {
    if (!interruptExtract(result, paths, flatList, list)) {
      if (result instanceof Map) {
        extractResult(Map.class.cast(result), paths, flatList, list);
      } else if (result instanceof Iterable) {
        extractResult(Iterable.class.cast(result), paths, flatList, list);
      } else if (result != null) {
        extractResult(asList((Object[]) result), paths, flatList, list);// may be array
      }
    }
  }

  public static boolean interruptExtract(Object result, String[] paths, boolean flatList,
      List<Object> list) {
    if (isEmpty(paths) || paths.length == 1) {
      if (result instanceof Iterable && flatList) {
        asList((Iterable<?>) result).forEach(list::add);
      } else {
        list.add(result);
      }
      return true;
    }
    return false;
  }

}

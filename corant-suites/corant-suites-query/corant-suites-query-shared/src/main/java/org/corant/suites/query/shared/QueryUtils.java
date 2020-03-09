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

import static org.corant.shared.util.StringUtils.split;
import java.util.List;
import java.util.Map;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Names;

/**
 * corant-suites-query
 *
 * @author bingo 上午10:54:15
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class QueryUtils {

  public static void extractMapValue(Object value, String paths, boolean flatList,
      List<Object> list) {
    extractMapValue(value, split(paths, Names.NAME_SPACE_SEPARATORS, true, false), 0, flatList,
        list);
  }

  public static void extractMapValue(Object value, String[] paths, boolean flatList,
      List<Object> list) {
    extractMapValue(value, paths, 0, flatList, list);
  }

  public static void extractMapValue(Object value, String[] paths, int deep, boolean flatList,
      List<Object> list) {
    if (paths.length > deep) {
      if (value instanceof Map) {
        final Object next;
        if ((next = ((Map) value).get(paths[deep])) != null) {
          extractMapValue(next, paths, deep + 1, flatList, list);
        }
      } else if (value instanceof Iterable) {
        for (Object next : (Iterable<?>) value) {
          if (next != null) {
            extractMapValue(next, paths, deep, flatList, list);
          }
        }
      } else if (value instanceof Object[]) {
        for (Object next : (Object[]) value) {
          extractMapValue(next, paths, deep, flatList, list);
        }
      } else {
        throw new NotSupportedException("We only extract value from map object");
      }
    } else {
      if (value instanceof Iterable && flatList) {
        for (Object next : (Iterable<?>) value) {
          list.add(next);
        }
      } else {
        list.add(value);
      }
    }
  }

  public static void implantMapValue(Map<String, Object> target, String paths, Object value) {
    implantMapValue(target, split(paths, Names.NAME_SPACE_SEPARATORS, true, false), 0, value);
  }

  public static void implantMapValue(Map<String, Object> target, String[] paths, int deep,
      Object value) {
    String key;
    if (target != null && paths.length > deep && target.containsKey(key = paths[deep])) {
      if (paths.length - deep == 1) {
        target.put(key, value);
      } else {
        Object tmp = target.get(key);
        int next = deep + 1;
        if (tmp instanceof Map) {
          implantMapValue((Map) tmp, paths, next, value);
        } else if (tmp instanceof Iterable) {
          for (Object item : (Iterable) tmp) {
            if (item instanceof Map) {
              implantMapValue((Map) item, paths, next, value);
            } else if (item != null) {
              throw new NotSupportedException("We only implant value to map object");
            }
          }
        } else if (tmp instanceof Object[]) {
          for (Object item : (Object[]) tmp) {
            if (item instanceof Map) {
              implantMapValue((Map) item, paths, next, value);
            } else if (item != null) {
              throw new NotSupportedException("We only implant value to map object");
            }
          }
        } else {
          throw new NotSupportedException("We only implant value to map object");
        }
      }
    }
  }

  public static void implantMapValue(Map<String, Object> target, String[] paths, Object value) {
    implantMapValue(target, paths, 0, value);
  }

  /*
   * public static void main(String... strings) { Map<String, Object> map = mapOf("a", mapOf("a1",
   * "a1_v", "a2", listOf(mapOf("a3_1", mapOf("a3_1_1", listOf(mapOf("a3_1_1_list_1", "a3_1_1_v1"),
   * mapOf("a3_1_1_list_2", "a3_1_1_v2"))), "a3_2", "a3_v2")).stream().toArray(Object[]::new)), "b",
   * 12); String key = "a.a2.a3_1.a3_1_1.a3_1_1_list_1"; List<Object> vl = new ArrayList<>();
   * System.out.println(JsonUtils.toString(map, true)); extractMapValue(map, key, true, vl);
   * System.out.println(JsonUtils.toString(vl, true)); vl.clear(); implantMapValue(map, split(key,
   * ".", true, true), 0, "bingo"); System.out.println("-----------------------------");
   * System.out.println(JsonUtils.toString(map, true)); extractMapValue(map, key, true, vl);
   * System.out.println(JsonUtils.toString(vl, true)); }
   */
}

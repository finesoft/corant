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
package org.corant.suites.query;

import static org.corant.shared.util.CollectionUtils.asList;
import static org.corant.shared.util.CollectionUtils.isEmpty;
import static org.corant.shared.util.MapUtils.getMapMap;
import static org.corant.shared.util.StringUtils.split;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.suites.query.mapping.FetchQuery;
import org.corant.suites.query.mapping.FetchQuery.FetchQueryParameterSource;

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
        extractResult(asList((Object[]) result), paths, flatList, list);// may be array
      }
    }
  }

  public static Map<String, Object> resolveFetchParam(Object obj, FetchQuery fetchQuery,
      Map<String, Object> param) {
    Map<String, Object> pmToUse = new HashMap<>();
    fetchQuery.getParameters().forEach(p -> {
      if (p.getSource() == FetchQueryParameterSource.P) {
        pmToUse.put(p.getName(), param.get(p.getSourceName()));
      } else if (obj != null) {
        if (obj instanceof Map) {
          String paramName = p.getName();
          String srcName = p.getSourceName();
          if (srcName.indexOf('.') != -1) {
            List<Object> srcVal = new ArrayList<>();
            extractResult(obj, srcName, true, srcVal);
            pmToUse.put(paramName,
                srcVal.isEmpty() ? null : srcVal.size() == 1 ? srcVal.get(0) : srcVal);
          } else {
            pmToUse.put(paramName, Map.class.cast(obj).get(srcName));
          }
        } else {
          try {
            pmToUse.put(p.getName(), BeanUtils.getProperty(obj, p.getSourceName()));
          } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new QueryRuntimeException(
                String.format("Can not extract value from query result for fetch query [%s] param!",
                    fetchQuery.getReferenceQuery()),
                e);
          }
        }
      }
    });
    return pmToUse;
  }

  @SuppressWarnings("unchecked")
  public static void resolveFetchResult(Object result, Object fetchedResult, String injectProName) {
    try {
      if (result instanceof Map) {
        if (injectProName.indexOf('.') != -1) {
          Map<Object, Object> mapResult = Map.class.cast(result);
          String proName = injectProName;
          String[] keys = split(injectProName, ".", true, false);
          for (String key : keys) {
            proName = key;
            if ((mapResult = getMapMap(mapResult, key)) == null) {
              break;
            }
          }
          if (mapResult != null) {
            mapResult.put(proName, fetchedResult);
          }
        } else {
          Map.class.cast(result).put(injectProName, fetchedResult);
        }
      } else if (result != null) {
        BeanUtils.setProperty(result, injectProName, fetchedResult);
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new QueryRuntimeException(e);
    }
  }

  static boolean interruptExtract(Object result, String[] paths, boolean flatList,
      List<Object> list) {
    if (isEmpty(paths)) {
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

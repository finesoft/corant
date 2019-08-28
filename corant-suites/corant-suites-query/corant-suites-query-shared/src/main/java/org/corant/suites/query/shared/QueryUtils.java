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
import static org.corant.shared.util.ConversionUtils.toBoolean;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getMapInteger;
import static org.corant.shared.util.MapUtils.putKeyPathMapValue;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines.ScriptFunction;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameterSource;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

/**
 * corant-suites-query
 *
 * @author bingo 上午10:54:15
 *
 */
public class QueryUtils {

  public static final String OFFSET_PARAM_NME = "_offset";
  public static final String LIMIT_PARAM_NME = "_limit";
  public static final int OFFSET_PARAM_VAL = 0;
  public static final int LIMIT_PARAM_VAL = 16;

  public static final ObjectMapper ESJOM = new ObjectMapper().registerModule(new JavaTimeModule())
          .registerModule(new SimpleModule()
          .addSerializer(new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE))
          .addSerializer(new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
          .addSerializer(new LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME)))
          .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
          .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);

  public static final ObjectMapper RCJOM =
      new ObjectMapper().registerModule(new JavaTimeModule()).registerModule(new SimpleModule())
              .enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
              .disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)
              .disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static <T> T converEsData(String str, Class<T> cls) {
    if (str == null) {
      return null;
    } else {
      try {
        return ESJOM.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(str, cls);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> convert(List<Map<String, Object>> result, Class<T> cls) {
    List<T> list = new ArrayList<>();
    if (!isEmpty(result)) {
      if (Map.class.isAssignableFrom(cls)) {
        for (Object r : result) {
          list.add((T) r);
        }
      } else {
        for (Map<String, Object> r : result) {
          list.add(convert(r, cls));
        }
      }
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  public static <T> T convert(Map<String, Object> result, Class<T> cls) {
    return result == null ? null
        : Map.class.isAssignableFrom(cls) ? (T) result : RCJOM.convertValue(result, cls);
  }

  public static <T> T convertEsData(Object data, Class<T> cls) {
    if (data == null) {
      return null;
    } else {
      return ESJOM.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .convertValue(data, cls);
    }
  }

  public static boolean decideFetch(Object obj, FetchQuery fetchQuery, Map<String, Object> param) {
    // precondition to decide whether execute fetch.
    if (isNotBlank(fetchQuery.getScript())) {
      ScriptFunction sf = NashornScriptEngines.compileFunction(fetchQuery.getScript(), "p", "r");
      if (sf != null) {
        Boolean b = toBoolean(sf.apply(new Object[] {param, obj}));
        if (b == null || !b.booleanValue()) {
          return false;
        }
      }
    }
    return true;
  }

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

  public static int getLimit(Map<String, Object> param) {
    return getMapInteger(param, LIMIT_PARAM_NME, LIMIT_PARAM_VAL);
  }

  public static int getOffset(Map<String, Object> param) {
    return getMapInteger(param, OFFSET_PARAM_NME, OFFSET_PARAM_VAL);
  }

  public static Map<String, Object> resolveFetchParam(Object obj, FetchQuery fetchQuery,
      Map<String, Object> param) {
    Map<String, Object> pmToUse = new HashMap<>();
    fetchQuery.getParameters().forEach(p -> {
      if (p.getSource() == FetchQueryParameterSource.C) {
        pmToUse.put(p.getName(), p.getValue());
      } else if (p.getSource() == FetchQueryParameterSource.P) {
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
    if (isBlank(injectProName)) {
      return;
    }
    if (result instanceof Map) {
      if (injectProName.indexOf('.') != -1) {
        Map<String, Object> mapResult = Map.class.cast(result);
        putKeyPathMapValue(mapResult, injectProName, ".", fetchedResult);
      } else {
        Map.class.cast(result).put(injectProName, fetchedResult);
      }
    } else if (result != null) {
      try {
        BeanUtils.setProperty(result, injectProName, fetchedResult);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new QueryRuntimeException(e);
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

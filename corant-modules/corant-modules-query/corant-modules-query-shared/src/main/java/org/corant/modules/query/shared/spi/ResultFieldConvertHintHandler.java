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
package org.corant.modules.query.shared.spi;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Primitives.wrapArray;
import static org.corant.shared.util.Sets.linkedHashSetOf;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isNoneBlank;
import static org.corant.shared.util.Strings.split;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.corant.context.service.ConversionService;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.QueryHint.QueryHintParameter;
import org.corant.modules.query.spi.ResultHintHandler;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Triple;

/**
 * corant-modules-query-shared
 *
 * <p>
 * The result field conversion hints.
 * <ul>
 * <li>The key is {@code result-field-convert}</li>
 * <li>The value of the parameter that named {@code field-name} is the field name that will be
 * convert.</li>
 * <li>The optional parameter value named {@code default-value} is used to use the value as the
 * field value when the field value is null</li>
 * <li>The value of the parameter that named {@code target-type} is the target class name that the
 * field value will be convert to.</li>
 * <li>The optional values of the parameter that named {@code convert-hint-key} and
 * {@code convert-hint-value} are the conversion service hints, use for intervene conversion
 * process.</li>
 * </ul>
 * </p>
 * <p>
 * Use case:
 *
 * <pre>
 * &lt;query name="QueryService.get" result-class="java.util.Map"&gt;
 *       &lt;script&gt;
 *           &lt;![CDATA[
 *               SELECT id,enum FROM Table
 *           ]]&gt;
 *       &lt;/script&gt;
 *       &lt;hint key="result-field-convert"&gt;
 *           &lt;parameter name="field-name" value="enum" /&gt;
 *           &lt;parameter name="default-value" value="TargetEnum.Test" /&gt;
 *           &lt;parameter name="target-type" value="xxx.xxx.TargetEnum" /&gt;
 *       &lt;/hint&gt;
 * &lt;/query&gt;
 * </pre>
 *
 * @see ConversionService
 * @see org.corant.shared.conversion.Conversion
 * @author bingo 下午12:02:08
 */
@ApplicationScoped
public class ResultFieldConvertHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-field-convert";
  public static final String HNIT_PARA_FIELD_NME = "field-name";
  public static final String HNIT_PARA_TARGET_TYP = "target-type";
  public static final String HNIT_PARA_DEFAULT_VAL = "default-value";
  public static final String HNIT_PARA_CVT_HIT_KEY = "convert-hint-key";
  public static final String HNIT_PARA_CVT_HIT_VAL = "convert-hint-value";

  // <QueryHint.id, <FieldPath, <TargetClass, <ConvertHintKey,ConvertHintValue>, DefaultValue>>>
  protected final Map<String, List<Pair<String[], Triple<Class<?>, Map<String, Object>, String>>>> caches =
      new ConcurrentHashMap<>();// static?
  protected final Set<String> brokens = new CopyOnWriteArraySet<>(); // static?

  @Inject
  protected Logger logger;

  @Inject
  protected ConversionService conversionService;

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void convertMapValue(Map<Object, Object> map, String[] keyPath,
      Function<Object, Object> func) {
    if (map == null || isEmpty(keyPath)) {
      return;
    }
    Map<Object, Object> useMap = map;
    int len = keyPath.length - 1;
    String putKey = keyPath[len];
    for (int i = 0; i < len; i++) {
      Object pathVal = useMap.get(keyPath[i]);
      if (pathVal instanceof Map) {
        useMap = (Map) pathVal;
      } else {
        useMap = null;
        if (pathVal instanceof Iterable) {
          String[] subKeyPath = Arrays.copyOfRange(keyPath, i + 1, keyPath.length);
          Iterable<?> pathVals = (Iterable) pathVal;
          for (Object ele : pathVals) {
            if (ele instanceof Map) {
              convertMapValue((Map) ele, subKeyPath, func);
            }
          }
        } else if (pathVal instanceof Object[]) {
          String[] subKeyPath = Arrays.copyOfRange(keyPath, i + 1, keyPath.length);
          for (Object ele : (Object[]) pathVal) {
            if (ele instanceof Map) {
              convertMapValue((Map) ele, subKeyPath, func);
            }
          }
        }
      }
      if (useMap == null) {
        break;
      }
    }
    if (useMap != null) {
      useMap.put(putKey, func.apply(useMap.get(putKey)));
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void handle(QueryHint qh, Query query, Object parameter, Object result) throws Exception {
    List<Pair<String[], Triple<Class<?>, Map<String, Object>, String>>> hints;
    if (brokens.contains(qh.getId()) || (hints = resolveHint(qh)) == null) {
      return;
    }
    if (result instanceof Map) {
      for (Pair<String[], Triple<Class<?>, Map<String, Object>, String>> hint : hints) {
        handle((Map) result, hint.getLeft(), hint.getRight().getLeft(), hint.getRight().getMiddle(),
            hint.getRight().getRight());
      }
    } else {
      List<?> list = null;
      if (result instanceof Forwarding) {
        list = ((Forwarding) result).getResults();
      } else if (result instanceof List) {
        list = (List) result;
      } else if (result instanceof Paging) {
        list = ((Paging) result).getResults();
      }
      if (!isEmpty(list)) {
        for (Object item : list) {
          if (item instanceof Map) {
            for (Pair<String[], Triple<Class<?>, Map<String, Object>, String>> hint : hints) {
              handle((Map) item, hint.getLeft(), hint.getRight().getLeft(),
                  hint.getRight().getMiddle(), hint.getRight().getRight());
            }
          }
        }
      }
    }
  }

  public List<Pair<String[], Triple<Class<?>, Map<String, Object>, String>>> resolveConversions(
      QueryHint qh) {
    List<QueryHintParameter> pnPs = qh.getParameters(HNIT_PARA_FIELD_NME);
    List<QueryHintParameter> ptPs = qh.getParameters(HNIT_PARA_TARGET_TYP);
    List<QueryHintParameter> dvPs = qh.getParameters(HNIT_PARA_DEFAULT_VAL);
    List<QueryHintParameter> pthk = qh.getParameters(HNIT_PARA_CVT_HIT_KEY);
    List<QueryHintParameter> pthv = qh.getParameters(HNIT_PARA_CVT_HIT_VAL);
    try {
      if (isNotEmpty(pnPs) && isNotEmpty(ptPs)) {
        String propertyName = defaultString(pnPs.get(0).getValue());
        String propertyType = defaultString(ptPs.get(0).getValue());
        if (isNoneBlank(propertyName, propertyType)) {
          Class<?> targetClass = tryAsClass(propertyType);
          String defaultValueParameter = isNotEmpty(dvPs) ? dvPs.get(0).getValue() : null;
          if (targetClass != null) {
            return resolveConversions(propertyName, targetClass, pthk, pthv, defaultValueParameter);
          }
        }
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, e, () -> "The query hint has some error!");
    }
    return null;
  }

  @Override
  public boolean supports(Class<?> resultClass, QueryHint hint) {
    return hint != null && areEqual(hint.getKey(), HINT_NAME);
  }

  protected <C extends Collection<Object>> C convertCollection(Collection<?> originalValue,
      Class<?> targetClass, IntFunction<C> collectionFactory, Map<String, Object> convertHits,
      String defaultValueParameter) {
    C results = collectionFactory.apply(originalValue.size());
    for (Object result : originalValue) {
      results.add(convertSingle(result, targetClass, convertHits, defaultValueParameter));
    }
    return results;
  }

  protected Object convertSingle(Object originalValue, Class<?> targetClass,
      Map<String, Object> convertHits, String defaultValueParameter) {
    Object converted = null;
    if (originalValue != null) {
      converted = conversionService.convert(targetClass, convertHits, originalValue);
    }
    if (converted == null && defaultValueParameter != null) {
      converted = conversionService.convert(targetClass, convertHits, defaultValueParameter);
    }
    return converted;
  }

  protected void handle(Map<Object, Object> map, String[] keyPath, Class<?> targetClass,
      Map<String, Object> convertHits, String defaultValueParameter) {
    convertMapValue(map, keyPath, originalValue -> {
      try {
        if (originalValue instanceof List<?> list) {
          return convertCollection(list, targetClass, ArrayList::new, convertHits,
              defaultValueParameter);
        } else if (originalValue instanceof Set<?> set) {
          return convertCollection(set, targetClass, LinkedHashSet::new, convertHits,
              defaultValueParameter);
        } else if (originalValue != null && originalValue.getClass().isArray()) {
          return convertCollection(listOf(wrapArray(originalValue)), targetClass, ArrayList::new,
              convertHits, defaultValueParameter);
        } else {
          return convertSingle(originalValue, targetClass, convertHits, defaultValueParameter);
        }
      } catch (Exception e) {
        logger.log(Level.WARNING, e,
            () -> format("Handle result conversion error on property %s with value %s.",
                String.join(".", keyPath), originalValue));
      }
      return originalValue;
    });
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    caches.clear();
    brokens.clear();
    logger.fine(() -> "Clear result field converter hint handler caches.");
  }

  protected List<Pair<String[], Triple<Class<?>, Map<String, Object>, String>>> resolveConversions(
      String propertyName, Class<?> targetClass, List<QueryHintParameter> pthk,
      List<QueryHintParameter> pthv, String defaultValueParameter) {
    final Map<String, Object> convertHints = isNotEmpty(pthk) && isNotEmpty(pthv)
        ? singletonMap(pthk.get(0).getValue(), pthv.get(0).getValue())
        : emptyMap();
    List<Pair<String[], Triple<Class<?>, Map<String, Object>, String>>> list = new ArrayList<>();
    linkedHashSetOf(split(propertyName, ",", true, true))
        .forEach(p -> list.add(Pair.of(split(p, ".", true, true),
            Triple.of(targetClass, convertHints, defaultValueParameter))));
    return list;
  }

  protected List<Pair<String[], Triple<Class<?>, Map<String, Object>, String>>> resolveHint(
      QueryHint qh) {
    if (caches.containsKey(qh.getId())) {
      return caches.get(qh.getId());
    } else {
      List<Pair<String[], Triple<Class<?>, Map<String, Object>, String>>> hit =
          resolveConversions(qh);
      if (hit != null) {
        return caches.computeIfAbsent(qh.getId(), k -> hit);
      }
    }
    brokens.add(qh.getId());
    return null;
  }
}

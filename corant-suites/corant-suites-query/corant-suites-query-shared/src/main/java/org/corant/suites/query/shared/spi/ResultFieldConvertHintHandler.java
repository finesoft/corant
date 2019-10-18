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
package org.corant.suites.query.shared.spi;

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import static org.corant.shared.util.StringUtils.split;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.kernel.api.ConversionService;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.shared.QueryService.ForwardList;
import org.corant.suites.query.shared.QueryService.PagedList;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.mapping.QueryHint.QueryHintParameter;

/**
 * corant-suites-query
 *
 * <p>
 * The result field conversion hints.
 * <li>The key is 'result-field-convert'</li>
 * <li>The value of the parameter that named 'field-name' is the field name that will be
 * convert.</li>
 * <li>The value of the parameter that named 'target-type' is the target class name that the field
 * value will be convert to.</li>
 * <li>The values of the parameter that named 'convert-hint-key' and 'convert-hint-value' are the
 * conversion service hints, use for intervene conversion process.</li>
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
 *           &lt;parameter name="target-type" value="xxx.xxx.TargetEnum" /&gt;
 *       &lt;/hint&gt;
 * &lt;/query&gt;
 * </pre>
 * </p>
 *
 * @see ConversionService
 * @see org.corant.shared.conversion.Conversions
 * @author bingo 下午12:02:08
 *
 */
@ApplicationScoped
public class ResultFieldConvertHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-field-convert";
  public static final String HNIT_PARA_FIELD_NME = "field-name";
  public static final String HNIT_PARA_TARGET_TYP = "target-type";
  public static final String HNIT_PARA_CVT_HIT_KEY = "convert-hint-key";
  public static final String HNIT_PARA_CVT_HIT_VAL = "convert-hint-value";

  static final Map<String, Pair<String[], Pair<Class<?>, Object[]>>> caches =
      new ConcurrentHashMap<>();
  static final Set<String> brokens = new CopyOnWriteArraySet<>();

  @Inject
  Logger logger;

  @Inject
  ConversionService conversionService;

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

  @Override
  public boolean canHandle(Class<?> resultClass, QueryHint hint) {
    return conversionService != null && hint != null && isEquals(hint.getKey(), HINT_NAME);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void handle(QueryHint qh, Object parameter, Object result) throws Exception {
    Pair<String[], Pair<Class<?>, Object[]>> hint = null;
    if (brokens.contains(qh.getId()) || (hint = resolveHint(qh)) == null) {
      return;
    }
    if (result instanceof Map) {
      handle((Map) result, hint.getLeft(), hint.getRight().getKey(), hint.getRight().getRight());
    } else {
      List<?> list = null;
      if (result instanceof ForwardList) {
        list = ((ForwardList) result).getResults();
      } else if (result instanceof List) {
        list = (List) result;
      } else if (result instanceof PagedList) {
        list = ((PagedList) result).getResults();
      }
      if (!isEmpty(list)) {
        for (Object item : list) {
          if (item instanceof Map) {
            handle((Map) item, hint.getLeft(), hint.getRight().getKey(),
                hint.getRight().getRight());
          }
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected void handle(Map<Object, Object> map, String[] keyPath, Class<?> targetClass,
      Object[] convertHits) {
    convertMapValue(map, keyPath, (orginalVal) -> {
      if (orginalVal != null) {
        try {
          if (orginalVal instanceof List) {
            return conversionService.convert(orginalVal, List.class, targetClass, convertHits);
          } else if (orginalVal instanceof Set) {
            return conversionService.convert(orginalVal, Set.class, targetClass, convertHits);
          } else if (orginalVal instanceof Object[]) {
            return conversionService.convert(orginalVal, List.class, targetClass, convertHits);
          } else {
            return conversionService.convert(orginalVal, targetClass, convertHits);
          }
        } catch (Exception e) {
          logger.log(Level.WARNING, e,
              () -> String.format("Hanle result conversion error on property %s with value %s",
                  String.join(".", keyPath), orginalVal));
        }
      }
      return orginalVal;
    });
  }

  protected Pair<String[], Pair<Class<?>, Object[]>> resolveHint(QueryHint qh) {
    if (caches.containsKey(qh.getId())) {
      return caches.get(qh.getId());
    } else {
      List<QueryHintParameter> pnPs = qh.getParameters(HNIT_PARA_FIELD_NME);
      List<QueryHintParameter> ptPs = qh.getParameters(HNIT_PARA_TARGET_TYP);
      List<QueryHintParameter> pthk = qh.getParameters(HNIT_PARA_CVT_HIT_KEY);
      List<QueryHintParameter> pthv = qh.getParameters(HNIT_PARA_CVT_HIT_VAL);
      try {
        if (isNotEmpty(pnPs) && isNotEmpty(ptPs)) {
          String propertyName = defaultString(pnPs.get(0).getValue());
          String propertyType = defaultString(ptPs.get(0).getValue());
          if (isNoneBlank(propertyName, propertyType)) {
            Class<?> targetClass = tryAsClass(propertyType);
            if (targetClass != null) {
              Pair<Class<?>, Object[]> converterParam = Pair.of(targetClass, new Object[0]);
              return caches.computeIfAbsent(qh.getId(),
                  (k) -> Pair.of(split(propertyName, ".", true, true),
                      isNotEmpty(pthk) && isNotEmpty(pthv)
                          ? converterParam.withRight(
                              new Object[] {pthk.get(0).getValue(), pthv.get(0).getValue()})
                          : converterParam));
            }
          }
        }
      } catch (Exception e) {
        logger.log(Level.WARNING, e, () -> "The query hint has some error!");
      }
    }
    brokens.add(qh.getId());
    return null;
  }
}

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
package org.corant.asosat.ddd.application.query;

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.MapUtils.replaceKeyPathMapValue;
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isNoneBlank;
import static org.corant.shared.util.StringUtils.split;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.kernel.service.ConversionService;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.ddd.annotation.stereotype.ApplicationServices;
import org.corant.suites.query.Query.ForwardList;
import org.corant.suites.query.Query.PagedList;
import org.corant.suites.query.mapping.QueryHint;
import org.corant.suites.query.mapping.QueryHint.QueryHintParameter;
import org.corant.suites.query.spi.ResultHintHandler;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午12:02:08
 *
 */
@ApplicationScoped
@ApplicationServices
public class ConverterHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-convert";
  public static final String HNIT_PARA_PRO_NME = "property-name";
  public static final String HNIT_PARA_PRO_TYP = "property-type";
  public static final String HNIT_PARA_CVT_HIT_KEY = "convert-hint-key";
  public static final String HNIT_PARA_CVT_HIT_VAL = "convert-hint-value";

  static final Map<QueryHint, Pair<String[], Pair<Class<?>, Object[]>>> caches =
      new ConcurrentHashMap<>();
  static final Set<QueryHint> brokens = new CopyOnWriteArraySet<>();

  @Inject
  Logger logger;

  @Inject
  ConversionService cs;

  @Override
  public boolean canHandle(QueryHint hint) {
    return hint != null && isEquals(hint.getKey(), HINT_NAME);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(QueryHint qh, Object result) throws Exception {
    Pair<String[], Pair<Class<?>, Object[]>> hint = null;
    if (brokens.contains(qh) || (hint = resolveHint(qh)) == null) {
      return;
    }
    if (result instanceof Map) {
      handle(Map.class.cast(result), hint.getLeft(), hint.getRight().getKey(),
          hint.getRight().getRight());
    } else {
      List<?> list = null;
      if (result instanceof ForwardList) {
        list = ForwardList.class.cast(result).getResults();
      } else if (result instanceof List) {
        list = List.class.cast(result);
      } else if (result instanceof PagedList) {
        list = PagedList.class.cast(result).getResults();
      }
      if (!isEmpty(list)) {
        for (Object item : list) {
          if (item instanceof Map) {
            handle(Map.class.cast(item), hint.getLeft(), hint.getRight().getKey(),
                hint.getRight().getRight());
          }
        }
      }
    }
  }

  protected void handle(Map<String, Object> map, String[] keyPath, Class<?> targetClass,
      Object[] convertHits) {
    replaceKeyPathMapValue(map, keyPath, (orginalVal) -> {
      if (orginalVal != null) {
        try {
          return cs.convert(orginalVal, targetClass, convertHits);
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
    if (caches.containsKey(qh)) {
      return caches.get(qh);
    } else {
      List<QueryHintParameter> pnPs = qh.getParameters(HNIT_PARA_PRO_NME);
      List<QueryHintParameter> ptPs = qh.getParameters(HNIT_PARA_PRO_TYP);
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
              return caches.computeIfAbsent(qh,
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
    brokens.add(qh);
    return null;
  }
}

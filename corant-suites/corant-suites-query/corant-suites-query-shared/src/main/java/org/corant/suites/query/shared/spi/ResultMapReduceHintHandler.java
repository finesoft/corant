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

import static org.corant.shared.util.CollectionUtils.linkedHashSetOf;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.query.shared.QueryService.Forwarding;
import org.corant.suites.query.shared.QueryService.Paging;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.mapping.QueryHint.QueryHintParameter;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午7:53:36
 *
 */
@ApplicationScoped
@SuppressWarnings({"unchecked", "rawtypes"})
public class ResultMapReduceHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-map-reduce";
  public static final String HNIT_PARA_REDUCE_FIELD_NME = "reduce-field-names";
  public static final String HNIT_PARA_MAP_FIELD_NME = "map-field-name";
  public static final String HINT_PARA_MAP_TYP_NME = "java.util.Map";

  static final Map<String, Consumer<Map>> caches = new ConcurrentHashMap<>();
  static final Set<String> brokens = new CopyOnWriteArraySet<>();

  @Inject
  Logger logger;

  @Override
  public boolean canHandle(Class<?> resultClass, QueryHint hint) {
    return (resultClass == null || Map.class.isAssignableFrom(resultClass)) && hint != null
        && isEquals(hint.getKey(), HINT_NAME);
  }

  @Override
  public void handle(QueryHint qh, Object parameter, Object result) throws Exception {
    Consumer<Map> handler = null;
    if (brokens.contains(qh.getId()) || (handler = resolveHint(qh)) == null) {
      return;
    }
    if (result instanceof Map) {
      handler.accept((Map) result);
    } else {
      List<Map<?, ?>> list = null;
      if (result instanceof Forwarding) {
        list = ((Forwarding) result).getResults();
      } else if (result instanceof List) {
        list = (List) result;
      } else if (result instanceof Paging) {
        list = ((Paging) result).getResults();
      }
      if (!isEmpty(list)) {
        for (Map map : list) {
          handler.accept(map);
        }
      }
    }
  }

  protected Consumer<Map> resolveHint(QueryHint qh) {
    if (caches.containsKey(qh.getId())) {
      return caches.get(qh.getId());
    } else {
      List<QueryHintParameter> mapFieldNameParams = qh.getParameters(HNIT_PARA_MAP_FIELD_NME);
      List<QueryHintParameter> reduceFieldNamesParams =
          qh.getParameters(HNIT_PARA_REDUCE_FIELD_NME);
      // List<QueryHintParameter> mapFieldTypeParams = qh.getParameters(HNIT_PARA_REDUCE_FIELD_NME);
      try {
        Set<String> reduceFieldNames;
        String mapFieldName = null;
        if (isNotEmpty(reduceFieldNamesParams) && isNotEmpty(mapFieldNameParams)
            && isNotBlank(mapFieldName = mapFieldNameParams.get(0).getValue())
            && isNotEmpty(reduceFieldNames = linkedHashSetOf(
                split(reduceFieldNamesParams.get(0).getValue(), ",", true, true)))) {
          final String useMapFieldName = mapFieldName;
          return caches.computeIfAbsent(qh.getId(), k -> map -> {
            Map<String, Object> obj = new HashMap<>();
            for (String rfn : reduceFieldNames) {
              obj.put(rfn, map.remove(rfn));
            }
            map.put(useMapFieldName, obj);
          });
        }
      } catch (Exception e) {
        logger.log(Level.WARNING, e, () -> "The query hint has some error!");
      }
    }
    brokens.add(qh.getId());
    return null;
  }
}

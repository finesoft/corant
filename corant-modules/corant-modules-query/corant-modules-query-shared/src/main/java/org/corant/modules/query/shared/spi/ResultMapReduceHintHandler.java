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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.extractMapKeyPathValue;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Sets.linkedHashSetOf;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.QueryHint.QueryHintParameter;
import org.corant.modules.query.spi.ResultHintHandler;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-query-shared
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

  protected final Map<String, Consumer<Map>> caches = new ConcurrentHashMap<>();// static?
  protected final Set<String> brokens = new CopyOnWriteArraySet<>();// static?

  @Inject
  protected Logger logger;

  @Override
  public void handle(QueryHint qh, Query query, Object parameter, Object result) throws Exception {
    Consumer<Map> handler;
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

  @Override
  public boolean supports(Class<?> resultClass, QueryHint hint) {
    return (resultClass == null || Map.class.isAssignableFrom(resultClass)) && hint != null
        && areEqual(hint.getKey(), HINT_NAME);
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    caches.clear();
    brokens.clear();
    logger.fine(() -> "Clear result map reduce hint handler caches.");
  }

  protected Consumer<Map> resolveHint(QueryHint qh) {
    if (caches.containsKey(qh.getId())) {
      return caches.get(qh.getId());
    } else {
      try {
        final String mapFieldName = resolveMapFieldname(qh);
        final List<Pair<String, String[]>> reduceFields = resolveReduceFields(qh);
        if (isNotEmpty(reduceFields) && isNotBlank(mapFieldName)) {
          return caches.computeIfAbsent(qh.getId(), k -> map -> {
            Map<String, Object> obj = new HashMap<>();
            for (Pair<String, String[]> rfn : reduceFields) {
              obj.put(rfn.getLeft(), extractMapKeyPathValue(map, rfn.getRight()));
            }
            map.put(mapFieldName, obj);
          });
        }
      } catch (Exception e) {
        logger.log(Level.WARNING, e, () -> "The query hint has some error!");
      }
    }
    brokens.add(qh.getId());
    return null;
  }

  protected String resolveMapFieldname(QueryHint qh) {
    List<QueryHintParameter> params = qh.getParameters(HNIT_PARA_MAP_FIELD_NME);
    return isNotEmpty(params) ? params.get(0).getValue() : null;
  }

  protected List<Pair<String, String[]>> resolveReduceFields(QueryHint qh) {
    List<Pair<String, String[]>> fields = new ArrayList<>();
    List<QueryHintParameter> params = qh.getParameters(HNIT_PARA_REDUCE_FIELD_NME);
    if (isNotEmpty(params)) {
      linkedHashSetOf(split(params.get(0).getValue(), ",", true, true)).forEach(fn -> {
        String[] pathAndKey = split(fn, Names.DOMAIN_SPACE_SEPARATORS, true, true);
        if (pathAndKey.length > 1) {
          fields.add(Pair.of(pathAndKey[1], split(pathAndKey[0], ".", true, true)));
        } else if (pathAndKey.length > 0) {
          fields.add(Pair.of(pathAndKey[0], split(pathAndKey[0], ".", true, true)));
        }
      });
    }
    return fields;
  }
}

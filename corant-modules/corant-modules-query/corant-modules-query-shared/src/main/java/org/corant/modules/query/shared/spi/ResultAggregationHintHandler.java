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
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Sets.linkedHashSetOf;
import static org.corant.shared.util.Strings.defaultStrip;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-query-shared
 *
 * <p>
 * The simple result aggregation hints.
 * <ul>
 * <li>The key is 'result-aggregation'</li>
 * <li>The value of the parameter that named 'aggs-field-names' are the reserved field names during
 * aggregation; if the first character of this parameter is'!', it means the names of the fields
 * that will be aggregated during aggregation.</li>
 * <li>The value of the parameter that named 'aggs-name' is the name of the field that stores the
 * aggregation result. The aggregated objects are generally placed in a container object (usually a
 * List).</li>
 * </ul>
 * </p>
 * <p>
 * Use case:
 *
 * <pre>
 * &lt;query name="QueryService.get" result-class="java.util.Map"&gt;
 *       &lt;script&gt;
 *           &lt;![CDATA[
 *               SELECT o.id,o.name,m.f1,m.f2 FROM ONE o LEFT JOIN MANY m ON o.id = m.oId
 *           ]]&gt;
 *       &lt;/script&gt;
 *       &lt;hint key="result-aggregation"&gt;
 *           &lt;parameter name="aggs-field-names" value="id,name" /&gt;
 *           &lt;parameter name="aggs-name" value="list" /&gt;
 *       &lt;/hint&gt;
 * &lt;/query&gt;
 * </pre>
 *
 * <pre>
 * Use case explain:
 *
 *      the query results before aggregation like below:
 *
 *          record1: {"id":1, "name":"a", "f1":"1", "f2":"1"}
 *          record2: {"id":1, "name":"a", "f1":"2", "f2":"2"}
 *          record3: {"id":2, "name":"b", "f1":"3", "f2":"3"}
 *          record4: {"id":2, "name":"b", "f1":"4", "f2":"4"}
 *
 *      the query results after aggregation like below:
 *
 *          record1: {"id":1, "name":"a", list:[{"f1":"1", "f2":"1"},{"f1":"2", "f2":"2"}]}
 *          record2: {"id":2, "name":"b", list:[{"f1":"3", "f2":"3"},{"f1":"4", "f2":"4"}]}
 *
 * </pre>
 * </p>
 *
 * @author bingo 下午12:02:08
 *
 */
@ApplicationScoped
public class ResultAggregationHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-aggregation";
  public static final String HNIT_PARA_AGGS_FIELD_NME = "aggs-field-names";
  public static final String HNIT_PARA_AGGS_NME = "aggs-name";

  protected final Map<String, Consumer<List<Map<?, ?>>>> caches = new ConcurrentHashMap<>();// static?
  protected final Set<String> brokens = new CopyOnWriteArraySet<>();

  @Inject
  protected Logger logger;

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void handle(QueryHint qh, Query query, Object parameter, Object result) throws Exception {
    Consumer<List<Map<?, ?>>> handler;
    if (brokens.contains(qh.getId()) || (handler = resolveHint(qh)) == null) {
      return;
    }
    List<Map<?, ?>> list = null;
    if (result instanceof Forwarding) {
      list = ((Forwarding) result).getResults();
    } else if (result instanceof List) {
      list = (List) result;
    } else if (result instanceof Paging) {
      list = ((Paging) result).getResults();
    }
    if (!isEmpty(list)) {
      handler.accept(list);
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
    logger.fine(() -> "Clear result aggregation hint handler caches.");
  }

  protected Pair<Boolean, Set<String>> resolveAggFieldNames(
      List<QueryHintParameter> aggFieldNames) {
    if (isEmpty(aggFieldNames)) {
      return Pair.empty();
    } else {
      String names = defaultStrip(aggFieldNames.get(0).getValue());
      boolean exclude = !names.isEmpty() && names.charAt(0) == '!';
      if (exclude) {
        names = names.substring(1);
      }
      return Pair.of(exclude, linkedHashSetOf(split(names, ",", true, true)));
    }
  }

  protected Consumer<List<Map<?, ?>>> resolveHint(QueryHint qh) {
    if (caches.containsKey(qh.getId())) {
      return caches.get(qh.getId());
    } else {
      List<QueryHintParameter> aggNames = qh.getParameters(HNIT_PARA_AGGS_NME);
      List<QueryHintParameter> aggFieldNames = qh.getParameters(HNIT_PARA_AGGS_FIELD_NME);
      try {
        Pair<Boolean, Set<String>> fieldNames = resolveAggFieldNames(aggFieldNames);
        String aggName;
        if (!fieldNames.isEmpty() && isNotEmpty(aggNames)
            && isNotBlank(aggName = aggNames.get(0).getValue())) {
          final String useAggName = aggName;
          return caches.computeIfAbsent(qh.getId(), k -> list -> {
            Map<Map<Object, Object>, List<Map<Object, Object>>> temp =
                new LinkedHashMap<>(list.size());
            for (Map<?, ?> src : list) {
              Map<Object, Object> key = new LinkedHashMap<>();
              Map<Object, Object> val = new LinkedHashMap<>();
              for (Map.Entry<?, ?> e : src.entrySet()) {
                if (fieldNames.getKey()) {
                  if (fieldNames.getValue().contains(e.getKey())) {
                    val.put(e.getKey(), e.getValue());
                  } else {
                    key.put(e.getKey(), e.getValue());
                  }
                } else if (!fieldNames.getValue().contains(e.getKey())) {
                  val.put(e.getKey(), e.getValue());
                } else {
                  key.put(e.getKey(), e.getValue());
                }
              }
              temp.computeIfAbsent(key, vk -> new ArrayList<>()).add(val);
            }
            list.clear();
            for (Entry<Map<Object, Object>, List<Map<Object, Object>>> e : temp.entrySet()) {
              e.getKey().put(useAggName, e.getValue());
              list.add(e.getKey());
            }
            temp.clear();
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

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

import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.extractMapKeyPathValue;
import static org.corant.shared.util.Maps.getMapKeyPathValue;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Sets.linkedHashSetOf;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import static org.corant.shared.util.Strings.strip;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import org.corant.shared.ubiquity.Tuple.Triple;
import org.corant.shared.util.Classes;
import org.corant.shared.util.Objects;

/**
 * corant-modules-query-shared
 *
 * <p>
 * The simple result map reduce hints.
 * <ul>
 * <li>The key is 'result-map-reduce'</li>
 * <li>The value of the parameter that named 'reduce-field-names' are the reduce field names during
 * reduction.</li>
 * <li>Multiple reduce field names use ',' to split, and if the field name end with ':?1@?2', the
 * '?1' is the projection name, '?2' is the projection type class name, both '?1' and '?2' are
 * optional.</li>
 * <li>The value of the parameter named 'retain-reduce-fields' is used to keep the reduced fields,
 * default is false.</li>
 * <li>The value of the parameter named 'nullable-reduce' is used to determine whether or not to
 * keep the reduced object. If 'nullable-reduce' is false, a null value is reduced if all mapped
 * fields are null, otherwise the reduced object is returned, but all properties of the object are
 * null, default is true.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Use case:
 *
 * <pre>
* &lt;query name="QueryService.get" result-class="java.util.Map"&gt;
*       &lt;script&gt;
*           &lt;![CDATA[
*               SELECT id, name, binId, binName FROM Table
*           ]]&gt;
*       &lt;/script&gt;
*        &lt;hint key="result-map-reduce"
*         &lt;parameter name="reduce-field-names" value="binId:id,binName:name" /&gt;
*         &lt;parameter name="map-field-name" value="bin" /&gt;
*         &lt;parameter name="retain-reduce-fields" value="false" /&gt;
*       &lt;/hint&gt;
* &lt;/query&gt;
 * </pre>
 *
 * <pre>
 * Use case explain:
 *
 *      the query results before map-reduce like below:
 *
 *          record1: {"id":1, "name":"a", "binId":"1", "binName":"one"}
 *          record1: {"id":2, "name":"b", "binId":"2", "binName":"two"}
 *          record1: {"id":3, "name":"c", "binId":null, "binName":null}
 *
 *      the query results after map-reduce like below:
 *
 *          record1: {"id":1, "name":"a", bin:{"id":"1", "name":"one"}}
 *          record2: {"id":2, "name":"b", bin:{"id":"2", "name":"two"}}
 *          record2: {"id":3, "name":"c", bin:{"id":null, "name":null}}
 *
 *      and if the hint parameter "retain-reduce-fields" is true:
 *
 *          record1: {"id":1, "name":"a", "binId":"1", "binName":"one", bin:{"id":"1", "name":"one"}}
 *          record2: {"id":2, "name":"b", "binId":"2", "binName":"two", bin:{"id":"2", "name":"two"}}
 *          record2: {"id":3, "name":"c", "binId":null, "binName":null, bin:{"id":null, "name":null}}
 *
 *      or if the hint parameter "reduce-field-names" contains ':' or '@', for example:
 *
 *          &lt;parameter name="reduce-field-names" value="binId:id@java.lang.Integer, binName:name" /&gt; the results:
 *
 *          record1: {"id":1, "name":"a", bin:{"binId":1, "binName":"one"}}
 *          record2: {"id":2, "name":"b", bin:{"binId":2, "binName":"two"}}
 *          record2: {"id":3, "name":"c", bin:{"binId":null, "binName":null}}
 *
 *      or if the hint parameter "nullable-reduce" is false:
 *
 *          record1: {"id":1, "name":"a", "binId":"1", "binName":"one", bin:{"id":"1", "name":"one"}}
 *          record2: {"id":2, "name":"b", "binId":"2", "binName":"two", bin:{"id":"2", "name":"two"}}
 *          record2: {"id":3, "name":"c", "binId":null, "binName":null, bin:null}
 * </pre>
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
  public static final String HINT_PARA_RETAIN_FEILDS = "retain-reduce-fields";
  public static final String HINT_PARA_NULLABLE_REDUCE = "nullable-reduce";
  public static final String TYPE_VALUE_PREFIX = "@";
  public static final char TYPE_VALUE_PREFIX_SIGN = '@';

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
        // Triple <ProjectName, fieldNamePath, ProjectTypeClass>
        final List<Triple<String, String[], Class<?>>> reduceFields = resolveReduceFields(qh);
        final boolean retainFields = resolveRetainFields(qh);
        final boolean nullableReduce = resolveNullableReduce(qh);
        if (isNotEmpty(reduceFields) && isNotBlank(mapFieldName)) {
          return caches.computeIfAbsent(qh.getId(), k -> map -> {
            Map<String, Object> obj = new LinkedHashMap<>();
            if (retainFields) {
              for (Triple<String, String[], Class<?>> rfn : reduceFields) {
                obj.put(rfn.getLeft(),
                    rfn.right() != null ? getMapKeyPathValue(map, rfn.getMiddle(), rfn.right())
                        : getMapKeyPathValue(map, rfn.getMiddle()));
              }
            } else {
              for (Triple<String, String[], Class<?>> rfn : reduceFields) {
                obj.put(rfn.getLeft(),
                    rfn.right() != null ? extractMapKeyPathValue(map, rfn.getMiddle(), rfn.right())
                        : extractMapKeyPathValue(map, rfn.getMiddle()));
              }
            }
            if (!nullableReduce && obj.values().stream().allMatch(Objects::isNull)) {
              map.put(mapFieldName, null);
            } else {
              map.put(mapFieldName, obj);
            }
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

  protected boolean resolveNullableReduce(QueryHint qh) {
    List<QueryHintParameter> params = qh.getParameters(HINT_PARA_NULLABLE_REDUCE);
    if (isNotEmpty(params)) {
      return true;
    }
    return toBoolean(params.get(0).getValue());
  }

  protected Triple<String, String[], Class<?>> resolveReduceField(String fieldExp) {
    if (!fieldExp.contains(Names.DOMAIN_SPACE_SEPARATORS)
        && !fieldExp.contains(TYPE_VALUE_PREFIX)) {
      // System.out.println(fieldExp);
      return Triple.of(fieldExp, Names.splitNameSpace(fieldExp, true, false),
          java.lang.Object.class);
    } else {
      int e = -1;
      int pps = -1;
      int ppe = -1;
      int cps = -1;
      int cpe = -1;
      int len = fieldExp.length();
      for (int i = 0; i < len; i++) {
        char c = fieldExp.charAt(i);
        if (c == Names.DOMAIN_SPACE_SEPARATOR) {
          if (e == -1) {
            e = i;
          }
          if (pps == -1) {
            pps = ppe = i + 1;
            for (; ppe < len; ppe++) {
              c = fieldExp.charAt(ppe);
              if (c == TYPE_VALUE_PREFIX_SIGN || c == Names.DOMAIN_SPACE_SEPARATOR) {
                break;
              }
            }
          }
        } else if (c == TYPE_VALUE_PREFIX_SIGN) {
          if (e == -1) {
            e = i;
          }
          if (cps == -1) {
            cps = cpe = i + 1;
            for (; cpe < len; cpe++) {
              c = fieldExp.charAt(cpe);
              if (c == Names.DOMAIN_SPACE_SEPARATOR || c == TYPE_VALUE_PREFIX_SIGN) {
                break;
              }
            }
          }
        }
      }
      String fieldName = fieldExp.substring(0, e);
      String projectName = ppe > pps ? fieldExp.substring(pps, ppe) : fieldName;
      String className = cpe > cps ? fieldExp.substring(cps, cpe) : null;
      return Triple.of(projectName, Names.splitNameSpace(fieldName, true, false),
          isBlank(className) ? null : Classes.asClass(strip(className)));
    }
  }

  protected List<Triple<String, String[], Class<?>>> resolveReduceFields(QueryHint qh) {
    // Triple <ProjectName, fieldNamePath, ProjectTypeClass>
    List<Triple<String, String[], Class<?>>> fields = new ArrayList<>();
    List<QueryHintParameter> params = qh.getParameters(HNIT_PARA_REDUCE_FIELD_NME);
    if (isNotEmpty(params)) {
      linkedHashSetOf(split(params.get(0).getValue(), ",", true, true)).stream()
          .map(this::resolveReduceField).forEach(fields::add);
    }
    return fields;
  }

  protected boolean resolveRetainFields(QueryHint qh) {
    List<QueryHintParameter> params = qh.getParameters(HINT_PARA_RETAIN_FEILDS);
    return isNotEmpty(params) ? toBoolean(params.get(0).getValue()) : false;
  }
}

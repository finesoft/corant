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
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.newHashMap;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.mapping.QueryHint.QueryHintParameter;
import org.corant.modules.query.spi.ResultHintHandler;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-shared
 *
 * <p>
 * The result bean mapper hints, use bean to intervene the result.
 * </p>
 * <ul>
 * <li>The key is {@code result-bean-mapper}</li>
 * <li>All beans must implement {@link ResultBeanMapper} with a {@link Named} qualifier and beans
 * scope are {@link ApplicationScoped}. The value of the {@link Named} annotation corresponds to the
 * hint parameter {@code named}, This annotation is used to distinguish bean instances at
 * runtime.</li>
 * </ul>
 *
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
 *       &lt;hint key="result-bean-mapper"&gt;
 *            &lt;parameter name="named" value="theName" /&gt;
 *       &lt;/hint&gt;
 * &lt;/query&gt;
 *
 * &#64;ApplicationScope
 * &#64;Named("theName")
 * public class MyResultBeanMapper implement ResultBeanMapper{
 *   public void accept(Object queryParameter,List<Map<?,?> result){
 *     //TODO
 *   }
 * }
 * </pre>
 * </p>
 *
 * @see ResultScriptMapperHintHandler
 *
 * @author bingo 下午12:02:08
 */
@ApplicationScoped
public class ResultBeanMapperHintHandler implements ResultHintHandler {

  public static final String HINT_NAME = "result-bean-mapper";
  public static final String HINT_PARA_BEAN_NME = "named";

  protected final Map<QueryHint, Named> nameds = new ConcurrentHashMap<>();
  protected final Map<QueryHint, Map<String, Object>> extraParams = new ConcurrentHashMap<>();

  @Inject
  protected Logger logger;

  @Inject
  @Any
  protected Instance<ResultBeanMapper> instances;

  public static Named resolveNamedQualifier(QueryHint qh) {
    List<QueryHintParameter> nameds = qh.getParameters(HINT_PARA_BEAN_NME);
    String named = null;
    if (isNotEmpty(nameds)) {
      named = defaultString(nameds.get(0).getValue(), null);
    }
    return named != null ? NamedLiteral.of(named) : null;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void handle(QueryHint qh, Query query, Object parameter, Object result) throws Exception {
    List<Map<Object, Object>> list = null;
    if (result instanceof Map) {
      list = listOf((Map) result);
    } else if (result instanceof Forwarding) {
      list = ((Forwarding) result).getResults();
    } else if (result instanceof List) {
      list = (List) result;
    } else if (result instanceof Paging) {
      list = ((Paging) result).getResults();
    }
    if (!isEmpty(list)) {
      List<ResultBeanMapper> mappers = resolveBeanMapper(resolveBeanNamed(qh));
      if (mappers.isEmpty()) {
        return;
      }
      for (ResultBeanMapper mapper : mappers) {
        mapper.accept(query, parameter, resolveExtraParams(qh), list);
      }
    }
  }

  @Override
  public boolean supports(Class<?> resultClass, QueryHint hint) {
    boolean can = hint != null && areEqual(hint.getKey(), HINT_NAME) && !instances.isUnsatisfied();
    if (can) {
      Named named = resolveBeanNamed(hint);
      can = named != null && !instances.select(named).isUnsatisfied();
      if (!can && named != null) {
        logger.warning(format("Can't find any result bean mapper named %s", named.value()));
      }
    }
    return can;
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    nameds.clear();
    extraParams.clear();
    logger.fine(() -> "Clear result bean mapper hint handler caches.");
  }

  protected List<ResultBeanMapper> resolveBeanMapper(Annotation named) {
    if (named != null && !instances.isUnsatisfied()) {
      Instance<ResultBeanMapper> matched = instances.select(named);
      if (!matched.isUnsatisfied()) {
        return matched.stream().sorted(Sortable::compare).collect(Collectors.toList());
      }
    }
    return Collections.emptyList();
  }

  protected Named resolveBeanNamed(QueryHint qh) {
    return nameds.computeIfAbsent(qh, k -> resolveNamedQualifier(qh));
  }

  protected Map<String, Object> resolveExtraParams(QueryHint qh) {
    return extraParams.computeIfAbsent(qh, h -> {
      Map<String, List<QueryHintParameter>> params = newHashMap(qh.getParameters());
      params.remove(HINT_PARA_BEAN_NME);
      Map<String, Object> map = new HashMap<>();
      params.forEach((k, v) -> {
        Object value = null;
        int size = sizeOf(v);
        if (size == 1) {
          if (v.get(0) != null) {
            value = toObject(v.get(0).getValue(), v.get(0).getType());
          }
        } else if (size > 0) {
          value =
              v.stream().map(e -> toObject(e.getValue(), e.getType())).collect(Collectors.toList());
        }
        map.put(k, value);
      });
      return Collections.unmodifiableMap(map);
    });
  }

  /**
   * corant-modules-query-shared
   *
   * @author bingo 上午11:33:20
   */
  @FunctionalInterface
  public interface ResultBeanMapper extends Sortable {

    /**
     * Handle query result hint
     *
     * @param query the query object
     * @param queryParameter the parameters of the query corresponding to hint when executing the
     *        query
     * @param extraParameters the extra parameters from query configuration
     * @param queryResult query result set of query corresponding to hint
     **/
    void accept(Query query, Object queryParameter, Map<String, Object> extraParameters,
        List<Map<Object, Object>> queryResult);

  }

}

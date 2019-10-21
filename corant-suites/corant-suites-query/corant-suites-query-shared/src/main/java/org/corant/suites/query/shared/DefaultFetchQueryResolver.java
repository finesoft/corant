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

import static org.corant.shared.util.ConversionUtils.toBoolean;
import static org.corant.shared.util.MapUtils.putKeyPathMapValue;
import static org.corant.shared.util.StringUtils.asDefaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.kernel.api.ConversionService;
import org.corant.suites.query.shared.QueryParameter.DefaultQueryParameter;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines;
import org.corant.suites.query.shared.dynamic.javascript.NashornScriptEngines.ScriptFunction;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameter;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.suites.query.shared.mapping.Script.ScriptType;

/**
 * corant-suites-query-shared
 *
 * @author bingo 上午10:05:02
 *
 */
@ApplicationScoped
public class DefaultFetchQueryResolver implements FetchQueryResolver {

  protected final Map<String, ScriptFunction> predicates = new ConcurrentHashMap<>();

  @Inject
  ConversionService conversionService;

  @Override
  public boolean canFetch(Object result, QueryParameter queryParameter, FetchQuery fetchQuery) {
    // precondition to decide whether execute fetch.
    if (fetchQuery.getPredicateScript().isValid()
        && fetchQuery.getPredicateScript().getType() == ScriptType.JS) {
      ScriptFunction sf = predicates.computeIfAbsent(fetchQuery.getId(), (k) -> {
        return NashornScriptEngines.compileFunction(fetchQuery.getPredicateScript().getCode(), "p",
            "r");
      });
      if (sf != null) {
        Boolean b = toBoolean(sf.apply(new Object[] {queryParameter, result}));
        if (b == null || !b.booleanValue()) {
          return false;
        }
      }
    }
    return true;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void resolveFetchedResult(Object result, Object fetchedResult, String injectProName) {
    if (isBlank(injectProName)) {
      return;
    }
    if (result instanceof Map) {
      if (injectProName.indexOf('.') != -1) {
        Map<String, Object> mapResult = (Map) result;
        putKeyPathMapValue(mapResult, injectProName, ".", fetchedResult);
      } else {
        ((Map) result).put(injectProName, fetchedResult);
      }
    } else if (result != null) {
      try {
        BeanUtils.setProperty(result, injectProName, fetchedResult);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new QueryRuntimeException(e);
      }
    }
  }

  @Override
  public QueryParameter resolveFetchQueryParameter(Object result, FetchQuery query,
      QueryParameter parentQueryparameter) {
    return new DefaultQueryParameter().context(parentQueryparameter.getContext())
        .criteria(resolveFetchQueryCriteria(result, query, extractCriterias(parentQueryparameter)));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected Map<String, Object> extractCriterias(QueryParameter parameter) {
    Map<String, Object> map = new HashMap<>();
    if (parameter != null) {
      Object criteria = parameter.getCriteria();
      if (criteria instanceof Map) {
        ((Map) criteria).forEach((k, v) -> {
          map.put(asDefaultString(k), v);
        });
      } else if (criteria != null) {
        QueryObjectMapper.OM.convertValue(criteria, Map.class).forEach((k, v) -> {
          map.put(asDefaultString(k), v);
        });
      }
    }
    return map;
  }

  @SuppressWarnings("rawtypes")
  protected Map<String, Object> resolveFetchQueryCriteria(Object result, FetchQuery fetchQuery,
      Map<String, Object> criteria) {
    Map<String, Object> fetchCriteria = new HashMap<>();
    for (FetchQueryParameter parameter : fetchQuery.getParameters()) {
      if (parameter.getSource() == FetchQueryParameterSource.C) {
        fetchCriteria.put(parameter.getName(), parameter.getValue());
      } else if (parameter.getSource() == FetchQueryParameterSource.P) {
        fetchCriteria.put(parameter.getName(), criteria.get(parameter.getSourceName()));
      } else if (result != null) {
        String parameterName = parameter.getName();
        String sourceName = parameter.getSourceName();
        if (result instanceof Map) {
          if (sourceName.indexOf('.') != -1) {
            List<Object> values = new ArrayList<>();
            QueryUtils.extractResult(result, sourceName, true, values);
            Object value = values.isEmpty() ? null : values.size() == 1 ? values.get(0) : values;
            fetchCriteria.put(parameterName, value);
          } else {
            fetchCriteria.put(parameterName, ((Map) result).get(sourceName));
          }
        } else {
          try {
            fetchCriteria.put(parameterName, BeanUtils.getProperty(result, sourceName));
          } catch (Exception e) {
            throw new QueryRuntimeException(e,
                "Can not extract value from query result for resolve fetch query [%s] parameter!",
                fetchQuery.getReferenceQuery());
          }
        }
      }
    }
    return fetchCriteria;
  }
}

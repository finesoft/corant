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
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.putKeyPathMapValue;
import static org.corant.shared.util.StringUtils.asDefaultString;
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
import org.corant.shared.exception.NotSupportedException;
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
  protected final Map<String, ScriptFunction> injections = new ConcurrentHashMap<>();

  @Inject
  ConversionService conversionService;

  @Override
  public boolean canFetch(QueryParameter queryParameter, FetchQuery fetchQuery) {
    ScriptFunction sf = resolveFetchPredicate(fetchQuery);
    if (sf != null) {
      Boolean b = toBoolean(sf.apply(new Object[] {queryParameter}));
      if (b == null || !b.booleanValue()) {
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public void resolveFetchedResult(List<?> results, List<?> fetchedResults, FetchQuery fetchQuery) {
    if (isEmpty(results)) {
      return;
    }
    final String injectProName = fetchQuery.getInjectPropertyName();
    if (isEmpty(fetchedResults)) {
      for (Object result : results) {
        injectFetchedResult(result, null, injectProName);
      }
      return;
    }
    final ScriptFunction injection = resolveFetchInjection(fetchQuery);
    if (injection != null) {
      List<Object> useFetchedResultList = new ArrayList<>();
      for (Object result : results) {
        List<Object> injectList = new ArrayList<>();
        for (final Object fetchResult : fetchedResults) {
          Boolean fit = toBoolean(injection.apply(new Object[] {result, fetchResult}));
          if (fit != null && fit.booleanValue()) {
            if (injectList.add(fetchResult)) {
              if (!fetchQuery.isMultiRecords()) {
                break;
              }
            }
          }
        }
        if (!injectList.isEmpty()) {
          if (fetchQuery.isMultiRecords()) {
            injectFetchedResult(result, injectList, injectProName);
          } else {
            injectFetchedResult(result, injectList.get(0), injectProName);
          }
        } else {
          injectFetchedResult(result, null, injectProName);
        }
        useFetchedResultList.addAll(injectList);
      }
      fetchedResults.retainAll(useFetchedResultList);
    } else {
      if (!fetchQuery.isMultiRecords() && !fetchedResults.isEmpty()) {
        final List refList = fetchedResults;
        final Object fst = refList.get(0);
        refList.clear();
        refList.add(fst);
        // while (fetchedResultList.size() > 1) {
        // fetchedResultList.remove(1);
        // }
      }
      for (Object result : results) {
        if (fetchQuery.isMultiRecords()) {
          injectFetchedResult(result, fetchedResults, injectProName);
        } else {
          injectFetchedResult(result, fetchedResults.get(0), injectProName);
        }
      }
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void resolveFetchedResult(Object result, List<?> fetchedResults, FetchQuery fetchQuery) {
    if (result == null) {
      return;
    }
    final String injectProName = fetchQuery.getInjectPropertyName();
    if (isEmpty(fetchedResults)) {
      injectFetchedResult(result, null, injectProName);
      return;
    }
    final ScriptFunction injection = resolveFetchInjection(fetchQuery);
    if (injection != null) {
      fetchedResults.removeIf(fri -> {
        Boolean fit = toBoolean(injection.apply(new Object[] {result, fri}));
        return fit == null || !fit.booleanValue();
      });
    }
    if (!fetchQuery.isMultiRecords() && !fetchedResults.isEmpty()) {
      final List refList = fetchedResults;
      final Object fst = refList.get(0);
      refList.clear();
      refList.add(fst);
      // while (fetchedResult.size() > 1) {
      // fetchedResult.remove(1);
      // }
    }
    if (fetchQuery.isMultiRecords()) {
      injectFetchedResult(result, fetchedResults, injectProName);
    } else {
      injectFetchedResult(result, fetchedResults.iterator().next(), injectProName);
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

  @SuppressWarnings({"unchecked", "rawtypes"})
  protected void injectFetchedResult(Object result, Object fetchedResult, String injectProName) {
    if (result instanceof Map) {
      Map<String, Object> mapResult = (Map) result;
      if (injectProName.indexOf('.') != -1) {
        putKeyPathMapValue(mapResult, injectProName, ".", fetchedResult);
      } else {
        mapResult.put(injectProName, fetchedResult);
      }
    } else if (result != null) {
      try {
        BeanUtils.setProperty(result, injectProName, fetchedResult);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new QueryRuntimeException(e);
      }
    }
  }

  protected ScriptFunction resolveFetchInjection(FetchQuery fetchQuery) {
    return injections.computeIfAbsent(fetchQuery.getId(), (id) -> {
      if (fetchQuery.getInjectionScript().isValid()) {
        if (fetchQuery.getInjectionScript().getType() != ScriptType.JS) {
          throw new NotSupportedException();// For now we only support js script
        }
        return NashornScriptEngines.compileFunction(fetchQuery.getInjectionScript().getCode(), "r",
            "fr");
      } else {
        return null;
      }
    });
  }

  protected ScriptFunction resolveFetchPredicate(FetchQuery fetchQuery) {
    return predicates.computeIfAbsent(fetchQuery.getId(), (k) -> {
      if (fetchQuery.getPredicateScript().isValid()) {
        if (fetchQuery.getPredicateScript().getType() != ScriptType.JS) {
          throw new NotSupportedException();// For now we only support js script
        }
        return NashornScriptEngines.compileFunction(fetchQuery.getPredicateScript().getCode(), "p");
      }
      return null;
    });
  }

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
        try {
          Object parameterValue = null;
          // handle multi results
          if (result instanceof List) {
            List<Object> listParameterValue = new ArrayList<>();
            List<?> resultList = (List<?>) result;
            for (Object resultItem : resultList) {
              Object itemParameterValue = resolveFetchQueryCriteriaValue(resultItem, sourceName);
              if (itemParameterValue != null) {
                listParameterValue.add(itemParameterValue);
              }
            }
            parameterValue = listParameterValue;
          } else {
            parameterValue = resolveFetchQueryCriteriaValue(result, sourceName);
          }
          fetchCriteria.put(parameterName, parameterValue);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
          throw new QueryRuntimeException(e,
              "Can not extract value from query result for resolve fetch query [%s] parameter!",
              fetchQuery.getReferenceQuery());
        }
      }
    }
    return fetchCriteria;
  }

  @SuppressWarnings("rawtypes")
  protected Object resolveFetchQueryCriteriaValue(Object result, String sourceName)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    if (result instanceof Map) {
      if (sourceName.indexOf('.') != -1) {
        List<Object> values = new ArrayList<>();
        QueryUtils.extractResult(result, sourceName, true, values);
        return values.isEmpty() ? null : values.size() == 1 ? values.get(0) : values;
      } else {
        return ((Map) result).get(sourceName);
      }
    } else {
      return BeanUtils.getProperty(result, sourceName);
    }
  }

}

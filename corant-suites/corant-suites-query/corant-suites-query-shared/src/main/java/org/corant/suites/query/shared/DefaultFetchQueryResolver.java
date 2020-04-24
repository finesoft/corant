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

import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.ConversionUtils.toBoolean;
import static org.corant.shared.util.ConversionUtils.toList;
import static org.corant.shared.util.ConversionUtils.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getMapKeyPathValues;
import static org.corant.shared.util.MapUtils.putMapKeyPathValue;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.asDefaultString;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.normal.Names;
import org.corant.suites.cdi.ConversionService;
import org.corant.suites.lang.javascript.NashornScriptEngines;
import org.corant.suites.lang.kotlin.KotlinScriptEngines;
import org.corant.suites.query.shared.QueryParameter.DefaultQueryParameter;
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
@SuppressWarnings({"unchecked", "rawtypes"})
@ApplicationScoped
public class DefaultFetchQueryResolver implements FetchQueryResolver {

  protected final Map<String, Function<Object[], Object>> predicates = new ConcurrentHashMap<>();
  protected final Map<String, Function<Object[], Object>> injections = new ConcurrentHashMap<>();

  @Inject
  protected ConversionService conversionService;

  @Inject
  protected Logger logger;

  @Override
  public boolean canFetch(Object result, QueryParameter queryParameter, FetchQuery fetchQuery) {
    Function<Object[], Object> sf = resolveFetchPredicate(fetchQuery);
    if (sf != null) {
      Boolean b = toBoolean(sf.apply(new Object[] {queryParameter, result}));
      if (b == null || !b.booleanValue()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void resolveFetchedResult(Object result, List<?> fetchedResults, FetchQuery fetchQuery) {
    if (result == null) {
      return;
    }
    final Function<Object[], Object> injection = resolveFetchInjection(fetchQuery);
    final String[] injectProNamePath = fetchQuery.getInjectPropertyNamePath();
    if (injection == null) {
      // use inject pro name
      shouldNotEmpty(injectProNamePath);
      if (isEmpty(fetchedResults)) {
        injectFetchedResult(result, null, injectProNamePath);
      } else {
        if (fetchQuery.isMultiRecords()) {
          injectFetchedResult(result, fetchedResults, injectProNamePath);
        } else {
          injectFetchedResult(result, fetchedResults.iterator().next(), injectProNamePath);
        }
      }
    } else {
      injection.apply(
          new Object[] {new Object[] {result}, defaultObject(fetchedResults, ArrayList::new)});
    }
  }

  @Override
  public void resolveFetchedResults(List<?> results, List<?> fetchedResults,
      FetchQuery fetchQuery) {
    if (isEmpty(results)) {
      return;
    }
    final Function<Object[], Object> injection = resolveFetchInjection(fetchQuery);
    final String[] injectProNamePath = fetchQuery.getInjectPropertyNamePath();
    if (injection == null) {
      // use inject pro name
      shouldNotEmpty(injectProNamePath);
      if (isEmpty(fetchedResults)) {
        for (Object result : results) {
          injectFetchedResult(result, null, injectProNamePath);
        }
      } else {
        for (Object result : results) {
          if (fetchQuery.isMultiRecords()) {
            injectFetchedResult(result, fetchedResults, injectProNamePath);
          } else {
            injectFetchedResult(result, fetchedResults.get(0), injectProNamePath);
          }
        }
      }
    } else {
      // use inject script
      // for (Object result : results) {
      injection.apply(new Object[] {results, defaultObject(fetchedResults, ArrayList::new)});
      // }
    }
  }

  @Override
  public QueryParameter resolveFetchQueryParameter(Object result, FetchQuery query,
      QueryParameter parentQueryparameter) {
    return new DefaultQueryParameter().context(parentQueryparameter.getContext())
        .criteria(resolveFetchQueryCriteria(result, query, extractCriterias(parentQueryparameter)));
  }

  protected Object convertCriteriaValue(Object obj, Class<?> type) {
    if (type == null || obj == null) {
      return obj;
    } else {
      return obj instanceof Collection ? toList(obj, type) : toObject(obj, type);
    }
  }

  protected Map<String, Object> extractCriterias(QueryParameter parameter) {
    Map<String, Object> map = new HashMap<>();
    if (parameter != null) {
      Object criteria = parameter.getCriteria();
      if (criteria instanceof Map) {
        ((Map) criteria).forEach((k, v) -> map.put(asDefaultString(k), v));
      } else if (criteria != null) {
        QueryObjectMapper.OM.convertValue(criteria, Map.class)
            .forEach((k, v) -> map.put(asDefaultString(k), v));
      }
    }
    return map;
  }

  protected void injectFetchedResult(Object result, Object fetchedResult,
      String[] injectProNamePath) {
    if (result instanceof Map) {
      putMapKeyPathValue((Map) result, injectProNamePath, fetchedResult);
    } else if (result != null) {
      try {
        BeanUtils.setProperty(result, String.join(Names.NAME_SPACE_SEPARATORS, injectProNamePath),
            fetchedResult);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new QueryRuntimeException(e, "Inject fetched result occurred error %s",
            e.getMessage());
      }
    }
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    predicates.clear();
    injections.clear();
    logger.fine(() -> "Clear default fetch query resolver caches.");
  }

  protected Function<Object[], Object> resolveFetchInjection(FetchQuery fetchQuery) {
    return injections.computeIfAbsent(fetchQuery.getId(), id -> {
      if (fetchQuery.getInjectionScript().isValid()) {
        if (fetchQuery.getInjectionScript().getType() == ScriptType.JS) {
          return NashornScriptEngines.createFunction(fetchQuery.getInjectionScript().getCode(),
              RESULTS_FUNC_PARAMETER_NAME, FETCHED_RESULTS_FUNC_PARAMETER_NAME);
        } else if (fetchQuery.getInjectionScript().getType() == ScriptType.KT) {
          return KotlinScriptEngines.createFunction(fetchQuery.getInjectionScript().getCode(),
              RESULTS_FUNC_PARAMETER_NAME, FETCHED_RESULTS_FUNC_PARAMETER_NAME);
        } else {
          throw new NotSupportedException(
              "Currently we only support using javascript / kotlin as fetch query injection script.");
        }

      } else {
        return null;
      }
    });
  }

  protected Function<Object[], Object> resolveFetchPredicate(FetchQuery fetchQuery) {
    return predicates.computeIfAbsent(fetchQuery.getId(), k -> {
      if (fetchQuery.getPredicateScript().isValid()) {
        if (fetchQuery.getPredicateScript().getType() == ScriptType.JS) {
          return NashornScriptEngines.createFunction(fetchQuery.getPredicateScript().getCode(),
              PARAMETER_FUNC_PARAMETER_NAME, RESULT_FUNC_PARAMETER_NAME);
        } else if (fetchQuery.getPredicateScript().getType() == ScriptType.KT) {
          return KotlinScriptEngines.createFunction(fetchQuery.getPredicateScript().getCode(),
              PARAMETER_FUNC_PARAMETER_NAME, RESULT_FUNC_PARAMETER_NAME);
        } else {
          throw new NotSupportedException(
              "Currently we only support using javascript / kotlin as fetch query predication script.");
        }

      }
      return null;
    });
  }

  protected Map<String, Object> resolveFetchQueryCriteria(Object result, FetchQuery fetchQuery,
      Map<String, Object> criteria) {
    Map<String, Object> fetchCriteria = new HashMap<>();
    for (FetchQueryParameter parameter : fetchQuery.getParameters()) {
      Class<?> type = parameter.getType();
      boolean distinct = parameter.isDistinct();
      String name = parameter.getName();
      if (parameter.getSource() == FetchQueryParameterSource.C) {
        fetchCriteria.put(name, convertCriteriaValue(parameter.getValue(), type));
      } else if (parameter.getSource() == FetchQueryParameterSource.P) {
        String sourceName = parameter.getSourceName();
        fetchCriteria.put(name, convertCriteriaValue(criteria.get(sourceName), type));
      } else if (result != null) {
        String[] namePath = parameter.getSourceNamePath();
        Object criteriaValue = null;
        try {
          // handle multi results
          if (result instanceof List) {
            Collection<Object> values = distinct ? new LinkedHashSet<>() : new ArrayList<>();
            for (Object resultItem : (List<?>) result) {
              Object resultItemValue = resolveFetchQueryCriteriaValue(resultItem, namePath);
              if (resultItemValue instanceof Collection) {
                values.addAll((Collection) convertCriteriaValue(resultItemValue, type));
              } else if (resultItemValue != null) {
                values.add(convertCriteriaValue(resultItemValue, type));
              }
            }
            criteriaValue = values;
          } else {
            Object resultValue = resolveFetchQueryCriteriaValue(result, namePath);
            criteriaValue = convertCriteriaValue(resultValue, type);
          }
          fetchCriteria.put(name, criteriaValue);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
          throw new QueryRuntimeException(e,
              "Can not extract value from query result for resolve fetch query [%s] parameter!",
              fetchQuery.getReferenceQuery());
        }
      }
    }
    return fetchCriteria;
  }

  protected Object resolveFetchQueryCriteriaValue(Object result, String[] sourceNamePath)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    if (result instanceof Map) {
      if (sourceNamePath.length > 1) {
        List<Object> values = getMapKeyPathValues(result, sourceNamePath);
        return values.isEmpty() ? null : values.size() == 1 ? values.get(0) : values;
      } else {
        return ((Map) result).get(sourceNamePath[0]);
      }
    } else {
      return BeanUtils.getProperty(result, String.join(".", sourceNamePath));
    }
  }

  @Deprecated
  void resolveFetchedResultDeprecate(List<?> results, List<?> fetchedResults,
      FetchQuery fetchQuery) {
    if (isEmpty(results)) {
      return;
    }
    final String[] injectProName = fetchQuery.getInjectPropertyNamePath();
    if (isEmpty(fetchedResults)) {
      for (Object result : results) {
        injectFetchedResult(result, null, injectProName);
      }
      return;
    }
    final Function<Object[], Object> injection = resolveFetchInjection(fetchQuery);
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

  @Deprecated
  void resolveFetchedResultDeprecate(Object result, List<?> fetchedResults, FetchQuery fetchQuery) {
    if (result == null) {
      return;
    }
    final String[] injectProName = fetchQuery.getInjectPropertyNamePath();
    if (isEmpty(fetchedResults)) {
      injectFetchedResult(result, null, injectProName);
      return;
    }
    final Function<Object[], Object> injection = resolveFetchInjection(fetchQuery);
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
}

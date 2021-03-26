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
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.getMapKeyPathValues;
import static org.corant.shared.util.Maps.putMapKeyPathValue;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.asDefaultString;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.context.service.ConversionService;
import org.corant.shared.normal.Names;
import org.corant.suites.query.shared.QueryParameter.DefaultQueryParameter;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameter;
import org.corant.suites.query.shared.mapping.FetchQuery.FetchQueryParameterSource;

/**
 * corant-suites-query-shared
 *
 * @author bingo 上午10:05:02
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@ApplicationScoped
public class DefaultFetchQueryHandler implements FetchQueryHandler {

  @Inject
  protected ConversionService conversionService;

  @Inject
  protected QueryObjectMapper objectMapper;

  @Inject
  protected Logger logger;

  @Override
  public boolean canFetch(Object result, QueryParameter queryParameter, FetchQuery fetchQuery) {
    Function<Object[], Object> fun = QueryScriptEngines.resolveFetchPredicates(fetchQuery);
    return fun == null || toBoolean(fun.apply(new Object[] {queryParameter, result}));
  }

  @Override
  public QueryObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public void handleFetchedResult(Object result, List<?> fetchedResults, FetchQuery fetchQuery) {
    if (result == null) {
      return;
    }
    Function<Object[], Object> fun = QueryScriptEngines.resolveFetchInjections(fetchQuery);
    if (fun != null) {
      fun.apply(new Object[] {new Object[] {result}, fetchedResults});
    } else {
      String[] injectProNamePath = shouldNotEmpty(fetchQuery.getInjectPropertyNamePath());
      if (isEmpty(fetchedResults)) {
        injectFetchedResult(result, null, injectProNamePath);
      } else {
        if (fetchQuery.isMultiRecords()) {
          injectFetchedResult(result, fetchedResults, injectProNamePath);
        } else {
          injectFetchedResult(result, fetchedResults.iterator().next(), injectProNamePath);
        }
      }
    }
  }

  @Override
  public void handleFetchedResults(List<?> results, List<?> fetchedResults,
      FetchQuery fetchQuery) {
    if (isEmpty(results)) {
      return;
    }
    Function<Object[], Object> fun = QueryScriptEngines.resolveFetchInjections(fetchQuery);
    if (fun != null) {
      fun.apply(new Object[] {results, defaultObject(fetchedResults, ArrayList::new)});
    } else {
      String[] injectProNamePath = shouldNotEmpty(fetchQuery.getInjectPropertyNamePath());
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
        objectMapper.toObject(criteria, Map.class)
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
        throw new QueryRuntimeException(e, "Inject fetched result occurred error %s.",
            e.getMessage());
      }
    }
  }

  @PreDestroy
  protected synchronized void onPreDestroy() {
    logger.fine(() -> "Clear default fetch query resolver caches.");
  }

  protected Map<String, Object> resolveFetchQueryCriteria(Object result, FetchQuery fetchQuery,
      Map<String, Object> criteria) {
    Map<String, Object> fetchCriteria = new HashMap<>();
    for (FetchQueryParameter parameter : fetchQuery.getParameters()) {
      Class<?> type = parameter.getType();
      boolean distinct = parameter.isDistinct();
      String name = parameter.getName();
      FetchQueryParameterSource source = parameter.getSource();
      if (source == FetchQueryParameterSource.C) {
        fetchCriteria.put(name, convertCriteriaValue(parameter.getValue(), type));
      } else if (source == FetchQueryParameterSource.P) {
        String sourceName = parameter.getSourceName();
        fetchCriteria.put(name, convertCriteriaValue(criteria.get(sourceName), type));
      } else if (result != null) {
        String[] namePath = parameter.getSourceNamePath();
        try {
          if (result instanceof List) {
            // handle multi results
            Collection<Object> values = distinct ? new LinkedHashSet<>() : new ArrayList<>();
            for (Object resultItem : (List<?>) result) {
              Object resultItemValue = resolveFetchQueryCriteriaValue(resultItem, namePath);
              if (resultItemValue instanceof Collection) {
                values.addAll((Collection) convertCriteriaValue(resultItemValue, type));
              } else if (resultItemValue != null) {
                values.add(convertCriteriaValue(resultItemValue, type));
              }
            }
            fetchCriteria.put(name, values);
          } else {
            // handle single results
            Object resultValue = resolveFetchQueryCriteriaValue(result, namePath);
            fetchCriteria.put(name, convertCriteriaValue(resultValue, type));
          }
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

}

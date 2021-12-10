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
package org.corant.modules.query.shared;

import static org.corant.context.Beans.select;
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.getMapKeyPathValues;
import static org.corant.shared.util.Maps.putMapKeyPathValue;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.asDefaultString;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.beanutils.BeanUtils;
import org.corant.context.service.ConversionService;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.modules.query.shared.QueryScriptEngines.ParameterAndResult;
import org.corant.modules.query.shared.QueryScriptEngines.ParameterAndResultPair;
import org.corant.modules.query.spi.QueryParameterReviser;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Mutable.MutableObject;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-modules-query-shared
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
    Function<ParameterAndResult, Object> fun =
        QueryScriptEngines.resolveFetchPredicates(fetchQuery);
    return fun == null || toBoolean(fun.apply(new ParameterAndResult(queryParameter, result)));
  }

  @Override
  public QueryObjectMapper getObjectMapper() {
    return objectMapper;
  }

  @Override
  public void handleFetchedResult(QueryParameter parameter, Object result, List<?> fetchedResults,
      FetchQuery fetchQuery) {
    if (result == null) {
      return;
    }
    Function<ParameterAndResultPair, Object> fun =
        QueryScriptEngines.resolveFetchInjections(fetchQuery);
    if (fun != null) {
      fun.apply(new ParameterAndResultPair(parameter, listOf(result), fetchedResults));
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
  public void handleFetchedResults(QueryParameter parameter, List<?> results,
      List<?> fetchedResults, FetchQuery fetchQuery) {
    if (isEmpty(results)) {
      return;
    }
    Function<ParameterAndResultPair, Object> fun =
        QueryScriptEngines.resolveFetchInjections(fetchQuery);
    if (fun != null) {
      fun.apply(new ParameterAndResultPair(parameter, results,
          defaultObject(fetchedResults, ArrayList::new)));
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
    MutableObject<QueryParameter> resolved =
        new MutableObject<>(new DefaultQueryParameter().context(parentQueryparameter.getContext())
            .criteria(resolveFetchQueryCriteria(result, query, parentQueryparameter)));
    select(QueryParameterReviser.class).stream().filter(r -> r.supports(query))
        .sorted(Sortable::compare).forEach(resolved::apply);
    return resolved.get();
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
        objectMapper.mapOf(criteria, true).forEach((k, v) -> map.put(asDefaultString(k), v));
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
    logger.fine(() -> "Clear default fetch query handler caches.");
  }

  protected Map<String, Object> resolveFetchQueryCriteria(Object result, FetchQuery fetchQuery,
      QueryParameter parentQueryParameter) {
    Map<String, Object> criteria = extractCriterias(parentQueryParameter);
    Map<String, Object> fetchCriteria = new HashMap<>();
    for (FetchQueryParameter parameter : fetchQuery.getParameters()) {
      final Class<?> type = parameter.getType();
      final boolean distinct = parameter.isDistinct();
      final boolean singleAsList = parameter.isSingleAsList();
      final String name = parameter.getName();
      final FetchQueryParameterSource source = parameter.getSource();
      if (source == FetchQueryParameterSource.C) {
        fetchCriteria.put(name, convertCriteriaValue(parameter.getValue(), type));
      } else if (source == FetchQueryParameterSource.P) {
        String sourceName = parameter.getSourceName();
        fetchCriteria.put(name, convertCriteriaValue(criteria.get(sourceName), type));
      } else if (source == FetchQueryParameterSource.S) {
        // the parameter script handling
        Function<ParameterAndResult, Object> fun =
            QueryScriptEngines.resolveFetchParameter(parameter);
        List<Object> parentReuslt = result instanceof List ? (List) result : listOf(result);
        Object resultValue = fun.apply(new ParameterAndResult(parentQueryParameter, parentReuslt));
        resultValue = resolveFetchQueryCriteriaValueResult(resultValue, distinct, singleAsList);
        fetchCriteria.put(name, convertCriteriaValue(resultValue, type));
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
            resultValue = resolveFetchQueryCriteriaValueResult(resultValue, distinct, singleAsList);
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

  protected Object resolveFetchQueryCriteriaValueResult(Object resultValue, boolean distinct,
      boolean singleAsList) {
    Object theValue = resultValue;
    if (theValue instanceof Collection) {
      if (distinct && !(theValue instanceof Set)) {
        theValue = new LinkedHashSet<>((Collection) theValue);
      }
    } else if (singleAsList && theValue != null) {
      theValue = distinct ? setOf(theValue) : listOf(theValue);
    }
    return theValue;
  }

}

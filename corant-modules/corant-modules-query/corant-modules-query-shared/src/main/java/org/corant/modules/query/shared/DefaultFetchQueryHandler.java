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

import static org.corant.modules.query.QueryParameter.CTX_QHH_EXCLUDE_FETCH_QUERY;
import static org.corant.shared.util.Assertions.shouldInstanceOf;
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Conversions.toCollection;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.asDefaultString;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.matchWildcard;
import static org.corant.shared.util.Strings.split;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.corant.modules.query.FetchQueryHandler;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryParameter.DefaultQueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameter;
import org.corant.modules.query.mapping.FetchQuery.FetchQueryParameterSource;
import org.corant.modules.query.mapping.Nullable;
import org.corant.modules.query.shared.ScriptProcessor.ParameterAndResult;
import org.corant.modules.query.shared.ScriptProcessor.ParameterAndResultPair;
import org.corant.modules.query.spi.QueryParameterReviser;
import org.corant.shared.ubiquity.Mutable.MutableObject;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings.WildcardMatcher;

/**
 * corant-modules-query-shared
 *
 * @author bingo 上午10:05:02
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@ApplicationScoped
public class DefaultFetchQueryHandler implements FetchQueryHandler {

  @Inject
  protected QueryObjectMapper objectMapper;

  @Inject
  protected QueryScriptEngines scriptEngines;

  @Inject
  @Any
  protected Instance<QueryParameterReviser> parameterRevisers;

  @Inject
  protected Logger logger;

  @Override
  public boolean canFetch(Object result, QueryParameter queryParameter, FetchQuery fetchQuery) {
    String exs = getMapString(queryParameter.getContext(), CTX_QHH_EXCLUDE_FETCH_QUERY);
    if (isNotBlank(exs)) {
      final String fetchQueryName = fetchQuery.getQueryReference().getVersionedName();
      for (String ex : split(exs, ",", true, true)) {
        if (WildcardMatcher.hasWildcard(ex) && matchWildcard(fetchQueryName, false, ex)
            || areEqual(fetchQueryName, ex)) {
          return false;
        }
      }
    }
    Function<ParameterAndResult, Object> fun = scriptEngines.resolveFetchPredicates(fetchQuery);
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
    Function<ParameterAndResultPair, Object> fun = scriptEngines.resolveFetchInjections(fetchQuery);
    if (fun != null) {
      fun.apply(new ParameterAndResultPair(parameter, listOf(result), fetchQuery, fetchedResults));
    } else {
      String[] injectProNamePath = shouldNotEmpty(fetchQuery.getInjectPropertyNamePath());
      if (isEmpty(fetchedResults)) {
        objectMapper.putMappedValue(result, injectProNamePath, null);
      } else if (fetchQuery.isMultiRecords()) {
        Object exists = objectMapper.getMappedValue(result, injectProNamePath);
        if (exists != null) {
          shouldInstanceOf(exists, List.class,
              () -> new QueryRuntimeException("Inject property [%s] must be a list",
                  fetchQuery.getInjectPropertyName())).addAll(fetchedResults);
        } else {
          objectMapper.putMappedValue(result, injectProNamePath, new ArrayList(fetchedResults));
        }
      } else {
        objectMapper.putMappedValue(result, injectProNamePath, fetchedResults.iterator().next());
      }
    }
  }

  @Override
  public void handleFetchedResults(QueryParameter parameter, List<?> results,
      List<?> fetchedResults, FetchQuery fetchQuery) {
    if (isEmpty(results)) {
      return;
    }
    Function<ParameterAndResultPair, Object> fun = scriptEngines.resolveFetchInjections(fetchQuery);
    if (fun != null) {
      fun.apply(new ParameterAndResultPair(parameter, results, fetchQuery,
          defaultObject(fetchedResults, ArrayList::new)));
    } else {
      String[] injectProNamePath = shouldNotEmpty(fetchQuery.getInjectPropertyNamePath());
      if (isEmpty(fetchedResults)) {
        for (Object result : results) {
          objectMapper.putMappedValue(result, injectProNamePath, null);
        }
      } else {
        for (Object result : results) {
          if (fetchQuery.isMultiRecords()) {
            Object exists = objectMapper.getMappedValue(result, injectProNamePath);
            if (exists != null) {
              shouldInstanceOf(exists, List.class,
                  () -> new QueryRuntimeException("Inject property [%s] must be a list",
                      fetchQuery.getInjectPropertyName())).addAll(fetchedResults);
            } else {
              objectMapper.putMappedValue(result, injectProNamePath, new ArrayList(fetchedResults));
            }
          } else {
            objectMapper.putMappedValue(result, injectProNamePath, fetchedResults.get(0));
          }
        }
      }
    }
  }

  @Override
  public QueryParameter resolveFetchQueryParameter(Object result, FetchQuery query,
      QueryParameter parentQueryParameter) {
    MutableObject<QueryParameter> resolved =
        new MutableObject<>(new DefaultQueryParameter().context(parentQueryParameter.getContext())
            .criteria(resolveFetchQueryCriteria(result, query, parentQueryParameter)));
    parameterRevisers.stream().filter(r -> r.supports(query)).sorted(Sortable::compare)
        .forEach(resolved::apply);
    return resolved.get();
  }

  protected Object convertCriteriaValue(Object obj, Class<?> type, boolean flatten) {
    if (obj == null) {
      return null;
    }
    Object value = obj instanceof Object[] ? listOf((Object[]) obj) : obj;
    if (flatten) {
      value = flatten(value);
    }
    if (type != null) {
      if (value instanceof Collection) {
        value = toList(value, type);
      } else {
        value = toObject(value, type);
      }
    }
    return value;
  }

  protected Map<String, Object> extractCriteria(QueryParameter parameter) {
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

  @PreDestroy
  protected synchronized void onPreDestroy() {
    logger.fine(() -> "Clear default fetch query handler caches.");
  }

  protected Map<String, Object> resolveFetchQueryCriteria(Object parentResult,
      FetchQuery fetchQuery, QueryParameter parentQueryParameter) {
    Map<String, Object> criteria = extractCriteria(parentQueryParameter);
    Map<String, Object> fetchCriteria = new HashMap<>();
    Map<String[], Map<String, Pair<FetchQueryParameterSource, Object>>> groupFetchCriteria =
        new HashMap<>();
    for (FetchQueryParameter parameter : fetchQuery.getParameters()) {
      final Class<?> type = parameter.getType();
      final boolean distinct = parameter.isDistinct();
      final boolean singleAsList = parameter.isSingleAsList();
      final boolean flatten = parameter.isFlatten();
      final Nullable nullable = parameter.getNullable();
      final String name = parameter.getName();
      final FetchQueryParameterSource source = parameter.getSource();
      final String[] groupPath = parameter.getGroupPath();
      final boolean useGroup = isNotEmpty(groupPath);
      Object value;
      if (source == FetchQueryParameterSource.C) {
        // Handle values from a specified constant
        String constantValue = parameter.getValue();
        if (constantValue != null || nullable != Nullable.FALSE) {
          value =
              resolveFetchQueryCriteriaValue(constantValue, type, distinct, singleAsList, flatten);
        } else {
          continue;
        }
      } else if (source == FetchQueryParameterSource.P) {
        // Handle values from parent query parameters
        Object parentQueryParameterValue = criteria.get(parameter.getSourceName());
        if (parentQueryParameterValue != null || nullable != Nullable.FALSE) {
          value = resolveFetchQueryCriteriaValue(parentQueryParameterValue, type, distinct,
              singleAsList, flatten);
        } else {
          continue;
        }
      } else if (source == FetchQueryParameterSource.S) {
        // handle values from a specified script
        Function<ParameterAndResult, Object> fun = scriptEngines.resolveFetchParameter(parameter);
        List<Object> parentResults =
            parentResult instanceof List ? (List) parentResult : listOf(parentResult);
        Object evalValue = fun.apply(new ParameterAndResult(parentQueryParameter, parentResults));
        if (evalValue != null || nullable != Nullable.FALSE) {
          value = resolveFetchQueryCriteriaValue(evalValue, type, distinct, singleAsList, flatten);
        } else {
          continue;
        }
      } else if (parentResult != null) {
        String[] namePath = parameter.getSourceNamePath();
        try {
          if (parentResult instanceof List parentRecords) {
            // handle values from parent multi-records result
            List<Object> criteriaValueList = new ArrayList<>(parentRecords.size());
            if (useGroup) {
              for (Object parentRecord : parentRecords) {
                Object criteriaValue = objectMapper.getMappedValue(parentRecord, namePath);
                if (criteriaValue != null || nullable == Nullable.TRUE) {
                  criteriaValueList.add(convertCriteriaValue(criteriaValue, type, flatten));
                }
              }
              if (!criteriaValueList.isEmpty()) {
                value = criteriaValueList;
              } else {
                continue;
              }
            } else {
              for (Object parentRecord : parentRecords) {
                Object criteriaValue = objectMapper.getMappedValue(parentRecord, namePath);
                if (criteriaValue != null || nullable == Nullable.TRUE) {
                  criteriaValueList.add(criteriaValue);
                }
              }
              if (!criteriaValueList.isEmpty()) {
                value = resolveFetchQueryCriteriaValue(criteriaValueList, type, distinct,
                    singleAsList, flatten);
              } else {
                continue;
              }
            }
          } else {
            // handle values from parent single record result
            Object criteriaValue = objectMapper.getMappedValue(parentResult, namePath);
            if (criteriaValue != null || nullable == Nullable.TRUE) {
              if (useGroup) {
                value = listOf(convertCriteriaValue(criteriaValue, type, flatten));
              } else {
                value = resolveFetchQueryCriteriaValue(criteriaValue, type, distinct, singleAsList,
                    flatten);
              }
            } else {
              continue;
            }
          }
        } catch (Exception e) {
          throw new QueryRuntimeException(e,
              "Can not extract value from query result for resolve fetch query [%s] parameter!",
              fetchQuery.getQueryReference());
        }
      } else {
        continue;// never happen??
      }
      if (useGroup) {
        groupFetchCriteria.computeIfAbsent(groupPath, k -> new HashMap<>()).put(name,
            Pair.of(source, value));
      } else {
        fetchCriteria.put(name, value);
      }
    }
    if (!groupFetchCriteria.isEmpty()) {
      Map<String, Object> groupCriteria = resolveFetchQueryCriteriaGroupValue(groupFetchCriteria);
      if (groupCriteria != null) {
        fetchCriteria.putAll(groupCriteria);
      }
    }
    return fetchCriteria;
  }

  protected Map<String, Object> resolveFetchQueryCriteriaGroupValue(
      Map<String[], Map<String, Pair<FetchQueryParameterSource, Object>>> groupFetchCriteria) {
    Map<String, Object> groupCriteria = new LinkedHashMap<>();
    groupFetchCriteria.forEach((g, vs) -> {
      Map<String, List<Object>> resultCriteria = new HashMap<>();
      Map<String, Object> notResultCriteria = new HashMap<>();
      vs.forEach((pn, pv) -> {
        if (pv.left() == FetchQueryParameterSource.R) {
          if (pv.right() != null) {
            resultCriteria.put(pn, (List) pv.right());
          }
        } else {
          notResultCriteria.put(pn, pv.right());
        }
      });

      List<Map<String, Object>> criteria = new ArrayList<>();
      int size = resultCriteria.values().stream().filter(Objects::isNotNull).mapToInt(List::size)
          .min().orElse(0);
      if (size > 0) {
        // flatten & merge the not result criteria to result criteria
        for (int i = 0; i < size; i++) {
          Map<String, Object> map = new HashMap<>(notResultCriteria);
          final int ii = i;
          resultCriteria.forEach((k, v) -> map.put(k, v.get(ii)));
          criteria.add(map);
        }
      } else {
        criteria.add(notResultCriteria);
      }
      objectMapper.putMappedValue(groupCriteria, g, criteria);
      // groupCriteria.put(g, criteria);
    });
    return groupCriteria;
  }

  protected Object resolveFetchQueryCriteriaValue(Object value, Class<?> type, boolean distinct,
      boolean singleAsList, boolean flatten) {
    Object theValue = value instanceof Object[] ? listOf((Object[]) value) : value;
    if (flatten) {
      theValue = flatten(theValue);
    }
    if (theValue instanceof Collection) {
      if (distinct) {
        if (type != null) {
          theValue = toCollection(theValue, type, LinkedHashSet::new);
        } else if (!(theValue instanceof Set)) {
          theValue = new LinkedHashSet<>((Collection) theValue);
        }
      } else if (type != null) {
        theValue = toCollection(theValue, type, ArrayList::new);
      }
    } else if (singleAsList && theValue != null) {
      if (type != null) {
        theValue = toObject(theValue, type);
      }
      theValue = distinct ? setOf(theValue) : listOf(theValue);
    } else if (type != null) {
      theValue = toObject(theValue, type);
    }
    return theValue;
  }

  Object flatten(Object value) {
    if (value instanceof Iterable iterableValue) {
      List<Object> flattened = new ArrayList<>();
      for (Object element : iterableValue) {
        if (element instanceof Iterable iterableElement) {
          for (Object flattenedElement : iterableElement) {
            flattened.add(flattenedElement);
          }
        } else {
          flattened.add(element);
        }
      }
      return flattened;
    }
    return value;
  }
}

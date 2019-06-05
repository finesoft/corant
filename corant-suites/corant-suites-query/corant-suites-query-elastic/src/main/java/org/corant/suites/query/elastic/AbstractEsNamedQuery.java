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
package org.corant.suites.query.elastic;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.suites.query.shared.QueryUtils.getLimit;
import static org.corant.suites.query.shared.QueryUtils.getOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.elastic.EsInLineNamedQueryResolver.Querier;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.QueryUtils;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.spi.ResultHintHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:20:43
 *
 */
@ApplicationScoped
public abstract class AbstractEsNamedQuery implements EsNamedQuery {

  protected EsQueryExecutor executor;

  @Inject
  Logger logger;

  @Inject
  EsInLineNamedQueryResolver<String, Map<String, Object>, String, FetchQuery, QueryHint> resolver;

  @Inject
  @Any
  Instance<ResultHintHandler> resultHintHandlers;

  public Object adaptiveSelect(String q, Map<String, Object> param) {
    if (param != null && param.containsKey(QueryUtils.OFFSET_PARAM_NME)) {
      if (param.containsKey(QueryUtils.LIMIT_PARAM_NME)) {
        return this.page(q, param);
      } else {
        return this.forward(q, param);
      }
    } else {
      return this.select(q, param);
    }
  }

  @Override
  public Map<String, Object> aggregate(String q, Map<String, Object> param) {
    Querier<String, FetchQuery, QueryHint> querier = getResolver().resolve(q, param);
    String script = querier.getScript();
    try {
      Map<String, Object> result = getExecutor().searchAggregation(resolveIndexName(q), script);
      handleResultHints(Map.class, querier.getHints(), param, result);
      return result;
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the aggregate query [%s].", q);
    }
  }

  @Override
  public <T> ForwardList<T> forward(String q, Map<String, Object> param) {
    int offset = getOffset(param);
    int limit = getLimit(param);
    Pair<Long, List<T>> hits = searchHits(q, param);
    List<T> result = hits.getValue();
    return ForwardList.of(result, hits.getLeft() > offset + limit);
  }

  @Override
  public <T> T get(String q, Map<String, Object> param) {
    Pair<Long, List<T>> hits = searchHits(q, param);
    List<T> result = hits.getValue();
    if (!isEmpty(result)) {
      return result.get(0);
    }
    return null;
  }

  @Override
  public <T> PagedList<T> page(String q, Map<String, Object> param) {
    int offset = getOffset(param);
    int limit = getLimit(param);
    Pair<Long, List<T>> hits = searchHits(q, param);
    List<T> result = hits.getValue();
    return PagedList.of(hits.getLeft().intValue(), result, offset, limit);
  }

  @Override
  public Map<String, Object> search(String q, Map<String, Object> param) {
    Querier<String, FetchQuery, QueryHint> querier = getResolver().resolve(q, param);
    String script = querier.getScript();
    try {
      Map<String, Object> result = getExecutor().search(resolveIndexName(q), script);
      handleResultHints(Map.class, querier.getHints(), param, result);
      return result;
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the search query [%s].",
          q);
    }
  }

  @Override
  public <T> List<T> select(String q, Map<String, Object> param) {
    param.putIfAbsent(QueryUtils.LIMIT_PARAM_NME, 128);
    Pair<Long, List<T>> hits = searchHits(q, param);
    return hits.getValue();
  }

  @Override
  public <T> Stream<T> stream(String q, Map<String, Object> param) {
    return Stream.empty();
  }

  protected <T> void fetch(List<T> list, List<FetchQuery> fetchQueries, Map<String, Object> param) {
    if (!isEmpty(list) && !isEmpty(fetchQueries)) {
      list.forEach(e -> fetchQueries.stream().forEach(f -> fetch(e, f, new HashMap<>(param))));
    }
  }

  protected <T> void fetch(T obj, FetchQuery fetchQuery, Map<String, Object> param) {
    if (null == obj || fetchQuery == null) {
      return;
    }
    Map<String, Object> fetchParam = QueryUtils.resolveFetchParam(obj, fetchQuery, param);
    if (!QueryUtils.decideFetch(obj, fetchQuery, fetchParam)) {
      return;
    }
    boolean multiRecords = fetchQuery.isMultiRecords();
    String injectProName = fetchQuery.getInjectPropertyName();
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    Querier<String, FetchQuery, QueryHint> querier = resolver.resolve(refQueryName, fetchParam);
    String script = querier.getScript();
    Class<?> rcls = defaultObject(fetchQuery.getResultClass(), querier.getResultClass());
    List<QueryHint> hints = querier.getHints();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    try {
      log("fetch-> " + refQueryName, fetchParam, script);
      Pair<Long, List<Map<String, Object>>> hits =
          getExecutor().searchHits(resolveIndexName(refQueryName), script);
      if (!isEmpty(hits.getValue())) {
        List<Object> fetchedList = new ArrayList<>();
        for (Map<String, Object> map : hits.getValue()) {
          fetchedList.add(getObjectMapper().convertValue(map, rcls));
        }
        Object fetchedResult = null;
        if (multiRecords) {
          fetchedResult = fetchedList;
        } else if (!isEmpty(fetchedList)) {
          fetchedResult = fetchedList.get(0);
          fetchedList = fetchedList.subList(0, 1);
        }
        QueryUtils.resolveFetchResult(obj, fetchedResult, injectProName);
        this.fetch(fetchedList, fetchQueries, fetchParam);
        handleResultHints(rcls, hints, fetchParam, fetchedList);
      }
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the fetch query [%s].",
          fetchQuery.getReferenceQuery());
    }
  }

  protected <T> void fetch(T obj, List<FetchQuery> fetchQueries, Map<String, Object> param) {
    if (obj != null && !isEmpty(fetchQueries)) {
      fetchQueries.stream().forEach(f -> this.fetch(obj, f, new HashMap<>(param)));
    }
  }

  protected EsQueryExecutor getExecutor() {
    return executor;
  }

  protected ObjectMapper getObjectMapper() {
    return QueryUtils.ESJOM;
  }

  protected EsInLineNamedQueryResolver<String, Map<String, Object>, String, FetchQuery, QueryHint> getResolver() {
    return resolver;
  }

  protected void handleResultHints(Class<?> resultClass, List<QueryHint> hints, Object param,
      Object result) {
    if (result != null && !resultHintHandlers.isUnsatisfied()) {
      hints.forEach(qh -> {
        AtomicBoolean exclusive = new AtomicBoolean(false);
        resultHintHandlers.stream().filter(h -> h.canHandle(resultClass, qh))
            .sorted(ResultHintHandler::compare).forEachOrdered(h -> {
              if (!exclusive.get()) {
                try {
                  h.handle(qh, param, result);
                } catch (Exception e) {
                  throw new CorantRuntimeException(e);
                } finally {
                  if (h.exclusive()) {
                    exclusive.set(true);
                  }
                }
              }
            });
      });
    }
  }

  protected void log(String name, Map<String, Object> param, String... script) {
    logger.fine(
        () -> String.format("%n[Query name]: %s; %n[Query parameters]: [%s]; %n[Query script]: %s",
            name, String.join(",", asStrings(param)), String.join("; ", script)));
  }

  protected String resolveIndexName(String q) {
    int pos = 0;
    shouldBeTrue(isNotBlank(q) && (pos = q.indexOf('.')) != -1);
    return q.substring(0, pos);
  }

  protected <T> Pair<Long, List<T>> searchHits(String q, Map<String, Object> param) {
    Querier<String, FetchQuery, QueryHint> querier = getResolver().resolve(q, param);
    Class<T> rcls = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    String script = querier.getScript();
    try {
      log(q, param, script);
      Pair<Long, List<Map<String, Object>>> hits =
          getExecutor().searchHits(resolveIndexName(q), script);
      List<T> result = new ArrayList<>();
      if (!isEmpty(hits.getValue())) {
        for (Map<String, Object> map : hits.getValue()) {
          result.add(getObjectMapper().convertValue(map, rcls));
        }
        this.fetch(result, fetchQueries, param);
      }
      handleResultHints(rcls, querier.getHints(), param, result);
      return Pair.of(hits.getLeft(), result);
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the search hits query [%s].", q);
    }
  }

  protected void setExecutor(EsQueryExecutor executor) {
    this.executor = executor;
  }
}

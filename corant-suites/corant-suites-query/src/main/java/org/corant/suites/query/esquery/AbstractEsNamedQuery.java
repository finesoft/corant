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
package org.corant.suites.query.esquery;

import static org.corant.shared.util.CollectionUtils.isEmpty;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.shouldBeTrue;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.suites.query.sqlquery.SqlHelper.getLimit;
import static org.corant.suites.query.sqlquery.SqlHelper.getOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.QueryRuntimeException;
import org.corant.suites.query.QueryUtils;
import org.corant.suites.query.esquery.EsInLineNamedQueryResolver.Querier;
import org.corant.suites.query.mapping.FetchQuery;
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
  EsInLineNamedQueryResolver<String, Map<String, Object>, String, FetchQuery> resolver;

  @Override
  public Map<String, Object> aggregate(String q, Map<String, Object> param) {
    Querier<String, FetchQuery> querier = getResolver().resolve(q, param);
    String script = querier.getScript();
    try {
      return getExecutor().searchAggregation(resolveIndexName(q), script, querier.getHints());
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  public <T> ForwardList<T> forward(String q, Map<String, Object> param) {
    Querier<String, FetchQuery> querier = getResolver().resolve(q, param);
    Class<T> rcls = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    String script = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    try {
      log(q, param, script);
      Pair<Long, List<Map<String, Object>>> hits =
          getExecutor().searchHits(resolveIndexName(q), script, querier.getHints());
      List<T> result = new ArrayList<>();
      int total = hits.getKey().intValue();
      if (!isEmpty(hits.getValue())) {
        for (Map<String, Object> map : hits.getValue()) {
          result.add(getObjectMapper().convertValue(map, rcls));
        }
        this.fetch(result, fetchQueries, param);
      }
      return ForwardList.of(result, total > offset + limit);
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  public <T> T get(String q, Map<String, Object> param) {
    Querier<String, FetchQuery> querier = getResolver().resolve(q, param);
    Class<T> rcls = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    String script = querier.getScript();
    try {
      log(q, param, script);
      Pair<Long, List<Map<String, Object>>> hits =
          getExecutor().searchHits(resolveIndexName(q), script, querier.getHints());
      if (!isEmpty(hits.getValue())) {
        Map<String, Object> hit = hits.getRight().get(0);
        T result = getObjectMapper().convertValue(hit, rcls);
        fetch(result, fetchQueries, param);
        return result;
      }
      return null;
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  public <T> PagedList<T> page(String q, Map<String, Object> param) {
    Querier<String, FetchQuery> querier = getResolver().resolve(q, param);
    Class<T> rcls = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    String script = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    try {
      log(q, param, script);
      Pair<Long, List<Map<String, Object>>> hits =
          getExecutor().searchHits(resolveIndexName(q), script, querier.getHints());
      List<T> result = new ArrayList<>();
      int total = hits.getKey().intValue();
      if (!isEmpty(hits.getValue())) {
        for (Map<String, Object> map : hits.getValue()) {
          result.add(getObjectMapper().convertValue(map, rcls));
        }
        this.fetch(result, fetchQueries, param);
      }
      return PagedList.of(total, result, offset, limit);
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  public Map<String, Object> search(String q, Map<String, Object> param) {
    Querier<String, FetchQuery> querier = getResolver().resolve(q, param);
    String script = querier.getScript();
    try {
      return getExecutor().search(resolveIndexName(q), script, querier.getHints());
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  public <T> List<T> select(String q, Map<String, Object> param) {
    Querier<String, FetchQuery> querier = getResolver().resolve(q, param);
    Class<T> rcls = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    String script = querier.getScript();
    try {
      log(q, param, script);
      Pair<Long, List<Map<String, Object>>> hits =
          getExecutor().searchHits(resolveIndexName(q), script, querier.getHints());
      List<T> result = new ArrayList<>();
      if (!isEmpty(hits.getValue())) {
        for (Map<String, Object> map : hits.getValue()) {
          result.add(getObjectMapper().convertValue(map, rcls));
        }
        this.fetch(result, fetchQueries, param);
      }
      return result;
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
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
    boolean multiRecords = fetchQuery.isMultiRecords();
    String injectProName = fetchQuery.getInjectPropertyName();
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    Querier<String, FetchQuery> querier = resolver.resolve(refQueryName, fetchParam);
    String script = querier.getScript();
    Class<?> rcls = defaultObject(fetchQuery.getResultClass(), querier.getResultClass());
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    try {
      log("fetch-> " + refQueryName, fetchParam, script);
      Pair<Long, List<Map<String, Object>>> hits =
          getExecutor().searchHits(resolveIndexName(refQueryName), script, querier.getHints());
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
        this.fetch(fetchedList, fetchQueries, param);
      }
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
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

  protected EsInLineNamedQueryResolver<String, Map<String, Object>, String, FetchQuery> getResolver() {
    return resolver;
  }

  protected void log(String name, Map<String, Object> param, String... sql) {
    logger.fine(
        () -> String.format("%n[Query name]: %s; %n[Query parameters]: [%s]; %n[Query sql]: %s",
            name, String.join(",", asStrings(param)), String.join("; ", sql)));
  }

  protected String resolveIndexName(String q) {
    int pos = 0;
    shouldBeTrue(isNotBlank(q) && (pos = q.indexOf('.')) != -1);
    return q.substring(0, pos);
  }

  protected void setExecutor(EsQueryExecutor executor) {
    this.executor = executor;
  }
}

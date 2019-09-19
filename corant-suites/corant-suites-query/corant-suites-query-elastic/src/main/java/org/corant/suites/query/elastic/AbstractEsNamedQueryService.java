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
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.elastic.EsInLineNamedQueryResolver.EsQuerier;
import org.corant.suites.query.shared.AbstractNamedQueryService;
import org.corant.suites.query.shared.Querier;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.FetchQuery;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:20:43
 *
 */
@ApplicationScoped
public abstract class AbstractEsNamedQueryService extends AbstractNamedQueryService
    implements EsNamedQueryService {

  @Inject
  protected EsInLineNamedQueryResolver<String, Object> resolver;

  @Override
  public Map<String, Object> aggregate(String queryName, Object parameter) {
    EsQuerier querier = getResolver().resolve(queryName, parameter);
    String script = querier.getScript();
    try {
      Map<String, Object> result =
          getExecutor().searchAggregation(resolveIndexName(queryName), script);
      querier.resolveResultHints(result);
      return result;
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the aggregate query [%s].", queryName);
    }
  }

  @Override
  public <T> ForwardList<T> forward(String queryName, Object parameter) {
    EsQuerier querier = getResolver().resolve(queryName, parameter);
    int offset = querier.getQueryParameter().getOffset();
    int limit = querier.getQueryParameter().getLimit();
    Pair<Long, List<T>> hits = searchHits(queryName, querier);
    List<T> result = hits.getValue();
    return ForwardList.of(result, hits.getLeft() > offset + limit);
  }

  @Override
  public <T> T get(String queryName, Object parameter) {
    EsQuerier querier = getResolver().resolve(queryName, parameter);
    Pair<Long, List<T>> hits = searchHits(queryName, querier);
    List<T> result = hits.getValue();
    if (!isEmpty(result)) {
      return result.get(0);
    }
    return null;
  }

  @Override
  public <T> PagedList<T> page(String queryName, Object parameter) {
    EsQuerier querier = getResolver().resolve(queryName, parameter);
    int offset = querier.getQueryParameter().getOffset();
    int limit = querier.getQueryParameter().getLimit();
    Pair<Long, List<T>> hits = searchHits(queryName, querier);
    List<T> result = hits.getValue();
    return PagedList.of(hits.getLeft().intValue(), result, offset, limit);
  }

  @Override
  public Map<String, Object> search(String queryName, Object parameter) {
    EsQuerier querier = getResolver().resolve(queryName, parameter);
    String script = querier.getScript();
    try {
      Map<String, Object> result = getExecutor().search(resolveIndexName(queryName), script);
      querier.resolveResultHints(result);
      return result;
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the search query [%s].",
          queryName);
    }
  }

  @Override
  public <T> List<T> select(String queryName, Object parameter) {
    EsQuerier querier = getResolver().resolve(queryName, parameter);
    Pair<Long, List<T>> hits = searchHits(queryName, querier);
    return hits.getValue();
  }

  @Override
  public <T> Stream<T> stream(String q, Object param) {
    EsQuerier querier = getResolver().resolve(q, param);
    String script = querier.getScript();
    try {
      return getExecutor().stream(resolveIndexName(q), script).map(result -> {
        this.fetch(result, querier);
        return querier.resolveResult(result);
      });
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
  }

  @Override
  protected <T> void fetch(T obj, FetchQuery fetchQuery, Querier parentQuerier) {
    if (null == obj || fetchQuery == null) {
      return;
    }
    QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(obj, fetchQuery);
    if (!decideFetch(obj, fetchQuery, fetchParam)) {
      return;
    }
    boolean multiRecords = fetchQuery.isMultiRecords();
    String injectProName = fetchQuery.getInjectPropertyName();
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    EsQuerier querier = resolver.resolve(refQueryName, fetchParam);
    String script = querier.getScript();
    try {
      log("fetch-> " + refQueryName, querier.getQueryParameter(), script);
      List<Map<String, Object>> fetchedList =
          getExecutor().searchHits(resolveIndexName(refQueryName), script).getValue();
      if (!isEmpty(fetchedList)) {
        Object fetchedResult = null;
        if (multiRecords) {
          fetchedResult = fetchedList;
          this.fetch((List<?>) fetchedResult, querier);
        } else if (!isEmpty(fetchedList)) {
          fetchedResult = fetchedList.get(0);
          this.fetch(fetchedResult, querier);
        }
        querier.resolveResultHints(fetchedResult);
        querier.resolveFetchedResult(obj, fetchedResult, injectProName);
      }
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the fetch query [%s].",
          fetchQuery.getReferenceQuery());
    }
  }

  protected abstract EsQueryExecutor getExecutor();

  @Override
  protected int getMaxSelectSize(Querier querier) {
    return super.getMaxSelectSize(querier);
  }

  protected EsInLineNamedQueryResolver<String, Object> getResolver() {
    return resolver;
  }

  protected String resolveIndexName(String q) {
    int pos = 0;
    shouldBeTrue(isNotBlank(q) && (pos = q.indexOf('.')) != -1);
    return q.substring(0, pos);
  }

  protected <T> Pair<Long, List<T>> searchHits(String q, EsQuerier querier) {
    String script = querier.getScript();
    try {
      log(q, querier.getQueryParameter(), script);
      Pair<Long, List<Map<String, Object>>> hits =
          getExecutor().searchHits(resolveIndexName(q), script);
      List<T> result = new ArrayList<>();
      if (!isEmpty(hits.getValue())) {
        this.fetch(hits.getValue(), querier);
        result = querier.resolveResult(hits.getValue());
      }
      return Pair.of(hits.getLeft(), result);
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the search hits query [%s].", q);
    }
  }

}

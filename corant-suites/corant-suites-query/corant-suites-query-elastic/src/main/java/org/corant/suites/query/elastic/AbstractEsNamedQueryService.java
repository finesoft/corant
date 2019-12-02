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
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.shared.AbstractNamedQueryService;
import org.corant.suites.query.shared.NamedQuerierResolver;
import org.corant.suites.query.shared.Querier;
import org.corant.suites.query.shared.QueryObjectMapper;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.FetchQuery;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:20:43
 *
 */
public abstract class AbstractEsNamedQueryService extends AbstractNamedQueryService
    implements EsNamedQueryService {

  @Override
  public Map<String, Object> aggregate(String queryName, Object parameter) {
    EsNamedQuerier querier = getResolver().resolve(queryName, parameter);
    String script = resolveScript(querier.getScript(null), null, null);
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
  public void fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier) {
    QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
    int maxSize = fetchQuery.isMultiRecords() ? fetchQuery.getMaxSize() : 1;
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    EsNamedQuerier querier = getResolver().resolve(refQueryName, fetchParam);
    String script = resolveScript(querier.getScript(null), null, maxSize > 0 ? maxSize : null);
    try {
      log("fetch-> " + refQueryName, querier.getQueryParameter(), script);
      List<Map<String, Object>> fetchedList =
          getExecutor().searchHits(resolveIndexName(refQueryName), script).getValue();
      if (result instanceof List) {
        parentQuerier.resolveFetchedResult((List<?>) result, fetchedList, fetchQuery);
      } else {
        parentQuerier.resolveFetchedResult(result, fetchedList, fetchQuery);
      }
      fetch(fetchedList, querier);
      querier.resolveResultHints(fetchedList);
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the fetch query [%s].",
          fetchQuery.getReferenceQuery());
    }
  }

  @Override
  public <T> ForwardList<T> forward(String queryName, Object parameter) {
    EsNamedQuerier querier = getResolver().resolve(queryName, parameter);
    int offset = resolveOffset(querier);
    int limit = resolveLimit(querier);
    Pair<Long, List<T>> hits = searchHits(queryName, querier, offset, limit);
    List<T> result = hits.getValue();
    return ForwardList.of(result, hits.getLeft() > offset + limit);
  }

  @Override
  public <T> T get(String queryName, Object parameter) {
    EsNamedQuerier querier = getResolver().resolve(queryName, parameter);
    Pair<Long, List<T>> hits = searchHits(queryName, querier, 0, 1);
    List<T> result = hits.getValue();
    if (!isEmpty(result)) {
      return result.get(0);
    }
    return null;
  }

  @Override
  public <T> PagedList<T> page(String queryName, Object parameter) {
    EsNamedQuerier querier = getResolver().resolve(queryName, parameter);
    int offset = resolveOffset(querier);
    int limit = resolveLimit(querier);
    Pair<Long, List<T>> hits = searchHits(queryName, querier, offset, limit);
    List<T> result = hits.getValue();
    return PagedList.of(hits.getLeft().intValue(), result, offset, limit);
  }

  @Override
  public Map<String, Object> search(String queryName, Object parameter) {
    EsNamedQuerier querier = getResolver().resolve(queryName, parameter);
    String script = resolveScript(querier.getScript(null), null, null);
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
    EsNamedQuerier querier = getResolver().resolve(queryName, parameter);
    Pair<Long, List<T>> hits = searchHits(queryName, querier, null, resolveMaxSelectSize(querier));
    return hits.getValue();
  }

  @Override
  public <T> Stream<T> stream(String q, Object param) {
    EsNamedQuerier querier = getResolver().resolve(q, param);
    String script = resolveScript(querier.getScript(null), null, null);
    log("stream-> " + q, querier.getQueryParameter(), script);
    try {
      return getExecutor().stream(resolveIndexName(q), script).map(result -> {
        this.fetch(result, querier);
        return querier.resolveResult(result);
      });
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
  }

  protected abstract EsQueryExecutor getExecutor();

  protected abstract NamedQuerierResolver<String, Object, EsNamedQuerier> getResolver();

  protected String resolveIndexName(String q) {
    int pos = 0;
    shouldBeTrue(isNotBlank(q) && (pos = q.indexOf('.')) != -1);
    return q.substring(0, pos);
  }

  protected String resolveScript(Map<Object, Object> s, Integer offset, Integer limit) {
    try {
      if (offset != null) {
        s.put("from", offset);
      }
      if (limit != null) {
        s.put("size", limit);
      }
      return QueryObjectMapper.OM.writer(JsonpCharacterEscapes.instance()).writeValueAsString(s);
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  protected <T> Pair<Long, List<T>> searchHits(String q, EsNamedQuerier querier, Integer offset,
      Integer limit) {
    String script = resolveScript(querier.getScript(null), offset, limit);
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

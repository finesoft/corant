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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.shared.util.ObjectUtils.Pair;
import org.corant.suites.query.shared.AbstractNamedQuerierResolver;
import org.corant.suites.query.shared.AbstractNamedQueryService;
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

  public static final String PRO_KEY_INDEX_NAME = ".index-name";

  @Override
  public Map<String, Object> aggregate(String queryName, Object parameter) {
    EsNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    String script = resolveScript(querier.getScript(null), null, null);
    try {
      Map<String, Object> result =
          getExecutor().searchAggregation(resolveIndexName(querier), script);
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
    EsNamedQuerier querier = getQuerierResolver().resolve(refQueryName, fetchParam);
    String script = resolveScript(querier.getScript(null), null, maxSize > 0 ? maxSize : null);
    try {
      log("fetch-> " + refQueryName, querier.getQueryParameter(), script);
      List<Map<String, Object>> fetchedList =
          getExecutor().searchHits(resolveIndexName(querier), script).getValue();
      fetch(fetchedList, querier);
      querier.resolveResultHints(fetchedList);
      if (result instanceof List) {
        parentQuerier.resolveFetchedResult((List<?>) result, fetchedList, fetchQuery);
      } else {
        parentQuerier.resolveFetchedResult(result, fetchedList, fetchQuery);
      }
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the fetch query [%s].",
          fetchQuery.getReferenceQuery());
    }
  }

  @Override
  public <T> Forwarding<T> forward(String queryName, Object parameter) {
    EsNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    int offset = resolveOffset(querier);
    int limit = resolveLimit(querier);
    Pair<Long, List<T>> hits = searchHits(queryName, querier, offset, limit);
    List<T> result = hits.getValue();
    return Forwarding.of(result, hits.getLeft() > offset + limit);
  }

  @Override
  public <T> T get(String queryName, Object parameter) {
    EsNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Pair<Long, List<T>> hits = searchHits(queryName, querier, 0, 1);
    List<T> result = hits.getValue();
    if (!isEmpty(result)) {
      return result.get(0);
    }
    return null;
  }

  @Override
  public <T> Paging<T> page(String queryName, Object parameter) {
    EsNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    int offset = resolveOffset(querier);
    int limit = resolveLimit(querier);
    Pair<Long, List<T>> hits = searchHits(queryName, querier, offset, limit);
    List<T> result = hits.getValue();
    return Paging.of(hits.getLeft().intValue(), result, offset, limit);
  }

  @Override
  public Map<String, Object> search(String queryName, Object parameter) {
    EsNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    String script = resolveScript(querier.getScript(null), null, null);
    try {
      Map<String, Object> result = getExecutor().search(resolveIndexName(querier), script);
      querier.resolveResultHints(result);
      return result;
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the search query [%s].",
          queryName);
    }
  }

  @Override
  public <T> List<T> select(String queryName, Object parameter) {
    EsNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Pair<Long, List<T>> hits = searchHits(queryName, querier, null, resolveMaxSelectSize(querier));
    return hits.getValue();
  }

  @Override
  public <T> Stream<T> stream(String q, Object param) {
    EsNamedQuerier querier = getQuerierResolver().resolve(q, param);
    String script = resolveScript(querier.getScript(null), null, null);
    log("stream-> " + q, querier.getQueryParameter(), script);
    try {
      return getExecutor().stream(resolveIndexName(querier), script).map(result -> {
        this.fetch(result, querier);
        return querier.resolveResult(result);
      });
    } catch (Exception e) {
      throw new QueryRuntimeException(e);
    }
  }

  protected abstract EsQueryExecutor getExecutor();

  @Override
  protected abstract AbstractNamedQuerierResolver<EsNamedQuerier> getQuerierResolver();

  protected String resolveIndexName(EsNamedQuerier querier) {
    String indexName = resolveProperties(querier, PRO_KEY_INDEX_NAME, String.class, null);
    return isNotBlank(indexName) ? indexName : split(querier.getQuery().getName(), ".")[0];
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
          getExecutor().searchHits(resolveIndexName(querier), script);
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

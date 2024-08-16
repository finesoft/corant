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
package org.corant.modules.query.elastic;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.modules.query.FetchableNamedQuerier;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.AbstractNamedQueryService;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.elasticsearch.common.unit.TimeValue;

/**
 * corant-modules-query-elastic
 *
 * @author bingo 下午8:20:43
 */
public abstract class AbstractEsNamedQueryService extends AbstractNamedQueryService
    implements EsNamedQueryService {

  public static final String PRO_KEY_INDEX_NAME = ".index-name";

  @Override
  public Map<String, Object> aggregate(String queryName, Object parameter) {
    try {
      Query query = getQuery(queryName);
      QueryParameter queryParameter = resolveQueryParameter(query, parameter);
      EsNamedQuerier querier = getQuerierResolver().resolve(query, queryParameter);
      String script = resolveScript(querier.getScript(), null, null);
      Map<String, Object> result = getExecutor().searchAggregation(resolveIndexName(querier),
          script, querier.getQuery().getProperties());
      querier.handleResultHints(result);
      return result;
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the aggregate query [%s].", queryName);
    }
  }

  @Override
  public FetchedResult fetch(Object result, FetchQuery fetchQuery,
      FetchableNamedQuerier parentQuerier) {
    try {
      QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
      String refQueryName = fetchQuery.getReferenceQuery().getVersionedName();
      EsNamedQuerier querier = getQuerierResolver().resolve(getQuery(refQueryName), fetchParam);
      int maxFetchSize = querier.resolveMaxFetchSize(result, fetchQuery);
      String script =
          resolveScript(querier.getScript(), null, maxFetchSize > 0 ? maxFetchSize : null);
      log("fetch-> " + refQueryName, querier.getQueryParameter(), script);
      List<Map<String, Object>> fetchedList = getExecutor().searchHits(resolveIndexName(querier),
          script, querier.getQuery().getProperties(), querier.getHintKeys()).getValue();
      return new FetchedResult(fetchQuery, querier, fetchedList);
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the fetch query [%s], exception [%s].",
          fetchQuery.getReferenceQuery().getVersionedName(), e.getMessage());
    }
  }

  @Override
  public <T> Stream<T> scrolledSearch(String queryName, Object parameter, TimeValue scrollKeepAlive,
      int batchSize) {
    try {
      Query query = getQuery(queryName);
      QueryParameter queryParameter = resolveQueryParameter(query, parameter);
      EsNamedQuerier querier = getQuerierResolver().resolve(query, queryParameter);
      String script = resolveScript(querier.getScript(), null, null);
      log("scrolled search-> " + queryName, querier.getQueryParameter(), script);
      return getExecutor()
          .scrolledSearch(resolveIndexName(querier), script, scrollKeepAlive, batchSize)
          .map(result -> {
            handleFetching(result, querier);
            return querier.handleResult(result);
          });
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the scrolled search [%s], exception [%s].", queryName,
          e.getMessage());
    }
  }

  @Override
  public Map<String, Object> search(String queryName, Object parameter) {
    try {
      Query query = getQuery(queryName);
      QueryParameter queryParameter = resolveQueryParameter(query, parameter);
      EsNamedQuerier querier = getQuerierResolver().resolve(query, queryParameter);
      String script = resolveScript(querier.getScript(), null, null);
      Map<String, Object> result = getExecutor().search(resolveIndexName(querier), script,
          querier.getQuery().getProperties());
      querier.handleResultHints(result);
      return result;
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the search query [%s].",
          queryName);
    }
  }

  @Override
  protected <T> Forwarding<T> doForward(Query query, QueryParameter parameter) throws Exception {
    EsNamedQuerier querier = getQuerierResolver().resolve(query, parameter);
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Pair<Long, List<T>> hits = searchHits(query.getVersionedName(), querier, offset, limit);
    List<T> result = hits.getValue();
    return Forwarding.of(result, hits.getLeft() > offset + limit);
  }

  @Override
  protected <T> T doGet(Query query, QueryParameter parameter) throws Exception {
    EsNamedQuerier querier = getQuerierResolver().resolve(query, parameter);
    Pair<Long, List<T>> hits = searchHits(query.getVersionedName(), querier, 0, 1);
    List<T> result = hits.getValue();
    if (!isEmpty(result)) {
      return result.get(0);
    }
    return null;
  }

  @Override
  protected <T> Paging<T> doPage(Query query, QueryParameter parameter) throws Exception {
    EsNamedQuerier querier = getQuerierResolver().resolve(query, parameter);
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Pair<Long, List<T>> hits = searchHits(query.getVersionedName(), querier, offset, limit);
    List<T> result = hits.getValue();
    return Paging.of(hits.getLeft().intValue(), result, offset, limit);
  }

  @Override
  protected <T> List<T> doSelect(Query query, QueryParameter parameter) throws Exception {
    EsNamedQuerier querier = getQuerierResolver().resolve(query, parameter);
    Pair<Long, List<T>> hits =
        searchHits(query.getVersionedName(), querier, null, querier.resolveSelectSize());
    return hits.getValue();
  }

  protected abstract EsQueryExecutor getExecutor();

  protected abstract AbstractNamedQuerierResolver<EsNamedQuerier> getQuerierResolver();

  @Override
  protected Query getQuery(String queryName) {
    return getQuerierResolver().resolveQuery(queryName);
  }

  @Override
  protected QueryHandler getQueryHandler() {
    return getQuerierResolver().getQueryHandler();
  }

  protected String resolveIndexName(EsNamedQuerier querier) {
    String indexName = querier.resolveProperty(PRO_KEY_INDEX_NAME, String.class, null);
    return isNotBlank(indexName) ? indexName : querier.getIndexName();// FIXME
  }

  protected String resolveScript(Map<Object, Object> s, Integer offset, Integer limit) {
    if (offset != null) {
      s.put("from", offset);
    }
    if (limit != null) {
      s.put("size", limit);
    }
    return getQuerierResolver().getQueryHandler().getObjectMapper().toJsonString(s, true, false);
  }

  protected <T> Pair<Long, List<T>> searchHits(String q, EsNamedQuerier querier, Integer offset,
      Integer limit) throws Exception {
    String script = resolveScript(querier.getScript(), offset, limit);
    log(q, querier.getQueryParameter(), script);
    Map<String, String> properties = new HashMap<>(querier.getQuery().getProperties());
    if (querier.resolveTimeout() != null) {
      properties.put(EsQueryExecutor.PRO_KEY_ACT_GET_TIMEOUT, querier.resolveTimeout().toString());
    }
    Pair<Long, List<Map<String, Object>>> hits = getExecutor().searchHits(resolveIndexName(querier),
        script, properties, querier.getHintKeys());
    List<T> result = new ArrayList<>();
    if (!isEmpty(hits.getValue())) {
      handleFetching(hits.getValue(), querier);
      result = querier.handleResults(hits.getValue());
    }
    return Pair.of(hits.getLeft(), result);
  }
}

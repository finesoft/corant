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
package org.corant.modules.query.cassandra;

import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.split;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.corant.modules.query.Querier;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.AbstractNamedQueryService;

/**
 * corant-modules-query-cassandra
 *
 * @author bingo 下午5:33:21
 *
 */
public abstract class AbstractCasNamedQueryService extends AbstractNamedQueryService {

  public static final String PRO_KEY_KEYSPACE = "cassandra.query.keyspace";

  @Override
  public FetchResult fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier) {
    try {
      QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
      String refQueryName = fetchQuery.getReferenceQuery().getVersionedName();
      CasNamedQuerier querier = getQuerierResolver().resolve(refQueryName, fetchParam);
      int maxFetchSize = querier.resolveMaxFetchSize(result, fetchQuery);
      String cql = querier.getScript();
      Duration timeout = querier.resolveTimeout();
      String ks = resolveKeyspace(querier);
      Object[] scriptParameter = querier.getScriptParameter();
      log("fetch-> " + refQueryName, scriptParameter, cql);
      List<Map<String, Object>> fetchedList;
      if (maxFetchSize > 0) {
        fetchedList = getExecutor().paging(ks, cql, 0, maxFetchSize, timeout, scriptParameter);
      } else {
        fetchedList = getExecutor().select(ks, cql, timeout, scriptParameter);
      }
      return new FetchResult(fetchQuery, querier, fetchedList);
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the fetch query [%s], exception [%s].",
          fetchQuery.getReferenceQuery().getVersionedName(), e.getMessage());
    }
  }

  @Override
  protected <T> Forwarding<T> doForward(String queryName, Object parameter) throws Exception {
    CasNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String cql = querier.getScript();
    String ks = resolveKeyspace(querier);
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Duration timeout = querier.resolveTimeout();
    log(queryName, scriptParameter, cql);
    Forwarding<T> result = Forwarding.inst();
    List<Map<String, Object>> list =
        getExecutor().paging(ks, cql, offset, limit + 1, timeout, scriptParameter);
    int size = sizeOf(list);
    if (size > 0) {
      if (size > limit) {
        list.remove(limit);
        result.withHasNext(true);
      }
      this.fetch(list, querier);
    }
    return result.withResults(querier.handleResults(list));
  }

  @Override
  protected <T> T doGet(String queryName, Object parameter) throws Exception {
    CasNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String cql = querier.getScript();
    String ks = resolveKeyspace(querier);
    Duration timeout = querier.resolveTimeout();
    log(queryName, scriptParameter, cql);
    Map<String, Object> result = getExecutor().get(ks, cql, timeout, scriptParameter);
    this.fetch(result, querier);
    return querier.handleResult(result);
  }

  @Override
  protected <T> Paging<T> doPage(String queryName, Object parameter) throws Exception {
    CasNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String cql = querier.getScript();
    String ks = resolveKeyspace(querier);
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Duration timeout = querier.resolveTimeout();
    log(queryName, scriptParameter, cql);
    List<Map<String, Object>> list =
        getExecutor().paging(ks, cql, offset, limit, timeout, scriptParameter);
    Paging<T> result = Paging.of(offset, limit);
    int size = sizeOf(list);
    if (size > 0) {
      if (size < limit) {
        result.withTotal(offset + size);
      } else {
        result.withTotal(getExecutor().total(ks, cql, timeout, scriptParameter));
      }
      this.fetch(list, querier);
    }
    return result.withResults(querier.handleResults(list));
  }

  @Override
  protected <T> List<T> doSelect(String queryName, Object parameter) throws Exception {
    CasNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String cql = querier.getScript();
    String ks = resolveKeyspace(querier);
    int maxSelectSize = querier.resolveMaxSelectSize();
    Duration timeout = querier.resolveTimeout();
    log(queryName, scriptParameter, cql);
    List<Map<String, Object>> results =
        getExecutor().paging(ks, cql, 0, maxSelectSize + 1, timeout, scriptParameter);
    if (querier.handleResultSize(results) > 0) {
      this.fetch(results, querier);
    }
    return querier.handleResults(results);
  }

  protected abstract CasQueryExecutor getExecutor();

  @Override
  protected abstract AbstractNamedQuerierResolver<CasNamedQuerier> getQuerierResolver();

  /**
   * Resolve key space from query parameter context or query object.
   *
   * @param querier
   * @return resolveKeyspace
   */
  protected String resolveKeyspace(CasNamedQuerier querier) {
    String keyspace = querier.resolveProperty(PRO_KEY_KEYSPACE, String.class, null);
    return isNotBlank(keyspace) ? keyspace : split(querier.getQuery().getName(), ".")[0];
  }
}

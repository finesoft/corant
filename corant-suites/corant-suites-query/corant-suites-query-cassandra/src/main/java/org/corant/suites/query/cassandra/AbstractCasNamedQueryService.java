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
package org.corant.suites.query.cassandra;

import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.util.List;
import java.util.Map;
import org.corant.suites.query.shared.AbstractNamedQuerierResolver;
import org.corant.suites.query.shared.AbstractNamedQueryService;
import org.corant.suites.query.shared.Querier;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.FetchQuery;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:33:21
 *
 */
public abstract class AbstractCasNamedQueryService extends AbstractNamedQueryService {

  public static final String PRO_KEY_KEYSPACE = "cassandra.query.keyspace";

  @Override
  public void fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier) {
    QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
    int maxSize = fetchQuery.isMultiRecords() ? fetchQuery.getMaxSize() : 1;
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    CasNamedQuerier querier = getQuerierResolver().resolve(refQueryName, fetchParam);
    String cql = querier.getScript(null);
    String ks = resolveKeyspace(querier);
    Object[] scriptParameter = querier.getScriptParameter();
    try {
      log("fetch-> " + refQueryName, scriptParameter, cql);
      List<Map<String, Object>> fetchedList;
      if (maxSize > 0) {
        fetchedList = getExecutor().paging(ks, cql, 0, maxSize, scriptParameter);
      } else {
        fetchedList = getExecutor().select(ks, cql, scriptParameter);
      }
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
    CasNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String cql = querier.getScript(null);
    String ks = resolveKeyspace(querier);
    int offset = resolveOffset(querier);
    int limit = resolveLimit(querier);
    try {
      log(queryName, scriptParameter, cql);
      Forwarding<T> result = Forwarding.inst();
      List<Map<String, Object>> list =
          getExecutor().paging(ks, cql, offset, limit + 1, scriptParameter);
      int size = sizeOf(list);
      if (size > 0) {
        if (size > limit) {
          list.remove(size - 1);
          result.withHasNext(true);
        }
        this.fetch(list, querier);
      }
      return result.withResults(querier.resolveResult(list));
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the forward query [%s].", queryName);
    }
  }

  @Override
  public <T> T get(String queryName, Object parameter) {
    CasNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String cql = querier.getScript(null);
    String ks = resolveKeyspace(querier);
    try {
      log(queryName, scriptParameter, cql);
      Map<String, Object> result = getExecutor().get(ks, cql, scriptParameter);
      this.fetch(result, querier);
      return querier.resolveResult(result);
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the get query [%s].",
          queryName);
    }
  }

  @Override
  public <T> Paging<T> page(String queryName, Object parameter) {
    CasNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String cql = querier.getScript(null);
    String ks = resolveKeyspace(querier);
    int offset = resolveOffset(querier);
    int limit = resolveLimit(querier);
    try {
      log(queryName, scriptParameter, cql);
      List<Map<String, Object>> list =
          getExecutor().paging(ks, cql, offset, limit, scriptParameter);
      Paging<T> result = Paging.of(offset, limit);
      int size = sizeOf(list);
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          result.withTotal(getExecutor().total(ks, cql, scriptParameter));
        }
        this.fetch(list, querier);
      }
      return result.withResults(querier.resolveResult(list));
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the page query [%s].",
          queryName);
    }
  }

  @Override
  public <T> List<T> select(String queryName, Object parameter) {
    CasNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String cql = querier.getScript(null);
    String ks = resolveKeyspace(querier);
    int maxSelectSize = resolveMaxSelectSize(querier);
    try {
      log(queryName, scriptParameter, cql);
      List<Map<String, Object>> results =
          getExecutor().paging(ks, cql, 0, maxSelectSize + 1, scriptParameter);
      int size = sizeOf(results);
      if (size > 0) {
        if (size > maxSelectSize) {
          throw new QueryRuntimeException(
              "[%s] Result record number overflow, the allowable range is %s.", queryName,
              maxSelectSize);
        }
        this.fetch(results, querier);
      }
      return querier.resolveResult(results);
    } catch (Exception e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the select query [%s].",
          queryName);
    }
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
  protected String resolveKeyspace(Querier querier) {
    String keyspace = resolveProperties(querier, PRO_KEY_KEYSPACE, String.class, null);
    return isNotBlank(keyspace) ? keyspace : split(querier.getQuery().getName(), ".")[0];
  }
}

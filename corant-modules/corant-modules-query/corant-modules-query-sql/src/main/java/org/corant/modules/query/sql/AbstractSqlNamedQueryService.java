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
package org.corant.modules.query.sql;

import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Maps.getMapInteger;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Streams.batchStream;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.modules.query.Querier;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryParameter.StreamQueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.AbstractNamedQueryService;
import org.corant.modules.query.sql.dialect.Dialect;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午5:33:21
 *
 */
public abstract class AbstractSqlNamedQueryService extends AbstractNamedQueryService {

  @Override
  public FetchResult fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier) {
    try {
      QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
      String refQueryName = fetchQuery.getReferenceQuery().getVersionedName();
      SqlNamedQuerier querier = getQuerierResolver().resolve(refQueryName, fetchParam);
      int maxFetchSize = querier.resolveMaxFetchSize(result, fetchQuery);
      String sql = querier.getScript();
      Duration timeout = querier.resolveTimeout();
      Object[] scriptParameter = querier.getScriptParameter();
      log("fetch-> " + refQueryName, scriptParameter, sql);
      return new FetchResult(fetchQuery, querier,
          getExecutor().select(sql, maxFetchSize, timeout, scriptParameter));
    } catch (SQLException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the fetch query [%s], exception [%s].",
          fetchQuery.getReferenceQuery().getVersionedName(), e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * If the value of query parameter offset > 0 or the value of query parameter retry time > 0 or
   * the enhancer of query parameter is not null then this method use
   * {@link #forward(String, Object)} to fetch next data records; otherwise, perform a complete
   * query.
   * </p>
   *
   * @see AbstractNamedQueryService#doStream(String, StreamQueryParameter)
   */
  @Override
  public <T> Stream<T> stream(String queryName, Object parameter) {
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    QueryParameter queryParam = querier.getQueryParameter();
    StreamQueryParameter useQueryParam;
    if (queryParam instanceof StreamQueryParameter) {
      useQueryParam = (StreamQueryParameter) queryParam;
    } else {
      useQueryParam = new StreamQueryParameter(queryParam);
    }
    useQueryParam.limit(max(querier.resolveStreamLimit(), 1));
    if (useQueryParam.getOffset() > 0 || useQueryParam.needRetry()
        || useQueryParam.getEnhancer() != null) {
      return doStream(queryName, useQueryParam);
    } else {
      Object[] scriptParameter = querier.getScriptParameter();
      String sql = querier.getScript();
      Duration timeout = querier.resolveTimeout();
      log(queryName, scriptParameter, sql);
      return batchStream(querier.getQueryParameter().getLimit(),
          getExecutor().stream(sql, useQueryParam.getTerminater(), timeout, scriptParameter))
              .flatMap(list -> {
                this.fetch(list, querier);
                List<T> results = querier.handleResults(list);
                return results.stream();
              });
    }
  }

  @Override
  protected <T> Forwarding<T> doForward(String queryName, Object parameter) throws Exception {
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Duration timeout = querier.resolveTimeout();
    Map<String, String> properties = querier.getQuery().getProperties();
    String limitSql = getDialect().getLimitSql(sql, offset, limit + 1, properties);
    log(queryName, scriptParameter, sql, "Limit: " + limitSql);
    Forwarding<T> result = Forwarding.inst();
    List<Map<String, Object>> list = getExecutor().select(limitSql, timeout, scriptParameter);
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
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    Duration timeout = querier.resolveTimeout();
    log(queryName, scriptParameter, sql);
    Map<String, Object> result = getExecutor().get(sql, timeout, scriptParameter);
    this.fetch(result, querier);
    return querier.handleResult(result);

  }

  @Override
  protected <T> Paging<T> doPage(String queryName, Object parameter) throws Exception {
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Duration timeout = querier.resolveTimeout();
    Map<String, String> properties = querier.getQuery().getProperties();
    String limitSql = getDialect().getLimitSql(sql, offset, limit, properties);
    log(queryName, scriptParameter, sql, "Limit: " + limitSql);
    List<Map<String, Object>> list = getExecutor().select(limitSql, timeout, scriptParameter);
    Paging<T> result = Paging.of(offset, limit);
    int size = sizeOf(list);
    if (size > 0) {
      if (size < limit) {
        result.withTotal(offset + size);
      } else {
        String totalSql = getDialect().getCountSql(sql, properties);
        log("total-> " + queryName, scriptParameter, totalSql);
        result.withTotal(getMapInteger(getExecutor().get(totalSql, timeout, scriptParameter),
            Dialect.COUNT_FIELD_NAME));
      }
      this.fetch(list, querier);
    }
    return result.withResults(querier.handleResults(list));
  }

  @Override
  protected <T> List<T> doSelect(String queryName, Object parameter) throws Exception {
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int maxSelectSize = querier.resolveMaxSelectSize();
    Duration timeout = querier.resolveTimeout();
    // sql = getDialect().getLimitSql(sql, maxSelectSize + 1);
    log(queryName, scriptParameter, sql);
    List<Map<String, Object>> results =
        getExecutor().select(sql, maxSelectSize + 1, timeout, scriptParameter);
    if (querier.handleResultSize(results) > 0) {
      this.fetch(results, querier);
    }
    return querier.handleResults(results);
  }

  protected Dialect getDialect() {
    return getExecutor().getDialect();
  }

  protected abstract SqlQueryExecutor getExecutor();// FIXME use one connection for one method stack

  @Override
  protected abstract AbstractNamedQuerierResolver<SqlNamedQuerier> getQuerierResolver();

}

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
import static org.corant.shared.util.Streams.batchStream;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.corant.modules.query.FetchableNamedQuerier;
import org.corant.modules.query.QueryHandler;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.StreamQueryParameter;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.AbstractNamedQueryService;
import org.corant.modules.query.sql.dialect.Dialect;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午5:33:21
 */
public abstract class AbstractSqlNamedQueryService extends AbstractNamedQueryService {

  @Override
  public FetchedResult fetch(Object result, FetchQuery fetchQuery,
      FetchableNamedQuerier parentQuerier) {
    try {
      QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
      String refQueryName = fetchQuery.getQueryReference().getVersionedName();
      SqlNamedQuerier querier = getQuerierResolver().resolve(getQuery(refQueryName), fetchParam);
      int maxFetchSize = querier.resolveMaxFetchSize(result, fetchQuery);
      String sql = querier.getScript();
      Duration timeout = querier.resolveTimeout();
      Object[] scriptParameter = querier.getScriptParameter();
      log("fetch-> " + refQueryName, scriptParameter, sql);
      return new FetchedResult(fetchQuery, querier,
          getExecutor().select(sql, maxFetchSize, timeout, scriptParameter));
    } catch (SQLException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the fetch query [%s], exception [%s].",
          fetchQuery.getQueryReference().getVersionedName(), e.getMessage());
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
   * @see AbstractNamedQueryService#doStream(Query, StreamQueryParameter)
   */
  @Override
  public <T> Stream<T> stream(String queryName, Object parameter) {
    Query query = getQuery(queryName);
    QueryParameter queryParam = resolveQueryParameter(query, parameter);
    StreamQueryParameter useQueryParam;
    if (queryParam instanceof StreamQueryParameter) {
      useQueryParam = (StreamQueryParameter) queryParam;
    } else {
      useQueryParam = new StreamQueryParameter(queryParam);
    }
    if (useQueryParam.getOffset() > 0 || useQueryParam.needRetry()
        || useQueryParam.getEnhancer() != null) {
      useQueryParam.limit(resolveStreamLimit(query, useQueryParam));
      return doStream(query, useQueryParam);
    } else {
      SqlNamedQuerier querier = getQuerierResolver().resolve(query, queryParam);
      Object[] scriptParameter = querier.getScriptParameter();
      String sql = querier.getScript();
      Duration timeout = querier.resolveTimeout();
      log(queryName, scriptParameter, sql);
      return batchStream(querier.resolveStreamLimit(), getExecutor().stream(sql,
          useQueryParam.getTerminator(), timeout, useQueryParam.isAutoClose(), scriptParameter))
              .flatMap(list -> {
                handleFetching(list, querier);
                List<T> results = querier.handleResults(list);
                return results.stream();
              });
    }
  }

  @Override
  protected <T> Forwarding<T> doForward(Query query, QueryParameter parameter) throws SQLException {
    SqlNamedQuerier querier = getQuerierResolver().resolve(query, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Duration timeout = querier.resolveTimeout();
    Map<String, String> properties = querier.getQuery().getProperties();
    String limitSql = getDialect().getLimitSql(sql, offset, limit + 1, properties);
    log(query.getVersionedName(), scriptParameter, sql, "Limit script: " + limitSql);
    Forwarding<T> result = Forwarding.inst();
    List<Map<String, Object>> list = getExecutor().select(limitSql, timeout, scriptParameter);
    int size = sizeOf(list);
    if (size > 0) {
      if (size > limit) {
        list.remove(limit);
        result.withHasNext(true);
      }
      handleFetching(list, querier);
    }
    return result.withResults(querier.handleResults(list));

  }

  @Override
  protected <T> T doGet(Query query, QueryParameter parameter) throws SQLException {
    SqlNamedQuerier querier = getQuerierResolver().resolve(query, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    Duration timeout = querier.resolveTimeout();
    log(query.getVersionedName(), scriptParameter, sql);
    Map<String, Object> result = getExecutor().get(sql, timeout, scriptParameter);
    handleFetching(result, querier);
    return querier.handleResult(result);

  }

  @Override
  protected <T> Paging<T> doPage(Query query, QueryParameter parameter) throws SQLException {
    SqlNamedQuerier querier = getQuerierResolver().resolve(query, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Duration timeout = querier.resolveTimeout();
    Map<String, String> properties = querier.getQuery().getProperties();
    String limitSql = getDialect().getLimitSql(sql, offset, limit, properties);
    log(query.getVersionedName(), scriptParameter, sql, "Limit script: " + limitSql);
    List<Map<String, Object>> list = getExecutor().select(limitSql, timeout, scriptParameter);
    Paging<T> result = Paging.of(offset, limit);
    int size = sizeOf(list);
    if (size > 0) {
      if (size < limit) {
        result.withTotal(offset + size);
      } else {
        String totalSql = getDialect().getCountSql(sql, properties);
        log(query.getVersionedName() + " -> total", scriptParameter, totalSql);
        result.withTotal(getMapInteger(getExecutor().get(totalSql, timeout, scriptParameter),
            Dialect.COUNT_FIELD_NAME));
      }
      handleFetching(list, querier);
    }
    return result.withResults(querier.handleResults(list));
  }

  @Override
  protected <T> List<T> doSelect(Query query, QueryParameter parameter) throws SQLException {
    SqlNamedQuerier querier = getQuerierResolver().resolve(query, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int maxSelectSize = querier.resolveSelectSize();
    Duration timeout = querier.resolveTimeout();
    // sql = getDialect().getLimitSql(sql, maxSelectSize + 1);
    log(query.getVersionedName(), scriptParameter, sql);
    List<Map<String, Object>> results =
        getExecutor().select(sql, maxSelectSize + 1, timeout, scriptParameter);
    if (querier.handleResultSize(results) > 0) {
      handleFetching(results, querier);
    }
    return querier.handleResults(results);
  }

  protected Dialect getDialect() {
    return getExecutor().getDialect();
  }

  protected abstract SqlQueryExecutor getExecutor();// FIXME use one connection for one method stack

  protected abstract AbstractNamedQuerierResolver<SqlNamedQuerier> getQuerierResolver();

  @Override
  protected Query getQuery(String queryName) {
    return getQuerierResolver().resolveQuery(queryName);
  }

  @Override
  protected QueryHandler getQueryHandler() {
    return getQuerierResolver().getQueryHandler();
  }

}

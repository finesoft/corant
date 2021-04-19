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

import static org.corant.shared.util.Maps.getMapInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.AbstractNamedQueryService;
import org.corant.modules.query.shared.Querier;
import org.corant.modules.query.shared.QueryParameter;
import org.corant.modules.query.shared.QueryRuntimeException;
import org.corant.modules.query.shared.mapping.FetchQuery;
import org.corant.modules.query.sql.dialect.Dialect;

/**
 * corant-modules-query-sql
 *
 * @author bingo 下午5:33:21
 *
 */
public abstract class AbstractSqlNamedQueryService extends AbstractNamedQueryService {

  @Override
  public void fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier) {
    try {
      QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
      int maxSize = fetchQuery.getMaxSize();
      String refQueryName = fetchQuery.getReferenceQuery().getVersionedName();
      SqlNamedQuerier querier = getQuerierResolver().resolve(refQueryName, fetchParam);
      String sql = querier.getScript();
      Object[] scriptParameter = querier.getScriptParameter();
      // if (maxSize > 0) {
      // sql = getDialect().getLimitSql(sql, maxSize);
      // }
      log("fetch-> " + refQueryName, scriptParameter, sql);
      List<Map<String, Object>> fetchedList = getExecutor().select(sql, maxSize, scriptParameter);
      postFetch(fetchQuery, querier, fetchedList, parentQuerier, result);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the fetch query [%s], exception [%s].",
          fetchQuery.getReferenceQuery().getVersionedName(), e.getMessage());
    }
  }

  @Override
  protected <T> Forwarding<T> doForward(String queryName, Object parameter) throws Exception {
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    String limitSql = getDialect().getLimitSql(sql, offset, limit + 1);
    log(queryName, scriptParameter, sql, "Limit: " + limitSql);
    Forwarding<T> result = Forwarding.inst();
    List<Map<String, Object>> list = getExecutor().select(limitSql, scriptParameter);
    int size = list == null ? 0 : list.size();
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
    log(queryName, scriptParameter, sql);
    Map<String, Object> result = getExecutor().get(sql, scriptParameter);
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
    String limitSql = getDialect().getLimitSql(sql, offset, limit);
    log(queryName, scriptParameter, sql, "Limit: " + limitSql);
    List<Map<String, Object>> list = getExecutor().select(limitSql, scriptParameter);
    Paging<T> result = Paging.of(offset, limit);
    int size = list == null ? 0 : list.size();
    if (size > 0) {
      if (size < limit) {
        result.withTotal(offset + size);
      } else {
        String totalSql = getDialect().getCountSql(sql);
        log("total-> " + queryName, scriptParameter, totalSql);
        result.withTotal(
            getMapInteger(getExecutor().get(totalSql, scriptParameter), Dialect.COUNT_FIELD_NAME));
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
    // sql = getDialect().getLimitSql(sql, maxSelectSize + 1);
    log(queryName, scriptParameter, sql);
    List<Map<String, Object>> results =
        getExecutor().select(sql, maxSelectSize + 1, scriptParameter);
    if (querier.validateResultSize(results) > 0) {
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

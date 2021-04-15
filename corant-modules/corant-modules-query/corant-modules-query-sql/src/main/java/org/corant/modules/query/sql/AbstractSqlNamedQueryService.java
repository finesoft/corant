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
    QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
    int maxSize = fetchQuery.isMultiRecords() ? fetchQuery.getMaxSize() : 1;
    String refQueryName = fetchQuery.getReferenceQuery().getVersionedName();
    SqlNamedQuerier querier = getQuerierResolver().resolve(refQueryName, fetchParam);
    String sql = querier.getScript();
    Object[] scriptParameter = querier.getScriptParameter();
    // if (maxSize > 0) {
    // sql = getDialect().getLimitSql(sql, maxSize);
    // }
    try {
      log("fetch-> " + refQueryName, scriptParameter, sql);
      List<Map<String, Object>> fetchedList = getExecutor().select(sql, maxSize, scriptParameter);
      fetch(fetchedList, querier);
      querier.handleResultHints(fetchedList);
      if (result instanceof List) {
        parentQuerier.handleFetchedResults((List<?>) result, fetchedList, fetchQuery);
      } else {
        parentQuerier.handleFetchedResult(result, fetchedList, fetchQuery);
      }
    } catch (SQLException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the fetch query [%s], exception [%s].",
          fetchQuery.getReferenceQuery(), e.getMessage());
    }
  }

  @Override
  public <T> Forwarding<T> forward(String queryName, Object parameter) {
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    String limitSql = getDialect().getLimitSql(sql, offset, limit + 1);
    try {
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
    } catch (SQLException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the forward query [%s].", queryName);
    }
  }

  @Override
  public <T> T get(String queryName, Object parameter) {
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    try {
      log(queryName, scriptParameter, sql);
      Map<String, Object> result = getExecutor().get(sql, scriptParameter);
      this.fetch(result, querier);
      return querier.handleResult(result);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the get query [%s].",
          queryName);
    }
  }

  @Override
  public <T> Paging<T> page(String queryName, Object parameter) {
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    String limitSql = getDialect().getLimitSql(sql, offset, limit);
    try {
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
          result.withTotal(getMapInteger(getExecutor().get(totalSql, scriptParameter),
              Dialect.COUNT_FIELD_NAME));
        }
        this.fetch(list, querier);
      }
      return result.withResults(querier.handleResults(list));
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the page query [%s].",
          queryName);
    }
  }

  @Override
  public <T> List<T> select(String queryName, Object parameter) {
    SqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int maxSelectSize = querier.resolveMaxSelectSize();
    try {
      // sql = getDialect().getLimitSql(sql, maxSelectSize + 1);
      log(queryName, scriptParameter, sql);
      List<Map<String, Object>> results =
          getExecutor().select(sql, maxSelectSize + 1, scriptParameter);
      if (querier.validateResultSize(results) > 0) {
        this.fetch(results, querier);
      }
      return querier.handleResults(results);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the select query [%s].",
          queryName);
    }
  }

  protected Dialect getDialect() {
    return getExecutor().getDialect();
  }

  protected abstract SqlQueryExecutor getExecutor();// FIXME use one connection for one method stack

  @Override
  protected abstract AbstractNamedQuerierResolver<SqlNamedQuerier> getQuerierResolver();

}

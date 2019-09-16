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
package org.corant.suites.query.sql;

import static org.corant.shared.util.CollectionUtils.getSize;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getMapInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.query.shared.AbstractNamedQueryService;
import org.corant.suites.query.shared.Querier;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.QueryService;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.sql.SqlNamedQueryResolver.SqlQuerier;
import org.corant.suites.query.sql.dialect.Dialect;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:33:21
 *
 */
@ApplicationScoped
public abstract class AbstractSqlNamedQueryService extends AbstractNamedQueryService {

  @Inject
  protected SqlNamedQueryResolver<String, Object> resolver;

  @Override
  public <T> ForwardList<T> forward(String queryName, Object parameter) {
    SqlQuerier querier = getResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int offset = querier.getQueryParameter().getOffset();
    int limit = querier.getQueryParameter().getLimit();
    String limitSql = getDialect().getLimitSql(sql, offset, limit + 1);
    try {
      log(queryName, scriptParameter, sql, "Limit: " + limitSql);
      ForwardList<T> result = ForwardList.inst();
      List<Map<String, Object>> list = getExecutor().select(limitSql, scriptParameter);
      int size = getSize(list);
      if (size > 0) {
        this.fetch(list, querier);
        if (size > limit) {
          list.remove(size - 1);
          result.withHasNext(true);
        }
      }
      return result.withResults(querier.resolveResult(list));
    } catch (SQLException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the forward query [%s].", queryName);
    }
  }

  @Override
  public <T> T get(String queryName, Object parameter) {
    SqlQuerier querier = getResolver().resolve(queryName, parameter);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    try {
      log(queryName, scriptParameter, sql);
      Map<String, Object> result = getExecutor().get(sql, scriptParameter);
      this.fetch(result, querier);
      return querier.resolveResult(result);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the get query [%s].",
          queryName);
    }
  }

  @Override
  public <T> PagedList<T> page(String q, Object param) {
    SqlQuerier querier = getResolver().resolve(q, param);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    int offset = querier.getQueryParameter().getOffset();
    int limit = querier.getQueryParameter().getLimit();
    String limitSql = getDialect().getLimitSql(sql, offset, limit);
    try {
      log(q, scriptParameter, sql, "Limit: " + limitSql);
      List<Map<String, Object>> list = getExecutor().select(limitSql, scriptParameter);
      PagedList<T> result = PagedList.of(offset, limit);
      int size = getSize(list);
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          String totalSql = getDialect().getCountSql(sql);
          log("total-> " + q, scriptParameter, totalSql);
          result.withTotal(getMapInteger(getExecutor().get(totalSql, scriptParameter),
              Dialect.COUNT_FIELD_NAME));
        }
        this.fetch(list, querier);
      }
      return result.withResults(querier.resolveResult(list));
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the page query [%s].",
          q);
    }
  }

  @Override
  public <T> List<T> select(String q, Object param) {
    SqlQuerier querier = getResolver().resolve(q, param);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    try {
      if (enableMaxSelect()) {
        sql =
            getDialect().getLimitSql(sql, QueryService.OFFSET_PARAM_VAL, getMaxSelectSize(querier));
      }
      log(q, scriptParameter, sql);
      List<Map<String, Object>> results = getExecutor().select(sql, scriptParameter);
      int size = getSize(results);
      if (size > 0) {
        this.fetch(results, querier);
      }
      return querier.resolveResult(results);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the select query [%s].",
          q);
    }
  }

  @Override
  public <T> Stream<T> stream(String q, Object param) {
    SqlQuerier querier = getResolver().resolve(q, param);
    Object[] scriptParameter = querier.getScriptParameter();
    String sql = querier.getScript();
    return getExecutor().stream(sql, scriptParameter).map(result -> {
      this.fetch(result, querier);
      return querier.resolveResult(result);
    });
  }

  protected boolean enableMaxSelect() {
    return true;
  }

  @Override
  protected <T> void fetch(T result, FetchQuery fetchQuery, Querier parentQuerier) {
    if (null == result || fetchQuery == null) {
      return;
    }
    Map<String, Object> fetchParam = parentQuerier.resolveFetchQueryCriteria(result, fetchQuery);
    if (!decideFetch(result, fetchQuery, fetchParam)) {
      return;
    }
    int maxSize = fetchQuery.getMaxSize();
    boolean multiRecords = fetchQuery.isMultiRecords();
    String injectProName = fetchQuery.getInjectPropertyName();
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    SqlQuerier querier = resolver.resolve(refQueryName, fetchParam);
    String sql = querier.getScript();
    Object[] scriptParameter = querier.getScriptParameter();
    if (maxSize > 0) {
      sql = getDialect().getLimitSql(sql, QueryService.OFFSET_PARAM_VAL, maxSize);
    }
    try {
      log("fetch-> " + refQueryName, scriptParameter, sql);
      List<Map<String, Object>> fetchedList = getExecutor().select(sql, scriptParameter);
      Object fetchedResult = null;
      if (multiRecords) {
        fetchedResult = fetchedList;
        this.fetch((List<?>) fetchedResult, querier);
      } else if (!isEmpty(fetchedList)) {
        fetchedResult = fetchedList.get(0);
        this.fetch(fetchedResult, querier);
      }
      querier.resolveResultHints(fetchedResult);
      querier.resolveFetchedResult(result, fetchedResult, injectProName);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the fetch query [%s].",
          fetchQuery.getReferenceQuery());
    }
  }

  protected Dialect getDialect() {
    return getExecutor().getDialect();
  }

  protected abstract SqlQueryExecutor getExecutor();// FIXME use one connection for one method stack

  protected SqlNamedQueryResolver<String, Object> getResolver() {
    return resolver;
  }
}

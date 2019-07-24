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
import static org.corant.suites.query.shared.QueryUtils.convert;
import static org.corant.suites.query.shared.QueryUtils.getLimit;
import static org.corant.suites.query.shared.QueryUtils.getOffset;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.suites.query.shared.AbstractNamedQuery;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.QueryUtils;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.sql.SqlNamedQueryResolver.SqlQuerier;
import org.corant.suites.query.sql.dialect.Dialect;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:33:21
 *
 */
@ApplicationScoped
public abstract class AbstractSqlNamedQuery extends AbstractNamedQuery {

  @Inject
  protected SqlNamedQueryResolver<String, Map<String, Object>> resolver;

  @Override
  public <T> ForwardList<T> forward(String q, Map<String, Object> param) {
    SqlQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    String sql = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    String limitSql = getDialect().getLimitSql(sql, offset, limit + 1);
    try {
      log(q, queryParam, sql, "Limit: " + limitSql);
      ForwardList<T> result = ForwardList.inst();
      List<Map<String, Object>> list = getExecutor().select(limitSql, queryParam);
      int size = getSize(list);
      if (size > 0) {
        this.fetch(list, fetchQueries, param);
        if (size > limit) {
          list.remove(size - 1);
          result.withHasNext(true);
        }
        handleResultHints(resultClass, hints, param, list);
      }
      return result.withResults(convert(list, resultClass));
    } catch (SQLException e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the forward query [%s].", q);
    }
  }

  @Override
  public <T> T get(String q, Map<String, Object> param) {
    SqlQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    String sql = querier.getScript();
    try {
      log(q, queryParam, sql);
      Map<String, Object> result = getExecutor().get(sql, queryParam);
      this.fetch(result, fetchQueries, param);
      handleResultHints(resultClass, hints, param, result);
      return convert(result, resultClass);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the get query [%s].",
          q);
    }
  }

  @Override
  public <T> PagedList<T> page(String q, Map<String, Object> param) {
    SqlQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    String sql = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    String limitSql = getDialect().getLimitSql(sql, offset, limit);
    try {
      log(q, queryParam, sql, "Limit: " + limitSql);
      List<Map<String, Object>> list = getExecutor().select(limitSql, queryParam);
      PagedList<T> result = PagedList.of(offset, limit);
      int size = getSize(list);
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          String totalSql = getDialect().getCountSql(sql);
          log("total-> " + q, queryParam, totalSql);
          result.withTotal(
              getMapInteger(getExecutor().get(totalSql, queryParam), Dialect.COUNT_FIELD_NAME));
        }
        this.fetch(list, fetchQueries, param);
        handleResultHints(resultClass, hints, param, list);
      }
      return result.withResults(convert(list, resultClass));
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the page query [%s].",
          q);
    }
  }

  @Override
  public <T> List<T> select(String q, Map<String, Object> param) {
    SqlQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    String sql = querier.getScript();
    try {
      if (enableMaxSelect()) {
        sql = getDialect().getLimitSql(sql, QueryUtils.OFFSET_PARAM_VAL, getMaxSelectSize(querier));
      }
      log(q, queryParam, sql);
      List<Map<String, Object>> result = getExecutor().select(sql, queryParam);
      int size = getSize(result);
      if (size > 0) {
        this.fetch(result, fetchQueries, param);
        handleResultHints(resultClass, hints, param, result);
      }
      return convert(result, resultClass);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the select query [%s].",
          q);
    }
  }

  @Override
  public <T> Stream<T> stream(String q, Map<String, Object> param) {
    SqlQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    String sql = querier.getScript();
    return getExecutor().stream(sql, queryParam).map(result -> {
      this.fetch(result, fetchQueries, param);
      handleResultHints(resultClass, hints, param, result);
      return convert(result, resultClass);
    });
  }

  protected boolean enableMaxSelect() {
    return true;
  }

  @Override
  protected <T> void fetch(T obj, FetchQuery fetchQuery, Map<String, Object> param) {
    if (null == obj || fetchQuery == null) {
      return;
    }
    Map<String, Object> fetchParam = QueryUtils.resolveFetchParam(obj, fetchQuery, param);
    if (!QueryUtils.decideFetch(obj, fetchQuery, fetchParam)) {
      return;
    }
    int maxSize = fetchQuery.getMaxSize();
    boolean multiRecords = fetchQuery.isMultiRecords();
    String injectProName = fetchQuery.getInjectPropertyName();
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    SqlQuerier querier = resolver.resolve(refQueryName, fetchParam);
    String sql = querier.getScript();
    Class<?> resultClass = Map.class;
    Object[] params = querier.getConvertedParameters();
    List<QueryHint> hints = querier.getHints();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    if (maxSize > 0) {
      sql = getDialect().getLimitSql(sql, QueryUtils.OFFSET_PARAM_VAL, maxSize);
    }
    try {
      log("fetch-> " + refQueryName, params, sql);
      List<Map<String, Object>> fetchedList = getExecutor().select(sql, params);
      Object fetchedResult = null;
      if (multiRecords) {
        fetchedResult = fetchedList;
      } else if (!isEmpty(fetchedList)) {
        fetchedResult = fetchedList.get(0);
        fetchedList = fetchedList.subList(0, 1);
      }
      QueryUtils.resolveFetchResult(obj, fetchedResult, injectProName);
      this.fetch(fetchedList, fetchQueries, fetchParam);
      handleResultHints(resultClass, hints, fetchParam, fetchedList);
    } catch (SQLException e) {
      throw new QueryRuntimeException(e, "An error occurred while executing the fetch query [%s].",
          fetchQuery.getReferenceQuery());
    }
  }

  protected Dialect getDialect() {
    return getExecutor().getDialect();
  }

  protected abstract SqlQueryExecutor getExecutor();// FIXME use one connection for one method stack

  protected SqlNamedQueryResolver<String, Map<String, Object>> getResolver() {
    return resolver;
  }
}

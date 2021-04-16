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
package org.corant.modules.query.shared.dynamic;

import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Objects.max;
import java.util.List;
import java.util.Map;
import org.corant.modules.query.shared.FetchQueryHandler;
import org.corant.modules.query.shared.QueryHandler;
import org.corant.modules.query.shared.QueryParameter;
import org.corant.modules.query.shared.QueryRuntimeException;
import org.corant.modules.query.shared.mapping.FetchQuery;
import org.corant.modules.query.shared.mapping.Query;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午5:56:22
 *
 */
public abstract class AbstractDynamicQuerier<P, S> implements DynamicQuerier<P, S> {

  protected final Query query;
  protected final QueryHandler queryHandler;
  protected final FetchQueryHandler fetchQueryHandler;
  protected final QueryParameter queryParameter;

  // Not thread safe
  protected Integer limit;
  protected Integer maxSelectSize;
  protected Integer offset;
  protected Boolean thrownExceedMaxSelectSize;

  protected AbstractDynamicQuerier(Query query, QueryParameter queryParameter,
      QueryHandler queryHandler, FetchQueryHandler fetchQueryHandler) {
    this.query = query;
    this.queryParameter = queryParameter;
    this.queryHandler = queryHandler;
    this.fetchQueryHandler = fetchQueryHandler;
  }

  @Override
  public boolean decideFetch(Object result, FetchQuery fetchQuery) {
    return fetchQueryHandler.canFetch(result, queryParameter, fetchQuery);
  }

  @Override
  public Query getQuery() {
    return query;
  }

  @Override
  public QueryParameter getQueryParameter() {
    return queryParameter;
  }

  @Override
  public void handleFetchedResult(Object result, List<?> fetchResult, FetchQuery fetchQuery) {
    fetchQueryHandler.handleFetchedResult(result, fetchResult, fetchQuery);
  }

  @Override
  public void handleFetchedResults(List<?> results, List<?> fetchResult, FetchQuery fetchQuery) {
    fetchQueryHandler.handleFetchedResults(results, fetchResult, fetchQuery);
  }

  @Override
  public <T> T handleResult(Object result) {
    return queryHandler.handleResult(result, forceCast(getQuery().getResultClass()),
        getQuery().getHints(), getQueryParameter());
  }

  @Override
  public void handleResultHints(Object result) {
    // FIXME map class
    queryHandler.handleResultHints(result, Map.class, getQuery().getHints(), getQueryParameter());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> handleResults(List<?> results) {
    return queryHandler.handleResults((List<Object>) results,
        forceCast(getQuery().getResultClass()), getQuery().getHints(), getQueryParameter());
  }

  @Override
  public QueryParameter resolveFetchQueryParameter(Object result, FetchQuery fetchQuery) {
    return fetchQueryHandler.resolveFetchQueryParameter(result, fetchQuery, getQueryParameter());
  }

  @Override
  public int resolveLimit() {
    if (limit == null) {
      limit = defaultObject(getQueryParameter().getLimit(),
          () -> resolveProperty(PRO_KEY_DEFAULT_LIMIT, Integer.class, DEFAULT_LIMIT));
      if (limit <= 0) {
        limit = getUnLimitSize();
      }
      int max = resolveMaxSelectSize();
      if (limit > max) {
        throw new QueryRuntimeException(
            "Exceeded the maximum number of query [%s] results, limit is [%s].",
            getQuery().getName(), max);
      }
    }
    return limit;
  }

  @Override
  public int resolveMaxSelectSize() {
    if (maxSelectSize == null) {
      maxSelectSize = resolveProperty(PRO_KEY_MAX_SELECT_SIZE, Integer.class, MAX_SELECT_SIZE);
      if (maxSelectSize <= 0) {
        maxSelectSize = getUnLimitSize();
      }
    }
    return maxSelectSize;
  }

  /**
   * Resolve offset from query parameter, if the resolved offset < 0 or offset is null then return
   * 0.
   *
   * @param querier
   * @return resolveOffset
   */
  @Override
  public int resolveOffset() {
    if (offset == null) {
      offset = max(getQueryParameter().getOffset(), 0);
    }
    return offset;
  }

  /**
   * Resolve properties from querier, first we try resolve it from the query parameter context, if
   * not found try resolve it from query properties.
   *
   * @param <X> the property value type
   * @param key the property key
   * @param cls the property value class
   * @param dflt the default value if property not set
   * @return the property
   *
   * @see QueryParameter#getContext()
   * @see Query#getProperties()
   */
  @Override
  public <X> X resolveProperty(String key, Class<X> cls, X dflt) {
    Object pro =
        defaultObject(getQueryParameter().getContext().get(key), () -> getQuery().getProperty(key));
    if (pro != null) {
      if (cls.isInstance(pro)) {
        return forceCast(pro);
      }
      return defaultObject(toObject(pro, cls), dflt);
    }
    return dflt;
  }

  @Override
  public int validateResultSize(List<?> results) {
    int size = sizeOf(results);
    int maxSize = resolveMaxSelectSize();
    if (size > 0 && size > maxSize && thrownExceedMaxSelectSize()) {
      throw new QueryRuntimeException(
          "[%s] Result record number overflow, the allowable range is %s.", getQuery().getName(),
          maxSize);
    }
    return size;
  }

  protected int getUnLimitSize() {
    return UN_LIMIT_SELECT_SIZE;
  }

  boolean thrownExceedMaxSelectSize() {
    if (this.thrownExceedMaxSelectSize == null) {
      this.thrownExceedMaxSelectSize = resolveProperty(PRO_KEY_THROWN_EXCEED_LIMIT_SIZE,
          Boolean.class, THROWN_EXCEED_MAX_SELECT_SIZE);
    }
    return thrownExceedMaxSelectSize;
  }
}

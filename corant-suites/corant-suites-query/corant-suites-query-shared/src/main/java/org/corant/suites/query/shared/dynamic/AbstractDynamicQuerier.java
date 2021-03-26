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
package org.corant.suites.query.shared.dynamic;

import static org.corant.shared.util.Objects.forceCast;
import java.util.List;
import java.util.Map;
import org.corant.suites.query.shared.FetchQueryHandler;
import org.corant.suites.query.shared.QueryParameter;
import org.corant.suites.query.shared.QueryHandler;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午5:56:22
 *
 */
public abstract class AbstractDynamicQuerier<P, S> implements DynamicQuerier<P, S> {

  protected final Query query;
  protected final QueryHandler queryHandler;
  protected final FetchQueryHandler fetchQueryHandler;
  protected final QueryParameter queryParameter;

  /**
   * @param query
   * @param queryParameter
   * @param queryHandler
   * @param fetchQueryHandler
   */
  protected AbstractDynamicQuerier(Query query, QueryParameter queryParameter,
      QueryHandler queryHandler, FetchQueryHandler fetchQueryHandler) {
    super();
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
  public QueryParameter resolveFetchQueryParameter(Object result, FetchQuery fetchQuery) {
    return fetchQueryHandler.resolveFetchQueryParameter(result, fetchQuery, getQueryParameter());
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> handleResults(List<?> results) {
    return queryHandler.handleResults((List<Object>) results,
        forceCast(getQuery().getResultClass()), getQuery().getHints(), getQueryParameter());
  }

  @Override
  public <T> T handleResult(Object result) {
    return queryHandler.handleResult(result, forceCast(getQuery().getResultClass()),
        getQuery().getHints(), getQueryParameter());
  }

  @Override
  public void handleResultHints(Object result) {
    queryHandler.handleResultHints(result, Map.class, getQuery().getHints(),
        getQueryParameter());// FIXME map class
  }

}

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
package org.corant.modules.query;

import static org.corant.shared.util.Maps.getMapBoolean;
import java.util.List;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.Query;

/**
 * corant-modules-query-api
 *
 * This interface will be refactored in the future to add more capabilities
 *
 * @author bingo 上午9:41:03
 *
 */
public interface Querier {

  /**
   * Returns whether to fetch
   *
   * @param result the parent query result for decide whether to fetch
   * @param fetchQuery the fetch query
   */
  boolean decideFetch(Object result, FetchQuery fetchQuery);

  /**
   * Returns the query object that this querier binds, the Query object defines the execution plan.
   */
  Query getQuery();

  /**
   * Returns the query parameter that this querier bind.
   */
  QueryParameter getQueryParameter();

  /**
   * Inject the fetched result to single result
   *
   * @param result the single parent query result
   * @param fetchedResult the fetched result use to inject
   * @param fetchQuery the fetch query
   * @see FetchQueryHandler#handleFetchedResult(QueryParameter, Object, List, FetchQuery)
   */
  void handleFetchedResult(Object result, List<?> fetchedResult, FetchQuery fetchQuery);

  /**
   * Inject the fetched result to result list
   *
   * @param results the parent query results
   * @param fetchedResult the fetched result use to inject
   * @param fetchQuery the fetch query
   * @see FetchQueryHandler#handleFetchedResults(QueryParameter, List, List, FetchQuery)
   */
  void handleFetchedResults(List<?> results, List<?> fetchedResult, FetchQuery fetchQuery);

  /**
   * Handle single result, handle hints and conversions.
   *
   * @param <T> the final query result object type
   * @param result the result to be handled
   * @see QueryHandler#handleResult(Object, Query, QueryParameter)
   */
  <T> T handleResult(Object result);

  /**
   * Handle the result hints
   *
   * @param result the result to handle
   * @see QueryHandler#handleResultHints(Object, Class, Query, QueryParameter)
   */
  void handleResultHints(Object result);

  /**
   * Handle result list, handle hints and conversions.
   *
   * @param <T> the final query result object type
   * @param results the results to be handled
   * @see QueryHandler#handleResults(List, Query, QueryParameter)
   */
  <T> List<T> handleResults(List<?> results);

  /**
   * Experiential functions for proof of concept.
   *
   * @return whether to fetch in parallel
   */
  default boolean parallelFetch() {
    QueryParameter param = getQueryParameter();
    if (param != null && param.getContext() != null) {
      return getMapBoolean(param.getContext(), QuerierConfig.CTX_KEY_PARALLEL_FETCH, false);
    }
    return false;
  }

  /**
   * Returns a resolved fetch query parameter, merge parent querier criteria.
   *
   * Note: The implementer must completely copy all contexts in the parent query parameter object.
   *
   * @param result the parent result use to extract the child query parameter
   * @param fetchQuery the fetch query
   * @see FetchQueryHandler#resolveFetchQueryParameter(Object, FetchQuery, QueryParameter)
   */
  QueryParameter resolveFetchQueryParameter(Object result, FetchQuery fetchQuery);
}

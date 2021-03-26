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
package org.corant.suites.query.shared;

import java.util.List;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query-shared
 *
 * This interface will be refactored in the future to add more capabilities
 *
 * @author bingo 上午9:41:03
 *
 */
public interface Querier {

  /**
   * Decide whether to fetch
   *
   * @param result
   * @param fetchQuery
   * @return decideFetch
   */
  boolean decideFetch(Object result, FetchQuery fetchQuery);

  /**
   * The query object that this querier bind, the Query object define execution plan.
   *
   * @return getQuery
   */
  Query getQuery();

  /**
   * The query parameter that this querier bind.
   *
   * @return getQueryParameter
   */
  QueryParameter getQueryParameter();

  /**
   * Inject the fetched result to single result
   *
   * @param result
   * @param fetchedResult
   * @param fetchQuery
   * @see FetchQueryHandler#handleFetchedResult(Object, List, FetchQuery)
   */
  void handleFetchedResult(Object result, List<?> fetchedResult, FetchQuery fetchQuery);

  /**
   * Inject the fetched result to result list
   *
   * @param result
   * @param fetchedResult
   * @param fetchQuery
   * @see FetchQueryHandler#handleFetchedResults(List, List, FetchQuery)
   */
  void handleFetchedResults(List<?> result, List<?> fetchedResult, FetchQuery fetchQuery);

  /**
   * Handle result, handle hints and conversions.
   *
   * @param <T>
   * @param result
   * @see QueryHandler#handleResult(Object, Class, List, QueryParameter)
   */
  <T> T handleResult(Object result);

  /**
   * @param result the result to handle
   * @see QueryHandler#handleResultHints(Object, Class, List, QueryParameter)
   */
  void handleResultHints(Object result);

  /**
   * Handle result list, handle hints and conversions.
   *
   * @param <T>
   * @param results
   * @see QueryHandler#handleResults(List, Class, List, QueryParameter)
   */
  <T> List<T> handleResults(List<?> results);

  /**
   * Resolve fetch query parameter, merge parent querier criteria.
   *
   * @param result
   * @param fetchQuery
   * @see QueryParameterResolver#resolveFetchQueryParameter(Object, FetchQuery, QueryParameter)
   */
  QueryParameter resolveFetchQueryParameter(Object result, FetchQuery fetchQuery);
}

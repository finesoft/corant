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

import java.util.List;
import org.corant.modules.query.mapping.FetchQuery;

/**
 * corant-modules-query-api
 *
 * <p>
 * This interface is used to process fetch queries, provide the query parameters of the fetch query,
 * and provide processing of the fetch query result set.
 *
 * @author bingo 上午10:03:39
 *
 */
public interface FetchQueryHandler {

  /**
   * Decide whether to fetch, generally, the given parameter 'result' is a single result record. If
   * the fetch query predicate script is not set, this method must return true.
   *
   * @param result the query result use for decide whether to need fetch
   * @param queryParameter the query parameter use for decide whether to need fetch
   * @param fetchQuery the fetch query use for decide whether to need fetch
   */
  boolean canFetch(Object result, QueryParameter queryParameter, FetchQuery fetchQuery);

  /**
   * Return the QueryObjectMapper to process result type conversion or query script conversion.
   *
   * @return getObjectMapper
   */
  QueryObjectMapper getObjectMapper();

  /**
   * Inject fetch query result into single parent query result, the parameter 'fetchResult' may be
   * modified.
   *
   * <p>
   * Note: In the default implementation, all injections may be only reference injections, and no
   * deep clone, which means that must be careful when modifying the result set.
   *
   * @param queryParameter the query parameter
   * @param result the parent query result
   * @param fetchResults the fetched results
   * @param fetchQuery the fetch query
   */
  void handleFetchedResult(QueryParameter queryParameter, Object result, List<?> fetchResults,
      FetchQuery fetchQuery);

  /**
   * Inject fetch query result into parent query result list, the parameter 'fetchResult' may be
   * modified.
   *
   * <p>
   * Note: In the default implementation, all injections may be only reference injections, and no
   * deep clone, which means that must be careful when modifying the result set.
   *
   * @param queryParameter the query parameter
   * @param results the parent query result list
   * @param fetchResults the fetched results
   * @param fetchQuery the fetch query
   */
  void handleFetchedResults(QueryParameter queryParameter, List<?> results, List<?> fetchResults,
      FetchQuery fetchQuery);

  /**
   * Resolve fetch query parameter. Generally, the fetch query parameters are composed of the parent
   * query parameters, the parent query result set and the constants. This method is used to parse
   * the parent query result set or the parent query parameters or the constants to construct the
   * fetch query parameter type and expression. The context information of the parent query must be
   * propagated to the query parameters of the fetch query.
   *
   * @param result the parent query result, may be single or multiple
   * @param fetchQuery the fetch query
   * @param parentQueryparam the parent query parameter
   * @return fetch query parameter
   *
   * @see FetchQuery
   */
  QueryParameter resolveFetchQueryParameter(Object result, FetchQuery fetchQuery,
      QueryParameter parentQueryparam);

}

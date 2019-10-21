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

import org.corant.suites.query.shared.mapping.FetchQuery;

/**
 * corant-suites-query-shared
 *
 * @author bingo 上午10:03:39
 *
 */
public interface FetchQueryResolver {

  /**
   * Decide whether to fetch
   *
   * @param result
   * @param queryParameter
   * @param fetchQuery
   * @return canFetch
   */
  boolean canFetch(Object result, QueryParameter queryParameter, FetchQuery fetchQuery);

  /**
   * Inject fetch query result in to parent query result
   *
   * @param result
   * @param fetchResult
   * @param injectProName
   */
  void resolveFetchedResult(Object result, Object fetchResult, String injectProName);

  /**
   * Resolve fetch query parameter. Generally, the fetch query parameters are composed of parent
   * query parameters, parent query result set and constant. This method is used to parse parent
   * query result sets or parent query parameters or constants to construct the fetch query
   * parameter type and expression.
   *
   * @param result the parent query result, may be single or multiple
   * @param query
   * @param parentQueryparam
   * @return fetch query parameter
   * @see FetchQuery
   */
  QueryParameter resolveFetchQueryParameter(Object result, FetchQuery query,
      QueryParameter parentQueryparam);

}

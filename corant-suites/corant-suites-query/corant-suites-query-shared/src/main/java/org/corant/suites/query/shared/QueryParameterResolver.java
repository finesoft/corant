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
import org.corant.suites.query.shared.mapping.Query;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午4:48:35
 *
 */
public interface QueryParameterResolver {

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

  /**
   * Resolve query parameter.
   *
   * @param query
   * @param parameter
   * @return query parameter
   */
  QueryParameter resolveQueryParameter(Query query, Object parameter);
}

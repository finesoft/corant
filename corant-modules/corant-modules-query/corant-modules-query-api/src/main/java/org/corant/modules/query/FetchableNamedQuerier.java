/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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

/**
 * corant-modules-query-api
 *
 * @author bingo 15:24:31
 */
public interface FetchableNamedQuerier extends NamedQuerier {

  /**
   * Returns whether to fetch
   *
   * @param result the parent query result for decide whether to fetch
   * @param fetchQuery the fetch query
   */
  boolean decideFetch(Object result, FetchQuery fetchQuery);

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
   * Check the size of the result set, trim according to the appropriate
   * size({@link #resolveSelectSize()}) if necessary, and return the size of the final result set.
   *
   * @param results the results
   */
  int handleResultSize(List<?> results);

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
   * <p>
   * Note: The implementer must completely copy all contexts in the parent query parameter object.
   *
   * @param result the parent result use to extract the child query parameter
   * @param fetchQuery the fetch query
   * @see FetchQueryHandler#resolveFetchQueryParameter(Object, FetchQuery, QueryParameter)
   */
  QueryParameter resolveFetchQueryParameter(Object result, FetchQuery fetchQuery);

  /**
   * Returns the max fetch size
   *
   * @param parentResult the parent results
   * @param fetchQuery the fetch query
   */
  int resolveMaxFetchSize(Object parentResult, FetchQuery fetchQuery);

}

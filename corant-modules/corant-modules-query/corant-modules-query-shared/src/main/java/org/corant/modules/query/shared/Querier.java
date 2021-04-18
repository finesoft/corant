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
package org.corant.modules.query.shared;

import java.util.List;
import org.corant.config.Configs;
import org.corant.modules.query.shared.mapping.FetchQuery;
import org.corant.modules.query.shared.mapping.Query;

/**
 * corant-modules-query-shared
 *
 * This interface will be refactored in the future to add more capabilities
 *
 * @author bingo 上午9:41:03
 *
 */
public interface Querier {

  String PRO_KEY_MAX_SELECT_SIZE = ".max-select-size";
  String PRO_KEY_DEFAULT_LIMIT = ".default-limit";
  String PRO_KEY_THROWN_EXCEED_LIMIT_SIZE = ".thrown-exceed-max-select-size";
  String GLOBAL = "corant.query";
  String GLOBAL_PRO_KEY_MAX_SELECT_SIZE = GLOBAL + PRO_KEY_MAX_SELECT_SIZE;
  String GLOBAL_PRO_KEY_DEFAULT_LIMIT = GLOBAL + PRO_KEY_DEFAULT_LIMIT;
  String GLOBAL_PRO_KEY_THROWN_EXCEED_LIMIT_SIZE = GLOBAL + PRO_KEY_THROWN_EXCEED_LIMIT_SIZE;
  int DEFALUT_STREAM_LIMIT =
      Configs.getValue("corant.query.default-stream-limit", Integer.class, 16);

  int UN_LIMIT_SELECT_SIZE = Integer.MAX_VALUE - 16;
  int MAX_SELECT_SIZE = Configs.getValue(GLOBAL_PRO_KEY_MAX_SELECT_SIZE, Integer.class, 128);
  int DEFAULT_LIMIT = Configs.getValue(GLOBAL_PRO_KEY_DEFAULT_LIMIT, Integer.class, 16);

  boolean THROWN_EXCEED_MAX_SELECT_SIZE =
      Configs.getValue(GLOBAL_PRO_KEY_THROWN_EXCEED_LIMIT_SIZE, Boolean.class, true);

  /**
   * Returns whether to fetch
   *
   * @param result the parent query result for decide whether to fetch
   * @param fetchQuery the fetch query
   */
  boolean decideFetch(Object result, FetchQuery fetchQuery);

  /**
   * Returns the query object that this querier bind, the Query object define execution plan.
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
   * @see FetchQueryHandler#handleFetchedResult(Object, List, FetchQuery)
   */
  void handleFetchedResult(Object result, List<?> fetchedResult, FetchQuery fetchQuery);

  /**
   * Inject the fetched result to result list
   *
   * @param results the parent query results
   * @param fetchedResult the fetched result use to inject
   * @param fetchQuery the fetch query
   * @see FetchQueryHandler#handleFetchedResults(List, List, FetchQuery)
   */
  void handleFetchedResults(List<?> results, List<?> fetchedResult, FetchQuery fetchQuery);

  /**
   * Handle single result, handle hints and conversions.
   *
   * @param <T> the finally query result object type
   * @param result the result to be handled
   * @see QueryHandler#handleResult(Object, Class, List, QueryParameter)
   */
  <T> T handleResult(Object result);

  /**
   * Handle the result hints
   *
   * @param result the result to handle
   * @see QueryHandler#handleResultHints(Object, Class, List, QueryParameter)
   */
  void handleResultHints(Object result);

  /**
   * Handle result list, handle hints and conversions.
   *
   * @param <T> the finally query result object type
   * @param results the results to be handled
   * @see QueryHandler#handleResults(List, Class, List, QueryParameter)
   */
  <T> List<T> handleResults(List<?> results);

  /**
   * Returns a resolved fetch query parameter, merge parent querier criteria.
   *
   * @param result the parent result use to extract the child query parameter
   * @param fetchQuery the fetch query
   * @see FetchQueryHandler#resolveFetchQueryParameter(Object, FetchQuery, QueryParameter)
   */
  QueryParameter resolveFetchQueryParameter(Object result, FetchQuery fetchQuery);
}

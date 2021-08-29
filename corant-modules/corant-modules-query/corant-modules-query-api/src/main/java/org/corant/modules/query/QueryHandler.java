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
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.mapping.QueryHint;
import org.corant.modules.query.spi.ResultHintHandler;

/**
 * corant-modules-query-api
 *
 * <p>
 * This interface is used to process normal queries, provide processing of the query parameters, and
 * provide processing of the query result set.
 *
 * @author bingo 下午4:56:52
 *
 */
public interface QueryHandler {

  /**
   * Return the QueryObjectMapper to process result type conversion or query script conversion.
   *
   * @return getObjectMapper
   */
  QueryObjectMapper getObjectMapper();

  /**
   * Returns the querier configuration.
   *
   * @return the querier configuration
   */
  QuerierConfig getQuerierConfig();

  /**
   * Handle a single result, perform the hint handler and convert the result to the expected typed
   * object.
   *
   * @param <T> the result type
   * @param result the original results, currently is <b> Map&lt;String,Object&gt;</b>
   * @param query the query object
   * @param parameter the query parameter
   *
   * @see #handleResultHints(Object, Class, Query, QueryParameter)
   * @see QueryHint
   * @see Query
   * @see ResultHintHandler
   */
  <T> T handleResult(Object result, Query query, QueryParameter parameter);

  /**
   * Handle query hints, in this step the result set may be adjusted or inserted with certain
   * values.
   *
   * @param result the result may be single object or list objects
   * @param originalResultClass the query result class
   * @param query the query object
   * @param parameter the query parameter
   *
   * @see ResultHintHandler
   */
  void handleResultHints(Object result, Class<?> originalResultClass, Query query,
      QueryParameter parameter);

  /**
   * Handle result list, The steps are as follows:
   *
   * <pre>
   * 1.Handle query hints, in this step the result set may be adjusted or inserted with
   * certain values.
   *
   * 2.Convert result by result class.
   * </pre>
   *
   * @param <T> the result class
   * @param results the original results, currently is <b> List&lt;Map&lt;String,Object&gt;&gt;</b>
   * @param query the query object
   * @param parameter the query parameter
   *
   *
   * @see #handleResultHints(Object, Class, Query, QueryParameter)
   * @see QueryHint
   * @see Query
   * @see ResultHintHandler
   */
  <T> List<T> handleResults(List<Object> results, Query query, QueryParameter parameter);

  /**
   * Resolve query parameter. If QueryParameterReviser exists, the given parameter may be adjusted.
   *
   * <p>
   * NOTE: If the given parameter is not an instance of QueryParameter then the given parameter is
   * regarded as the criteria of the normal query parameter.
   *
   * @param query the query
   * @param parameter the original query parameter
   * @return normal query parameter object
   */
  QueryParameter resolveParameter(Query query, Object parameter);
}

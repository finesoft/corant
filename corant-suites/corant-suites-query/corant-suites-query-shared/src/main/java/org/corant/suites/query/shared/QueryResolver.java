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
import org.corant.suites.query.shared.mapping.Query;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.spi.ResultHintHandler;

/**
 * corant-suites-query-shared
 *
 * @author bingo 下午4:56:52
 *
 */
public interface QueryResolver {

  /**
   * Return the QueryObjectMapper to process result type conversion or query script conversion.
   *
   * @return getObjectMapper
   */
  QueryObjectMapper getObjectMapper();

  /**
   * Resolve query parameter. NOTE: If parameter instanceof QueryParameter then implemention must be
   * return it.
   *
   * @param query
   * @param parameter
   * @return query parameter
   */
  QueryParameter resolveParameter(Query query, Object parameter);

  /**
   * Resolve single result {@link #resolveResult(List, Class, List, QueryParameter)}
   *
   * @param <T> the result class
   * @param result the original results, currently is <b> Map&lt;String,Object&gt;</b>
   * @param resultClass
   * @param hints
   * @param parameter
   * @return resolve
   * @see QueryHint
   * @see Query
   * @see ResultHintHandler
   */
  <T> T resolveResult(Object result, Class<T> resultClass, List<QueryHint> hints,
      QueryParameter parameter);

  /**
   * Resolve query hints, in this step the result set may be adjusted or inserted with certain
   * values.
   *
   * @param result
   * @param resultClass
   * @param hints
   * @param parameter resolveResultHints
   */
  void resolveResultHints(Object result, Class<?> resultClass, List<QueryHint> hints,
      QueryParameter parameter);

  /**
   * Resolve result list, The steps are as follows:
   *
   * <pre>
   * 1.Resolve query hints, in this step the result set may be adjusted or inserted with
   * certain values.
   *
   * 2.Convert result by result class.
   * </pre>
   *
   * @param <T> the result class
   * @param results the original results, currently is <b> List&lt;Map&lt;String,Object&gt;&gt;</b>
   * @param resultClass
   * @param hints
   * @param parameter
   * @return resolve
   * @see QueryHint
   * @see Query
   * @see ResultHintHandler
   */
  <T> List<T> resolveResults(List<Object> results, Class<T> resultClass, List<QueryHint> hints,
      QueryParameter parameter);
}

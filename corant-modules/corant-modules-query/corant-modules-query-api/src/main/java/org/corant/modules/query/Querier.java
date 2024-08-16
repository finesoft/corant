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

import java.time.Duration;
import java.util.List;
import org.corant.modules.query.mapping.Query;

/**
 * corant-modules-query-api
 * <p>
 * This interface will be refactored in the future to add more capabilities
 *
 * @author bingo 上午9:41:03
 */
public interface Querier {

  /**
   * Returns the query object that this querier binds, the Query object defines the execution plan.
   */
  Query getQuery();

  /**
   * Returns the query parameter that this querier bind.
   */
  QueryParameter getQueryParameter();

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
   * Returns either the limit value from the query parameter, or if the value is {@code null}, the
   * value of {@link QuerierConfig#getDefaultLimit()}.
   *
   * <p>
   * Note: If the limit value <=0 then returns {@link QuerierConfig#getMaxLimit()}, if the limit
   * value greater than the {@link #resolveSelectSize()} a {@link QueryRuntimeException} thrown.
   */
  int resolveLimit();

  /**
   * Returns the offset from query parameter
   */
  int resolveOffset();

  /**
   * Returns the resolved properties, first we try resolve it from the query parameter context, if
   * not found try resolve it from query properties.
   *
   * @param <X> the property value type
   * @param key the property key
   * @param cls the property value class
   * @param dflt the default value if property not set
   *
   * @see QueryParameter#getContext()
   * @see Query#getProperties()
   */
  <X> X resolveProperty(String key, Class<X> cls, X dflt);

  /**
   * Returns either the value of the query select size property(the property name is
   * {@link QuerierConfig#PRO_KEY_MAX_SELECT_SIZE}), or if the value is {@code null}, the value of
   * {@link QuerierConfig#getDefaultSelectSize()}.
   * <p>
   * Note: If the value <=0 then returns {@link QuerierConfig#getMaxSelectSize()}
   */
  int resolveSelectSize();

  /**
   * Returns either the limit value from the query parameter, or if the value is {@code null}, the
   * value of {@link QuerierConfig#getDefaultStreamLimit()}.
   *
   * <p>
   * Note: If the limit value <=0 then returns {@link QuerierConfig#getMaxLimit()}, if the limit
   * value greater than the {@link #resolveSelectSize()} a {@link QueryRuntimeException} thrown.
   */
  int resolveStreamLimit();

  /**
   * Returns either the value of the query timeout property (the property name is
   * {@link QuerierConfig#PRO_KEY_TIMEOUT}), or if the value is {@code null}, the value of
   * {@link QuerierConfig#getTimeout()}.
   *
   * @return query timeout
   */
  Duration resolveTimeout();
}

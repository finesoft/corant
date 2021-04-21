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
package org.corant.modules.query.shared.dynamic;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.corant.modules.query.shared.Querier;
import org.corant.modules.query.shared.QuerierConfig;
import org.corant.modules.query.shared.QueryParameter;
import org.corant.modules.query.shared.QueryRuntimeException;
import org.corant.modules.query.shared.mapping.Query;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午4:37:30
 *
 */
public interface DynamicQuerier<P, S> extends Querier {

  /**
   * Returns the processed query script with the underlying database dialect.
   */
  default S getScript() {
    return getScript(null);
  }

  /**
   * Returns the processed query script with the underlying database dialect., implemention can use
   * the pass in additional parameter to compute the script.
   *
   * @param additionals the additional adjustment parameter
   */
  S getScript(Map<?, ?> additionals);

  /**
   * Returns the query script parameter
   */
  P getScriptParameter();

  /**
   * Returns either the value of the query timeout property (the property name is
   * {@link QuerierConfig#PRO_KEY_TIMEOUT}), or if the value is {@code null}, the value of
   * {@link QuerierConfig#getTimeout()}.
   *
   * @return query timeout
   */
  Duration resolveTimeout();

  /**
   * Returns either the limit value from the query parameter, or if the value is {@code null}, the
   * value of {@link QuerierConfig#getDefaultLimit()}.
   *
   * <p>
   * Note: If the limit value <=0 then returns {@link QuerierConfig#getMaxLimit()}, if the limit
   * value great than the {@link #resolveMaxSelectSize()} a {@link QueryRuntimeException} thrown.
   */
  int resolveLimit();

  /**
   * Returns either the value of the query max select size property(the property name is
   * {@link QuerierConfig#PRO_KEY_MAX_SELECT_SIZE}), or if the value is {@code null}, the value of
   * {@link QuerierConfig#getDefaultSelectSize()}.
   * <p>
   * Note: If the value <=0 then returns {@link QuerierConfig#getMaxSelectSize()}
   */
  int resolveMaxSelectSize();

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
   * Return and validate results size.
   *
   * @param results
   */
  int validateResultSize(List<?> results);
}

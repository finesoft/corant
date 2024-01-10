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
import org.corant.modules.query.NamedQuerier;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mapping.Query;
import org.corant.modules.query.shared.DefaultQuerierConfig;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午4:37:30
 */
public interface DynamicQuerier<P, S> extends NamedQuerier {

  /**
   * Returns the processed query script with the underlying database dialect.
   */
  default S getScript() {
    return getScript(null);
  }

  /**
   * Returns the processed query script with the underlying database dialect., implementation can
   * use the pass in additional parameter to compute the script.
   *
   * @param additional the additional adjustment parameter
   */
  S getScript(Map<?, ?> additional);

  /**
   * Returns the query script parameter
   */
  P getScriptParameter();

  /**
   * Check the size of the result set, trim according to the appropriate
   * size({@link #resolveSelectSize()}) if necessary, and return the size of the final result set.
   *
   * @param results the results
   */
  int handleResultSize(List<?> results);

  /**
   * Returns either the limit value from the query parameter, or if the value is {@code null}, the
   * value of {@link DefaultQuerierConfig#getDefaultLimit()}.
   *
   * <p>
   * Note: If the limit value <=0 then returns {@link DefaultQuerierConfig#getMaxLimit()}, if the
   * limit value greater than the {@link #resolveSelectSize()} a {@link QueryRuntimeException}
   * thrown.
   */
  int resolveLimit();

  /**
   * Returns the max fetch size
   *
   * @param parentResult the parent results
   * @param fetchQuery the fetch query
   */
  int resolveMaxFetchSize(Object parentResult, FetchQuery fetchQuery);

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
   * {@link DefaultQuerierConfig#PRO_KEY_MAX_SELECT_SIZE}), or if the value is {@code null}, the
   * value of {@link DefaultQuerierConfig#getDefaultSelectSize()}.
   * <p>
   * Note: If the value <=0 then returns {@link DefaultQuerierConfig#getMaxSelectSize()}
   */
  int resolveSelectSize();

  /**
   * Returns either the limit value from the query parameter, or if the value is {@code null}, the
   * value of {@link DefaultQuerierConfig#getDefaultStreamLimit()}.
   *
   * <p>
   * Note: If the limit value <=0 then returns {@link DefaultQuerierConfig#getMaxLimit()}, if the
   * limit value greater than the {@link #resolveSelectSize()} a {@link QueryRuntimeException}
   * thrown.
   */
  int resolveStreamLimit();

  /**
   * Returns either the value of the query timeout property (the property name is
   * {@link DefaultQuerierConfig#PRO_KEY_TIMEOUT}), or if the value is {@code null}, the value of
   * {@link DefaultQuerierConfig#getTimeout()}.
   *
   * @return query timeout
   */
  Duration resolveTimeout();
}

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

import java.util.Map;
import org.corant.modules.query.FetchableNamedQuerier;

/**
 * corant-modules-query-shared
 *
 * @author bingo 下午4:37:30
 */
public interface DynamicQuerier<P, S> extends FetchableNamedQuerier {

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

}

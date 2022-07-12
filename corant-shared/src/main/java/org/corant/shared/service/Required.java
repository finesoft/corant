/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.service;

import org.corant.shared.ubiquity.Sortable;

/**
 * corant-shared
 *
 * @author bingo 下午2:34:19
 *
 */
public interface Required extends Sortable {

  /**
   * Add a vetoed service class to current required processing, after the addition is successful,
   * subsequent related loads will skip the service class.
   *
   * @param type the class of service to be vetoed
   * @return true if addition is successful else false
   */
  default boolean addVeto(Class<?> type) {
    return false;
  }

  /**
   * Remove a vetoed service class from current required processing, after the remove is successful,
   * subsequent related loads will not skip the service class.
   *
   * @param type the class of service to unveto
   * @return true if remove is successful else false
   */
  default boolean removeVeto(Class<?> type) {
    return false;
  }

  /**
   * Check whether the given service class can be vetoed.
   *
   * @param type the class of service to be checked
   * @return true if the given service class should be vetoed else false
   */
  boolean shouldVeto(Class<?> type);

}

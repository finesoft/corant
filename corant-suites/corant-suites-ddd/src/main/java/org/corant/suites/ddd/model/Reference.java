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
package org.corant.suites.ddd.model;

import java.io.Serializable;
import java.util.Optional;

/**
 * corant-suites-ddd
 *
 * <p>
 * The object reference. Usually used for reference between aggregates or entities, it usually holds
 * the id of the aggregate or entity to which it points, and is an immutable object that can be
 * retrieved from the persistence layer by holding the id.
 *
 * @author bingo 上午10:31:05
 *
 */
public interface Reference<T> extends Serializable {

  /**
   * Retrieve the object to which the reference refers, and throws an exception if it is not found.
   *
   * @return
   */
  T retrieve();

  /**
   * Try to retrive the object
   *
   * @return tryRetrieve
   */
  default Optional<T> tryRetrieve() {
    return Optional.empty();
  }
}

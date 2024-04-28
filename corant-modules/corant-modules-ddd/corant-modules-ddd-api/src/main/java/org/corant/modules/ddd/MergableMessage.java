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
package org.corant.modules.ddd;

import java.beans.Transient;

/**
 * corant-modules-ddd-api
 * <p>
 * Marking an integrated message object can be merged.
 * <p>
 * Imagine that in the transaction unit of works if an aggregate property value from '1' change to
 * '2' and finally change to '1' then it should not raise a property changed message, this is
 * experimental.
 *
 * @author bingo 下午3:37:07
 */
public interface MergableMessage extends Message {

  default boolean canMerge(MergableMessage other) {
    return true;
  }

  @Transient
  @jakarta.persistence.Transient
  default boolean isValid() {
    return true;
  }

  default MergableMessage merge(MergableMessage other) {
    return this;
  }

}

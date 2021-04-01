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
package org.corant.modules.ddd.unitwork;

import org.corant.modules.ddd.message.Message;
import org.corant.modules.ddd.model.Aggregate;

/**
 * corant-modules-ddd
 *
 * <p>
 * The unit of work represents a series of works within the business scope. These works may change
 * the state of the business entity (aggregate) and persist the entity state, or generate certain
 * messages that can be propagated to other business systems outside the boundary or even in other
 * areas.
 * </p>
 *
 * @see Aggregate
 * @see Message
 *
 * @author bingo 下午3:02:04
 *
 */
public interface UnitOfWork {

  /**
   * Complete unit of work, clean registers, release effect.
   */
  void complete(boolean success);

  /**
   * Deregister an object from this unit of works
   *
   * @param obj
   */
  default void deregister(Object obj) {}

  /**
   * The unit of works id
   *
   * @return
   */
  Object getId();

  /**
   * The registrations in this unit of works
   */
  Object getRegistrations();

  /**
   * Register an object to this unit of works.
   *
   * <p>
   * The object could be an aggregate or a message or a named object (represented by Pair<?,?>).
   *
   * @param obj
   */
  void register(Object obj);

}

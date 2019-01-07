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
package org.corant.suites.ddd.unitwork;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午3:02:04
 *
 */
public interface UnitOfWork {

  /**
   * Complete unit of works, clean registers, release effect.
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
   * The registers in this unit of works
   */
  Object getRegisters();

  /**
   * Register an object to this unit of works
   *
   * @param obj
   */
  void register(Object obj);


}

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

import java.util.stream.Stream;
import org.corant.modules.jpa.shared.PersistenceService;

/**
 * corant-modules-ddd
 *
 * <p>
 * The unit of works manager, responsible for creating or destroying work units, and providing
 * resources or services needed by work units
 * </p>
 *
 * @author bingo 上午11:51:01
 */
public interface UnitOfWorksManager {

  /**
   * Return the current unit of work
   */
  UnitOfWork getCurrentUnitOfWork();

  /**
   * Return the unit of work handler and provide them to the unit of work managed.
   */
  Stream<UnitOfWorksHandler> getHandlers();

  /**
   * Return the unit of work listener and provide them to the unit of work managed.
   */
  Stream<UnitOfWorksListener> getListeners();

  /**
   * Return the persistence service and provide them to the unit of work managed.
   */
  PersistenceService getPersistenceService();

  /**
   * corant-modules-ddd
   *
   * <p>
   * The unit of works handler is used as a callback notification to signal that the unit of work is
   * in the process of being completed.
   * </p>
   *
   * @author bingo 10:38:33
   *
   */
  @FunctionalInterface
  interface UnitOfWorksHandler {

    static int compare(UnitOfWorksHandler h1, UnitOfWorksHandler h2) {
      return Integer.compare(h1.getOrdinal(), h2.getOrdinal());
    }

    default int getOrdinal() {
      return 0;
    }

    /**
     * Callback notified before the unit of work is completed.
     *
     * @param uow the unit of work
     */
    void onPreComplete(UnitOfWork uow);
  }

  /**
   * corant-modules-ddd
   *
   * <p>
   * The unit of work listener, when the unit of work is completed, all listeners will be called.
   * The listener accepts two parameters, one is the information that has been registered in the
   * unit of work, and the other is whether the unit of work is successfully executed or not.
   * </p>
   *
   * @author bingo 10:44:46
   *
   */
  @FunctionalInterface
  interface UnitOfWorksListener {

    /**
     * Callback notified after the unit of work is completed.
     *
     * @param registration the information been registered in the unit of work
     * @param success whether the unit of work is successfully executed or not
     */
    void onCompleted(Object registration, boolean success);
  }

}

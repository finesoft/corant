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
package org.corant.modules.ddd.shared.unitwork;

import java.io.Serializable;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionScoped;
import org.corant.modules.jta.shared.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-ddd-shared
 * <p>
 * A CDI service abstract class with UnitOfWork (transaction) as the bean scope, which supports the
 * callback {@link #onComplete(boolean)} operation of the UnitOfWork.
 * <p>
 * This class has many uses, such as tracking entity state changes, encapsulating the changed state
 * as an event, and emitting the event when UnitOfWork is successfully completed.
 * <p>
 * Examples:
 *
 * <pre>
 * {@literal @}TransactionScoped
 * public class StateTracer extends AbstractUnitOfWorkService {
 *
 *   final Set<Serializable> states = new LinkedHashSet<>();
 *
 *   public void registerState(Serializable state) {
 *     this.states.add(state);
 *   }
 *
 *   protected void onComplete(boolean success) {
 *     if (success) {
 *       CDIs.fireEvent(new StateEvent(states));
 *     }
 *   }
 * }
 *
 * {@literal @}ApplicationScoped
 * {@literal @}Transactional
 * public class StateService {
 *   {@literal @}Inject
 *   StateTracer stateTracer;
 *
 *   public void update(){
 *     //...do update
 *     stateTracer.registerState(state);
 *   }
 * }
 * </pre>
 *
 *
 * @author bingo 下午12:23:20
 */
@TransactionScoped
public abstract class AbstractUnitOfWorkService implements Serializable {

  private static final long serialVersionUID = -4395687658846903572L;

  protected void onComplete(boolean success) {}

  /**
   * Called by {@link jakarta.transaction.Synchronization#afterCompletion(int)}
   */
  @PreDestroy
  protected void onPreDestroy() {
    try {
      final int status = TransactionService.currentTransaction().getStatus();
      onComplete((status == Status.STATUS_COMMITTED));
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }
}

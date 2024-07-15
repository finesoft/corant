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
package org.corant.modules.jta.shared;

import static org.corant.context.Beans.findAnyway;
import static org.corant.context.Beans.resolveAnyway;
import java.util.Optional;
import javax.transaction.xa.XAResource;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.corant.modules.jta.shared.TransactionalAction.TransactionalActuator;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-jta-shared
 *
 * @author bingo 下午6:18:58
 */
public interface TransactionService {

  /**
   * Unfinish yet! This is an experimental function, used to handle transaction-related operations
   * manually.
   *
   * <pre>
   * example:
   *
   * TransactionService.actuator().txType(TxType.REQUIRED).rollbackOn(SomeException.class)
   *     .run(() -> {
   *       // the business operation that run in transaction.
   *     });
   *
   * return TransactionService.actuator().txType(TxType.REQUIRED).rollbackOn(SomeException.class)
   *     .get(() -> {
   *       // the business operation that run in transaction;
   *       return operation result;
   *     });
   * return TransactionService.actuator().required().rollbackOn(SomeException.class)
   *     .get(() -> {
   *       // the business operation that run in transaction;
   *       return operation result;
   *     });
   * </pre>
   *
   */
  static TransactionalActuator actuator() {
    return new TransactionalActuator();
  }

  /**
   * Get current transaction or null if current has not transaction.
   *
   * @return currentTransaction
   */
  static Transaction currentTransaction() {
    try {
      return resolveAnyway(TransactionService.class).getTransaction();
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Delist XAResource and flag to current transaction.
   *
   * <p>
   * Note:The only case in which you need to handle a delist is when using the same resource
   * instance in multiple transaction contexts.
   * </p>
   *
   * @param xar The XAResource object associated with the resource(connection).
   * @param flag One of the values of TMSUCCESS, TMSUSPEND, or TMFAIL.
   */
  static void delistXAResourceFromCurrentTransaction(XAResource xar, int flag) {
    try {
      resolveAnyway(TransactionService.class).delistXAResource(xar, flag);
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Enlist the resource specified with the transaction associated with the current transaction
   * object.
   *
   * @param xar the XAResource object associated with the resource(connection)
   * @return true if the resource was enlisted successfully; otherwise false.
   */
  static boolean enlistXAResourceToCurrentTransaction(XAResource xar) {
    try {
      return resolveAnyway(TransactionService.class).enlistXAResource(xar);
    } catch (SystemException | RollbackException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Returns whether the current transaction is active.
   */
  static boolean isCurrentTransactionActive() {
    Optional<TransactionService> service = findAnyway(TransactionService.class);
    return service.isPresent() && service.get().isTransactionActive();
  }

  static void registerSynchronizationToCurrentTransaction(Synchronization synchronization) {
    resolveAnyway(TransactionService.class).registerSynchronization(synchronization);
  }

  static TransactionManager transactionManager() {
    return resolveAnyway(TransactionService.class).getTransactionManager();
  }

  static UserTransaction userTransaction() {
    return resolveAnyway(TransactionService.class).getUserTransaction();
  }

  /**
   * @see #actuator()
   */
  static TransactionalActuator withTransaction() {
    return new TransactionalActuator();
  }

  default void delistXAResource(XAResource xar, int flag) throws SystemException {
    if (isTransactionActive()) {
      getTransaction().delistResource(xar, flag);
    }
    throw new IllegalStateException();
  }

  default boolean enlistXAResource(XAResource xar) throws SystemException, RollbackException {
    if (isTransactionActive()) {
      return getTransaction().enlistResource(xar);
    }
    return false;
  }

  default Transaction getTransaction() throws SystemException {
    TransactionManager tm = getTransactionManager();
    if (tm != null) {
      return tm.getTransaction();
    }
    return null;
  }

  TransactionManager getTransactionManager();

  UserTransaction getUserTransaction();

  default boolean isTransactionActive() {
    try {
      return getTransactionManager().getStatus() != Status.STATUS_NO_TRANSACTION;
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  default void registerSynchronization(Synchronization synchronization) {
    try {
      getTransaction().registerSynchronization(synchronization);
    } catch (IllegalStateException | RollbackException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

}

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
package org.corant.kernel.service;

import static org.corant.kernel.util.Instances.resolveAnyway;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-kernel
 *
 * @author bingo 下午6:18:58
 *
 */
public interface TransactionService {

  static Transaction currentTransaction() {
    try {
      return resolveAnyway(TransactionService.class).getTransaction();
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  static void delistXAResourceFromCurrentTransaction(XAResource xar, int flag) {
    try {
      resolveAnyway(TransactionService.class).delistXAResource(xar, flag);
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  static boolean enlistXAResourceToCurrentTransaction(XAResource xar) {
    try {
      return resolveAnyway(TransactionService.class).enlistXAResource(xar);
    } catch (SystemException | RollbackException e) {
      throw new CorantRuntimeException(e);
    }
  }

  static boolean isCurrentTransactionActive() {
    return resolveAnyway(TransactionService.class).isTransactionActive();
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

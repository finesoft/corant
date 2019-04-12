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
package org.corant.suites.jta.shared;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-jta-shared
 *
 * @author bingo 下午3:26:28
 *
 */
// TODO
public interface TransactionService {

  default void delistXAResource(XAResource xar, int flag) {
    if (isTransactionActive()) {
      try {
        getTransaction().delistResource(xar, flag);
      } catch (IllegalStateException | SystemException e) {
        throw new CorantRuntimeException(e);
      }
    }
    throw new IllegalStateException();
  }

  default boolean enlistXAResource(XAResource xar) {
    if (isTransactionActive()) {
      try {
        return getTransaction().enlistResource(xar);
      } catch (IllegalStateException | RollbackException | SystemException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return false;
  }

  Transaction getTransaction();

  UserTransaction getUserTransaction();

  default boolean isTransactionActive() {
    Transaction transaction = getTransaction();
    try {
      return transaction != null && (transaction.getStatus() == Status.STATUS_ACTIVE
          || transaction.getStatus() == Status.STATUS_MARKED_ROLLBACK);
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  default void registerSynchronization(Synchronization synchronization) {
    if (isTransactionActive()) {
      try {
        getTransaction().registerSynchronization(synchronization);
      } catch (IllegalStateException | RollbackException | SystemException e) {
        throw new CorantRuntimeException(e);
      }
    }
    throw new IllegalStateException();
  }

}

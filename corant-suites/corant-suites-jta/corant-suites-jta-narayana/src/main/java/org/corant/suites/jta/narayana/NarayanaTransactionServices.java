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
package org.corant.suites.jta.narayana;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import org.corant.kernel.service.TransactionService;
import org.corant.shared.exception.CorantRuntimeException;
import org.jboss.weld.transaction.spi.TransactionServices;

/**
 * corant-suites-jta-narayana
 *
 * @author bingo 下午7:47:50
 *
 */
public class NarayanaTransactionServices implements TransactionServices {

  @Override
  public void cleanup() {
    // NOOP
  }

  public TransactionManager getTransactionManager() {
    return com.arjuna.ats.jta.TransactionManager.transactionManager();
  }

  @Override
  public UserTransaction getUserTransaction() {
    return com.arjuna.ats.jta.UserTransaction.userTransaction();
  }

  @Override
  public boolean isTransactionActive() {
    try {
      return getTransactionManager().getStatus() != Status.STATUS_NO_TRANSACTION;
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void registerSynchronization(Synchronization synchronizedObserver) {
    try {
      getTransactionManager().getTransaction().registerSynchronization(synchronizedObserver);
    } catch (IllegalStateException | RollbackException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @ApplicationScoped
  public static class NarayanaTransactionService implements TransactionService {

    @Override
    public TransactionManager getTransactionManager() {
      return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }

    @Override
    public UserTransaction getUserTransaction() {
      return com.arjuna.ats.jta.UserTransaction.userTransaction();
    }

  }
}

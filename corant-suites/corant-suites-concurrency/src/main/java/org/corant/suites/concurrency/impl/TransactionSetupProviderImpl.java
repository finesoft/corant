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
package org.corant.suites.concurrency.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.concurrent.ManagedTask;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.glassfish.enterprise.concurrent.spi.TransactionHandle;
import org.glassfish.enterprise.concurrent.spi.TransactionSetupProvider;

/**
 * corant-suites-concurrency
 *
 * @author bingo 下午7:49:26
 *
 */
public class TransactionSetupProviderImpl implements TransactionSetupProvider {
  static final Logger logger = Logger.getLogger(TransactionSetupProviderImpl.class.getName());
  private static final long serialVersionUID = -4745961016107658962L;
  private transient TransactionManager transactionManager;

  /**
   * @param transactionManager
   */
  public TransactionSetupProviderImpl(TransactionManager transactionManager) {
    super();
    this.transactionManager = transactionManager;
  }

  @Override
  public void afterProxyMethod(TransactionHandle handle, String transactionExecutionProperty) {
    if (handle instanceof TransactionHandleImpl) {
      Transaction suspendedTxn = ((TransactionHandleImpl) handle).getTransaction();
      if (suspendedTxn != null) {
        try {
          transactionManager.resume(suspendedTxn);
        } catch (InvalidTransactionException | SystemException e) {
          logger.log(Level.SEVERE, e.toString());
        }
      }
    }
  }

  @Override
  public TransactionHandle beforeProxyMethod(String transactionExecutionProperty) {
    if (!ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.equals(transactionExecutionProperty)) {
      try {
        return new TransactionHandleImpl(transactionManager.suspend());
      } catch (SystemException e) {
        logger.log(Level.SEVERE, e.toString());
      }
    }
    return null;
  }

}

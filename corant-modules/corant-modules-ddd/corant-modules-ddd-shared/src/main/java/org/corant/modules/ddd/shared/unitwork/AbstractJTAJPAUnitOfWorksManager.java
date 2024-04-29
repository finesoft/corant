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
package org.corant.modules.ddd.shared.unitwork;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import org.corant.modules.ddd.MessageDispatcher;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * The JTA JPA unit of works manager, use for create and destroy the unit of works, provide the
 * necessary transaction manager and message dispatch service for the unit of work.
 * </p>
 *
 * @author bingo 下午2:14:21
 */
public abstract class AbstractJTAJPAUnitOfWorksManager extends AbstractJPAUnitOfWorksManager {

  protected final Map<Transaction, AbstractJTAJPAUnitOfWork> uows = new ConcurrentHashMap<>();

  @Inject
  protected UnitOfWorkExtension extension;

  @Inject
  protected TransactionManager transactionManager;

  @Inject
  @Any
  protected Instance<MessageDispatcher> messageDispatcher;

  /**
   * {@inheritDoc}
   * <p>
   * Register current unit of work to JTA transaction session synchronization if current unit of
   * work doesn't register.
   */
  @Override
  public AbstractJTAJPAUnitOfWork getCurrentUnitOfWork() {
    try {
      final Transaction curTx = shouldNotNull(transactionManager.getTransaction(),
          "For now we only support transactional unit of work.");
      initializeCurrentUnitOfWork(curTx);
      return uows.get(curTx);
    } catch (SystemException e) {
      throw new CorantRuntimeException(e, PkgMsgCds.ERR_UOW_CREATE);
    }
  }

  public UnitOfWorkExtension getExtension() {
    return extension;
  }

  public MessageDispatcher getMessageDispatcher() {
    return messageDispatcher.isResolvable() ? messageDispatcher.get() : MessageDispatcher.empty();
  }

  public TransactionManager getTransactionManager() {
    return transactionManager;
  }

  public void initializeCurrentUnitOfWork(Transaction transaction) {
    uows.computeIfAbsent(transaction, key -> {
      try {
        logger.fine(() -> "Register an new unit of work with the current transaction context.");
        AbstractJTAJPAUnitOfWork uow = buildUnitOfWork(key);
        transaction.registerSynchronization(uow);
        return uow;
      } catch (IllegalStateException | RollbackException | SystemException e) {
        throw new CorantRuntimeException(e, PkgMsgCds.ERR_UOW_CREATE);
      }
    });
  }

  protected abstract AbstractJTAJPAUnitOfWork buildUnitOfWork(Transaction transaction);

  protected void clearCurrentUnitOfWorks(Transaction key) {
    logger.fine(() -> "Deregister the unit of work with the current transaction context.");
    uows.remove(key);
  }

  @PreDestroy
  protected synchronized void destroy() {
    uows.clear();
    logger.fine(() -> "Clear unit of works.");
  }

  protected Transaction unwrapUnitOfWorksKey(Object object) {
    return object == null ? null : (Transaction) object;
  }

}

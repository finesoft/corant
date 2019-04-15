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

import static org.corant.Corant.instance;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.MessageDispatcher;
import org.corant.suites.ddd.message.MessageStorage;
import org.corant.suites.ddd.saga.SagaService;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午2:14:21
 *
 */
@ApplicationScoped
@InfrastructureServices
public class JpaUnitOfWorksManager extends AbstractUnitOfWorksManager {

  protected final Map<Object, JpaUnitOfWork> uows = new ConcurrentHashMap<>();

  @Inject
  TransactionManager transactionManager;

  @Inject
  JpaPersistenceService persistenceService;

  @Inject
  @Any
  Instance<MessageDispatcher> messageDispatch;

  @Inject
  @Any
  Instance<MessageStorage> messageStroage;

  @Inject
  @Any
  Instance<SagaService> sagaService;

  public static JpaUnitOfWork curUow() {
    return instance().select(JpaUnitOfWorksManager.class).get().getCurrentUnitOfWork();
  }

  public static int getTxStatusFromCurUow() {
    try {
      return curUow().transaction.getStatus();
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void makeCurUowTxRollbackOnly() {
    try {
      curUow().transaction.setRollbackOnly();
    } catch (IllegalStateException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void registerTxSyncToCurUow(Synchronization sync) {
    try {
      curUow().transaction.registerSynchronization(sync);
    } catch (IllegalStateException | RollbackException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public JpaUnitOfWork getCurrentUnitOfWork() {
    try {
      final Transaction curTx = getTransactionManager().getTransaction();
      final JpaUnitOfWork curUow = uows.computeIfAbsent(wrapUintOfWorksKey(curTx), (key) -> {
        try {
          logger.fine(() -> "Register an new unit of work with the current transacion context.");
          JpaUnitOfWork uow = buildUnitOfWork(unwrapUnifOfWorksKey(key));
          curTx.registerSynchronization(uow);
          return uow;
        } catch (IllegalStateException | RollbackException | SystemException e) {
          throw new CorantRuntimeException(e, PkgMsgCds.ERR_UOW_CREATE);
        }
      });
      return curUow;
    } catch (SystemException | IllegalStateException e) {
      throw new CorantRuntimeException(e, PkgMsgCds.ERR_UOW_CREATE);
    }
  }

  @Override
  public MessageDispatcher getMessageDispatcher() {
    return messageDispatch.isResolvable() ? messageDispatch.get() : MessageDispatcher.DUMMY_INST;
  }

  @Override
  public MessageStorage getMessageStorage() {
    return messageStroage.isResolvable() ? messageStroage.get() : MessageStorage.DUMMY_INST;
  }

  @Override
  public SagaService getSagaService() {
    return sagaService.isResolvable() ? sagaService.get() : SagaService.empty();
  }

  public TransactionManager getTransactionManager() {
    return transactionManager;
  }

  protected EntityManager buildEntityManager(Annotation qualifier) {
    return getEntityManagerFactory(qualifier).createEntityManager(SynchronizationType.SYNCHRONIZED,
        getEntityManagerProperties());
  }

  protected JpaUnitOfWork buildUnitOfWork(Transaction transaction) {
    return new JpaUnitOfWork(this, transaction, this::buildEntityManager);
  }

  protected EntityManagerFactory getEntityManagerFactory(Annotation qualifier) {
    return persistenceService.getEntityManagerFactory(qualifier);
  }

  protected Map<?, ?> getEntityManagerProperties() {
    return Collections.emptyMap();
  }

  protected Transaction unwrapUnifOfWorksKey(Object object) {
    return object == null ? null : (Transaction) object;
  }

  protected Object wrapUintOfWorksKey(Transaction transaction) {
    return transaction;// if use narayana arjuna then their TransactionImple hc/eq use Uid.
  }

  void clearCurrentUnitOfWorks(Object key) {
    logger.fine(() -> "Deregister the unit of work with the current transacion context.");
    uows.remove(key);
  }

  @PreDestroy
  void destroy() {
    uows.clear();
  }

}

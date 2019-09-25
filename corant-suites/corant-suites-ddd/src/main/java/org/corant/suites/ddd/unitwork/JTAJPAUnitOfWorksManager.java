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

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.corant.kernel.api.PersistenceService;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
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
public class JTAJPAUnitOfWorksManager extends AbstractUnitOfWorksManager {

  protected final Map<Object, JTAJPAUnitOfWork> uows = new ConcurrentHashMap<>();

  @Inject
  TransactionManager transactionManager;

  @Inject
  PersistenceService persistenceService;

  @Inject
  @Any
  Instance<MessageDispatcher> messageDispatch;

  @Inject
  @Any
  Instance<MessageStorage> messageStorage;

  @Inject
  @Any
  Instance<SagaService> sagaService;

  public static int getTxStatus() {
    try {
      return curUow().transaction.getStatus();
    } catch (SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void makeTxRollbackOnly() {
    try {
      curUow().transaction.setRollbackOnly();
    } catch (IllegalStateException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static void registerAfterCompletion(final Consumer<Boolean> consumer) {
    if (consumer != null) {
      registerTxSynchronization(new SynchronizationAdapter() {
        @Override
        public void afterCompletion(int status) {
          consumer.accept(status == Status.STATUS_COMMITTED);
        }
      });
    }
  }

  public static void registerBeforeCompletion(final Runnable runner) {
    if (runner != null) {
      registerTxSynchronization(new SynchronizationAdapter() {
        @Override
        public void beforeCompletion() {
          runner.run();
        }
      });
    }
  }

  public static void registerTxSynchronization(Synchronization sync) {
    try {
      curUow().transaction.registerSynchronization(sync);
    } catch (IllegalStateException | RollbackException | SystemException e) {
      throw new CorantRuntimeException(e);
    }
  }

  static JTAJPAUnitOfWork curUow() {
    Optional<UnitOfWork> curuow = UnitOfWorksManager.currentUnitOfWork();
    if (curuow.isPresent() && curuow.get() instanceof JTAJPAUnitOfWork) {
      return (JTAJPAUnitOfWork) curuow.get();
    } else {
      throw new NotSupportedException();
    }
  }

  @Override
  public JTAJPAUnitOfWork getCurrentUnitOfWork() {
    try {
      final Transaction curTx = shouldNotNull(getTransactionManager().getTransaction(),
          "For now we only support transactional unit of work.");
      final JTAJPAUnitOfWork curUow = uows.computeIfAbsent(wrapUintOfWorksKey(curTx), (key) -> {
        try {
          logger.fine(() -> "Register an new unit of work with the current transacion context.");
          JTAJPAUnitOfWork uow = buildUnitOfWork(unwrapUnifOfWorksKey(key));
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
    return messageStorage.isResolvable() ? messageStorage.get() : MessageStorage.DUMMY_INST;
  }

  @Override
  public PersistenceService getPersistenceService() {
    return persistenceService;
  }

  @Override
  public SagaService getSagaService() {
    return sagaService.isResolvable() ? sagaService.get() : SagaService.empty();
  }

  public TransactionManager getTransactionManager() {
    return transactionManager;
  }

  protected JTAJPAUnitOfWork buildUnitOfWork(Transaction transaction) {
    return new JTAJPAUnitOfWork(this, transaction);
  }

  protected Transaction unwrapUnifOfWorksKey(Object object) {
    return object == null ? null : (Transaction) object;
  }

  protected Object wrapUintOfWorksKey(Transaction transaction) {
    return transaction;// JTA1.3 Spec-> 3.3.4 Transaction Equality and Hash Code
  }

  void clearCurrentUnitOfWorks(Object key) {
    logger.fine(() -> "Deregister the unit of work with the current transacion context.");
    uows.remove(key);
  }

  @PreDestroy
  void destroy() {
    uows.clear();
  }

  static abstract class SynchronizationAdapter implements Synchronization {
    @Override
    public void afterCompletion(int status) {
      // NOOP!
    }

    @Override
    public void beforeCompletion() {
      // NOOP!
    }
  }
}

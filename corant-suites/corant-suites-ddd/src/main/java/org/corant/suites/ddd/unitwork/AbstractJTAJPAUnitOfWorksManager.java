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

import static org.corant.context.Instances.resolve;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.ddd.message.MessageDispatcher;

/**
 * corant-suites-ddd
 *
 * <p>
 * The JTA JPA unit of works manager, use for create and destroy the unit of works, provide
 * thenecessary transaction manager and message dispatch service for the unit of work.
 * </p>
 *
 * @author bingo 下午2:14:21
 *
 */
public abstract class AbstractJTAJPAUnitOfWorksManager extends AbstractJPAUnitOfWorksManager {

  protected final Map<Object, AbstractJTAJPAUnitOfWork> uows = new ConcurrentHashMap<>();

  @Inject
  protected TransactionManager transactionManager;

  @Inject
  @Any
  protected Instance<MessageDispatcher> messageDispatcher;

  public static AbstractJTAJPAUnitOfWork curUow() {
    Optional<AbstractJTAJPAUnitOfWork> curuow =
        resolve(UnitOfWorks.class).currentDefaultUnitOfWork();
    if (curuow.isPresent() && curuow.get() instanceof JTAXAJPAUnitOfWork) {
      return curuow.get();
    } else {
      throw new NotSupportedException();
    }
  }

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

  @Override
  public AbstractJTAJPAUnitOfWork getCurrentUnitOfWork() {
    try {
      final Transaction curTx = shouldNotNull(transactionManager.getTransaction(),
          "For now we only support transactional unit of work.");
      return uows.computeIfAbsent(wrapUintOfWorksKey(curTx), key -> {
        try {
          logger.fine(() -> "Register an new unit of work with the current transacion context.");
          AbstractJTAJPAUnitOfWork uow = buildUnitOfWork(unwrapUnifOfWorksKey(key));
          curTx.registerSynchronization(uow);
          return uow;
        } catch (IllegalStateException | RollbackException | SystemException e) {
          throw new CorantRuntimeException(e, PkgMsgCds.ERR_UOW_CREATE);
        }
      });
    } catch (SystemException | IllegalStateException e) {
      throw new CorantRuntimeException(e, PkgMsgCds.ERR_UOW_CREATE);
    }
  }

  public MessageDispatcher getMessageDispatcher() {
    return messageDispatcher.isResolvable() ? messageDispatcher.get() : MessageDispatcher.empty();
  }

  public TransactionManager getTransactionManager() {
    return transactionManager;
  }

  protected abstract AbstractJTAJPAUnitOfWork buildUnitOfWork(Transaction transaction);

  protected void clearCurrentUnitOfWorks(Object key) {
    logger.fine(() -> "Deregister the unit of work with the current transacion context.");
    uows.remove(key);
  }

  @PreDestroy
  protected synchronized void destroy() {
    uows.clear();
    logger.fine(() -> "Clear unit of works.");
  }

  protected Transaction unwrapUnifOfWorksKey(Object object) {
    return object == null ? null : (Transaction) object;
  }

  protected Object wrapUintOfWorksKey(Transaction transaction) {
    return transaction;// JTA1.3 Spec-> 3.3.4 Transaction Equality and Hash Code
  }

  abstract static class SynchronizationAdapter implements Synchronization {
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

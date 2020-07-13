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

import javax.persistence.FlushModeType;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.bundle.exception.GeneralRuntimeException;
import org.corant.suites.ddd.message.MessageDispatcher;

/**
 * corant-suites-ddd
 *
 * <pre>
 * All entityManager from this unit of work are SynchronizationType.SYNCHRONIZED,
 * and must be in transactional.
 * </pre>
 *
 * @author bingo 上午11:38:39
 *
 */
public abstract class AbstractJTAJPAUnitOfWork extends AbstractJPAUnitOfWork
    implements Synchronization {

  protected final Transaction transaction;
  protected final MessageDispatcher messageDispatcher;

  protected AbstractJTAJPAUnitOfWork(AbstractJTAJPAUnitOfWorksManager manager,
      Transaction transaction) {
    super(manager);
    this.transaction = transaction;
    messageDispatcher = manager.getMessageDispatcher();
    messageDispatcher.prepare();
  }

  @Override
  public void afterCompletion(int status) {
    final boolean success = status == Status.STATUS_COMMITTED;
    try {
      complete(success);
    } finally {
      logger.fine(() -> String.format("End unit of work [%s].", transaction.toString()));
      handlePostCompleted(success);
      clear();
    }
  }

  @Override
  public void beforeCompletion() {
    entityManagers.values().forEach(em -> {
      logger.fine(() -> String.format(
          "Enforce entity managers %s flush to collect the messages, before %s completion.", em,
          transaction.toString()));
      if (USE_MANUAL_FLUSH_MODEL) {
        final FlushModeType fm = em.getFlushMode();
        try {
          if (fm != FlushModeType.COMMIT) {
            em.setFlushMode(FlushModeType.COMMIT);
          }
          em.flush();
        } catch (Exception e) {
          throw new CorantRuntimeException(e);
        } finally {
          if (fm != em.getFlushMode()) {
            em.setFlushMode(fm);
          }
        }
      } else {
        em.flush();
      }
    }); // flush to dump dirty
    handleMessage();
    handlePreComplete();
  }

  @Override
  public Transaction getId() {
    return transaction;
  }

  /**
   * Represents whether the current transaction is still in progress
   *
   * @return isInTransaction
   */
  public boolean isInTransaction() {
    try {
      if (transaction == null) {
        return false;
      } else {
        int status = transaction.getStatus();
        return status == Status.STATUS_ACTIVE || status == Status.STATUS_COMMITTING
            || status == Status.STATUS_MARKED_ROLLBACK || status == Status.STATUS_PREPARED
            || status == Status.STATUS_PREPARING || status == Status.STATUS_ROLLING_BACK;
      }
    } catch (SystemException e) {
      throw new GeneralRuntimeException(e, PkgMsgCds.ERR_UOW_TRANS);
    }
  }

  @Override
  public String toString() {
    return transaction.toString();
  }

  @Override
  protected void clear() {
    try {
      super.clear();
    } finally {
      getManager().clearCurrentUnitOfWorks(transaction);
    }
  }

  @Override
  protected AbstractJTAJPAUnitOfWorksManager getManager() {
    return (AbstractJTAJPAUnitOfWorksManager) super.getManager();
  }

  protected abstract void handleMessage();

  @Override
  protected boolean isActivated() {
    return activated && isInTransaction();
  }

}

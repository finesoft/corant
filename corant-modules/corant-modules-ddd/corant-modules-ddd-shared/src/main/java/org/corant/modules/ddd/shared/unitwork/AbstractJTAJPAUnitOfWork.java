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

import java.util.HashMap;
import java.util.Map;
import javax.persistence.FlushModeType;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import org.corant.context.CDIs;
import org.corant.modules.ddd.Aggregate.AggregateIdentifier;
import org.corant.modules.ddd.MessageDispatcher;
import org.corant.modules.ddd.annotation.AggregateType.AggregateTypeLiteral;
import org.corant.modules.ddd.shared.event.AggregateEvolutionaryEvent;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.GeneralRuntimeException;
import org.corant.shared.ubiquity.Mutable.MutableBoolean;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * The JPA unit of work based on JTA transaction boundaries. The unit of work implements
 * {@link Synchronization}, and all EntityManagers in the unit of work are
 * SynchronizationType.SYNCHRONIZED, and must be in transaction state. <br>
 * Before the transaction is committed (before prepare JTA in the two phases), all entity states in
 * EntityManagers are flushed to the underlying storage, and messages are collected at the same
 * time.
 * </p>
 *
 * @author bingo 上午11:38:39
 *
 */
public abstract class AbstractJTAJPAUnitOfWork extends AbstractJPAUnitOfWork
    implements Synchronization {

  protected final Transaction transaction;
  protected final MessageDispatcher messageDispatcher;
  protected final UnitOfWorkExtension extension;

  protected AbstractJTAJPAUnitOfWork(AbstractJTAJPAUnitOfWorksManager manager,
      Transaction transaction) {
    super(manager);
    extension = manager.getExtension();
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
    // fan-out the aggregate state changes to inform the event handlers in the current unit of work
    fanoutEvolutions();
    // fan-out the collected domain messages
    fanoutMessages();
    // handle unit of work call-backs
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

  protected void fanoutEvolutions() {
    // flush the aggregate state changes to persistence in the current unit of work
    flushEntityManagers();
    if (!evolutionaryAggregates.isEmpty()) {
      Map<AggregateIdentifier, Integer> evolutions = new HashMap<>();
      Map<AggregateIdentifier, Integer> temp = new HashMap<>(evolutionaryAggregates.size());
      MutableBoolean fanout = new MutableBoolean(false);
      int i = MAX_CHANGES_ITERATIONS;
      while (true) {
        temp.clear();
        fanout.set(false);
        evolutionaryAggregates.forEach((k, v) -> {
          if (extension.supportsEvolutionaryObserver(k.getTypeCls())) {
            temp.put(k, v.left());
          }
        });
        if (temp.isEmpty() || evolutions.equals(temp)) {
          break;
        }
        temp.forEach((k, v) -> {
          Integer last = evolutions.get(k);
          if (last == null || last.intValue() != v.intValue()) {
            evolutions.put(k, v);
            CDIs.fireEvent(new AggregateEvolutionaryEvent(registeredAggregates.get(k)),
                AggregateTypeLiteral.of(k.getTypeCls()));
            fanout.set(true);
          }
        });
        if (fanout.get()) {
          // the state of the aggregate in the current unit of work may change again, flush again
          flushEntityManagers();
        } else {
          break;
        }
        if (--i < 0) {
          throw new CorantRuntimeException(
              "Reach max changes iterations [%s], can't handle aggregate evolutionary event! ",
              MAX_CHANGES_ITERATIONS);
        }
      }
      evolutions.clear();
      temp.clear();
    }
  }

  protected abstract void fanoutMessages();

  protected void flushEntityManagers() {
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
    });
  }

  @Override
  protected AbstractJTAJPAUnitOfWorksManager getManager() {
    return (AbstractJTAJPAUnitOfWorksManager) super.getManager();
  }

  @Override
  protected boolean isActivated() {
    return activated && isInTransaction();
  }

  @Override
  protected boolean supportsPersistedObserver(Class<?> aggregateClass) {
    return extension.supportsPersistedObserver(aggregateClass);
  }

}

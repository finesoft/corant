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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import org.corant.Corant;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ObjectUtils;
import org.corant.suites.ddd.event.AggregationLifecycleEvent;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.message.MessageUtils;
import org.corant.suites.ddd.model.AbstractAggregation.DefaultAggregationIdentifier;
import org.corant.suites.ddd.model.Aggregation;
import org.corant.suites.ddd.model.Aggregation.AggregationIdentifier;
import org.corant.suites.ddd.model.Aggregation.Lifecycle;
import org.corant.suites.ddd.model.Entity.EntityManagerProvider;

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
public class JTAJPAUnitOfWork extends AbstractUnitOfWork
    implements Synchronization, EntityManagerProvider {

  static final String LOG_BEGIN_UOW_FMT = "Begin unit of work [%s].";
  static final String LOG_END_UOW_FMT = "End unit of work [%s].";
  static final String LOG_BEF_UOW_CMP_FMT =
      "Enforce entity managers flush to collect the messages, before %s completion.";
  static final String LOG_HDL_MSG_FMT = "Sorted the flushed messages and store them if nessuary,"
      + " dispatch them to the message dispatcher, before %s completion.";
  static final String LOG_MSG_CYCLE_FMT = "Can not handle messages!";

  final transient Transaction transaction;
  final Map<PersistenceContext, EntityManager> entityManagers = new HashMap<>();
  final Map<Lifecycle, Set<AggregationIdentifier>> registration = new EnumMap<>(Lifecycle.class);

  protected JTAJPAUnitOfWork(JTAJPAUnitOfWorksManager manager, Transaction transaction) {
    super(manager);
    this.transaction = transaction;
    Arrays.stream(Lifecycle.values()).forEach(e -> registration.put(e, new LinkedHashSet<>()));
    logger.fine(() -> String.format(LOG_BEGIN_UOW_FMT, transaction.toString()));
  }

  @Override
  public void afterCompletion(int status) {
    final boolean success = status == Status.STATUS_COMMITTED;
    final Map<Lifecycle, Set<AggregationIdentifier>> registers = new EnumMap<>(Lifecycle.class);
    try {
      complete(success);
      registers.putAll(getRegisters());
    } finally {
      clear();
      logger.fine(() -> String.format(LOG_END_UOW_FMT, transaction.toString()));
      handlePostCompleted(registers, success);
    }
  }

  @Override
  public void beforeCompletion() {
    handlePreComplete();
    logger.fine(() -> String.format(LOG_BEF_UOW_CMP_FMT, transaction.toString()));
    entityManagers.values().forEach(EntityManager::flush);
    registration.forEach((k, v) -> {
      if (k == Lifecycle.ENABLED || k == Lifecycle.DESTROYED) {
        v.forEach(ai -> Corant.fireEvent(new AggregationLifecycleEvent(ai, k)));
      }
    });
    handleMessage();
  }

  @Override
  public void deregister(Object obj) {
    if (activated) {
      if (obj instanceof Aggregation) {
        Aggregation aggregation = (Aggregation) obj;
        if (aggregation.getId() != null) {
          AggregationIdentifier ai = new DefaultAggregationIdentifier(aggregation);
          registration.values().forEach(v -> v.remove(ai));
          messages.removeIf(e -> ObjectUtils.isEquals(e.getMetadata().getSource(), ai));
        }
      } else if (obj instanceof Message) {
        messages.remove(obj);
      }
    } else {
      throw new GeneralRuntimeException(PkgMsgCds.ERR_UOW_NOT_ACT);
    }
  }

  @Override
  public EntityManager getEntityManager(PersistenceContext pc) {
    return entityManagers.computeIfAbsent(pc, persistenceService::getEntityManager);
  }

  @Override
  public Transaction getId() {
    return transaction;
  }

  @Override
  public Map<Lifecycle, Set<AggregationIdentifier>> getRegisters() {
    Map<Lifecycle, Set<AggregationIdentifier>> clone = new EnumMap<>(Lifecycle.class);
    registration.forEach((k, v) -> {
      clone.put(k, Collections.unmodifiableSet(new LinkedHashSet<>(v)));
    });
    return Collections.unmodifiableMap(clone);
  }

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
  public void register(Object obj) {
    if (activated && isInTransaction()) {
      if (obj instanceof Aggregation) {
        Aggregation aggregation = (Aggregation) obj;
        if (aggregation.getId() != null) {
          AggregationIdentifier ai = new DefaultAggregationIdentifier(aggregation);
          registration.forEach((k, v) -> {
            if (k != aggregation.getLifecycle()) {
              v.remove(ai);
            }
          });
          registration.get(aggregation.getLifecycle()).add(ai);
          aggregation.extractMessages(true)
              .forEach(message -> MessageUtils.mergeToQueue(messages, message));
        }
      } else if (obj instanceof Message) {
        MessageUtils.mergeToQueue(messages, Message.class.cast(obj));
      }
    } else {
      throw new GeneralRuntimeException(PkgMsgCds.ERR_UOW_NOT_ACT);
    }
  }

  @Override
  public String toString() {
    return transaction.toString();
  }

  @Override
  protected void clear() {
    try {
      entityManagers.values().forEach(em -> {
        if (em.isOpen()) {
          em.close();
        }
      });
      registration.clear();
    } finally {
      getManager().clearCurrentUnitOfWorks(transaction);
    }
  }

  @Override
  protected JTAJPAUnitOfWorksManager getManager() {
    return (JTAJPAUnitOfWorksManager) super.getManager();
  }

  protected void handleMessage() {
    logger.fine(() -> String.format(LOG_HDL_MSG_FMT, transaction.toString()));
    LinkedList<Message> messages = new LinkedList<>();
    extractMessages(messages);
    int safeExtracts = 128;
    while (!messages.isEmpty()) {
      Message msg = messages.pop();
      messageStorage.apply(msg);
      sagaService.trigger(msg);// FIXME Is it right to do so?
      messageDispatcher.accept(new Message[] {msg});
      if (extractMessages(messages) && --safeExtracts < 0) {
        throw new CorantRuntimeException(LOG_MSG_CYCLE_FMT);
      }
    }
  }

  boolean extractMessages(LinkedList<Message> messages) {
    if (!this.messages.isEmpty()) {
      this.messages.stream().sorted(Message::compare).forEach(messages::offer);
      this.messages.clear();
      return true;
    }
    return false;
  }
}

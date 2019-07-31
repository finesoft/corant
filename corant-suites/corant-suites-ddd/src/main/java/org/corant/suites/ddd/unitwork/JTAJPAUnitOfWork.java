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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
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
  final Map<AggregationIdentifier, Lifecycle> registrations = new LinkedHashMap<>();
  final Set<AggregationIdentifier> aggregations = new LinkedHashSet<>();
  final LinkedList<Message> messages = new LinkedList<>();

  protected JTAJPAUnitOfWork(JTAJPAUnitOfWorksManager manager, Transaction transaction) {
    super(manager);
    this.transaction = transaction;
    logger.fine(() -> String.format(LOG_BEGIN_UOW_FMT, transaction.toString()));
  }

  @Override
  public void afterCompletion(int status) {
    final boolean success = status == Status.STATUS_COMMITTED;
    try {
      complete(success);
    } finally {
      clear();
      logger.fine(() -> String.format(LOG_END_UOW_FMT, transaction.toString()));
      handlePostCompleted(new LinkedHashMap<>(registrations), success);
    }
  }

  @Override
  public void beforeCompletion() {
    handlePreComplete();
    logger.fine(() -> String.format(LOG_BEF_UOW_CMP_FMT, transaction.toString()));
    // flush to dump dirty
    entityManagers.values().forEach(EntityManager::flush);
    int cycles = 128;
    while (!aggregations.isEmpty()) {
      List<AggregationIdentifier> caches = new ArrayList<>(aggregations);
      aggregations.clear();
      boolean influence = false;
      for (AggregationIdentifier ai : caches) {
        Lifecycle v = registrations.get(ai);
        if (v == Lifecycle.ENABLED || v == Lifecycle.DESTROYED) {
          Corant.fireEvent(new AggregationLifecycleEvent(ai, v));
          influence = true;
        }
      }
      if (influence) {
        // flush again to find dirty
        entityManagers.values().forEach(EntityManager::flush);
      }
      if (--cycles < 0) {
        throw new CorantRuntimeException(LOG_MSG_CYCLE_FMT);
      }
    }
    handleMessage();
  }

  @Override
  public void deregister(Object obj) {
    if (activated) {
      if (obj instanceof Aggregation) {
        Aggregation aggregation = (Aggregation) obj;
        if (aggregation.getId() != null) {
          AggregationIdentifier ai = new DefaultAggregationIdentifier(aggregation);
          registrations.remove(ai);
          aggregations.remove(ai);
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

  public List<Message> getMessages() {
    return Collections.unmodifiableList(messages);
  }

  @Override
  public Map<AggregationIdentifier, Lifecycle> getRegistrations() {
    return Collections.unmodifiableMap(registrations);
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
          Lifecycle al = aggregation.getLifecycle();
          registrations.put(ai, al);
          aggregations.add(ai);
          for (Message message : aggregation.extractMessages(true)) {
            MessageUtils.mergeToQueue(messages, message);
          }
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

  protected void clear() {
    try {
      entityManagers.values().forEach(em -> {
        if (em.isOpen()) {
          em.close();
        }
      });
      registrations.clear();
      messages.clear();
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
    int cycles = 128;
    while (!messages.isEmpty()) {
      Message msg = messages.pop();
      messageStorage.apply(msg);
      sagaService.trigger(msg);// FIXME Is it right to do so?
      messageDispatcher.accept(new Message[] {msg});
      if (extractMessages(messages) && --cycles < 0) {
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

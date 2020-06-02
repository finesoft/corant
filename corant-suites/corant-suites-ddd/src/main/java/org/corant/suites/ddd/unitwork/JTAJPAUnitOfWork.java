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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Pair;
import org.corant.shared.util.ObjectUtils;
import org.corant.suites.bundle.exception.GeneralRuntimeException;
import org.corant.suites.cdi.CDIs;
import org.corant.suites.ddd.annotation.qualifier.AggregateType.AggregateTypeLiteral;
import org.corant.suites.ddd.event.AggregatePersistEvent;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.message.MessageUtils;
import org.corant.suites.ddd.model.AbstractAggregate.DefaultAggregateIdentifier;
import org.corant.suites.ddd.model.Aggregate;
import org.corant.suites.ddd.model.Aggregate.AggregateIdentifier;
import org.corant.suites.ddd.model.Aggregate.Lifecycle;
import org.corant.suites.ddd.model.Entity.EntityManagerProvider;
import org.eclipse.microprofile.config.ConfigProvider;

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
      "Enforce entity managers %s flush to collect the messages, before %s completion.";
  static final String LOG_HDL_MSG_FMT = "Sorted the flushed messages and store them if nessuary,"
      + " dispatch them to the message dispatcher, before %s completion.";
  static final String LOG_MSG_CYCLE_FMT = "Can not handle messages!";
  static final boolean USE_MANUAL_FLUSH_MODEL = ConfigProvider.getConfig()
      .getOptionalValue("ddd.unitofwork.use-manual-flush", Boolean.class).orElse(Boolean.FALSE);

  final Transaction transaction;
  final Map<PersistenceContext, EntityManager> entityManagers = new HashMap<>();
  final Map<AggregateIdentifier, Lifecycle> registeredAggregates = new LinkedHashMap<>();
  final Map<AggregateIdentifier, Lifecycle> evolutiveAggregates = new LinkedHashMap<>();
  final Map<Object, Object> registeredVariables = new LinkedHashMap<>();
  final LinkedList<Message> registeredMessages = new LinkedList<>();

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
      logger.fine(() -> String.format(LOG_END_UOW_FMT, transaction.toString()));
      handlePostCompleted(success);
      clear();
    }
  }

  @Override
  public void beforeCompletion() {
    entityManagers.values().forEach(em -> {
      logger.fine(() -> String.format(LOG_BEF_UOW_CMP_FMT, em, transaction.toString()));
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

  /**
   * {@inheritDoc}
   *
   * <p>
   * This method must be invoked in a transaction.
   *
   * @see #register(Object)
   */
  @Override
  public void deregister(Object obj) {
    if (activated) {
      if (obj instanceof Aggregate) {
        Aggregate aggregate = (Aggregate) obj;
        if (aggregate.getId() != null) {
          AggregateIdentifier ai = new DefaultAggregateIdentifier(aggregate);
          registeredAggregates.remove(ai);
          evolutiveAggregates.remove(ai);
          registeredMessages.removeIf(e -> ObjectUtils.isEquals(e.getMetadata().getSource(), ai));
        }
      } else if (obj instanceof Message) {
        registeredMessages.remove(obj);
      } else if (obj instanceof Pair<?, ?>) {
        Pair<?, ?> p = (Pair<?, ?>) obj;
        registeredVariables.remove(p.getKey());
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

  /**
   * Message queue collected by the current unit of work.
   *
   * @return getMessages
   */
  public List<Message> getMessages() {
    return Collections.unmodifiableList(registeredMessages);
  }

  @Override
  public Registration getRegistrations() {
    return new Registration(this);
  }

  /**
   * Variables in the scope of the current unit of work.
   *
   * @return getVariables
   */
  public Map<Object, Object> getVariables() {
    return registeredVariables;
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

  /**
   * {@inheritDoc}
   * <p>
   * This method must be invoked in a transaction.
   * <ul>
   * <b>Parameter descriptions</b>
   * <li>If the parameter is aggregation, the aggregation id and life cycle state, as well as the
   * message queue in the aggregation, are registered.</li>
   * <li>If the parameter is a message, the message is merged into a message queue when the message
   * type is mergeable, and non-mergeable message is added directly to the message queue.</li>
   * <li>If the parameter is a named object (represented by Pair), the object is added to a Map as a
   * key-value pair</li>
   * </ul>
   *
   * @see AggregateIdentifier
   * @see Pair
   * @see Message
   */
  @Override
  public void register(Object obj) {
    if (activated && isInTransaction()) {
      if (obj instanceof Aggregate) {
        Aggregate aggregate = (Aggregate) obj;
        if (aggregate.getId() != null) {
          AggregateIdentifier ai = new DefaultAggregateIdentifier(aggregate);
          Lifecycle al = aggregate.getLifecycle();
          registeredAggregates.put(ai, al);
          if (al.signFlushed()) {
            evolutiveAggregates.put(ai, al);
          }
          for (Message message : aggregate.extractMessages(true)) {
            MessageUtils.mergeToQueue(registeredMessages, message);
          }
        }
      } else if (obj instanceof Message) {
        MessageUtils.mergeToQueue(registeredMessages, (Message) obj);
      } else if (obj instanceof Map.Entry<?, ?>) {
        Map.Entry<?, ?> p = (Map.Entry<?, ?>) obj;
        registeredVariables.put(p.getKey(), p.getValue());
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
      evolutiveAggregates.clear();
      registeredAggregates.clear();
      registeredMessages.clear();
      registeredVariables.clear();
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

  protected void handlePostCompleted(final boolean success) {
    final Registration registration = new Registration(this);
    manager.getListeners().forEach(listener -> {
      try {
        listener.onCompleted(registration, success);
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex, () -> "Handle UOW post-completed occurred error!");
      }
    });
    if (success) {
      evolutiveAggregates.forEach((k, v) -> {
        if (v.signFlushed()) {
          try {
            CDIs.fireAsyncEvent(new AggregatePersistEvent(k, v),
                AggregateTypeLiteral.of(k.getTypeCls()));
          } catch (Exception ex) {
            logger.log(Level.WARNING, ex, () -> "Fire persist event occurred error!");
          }
        }
      });
    }
  }

  boolean extractMessages(LinkedList<Message> messages) {
    if (!registeredMessages.isEmpty()) {
      registeredMessages.stream().sorted(Message::compare).forEach(messages::offer);
      registeredMessages.clear();
      return true;
    }
    return false;
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 上午9:50:36
   *
   */
  public static class Registration {

    final Map<AggregateIdentifier, Lifecycle> aggregates;
    final Map<Object, Object> variables;

    Registration(JTAJPAUnitOfWork uow) {
      aggregates = Collections.unmodifiableMap(new LinkedHashMap<>(uow.registeredAggregates));
      variables = Collections.unmodifiableMap(new LinkedHashMap<>(uow.registeredVariables));
    }

    public Map<AggregateIdentifier, Lifecycle> getAggregates() {
      return aggregates;
    }

    public Map<Object, Object> getVariables() {
      return variables;
    }

  }
}

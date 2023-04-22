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
package org.corant.modules.ddd.shared.model;

import static org.corant.shared.util.Preconditions.requireFalse;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EntityManager;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import org.corant.modules.ddd.Aggregate;
import org.corant.modules.ddd.AggregateAssistant;
import org.corant.modules.ddd.AggregateLifecycleManageEvent;
import org.corant.modules.ddd.AggregateLifecycleManager;
import org.corant.modules.ddd.AggregateLifecycleManager.LifecycleAction;
import org.corant.modules.ddd.Event;
import org.corant.modules.ddd.Message;
import org.corant.modules.ddd.UnitOfWork;
import org.corant.modules.ddd.annotation.AggregateType.AggregateTypeLiteral;
import org.corant.modules.ddd.shared.event.AggregateLifecycleEvent;
import org.corant.modules.ddd.shared.message.AggregateLifecycleMessage;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午3:25:51
 *
 */
@MappedSuperclass
@EntityListeners(value = {DefaultAggregateListener.class})
public abstract class AbstractAggregate extends AbstractEntity implements Aggregate {

  private static final long serialVersionUID = -9184534676784775644L;

  protected transient volatile Lifecycle lifecycle = Lifecycle.INITIAL;

  @Version
  @Column(name = "vn")
  private volatile long vn = 1L;

  @Override
  public synchronized List<Message> extractMessages(boolean flush) {
    return callAssistant().dequeueMessages(flush);
  }

  /**
   * @see Lifecycle
   */
  @Override
  @Transient
  @jakarta.persistence.Transient
  public synchronized Lifecycle getLifecycle() {
    return lifecycle;
  }

  @Override
  public synchronized Long getVn() {
    return vn;
  }

  @Override
  public void raise(boolean anyway, Message... messages) {
    callAssistant().enqueueMessages(anyway, messages);
  }

  @Override
  public void raise(Event event, Annotation... qualifiers) {
    callAssistant().fireEvent(event, qualifiers);
  }

  /**
   * Send message as long as the unit of works is completed anyway.
   *
   * @param message the message to send
   */
  public void raise(Message... message) {
    raise(true, message);
  }

  @Override
  public void raiseAsync(Event event, Annotation... qualifiers) {
    callAssistant().fireAsyncEvent(event, qualifiers);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [id=" + getId() + ",vn = " + getVn() + "]";
  }

  protected Optional<AggregateLifecycleMessage> buildLifecycleMessage(Lifecycle lifecycle) {
    return Optional.empty();
  }

  /**
   * Obtain an assistant for the aggregate, subclass can rewrite this method for supply an assistant
   */
  protected abstract AggregateAssistant callAssistant();

  /**
   * The current unit of work or null
   *
   * @return currentUnitOfWork
   */
  @Transient
  @jakarta.persistence.Transient
  protected Optional<? extends UnitOfWork> currentUnitOfWork() {
    return callAssistant().currentUnitOfWork();
  }

  /**
   * Destroy the aggregate if is persisted then remove it from entity manager else just mark
   * destroyed, in JPA environment this method will invoke entity manager to remove the aggregate.
   *
   * @param immediately In JPA environment, if it true the entity manager will be flushed.
   * @see AggregateLifecycleManager
   * @see EntityManager#remove(Object)
   */
  protected synchronized void destroy(boolean immediately) {
    requireFalse(lifecycle.getSign() < 0, PkgMsgCds.ERR_AGG_LC);
    this.raise(new AggregateLifecycleManageEvent(this, LifecycleAction.REMOVE, immediately));
    setLifecycle(Lifecycle.DESTROYED);
  }

  /**
   * Destroy preconditions, in general use to validate the aggregate internal consistency, in JPA
   * environment this method is the EntityListener callback.
   * <p>
   * <b>Note:</b> In JPA environment, this method should not invoke EntityManager or query
   * operations, access other aggregate instances, or modify relationships within the same
   * persistence context.
   *
   * @see DefaultAggregateListener
   * @see PreRemove
   * @see EntityManager#flush()
   * @see EntityManager#setFlushMode(jakarta.persistence.FlushModeType)
   */
  protected void onPreDestroy() {}

  /**
   * Preserve preconditions, in general use to validate the aggregate internal consistency, in JPA
   * environment this method is the EntityListener callback.
   * <p>
   * <b>Note:</b> In JPA environment, this method should not invoke EntityManager or query
   * operations, access other aggregate instances, or modify relationships within the same
   * persistence context, this method may modify the non-relationship state of the aggregate on
   * which it is invoked.
   *
   * @see DefaultAggregateListener
   * @see PrePersist
   * @see PreUpdate
   * @see EntityManager#flush()
   * @see EntityManager#setFlushMode(jakarta.persistence.FlushModeType)
   */
  protected void onPrePreserve() {}

  /**
   * Preserve the aggregate if is not persisted then persist it else merge it, in JPA environment
   * this method will invoke entity manager to persist or merge the aggregate.
   *
   * @param immediately flush to storage immediately, in JPA environment, if it true the entity
   *        manager will be flushed.
   * @see AggregateLifecycleManager
   * @see EntityManager#persist(Object)
   * @see EntityManager#merge(Object)
   */
  protected synchronized AbstractAggregate preserve(boolean immediately) {
    requireFalse(lifecycle.getSign() < 0, PkgMsgCds.ERR_AGG_LC);
    this.raise(new AggregateLifecycleManageEvent(this, LifecycleAction.PERSIST, immediately));
    return setLifecycle(Lifecycle.PRESERVED);
  }

  /**
   * Recover from persistence, in JPA environment this method will invoke entity manager to refresh
   * the aggregate.
   *
   * @see AggregateLifecycleManager
   * @see EntityManager#refresh(Object)
   */
  protected synchronized AbstractAggregate recover() {
    requireFalse(lifecycle == null || !lifecycle.signRefreshable(), PkgMsgCds.ERR_AGG_LC);
    this.raise(new AggregateLifecycleManageEvent(this, LifecycleAction.RECOVER, true));
    return this;
  }

  /**
   * Set the aggregate lifecycle stage and raise lifecycle event.
   *
   * Raise Lifecycle.POST_REMOVED event by default.
   *
   * @param lifecycle the life cycle to change
   */
  protected synchronized AbstractAggregate setLifecycle(Lifecycle lifecycle) {
    if (this.lifecycle != lifecycle) {
      this.lifecycle = lifecycle;
      if (lifecycle == Lifecycle.POST_REMOVED) {
        this.raise(new AggregateLifecycleEvent(this), AggregateTypeLiteral.of(getClass()));
      }
    }
    return this;
  }

  /**
   * This method is not normally invoked manually
   *
   * @param vn setVn
   */
  protected synchronized void setVn(long vn) {
    this.vn = vn;
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }
}

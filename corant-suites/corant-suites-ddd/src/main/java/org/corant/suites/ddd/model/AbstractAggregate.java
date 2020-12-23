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
package org.corant.suites.ddd.model;

import static org.corant.suites.bundle.Preconditions.requireFalse;
import static org.corant.suites.bundle.Preconditions.requireNotNull;
import java.beans.Transient;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.EntityManager;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Version;
import org.corant.shared.util.Objects;
import org.corant.suites.bundle.GlobalMessageCodes;
import org.corant.suites.ddd.annotation.qualifier.AggregateType.AggregateTypeLiteral;
import org.corant.suites.ddd.event.AggregateLifecycleEvent;
import org.corant.suites.ddd.event.AggregateLifecycleManageEvent;
import org.corant.suites.ddd.event.Event;
import org.corant.suites.ddd.message.AggregateLifecycleMessage;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.model.AggregateLifecycleManager.LifecycleAction;
import org.corant.suites.ddd.unitwork.UnitOfWork;

/**
 * corant-suites-ddd
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
  @javax.persistence.Transient
  public synchronized Lifecycle getLifecycle() {
    return lifecycle;
  }

  @Override
  public synchronized Long getVn() {
    return vn;
  }

  @Override
  public void raise(Event event, Annotation... qualifiers) {
    callAssistant().fireEvent(event, qualifiers);
  }

  @Override
  public void raise(Message... messages) {
    callAssistant().enqueueMessages(messages);
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
  @javax.persistence.Transient
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
   * @see EntityManager#setFlushMode(javax.persistence.FlushModeType)
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
   * @see EntityManager#setFlushMode(javax.persistence.FlushModeType)
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
   * @param lifecycle
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

  /**
   * corant-suites-ddd
   *
   * @author bingo 上午11:58:12
   *
   */
  public static final class DefaultAggregateIdentifier implements AggregateIdentifier {

    private static final long serialVersionUID = -930151000998600572L;

    private final Serializable id;

    private final Class<? extends Aggregate> typeCls;

    private final int hash;

    public DefaultAggregateIdentifier(Aggregate aggregate) {
      id = requireNotNull(requireNotNull(aggregate, GlobalMessageCodes.ERR_OBJ_NON_FUD).getId(),
          GlobalMessageCodes.ERR_SYS);
      typeCls = requireNotNull(aggregate.getClass(), GlobalMessageCodes.ERR_SYS);
      hash = Objects.hash(id, typeCls);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      DefaultAggregateIdentifier other = (DefaultAggregateIdentifier) obj;
      if (id == null) {
        if (other.id != null) {
          return false;
        }
      } else if (!id.equals(other.id)) {
        return false;
      }
      if (typeCls == null) {
        return other.typeCls == null;
      } else {
        return typeCls.equals(other.typeCls);
      }
    }

    @Override
    public Serializable getId() {
      return id;
    }

    @Override
    public String getType() {
      return typeCls == null ? null : typeCls.getName();
    }

    @Override
    @Transient
    public Class<? extends Aggregate> getTypeCls() {
      return typeCls;
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public String toString() {
      return "{\"typeCls\":\"" + typeCls + "\",\"id\":" + id + "}";
    }

  }
}

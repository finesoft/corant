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

import static org.corant.kernel.util.Preconditions.requireFalse;
import static org.corant.kernel.util.Preconditions.requireNotNull;
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
import org.corant.suites.bundle.GlobalMessageCodes;
import org.corant.suites.ddd.annotation.qualifier.AggregateType.AggregateTypeLiteral;
import org.corant.suites.ddd.event.AggregateLifecycleEvent;
import org.corant.suites.ddd.event.AggregateLifecycleManageEvent;
import org.corant.suites.ddd.event.Event;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.model.EntityLifecycleManager.LifecycleAction;
import org.corant.suites.ddd.unitwork.UnitOfWork;
import org.corant.suites.ddd.unitwork.UnitOfWorksManager;

/**
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
  protected Optional<UnitOfWork> currentUnitOfWork() {
    return UnitOfWorksManager.currentUnitOfWork();
  }

  /**
   * Destroy the aggregate if is persisted then remove it from entity manager else just mark
   * destroyed
   */
  protected synchronized void destroy(boolean immediately) {
    requireFalse(getLifecycle().getSign() < 0, PkgMsgCds.ERR_AGG_LC);
    this.raise(new AggregateLifecycleManageEvent(this, LifecycleAction.REMOVE, immediately));
    lifecycle(Lifecycle.DESTROYED);
  }

  /**
   * Set the aggregate lifecycle stage and raise lifecycle event.
   *
   * Do not raise Lifecycle.LOAD event by default.
   *
   * @param lifecycle
   * @return lifecycle
   */
  protected synchronized AbstractAggregate lifecycle(Lifecycle lifecycle) {
    if (this.lifecycle != lifecycle) {
      this.lifecycle = lifecycle;
      if (lifecycle != Lifecycle.LOADED) {
        this.raise(new AggregateLifecycleEvent(this), AggregateTypeLiteral.of(getClass()));
      }
    }
    return this;
  }

  /**
   * Destroy preconditions, validate the aggregate consistency, this method is the EntityListener
   * callback.
   *
   * @see DefaultAggregateListener
   * @see PreRemove
   */
  protected void onPreDestroy() {}

  /**
   * Preserve preconditions, validate the aggregate consistency, this method is the EntityListener
   * callback.
   *
   * @see DefaultAggregateListener
   * @see PrePersist
   * @see PreUpdate
   */
  protected void onPrePreserve() {}

  /**
   * Preserve the aggregate if is not persisted then persist it else merge it.
   *
   * @param immediately flush to stroage immediately
   *
   * @see EntityManager#flush()
   */
  protected synchronized AbstractAggregate preserve(boolean immediately) {
    requireFalse(getLifecycle().getSign() < 0, PkgMsgCds.ERR_AGG_LC);
    this.raise(new AggregateLifecycleManageEvent(this, LifecycleAction.PERSIST, immediately));
    return lifecycle(Lifecycle.PRESERVED);
  }

  /**
   * recover from persistence
   *
   * @see EntityManager#refresh(Object)
   */
  protected synchronized AbstractAggregate recover() {
    requireFalse(lifecycle == null || !lifecycle.signRefreshable(), PkgMsgCds.ERR_AGG_LC);
    this.raise(new AggregateLifecycleManageEvent(this, LifecycleAction.RECOVER, true));
    return this;
  }

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

    private final Class<?> typeCls;

    private final int hash;

    public DefaultAggregateIdentifier(Aggregate aggregate) {
      id = requireNotNull(requireNotNull(aggregate, GlobalMessageCodes.ERR_OBJ_NON_FUD).getId(),
          GlobalMessageCodes.ERR_SYS);
      typeCls = requireNotNull(aggregate.getClass(), GlobalMessageCodes.ERR_SYS);
      hash = calHash(id, typeCls);
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
        if (other.typeCls != null) {
          return false;
        }
      } else if (!typeCls.equals(other.typeCls)) {
        return false;
      }
      return true;
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
    public Class<?> getTypeCls() {
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

    int calHash(Serializable id, Class<?> typeCls) {
      final int prime = 31;
      int result = 1;
      result = prime * result + (id == null ? 0 : id.hashCode());
      result = prime * result + (typeCls == null ? 0 : typeCls.hashCode());
      return result;
    }
  }
}
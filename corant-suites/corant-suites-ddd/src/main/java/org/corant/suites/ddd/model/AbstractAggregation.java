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
import org.corant.suites.ddd.annotation.qualifier.AggregationType.AggregationTypeLiteral;
import org.corant.suites.ddd.event.AggregationLifecycleEvent;
import org.corant.suites.ddd.event.AggregationLifecycleManageEvent;
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
@EntityListeners(value = {DefaultAggregationListener.class})
public abstract class AbstractAggregation extends AbstractEntity implements Aggregation {

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

  /**
   * Return an aggregation evolutionary version number, this is equivalent to
   * {@link javax.persistence.Version} in JPA
   */
  @Override
  public synchronized Long getVn() {
    return vn;
  }

  /**
   * Arise event, use CDI events
   */
  @Override
  public void raise(Event event, Annotation... qualifiers) {
    callAssistant().fireEvent(event, qualifiers);
  }

  /**
   * Arise message
   */
  @Override
  public void raise(Message... messages) {
    callAssistant().enqueueMessages(messages);
  }

  /**
   * Arise event, use CDI events
   */
  @Override
  public void raiseAsync(Event event, Annotation... qualifiers) {
    callAssistant().fireAsyncEvent(event, qualifiers);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [id=" + getId() + ",vn = " + getVn() + "]";
  }

  /**
   * Obtain an assistant for the aggregation, subclass can rewrite this method for supply an
   * assistant
   */
  protected abstract AggregationAssistant callAssistant();

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
   * Destroy the aggregation if is persisted then remove it from entity manager else just mark
   * destroyed
   */
  protected synchronized void disable(boolean immediately) {
    requireFalse(getLifecycle().getSign() < 0, PkgMsgCds.ERR_AGG_LC);
    this.raise(new AggregationLifecycleManageEvent(this, LifecycleAction.REMOVE, immediately));
    lifecycle(Lifecycle.DISABLED);
  }

  /**
   * Enable the aggregation if is not persisted then persist it else merge it.
   */
  protected synchronized AbstractAggregation enable(boolean immediately) {
    requireFalse(getLifecycle().getSign() < 0, PkgMsgCds.ERR_AGG_LC);
    this.raise(new AggregationLifecycleManageEvent(this, LifecycleAction.PERSIST, immediately));
    return lifecycle(Lifecycle.ENABLED);
  }

  protected synchronized AbstractAggregation lifecycle(Lifecycle lifecycle) {
    if (this.lifecycle != lifecycle) {
      this.lifecycle = lifecycle;
      this.raise(new AggregationLifecycleEvent(this), AggregationTypeLiteral.of(getClass()));
    }
    return this;
  }

  /**
   * Disable preconditions, validate the aggregation consistency, EntityListener callback.
   *
   * @see DefaultAggregationListener
   * @see PreRemove
   */
  protected void onPreDisable() {}

  /**
   * Enable preconditions, validate the aggregation consistency, EntityListener callback.
   *
   * @see DefaultAggregationListener
   * @see PrePersist
   * @see PreUpdate
   */
  protected void onPreEnable() {}

  /**
   * recover from persistence
   *
   * @see EntityManager#refresh(Object)
   */
  protected synchronized AbstractAggregation recover() {
    requireFalse(lifecycle == null || !lifecycle.signRefreshable(), PkgMsgCds.ERR_AGG_LC);
    this.raise(new AggregationLifecycleManageEvent(this, LifecycleAction.RECOVER, true));
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

  public static final class DefaultAggregationIdentifier implements AggregationIdentifier {

    private static final long serialVersionUID = -930151000998600572L;

    private final Serializable id;

    private final Class<?> typeCls;

    private final int hash;

    public DefaultAggregationIdentifier(Aggregation aggregation) {
      id = requireNotNull(requireNotNull(aggregation, GlobalMessageCodes.ERR_OBJ_NON_FUD).getId(),
          GlobalMessageCodes.ERR_SYS);
      typeCls = requireNotNull(aggregation.getClass(), GlobalMessageCodes.ERR_SYS);
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
      DefaultAggregationIdentifier other = (DefaultAggregationIdentifier) obj;
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

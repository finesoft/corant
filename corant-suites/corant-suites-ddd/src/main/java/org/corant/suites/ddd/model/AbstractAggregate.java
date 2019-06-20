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
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import org.corant.suites.bundle.GlobalMessageCodes;
import org.corant.suites.ddd.event.Event;
import org.corant.suites.ddd.event.LifecycleManageEvent;
import org.corant.suites.ddd.message.Message;

/**
 * @author bingo 下午3:25:51
 *
 */
@MappedSuperclass
@EntityListeners(value = {DefaultAggregateListener.class})
public abstract class AbstractAggregate extends AbstractEntity implements Aggregate {

  private static final long serialVersionUID = -9184534676784775644L;

  protected transient volatile Lifecycle lifecycle = Lifecycle.INITIAL;
  protected transient volatile AggregateAssistant assistant;

  @Version
  @Column(name = "vn")
  private volatile long vn = 1L;

  @Override
  public synchronized List<Message> extractMessages(boolean flush) {
    return callAssistant().dequeueMessages(flush);
  }

  /**
   * Identifies whether the aggregate has been persisted or deleted.
   * <li>INITIAL: Just created</li>
   * <li>ENABLED: Has been persisted</li>
   * <li>DESTROYED: If already persisted, the representation is removed from the persistence
   * facility; otherwise it is just a token</li>
   */
  @Override
  @Transient
  @javax.persistence.Transient
  public synchronized Lifecycle getLifecycle() {
    return lifecycle;
  }

  /**
   * Return an aggregate evolutionary version number, this is equivalent to
   * {@link javax.persistence.Version} in JPA
   */
  @Override
  public synchronized Long getVn() {
    return vn;
  }

  /**
   * Identifies whether the aggregate has been deleted or just built
   */
  @Override
  @Transient
  @javax.persistence.Transient
  public synchronized Boolean isPhantom() {
    return getId() == null || lifecycle != Lifecycle.ENABLED;
  }

  /**
   * Arise event
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
   * Arise event.
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
   * Obtain an assistant for the aggregate, subclass can rewrite this method for supply an assistant
   */
  protected abstract AggregateAssistant callAssistant();

  /**
   * Destroy the aggregate if is persisted then remove it from entity manager else just mark
   * destroyed
   */
  protected synchronized void destroy(boolean immediately) {
    this.raise(new LifecycleManageEvent(this, true, immediately));
  }

  /**
   * Enable the aggregate if is not persisted then persist it else merge it.
   */
  protected synchronized AbstractAggregate enable(boolean immediately) {
    requireFalse(getLifecycle() == Lifecycle.DESTROYED, PkgMsgCds.ERR_AGG_LC);
    this.raise(new LifecycleManageEvent(this, false, immediately));
    return this;
  }

  @Transient
  @javax.persistence.Transient
  protected boolean isPersisted() {
    return getId() != null;
  }

  protected Annotation[] lifecycleServiceQualifier() {
    return new Annotation[0];
  }

  /**
   * destroy preconditions, validate the aggregate consistency
   *
   * preDestroy
   */
  protected void preDestroy() {

  }

  /**
   * enable preconditions, validate the aggregate consistency
   *
   * preEnable
   */
  protected void preEnable() {

  }

  protected synchronized void setVn(long vn) {
    this.vn = vn;
  }

  protected synchronized AbstractAggregate withLifecycle(Lifecycle lifecycle) {
    requireFalse(getLifecycle() == Lifecycle.DESTROYED, PkgMsgCds.ERR_AGG_LC);
    this.lifecycle = lifecycle;
    return this;
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

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

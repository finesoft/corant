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

import static org.corant.shared.util.ClassUtils.tryAsClass;
import java.beans.Transient;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import org.corant.suites.ddd.event.Event;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.unitwork.JTAJPAUnitOfWorksManager;
import org.corant.suites.ddd.unitwork.UnitOfWork;

/**
 * Aggregate a cluster of associated objects that are treated as a unit for the purpose of data
 * changes. External references are restricted to one member of the AGGREGATE, designated as the
 * root. A set of consistency rules applies within the AGGREGATE’S boundaries.
 * <p>
 * Aggregates are the basic element of transfer of data storage - you request to load or save whole
 * aggregates. Transactions should not cross aggregate boundaries.
 * <p>
 * Generally speaking the aggregate is the DDD aggregate root entity.
 *
 * @author bingo 下午4:23:02
 * @since
 */
public interface Aggregate extends Entity {

  /**
   * Extract the buffered messages that aggregate raised, this method was invoked before unit of
   * work completed.
   *
   * @param flush if true then clear messages buffer
   * @return extractMessages
   */
  List<Message> extractMessages(boolean flush);

  /**
   * The lifeCycle of aggregate
   *
   * @see Lifecycle
   */
  @Transient
  @javax.persistence.Transient
  Lifecycle getLifecycle();

  /**
   * The aggregate evolution version number, use as persistence version
   * {@link javax.persistence.Version} by default.
   */
  Long getVn();

  /**
   * If the aggregate isn't persisted, or is destroyed, but still live in memory until the GC
   * recycle then return true else return false.
   */
  @Transient
  @javax.persistence.Transient
  default Boolean isPhantom() {
    return getId() == null || !isPreserved();
  }

  /**
   * Indicates whether the aggregate is persisted or not.
   */
  @Transient
  @javax.persistence.Transient
  default boolean isPreserved() {
    return getLifecycle() != null && getLifecycle().signPreserved();
  }

  /**
   * Raise event, use the CDI event mechanism to emit event.
   * <p>
   * Generally speaking the event is not the DDD domain event. It is simply event-driven
   * architecture for decoupling.
   *
   * @param event
   * @param qualifiers raise
   */
  void raise(Event event, Annotation... qualifiers);

  /**
   * Raise message, add the message to the buffer and do not publish it immediately.
   * {@link #extractMessages(boolean)}.
   * <p>
   *
   * <pre>
   * The Message sending timing:
   *
   * 1.In JTA JPA JMS environment, the buffered messages sending timing is between the entity
   * manager flushed and the unit of work completed, this is the default behavior.
   *
   * 2.In the other environment, the buffered message must be send after unit of work completed
   * successfully.
   * </pre>
   *
   * Generally speaking the message is the DDD domain event.
   *
   * @param messages
   *
   * @see UnitOfWork
   * @see JTAJPAUnitOfWorksManager
   */
  void raise(Message... messages);

  /**
   * Raise asynchronous events, {@link #raise(Event, Annotation...)}
   *
   * @param event
   * @param qualifiers raiseAsync
   *
   */
  void raiseAsync(Event event, Annotation... qualifiers);

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午9:04:52
   *
   */
  interface AggregateIdentifier extends EntityIdentifier {

    @Override
    Serializable getId();

    @Override
    String getType();

    default Class<?> getTypeCls() {
      return tryAsClass(getType());
    }
  }

  /**
   *
   * corant-suites-ddd
   *
   * @author bingo 下午9:05:24
   *
   */
  public enum Lifecycle {
    /**
     * Aggregate has just been created.
     */
    INITIAL(0),
    /**
     * Aggregate has been joined persistence context.
     */
    PRESERVED(2),
    /**
     * Aggregate retrieve from storage and has been joined persistence context.
     */
    LOADED(4),
    /**
     * Aggregate will be persist to storage
     *
     * @see PrePersist
     */
    PRE_PERSIST(8),
    /**
     * Aggregate will be update to storage
     *
     * @see PreUpdate
     */
    PRE_UPDATE(16),
    /**
     * Aggregate will be remove from storage
     *
     * @see PreRemove
     */
    PRE_REMOVE(32),
    /**
     * Aggregate has been persisted to storage
     *
     * @see PostPersist
     */
    POST_PERSISTED(64),
    /**
     * Aggregate has been update to storage
     *
     * @see PostUpdate
     */
    POST_UPDATED(128),
    /**
     * Aggregate has been removed from storage
     *
     * @see PostRemove
     */
    POST_REMOVED(256),
    /**
     * If aggregate has already been persisted, the representation is removed from the persistence
     * facility; otherwise it is just a token.
     */
    DESTROYED(512);

    int sign;

    private Lifecycle(int sign) {
      this.sign = sign;
    }

    public int getSign() {
      return sign;
    }

    /**
     * The aggregate remove from persistence context if has been persisted before, otherwise the
     * aggregate has been created and marked destroyed means it can't not participate in any domain
     * logic.
     *
     * @return signDisabled
     */
    public boolean signDestroyed() {
      return (sign & 768) != 0;
    }

    /**
     * The aggregate status have been synchronized to underling storage.
     *
     * @return signFlushed
     */
    public boolean signFlushed() {
      return (sign & 448) != 0;
    }

    /**
     * The aggregate has been joined persistence context and has identity.
     *
     * @return signPreserved
     */
    public boolean signPreserved() {
      return (sign & 254) != 0;
    }

    public boolean signRefreshable() {
      return (sign & 30) != 0 || (sign & 192) != 0;
    }
  }

}

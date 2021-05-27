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
package org.corant.modules.ddd;

import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Objects.forceCast;
import java.beans.Transient;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * corant-modules-ddd-api
 *
 * <p>
 * Aggregate a cluster of associated objects that are treated as a unit for the purpose of data
 * changes. External references are restricted to one member of the AGGREGATE, designated as the
 * root. A set of consistency rules applies within the AGGREGATE’S boundaries.
 * <p>
 * Aggregates are the basic element of transfer of data storage - you request to load or save whole
 * aggregates. If possible, the transactions({@link UnitOfWork}) should not cross aggregate
 * boundaries.
 * <p>
 * Generally the aggregate is the DDD aggregate root entity.
 *
 * @author bingo 下午4:23:02
 * @since
 */
public interface Aggregate extends Entity {

  /**
   * Extract the buffered messages that have been raised by the aggregate, this method was invoked
   * before unit of work completed.
   *
   * @param flush if true then clear messages buffer
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
   * recycle then return true else return false. It means that the value of aggregate life cycle is
   * in one of {@link Lifecycle#INITIAL}, {@link Lifecycle#PRE_PERSIST},
   * {@link Lifecycle#POST_REMOVED}, {@link Lifecycle#DESTROYED}.
   */
  @Transient
  @javax.persistence.Transient
  default boolean isPhantom() {
    return !getLifecycle().signPreserved();
  }

  /**
   * Indicates whether the aggregate is persisted or not, the return value just the opposite of
   * {@link #isPhantom()}.
   */
  @Transient
  @javax.persistence.Transient
  default boolean isPreserved() {
    return getLifecycle().signPreserved();
  }

  /**
   * Raise message, add the message to the buffer and do not publish it immediately.
   *
   * <p>
   * Note: This method may be implemented in other ways in the future.
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
   * Generally the message is the DDD domain event.
   *
   * @param anyway whether to send as long as the unit of works is completed anyway, or send only
   *        when the aggregation state changes and unit of works is completed.
   * @param messages the messages to be sent.
   *
   * @see UnitOfWork
   */
  void raise(boolean anyway, Message... messages);

  /**
   * Raise event, use the CDI event mechanism to emit event.
   * <p>
   * Generally the event is not the DDD domain event. It is simply event-driven architecture for
   * decoupling.
   * </p>
   *
   * @param event the events to be sent
   * @param qualifiers the CDI event listener qualifiers
   */
  void raise(Event event, Annotation... qualifiers);

  /**
   * Raise asynchronous events, {@link #raise(Event, Annotation...)}
   *
   * @param event the events to be sent
   * @param qualifiers the CDI event listener qualifiers
   *
   */
  void raiseAsync(Event event, Annotation... qualifiers);

  /**
   * corant-modules-ddd-api
   *
   * @author bingo 下午9:04:52
   *
   */
  interface AggregateIdentifier extends EntityIdentifier {

    @Override
    Serializable getId();

    @Override
    String getType();

    default Class<? extends Aggregate> getTypeCls() {
      return forceCast(tryAsClass(getType()));
    }
  }

  /**
   *
   * corant-modules-ddd-api
   *
   * @author bingo 下午9:05:24
   *
   */
  enum Lifecycle {
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
     * @see javax.persistence.PrePersist
     */
    PRE_PERSIST(8),
    /**
     * Aggregate will be update to storage
     *
     * @see javax.persistence.PreUpdate
     */
    PRE_UPDATE(16),
    /**
     * Aggregate will be remove from storage
     *
     * @see javax.persistence.PreRemove
     */
    PRE_REMOVE(32),
    /**
     * Aggregate has been persisted to storage
     *
     * @see javax.persistence.PostPersist
     */
    POST_PERSISTED(64),
    /**
     * Aggregate has been update to storage
     *
     * @see javax.persistence.PostUpdate
     */
    POST_UPDATED(128),
    /**
     * Aggregate has been removed from storage
     *
     * @see javax.persistence.PostRemove
     */
    POST_REMOVED(256),
    /**
     * If aggregate has already been persisted, the representation is removed from the persistence
     * facility; otherwise it is just a token.
     */
    DESTROYED(512);

    int sign;

    Lifecycle(int sign) {
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
      return (sign & 246) != 0;
    }

    public boolean signRefreshable() {
      return (sign & 30) != 0 || (sign & 192) != 0;
    }
  }
}

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

/**
 * @author bingo 下午4:23:02
 * @since
 */
public interface Aggregation extends Entity {

  /**
   * If flush is true then the integration event queue will be clear
   */
  List<Message> extractMessages(boolean flush);

  /**
   * the lifeCycle of being
   */
  @Transient
  @javax.persistence.Transient
  Lifecycle getLifecycle();

  /**
   * the evolution version number, use as persistence version
   */
  Long getVn();

  /**
   * In this case, it means whether it is persisted or not
   */
  @Transient
  @javax.persistence.Transient
  default boolean isEnabled() {
    return getLifecycle() != null && getLifecycle().signEnabled();
  }

  /**
   * The aggregation isn't persisted, or is destroyed, but still live in memory until the GC
   * recycle.
   */
  @Transient
  @javax.persistence.Transient
  default Boolean isPhantom() {
    return getId() == null || !isEnabled();
  }

  /**
   * Raise events.
   */
  void raise(Event event, Annotation... qualifiers);

  /**
   * Raise messages
   */
  void raise(Message... messages);

  /**
   * Raise asynchronous events
   */
  void raiseAsync(Event event, Annotation... qualifiers);

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午9:04:45
   *
   */
  public static abstract class AggregationHandlerAdapter<P, T extends Aggregation>
      extends EnablingHandlerAdapter<P, T> implements DisablingHandler<P, T> {

    @Override
    public void preDisable(P param, T destroyable) {}

  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午9:04:52
   *
   */
  interface AggregationIdentifier extends EntityIdentifier {

    @Override
    Serializable getId();

    @Override
    String getType();

    default Class<?> getTypeCls() {
      return tryAsClass(getType());
    }
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午9:04:55
   *
   */
  interface AggregationReference<T extends Aggregation> extends EntityReference<T> {

    Long getVn();

  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午9:05:13
   *
   */
  @FunctionalInterface
  interface DisablingHandler<P, T> {
    @SuppressWarnings("rawtypes")
    DisablingHandler EMPTY_INST = (p, t) -> {
    };

    void preDisable(P param, T enabling);
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午9:05:19
   *
   */
  public static abstract class DisablingHandlerAdapter<P, T> implements DisablingHandler<P, T> {
    @Override
    public void preDisable(P param, T enabling) {}
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午9:05:07
   *
   */
  interface Enabling<P, T> {

    void disable(P Param, DisablingHandler<P, T> handler);

    T enable(P param, EnablingHandler<P, T> handler);
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午9:05:13
   *
   */
  @FunctionalInterface
  interface EnablingHandler<P, T> {
    @SuppressWarnings("rawtypes")
    EnablingHandler EMPTY_INST = (p, t) -> {
    };

    void preEnable(P param, T enabling);
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午9:05:19
   *
   */
  public static abstract class EnablingHandlerAdapter<P, T> implements EnablingHandler<P, T> {
    @Override
    public void preEnable(P param, T enabling) {}
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
     * Aggregation has just been created.
     */
    INITIAL(0),
    /**
     * Aggregation has been joined persistence context.
     */
    ENABLED(2),
    /**
     * Aggregation retrieve from storage and has been joined persistence context.
     */
    LOADED(4),
    /**
     * Aggregation will be persist to storage
     *
     * @see PrePersist
     */
    PRE_PERSIST(8),
    /**
     * Aggregation will be update to storage
     *
     * @see PreUpdate
     */
    PRE_UPDATE(16),
    /**
     * Aggregation will be remove from storage
     *
     * @see PreRemove
     */
    PRE_REMOVE(32),
    /**
     * Aggregation has been persisted to storage
     *
     * @see PostPersist
     */
    POST_PERSISTED(64),
    /**
     * Aggregation has been update to storage
     *
     * @see PostUpdate
     */
    POST_UPDATED(128),
    /**
     * Aggregation has been removed from storage
     *
     * @see PostRemove
     */
    POST_REMOVED(256),
    /**
     * If aggregation has already been persisted, the representation is removed from the persistence
     * facility; otherwise it is just a token.
     */
    DISABLED(512);

    int sign;

    private Lifecycle(int sign) {
      this.sign = sign;
    }

    public int getSign() {
      return sign;
    }

    /**
     * The aggregation remove from persistence context if has been persisted before, otherwise the
     * aggregation has been created and marked disable means it can't not participate in any domain
     * logic.
     *
     * @return signDisabled
     */
    public boolean signDisabled() {
      return (sign & 768) != 0;
    }

    /**
     * The aggregation has been joined persistence context and has identity.
     *
     * @return signEnabled
     */
    public boolean signEnabled() {
      return (sign & 254) != 0;
    }

    /**
     * The aggregation status have been synchronized to underling storage.
     *
     * @return signFlushed
     */
    public boolean signFlushed() {
      return (sign & 448) != 0;
    }

    public boolean signRefreshable() {
      return (sign & 30) != 0 || (sign & 192) != 0;
    }
  }

}

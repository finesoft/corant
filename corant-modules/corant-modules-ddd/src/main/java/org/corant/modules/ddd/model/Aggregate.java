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
package org.corant.modules.ddd.model;

import static org.corant.modules.bundle.GlobalMessageCodes.ERR_OBJ_NON_FUD;
import static org.corant.modules.bundle.GlobalMessageCodes.ERR_PARAM;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.forceCast;
import java.beans.Transient;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import org.corant.modules.bundle.exception.GeneralRuntimeException;
import org.corant.modules.ddd.event.Event;
import org.corant.modules.ddd.message.Message;
import org.corant.modules.ddd.unitwork.JTARLJPAUnitOfWorksManager;
import org.corant.modules.ddd.unitwork.JTAXAJPAUnitOfWorksManager;
import org.corant.modules.ddd.unitwork.UnitOfWork;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-ddd
 *
 * <p>
 * Aggregate a cluster of associated objects that are treated as a unit for the purpose of data
 * changes. External references are restricted to one member of the AGGREGATE, designated as the
 * root. A set of consistency rules applies within the AGGREGATE’S boundaries.
 * <p>
 * Aggregates are the basic element of transfer of data storage - you request to load or save whole
 * aggregates. If possable, the transactions({@link UnitOfWork}) should not cross aggregate
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
   * Generally the event is not the DDD domain event. It is simply event-driven architecture for
   * decoupling.
   * </p>
   *
   * @param event
   * @param qualifiers raise
   */
  void raise(Event event, Annotation... qualifiers);

  /**
   * Raise message, add the message to the buffer and do not publish it immediately.
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
   * Generally the message is the DDD domain event.
   *
   * @param messages
   *
   * @see UnitOfWork
   * @see JTAXAJPAUnitOfWorksManager
   * @see JTARLJPAUnitOfWorksManager
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
   * corant-modules-ddd
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
   * corant-modules-ddd
   *
   * @author bingo 下午5:53:48
   *
   */
  interface AggregateReference<T extends Aggregate> extends EntityReference<T> {

    Map<Pair<Class<?>, Class<?>>, Constructor<?>> constructors = new ConcurrentHashMap<>();

    static <X> X invokeExactConstructor(Class<X> cls, Class<?> paramClasses, Object paramValue)
        throws InstantiationException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException {
      return forceCast(constructors.computeIfAbsent(Pair.of(cls, paramClasses), cp -> {
        Constructor<?> candidate = null;
        for (Constructor<?> ct : cp.getKey().getDeclaredConstructors()) {
          if (ct.getParameterCount() == 1 && ct.getParameterTypes().length == 1
              && ct.getParameterTypes()[0].isAssignableFrom(paramClasses)) {
            candidate = ct;
            break;
          }
        }
        return shouldNotNull(candidate);
      }).newInstance(paramValue));
    }

    static <A extends Aggregate, T extends AggregateReference<A>> T of(Class<T> cls, Object param) {
      if (param == null) {
        return null;
      }
      try {
        if (cls != null) {
          if (cls.isAssignableFrom(param.getClass())) {
            return forceCast(param);
          } else {
            return invokeExactConstructor(cls, param.getClass(), param);
          }
        }
      } catch (Exception e) {
        throw new GeneralRuntimeException(e, ERR_OBJ_NON_FUD,
            asString(cls).concat(":").concat(asString(param)));
      }
      throw new GeneralRuntimeException(ERR_PARAM);
    }

    static <X extends Aggregate> X resolve(Class<X> cls, Serializable id) {
      return Aggregates.tryResolve(cls, id)
          .orElseThrow(() -> new GeneralRuntimeException(ERR_OBJ_NON_FUD));
    }

    @Override
    default T retrieve() {
      return tryRetrieve().orElseThrow(() -> new GeneralRuntimeException(ERR_PARAM));
    }

    @SuppressWarnings("unchecked")
    @Override
    default Optional<T> tryRetrieve() {
      Class<T> resolvedClass = null;
      Class<?> referenceClass = getClass();
      do {
        if (referenceClass.getGenericSuperclass() instanceof ParameterizedType) {
          resolvedClass = (Class<T>) ((ParameterizedType) referenceClass.getGenericSuperclass())
              .getActualTypeArguments()[0];
          break;
        } else {
          Type[] genericInterfaces = referenceClass.getGenericInterfaces();
          for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
              ParameterizedType parameterizedType = (ParameterizedType) type;
              if (AggregateReference.class
                  .isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
                resolvedClass = (Class<T>) parameterizedType.getActualTypeArguments()[0];
                break;
              }
            }
          }
        }
      } while (resolvedClass == null && (referenceClass = referenceClass.getSuperclass()) != null);
      return Aggregates.tryResolve(resolvedClass, getId());
    }
  }

  /**
   *
   * corant-modules-ddd
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
      return (sign & 254) != 0;
    }

    public boolean signRefreshable() {
      return (sign & 30) != 0 || (sign & 192) != 0;
    }
  }
}

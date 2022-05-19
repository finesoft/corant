/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.modules.bundle.GlobalMessageCodes.ERR_OBJ_NON_FUD;
import static org.corant.modules.bundle.GlobalMessageCodes.ERR_PARAM;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.forceCast;
import java.beans.Transient;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.LockModeType;
import org.corant.modules.ddd.Aggregate;
import org.corant.shared.exception.GeneralRuntimeException;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 *
 * corant-modules-ddd-shared
 *
 * @author bingo 下午5:53:48
 *
 */
public interface AggregateReference<T extends Aggregate> extends EntityReference<T> {

  Map<Pair<Class<?>, Class<?>>, Constructor<?>> constructors = new ConcurrentHashMap<>();
  Map<Class<?>, Class<?>> classes = new ConcurrentHashMap<>();

  static <X extends Aggregate> boolean exists(Class<X> cls, Serializable id) {
    return Aggregates.exists(cls, id);
  }

  static <X> X invokeExactConstructor(Class<X> cls, Class<?> paramClasses, Object paramValue)
      throws InstantiationException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException {
    return forceCast(constructors.computeIfAbsent(Pair.of(cls, paramClasses), cp -> {
      Constructor<?> candidate = null;
      for (Constructor<?> ct : cp.getKey().getDeclaredConstructors()) {
        if (ct.getParameterCount() == 1
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
        .orElseThrow(() -> new GeneralRuntimeException(ERR_OBJ_NON_FUD, id));
  }

  @SuppressWarnings("unchecked")
  static <A extends Aggregate, R extends AggregateReference<A>> Class<A> resolveType(
      Class<R> refCls) {
    return (Class<A>) classes.computeIfAbsent(refCls, rc -> {
      Class<A> resolvedClass = null;
      Class<?> referenceClass = rc;
      do {
        if (referenceClass.getGenericSuperclass() instanceof ParameterizedType) {
          resolvedClass = (Class<A>) ((ParameterizedType) referenceClass.getGenericSuperclass())
              .getActualTypeArguments()[0];
          break;
        } else {
          Type[] genericInterfaces = referenceClass.getGenericInterfaces();
          for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
              ParameterizedType parameterizedType = (ParameterizedType) type;
              if (AggregateReference.class
                  .isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
                resolvedClass = (Class<A>) parameterizedType.getActualTypeArguments()[0];
                break;
              }
            }
          }
        }
      } while (resolvedClass == null && (referenceClass = referenceClass.getSuperclass()) != null);
      return resolvedClass;
    });
  }

  static <X extends Aggregate, I extends Serializable> I shouldExists(Class<X> cls, I id) {
    if (!exists(cls, id)) {
      throw new GeneralRuntimeException(ERR_OBJ_NON_FUD, id);
    }
    return id;
  }

  @Transient
  @javax.persistence.Transient
  default boolean exists() {
    return Aggregates.exists(resolveType(getClass()), getId());
  }

  default void lock(LockModeType lockModeType, Object... properties) {
    Aggregates.lock(retrieve(), lockModeType, properties);
  }

  @Override
  default T retrieve() {
    return tryRetrieve().orElseThrow(() -> new GeneralRuntimeException(ERR_OBJ_NON_FUD));
  }

  default T retrieve(LockModeType lockModeType, Object... properties) {
    return forceCast(
        Aggregates.resolve(resolveType(getClass()), getId(), lockModeType, properties));
  }

  @Override
  default Optional<T> tryRetrieve() {
    return Aggregates.tryResolve(resolveType(getClass()), getId());
  }
}

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
package org.corant.context;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.forceCast;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import org.corant.context.qualifier.Qualifiers;
import org.corant.context.qualifier.Unnamed;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Services;

/**
 * corant-context
 *
 * <p>
 * A convenient context bean object retrieval class for retrieving bean instances
 *
 * @author bingo 下午2:22:40
 *
 */
public class Beans {

  public static <T> T create(Class<T> clazz, Annotation... qualifiers) {
    if (clazz != null && CDIs.isEnabled()) {
      BeanManager bm = CDI.current().getBeanManager();
      Set<Bean<?>> beans = bm.getBeans(clazz, qualifiers);
      if (isNotEmpty(beans)) {
        if (beans.size() > 1) {
          beans = beans.stream().filter(b -> (b.getBeanClass().equals(clazz) || b.isAlternative()))
              .collect(Collectors.toSet());
        }
        if (isNotEmpty(beans)) {
          Bean<?> bean = bm.resolve(beans);
          if (bean != null) {
            CreationalContext<?> context = bm.createCreationalContext(bean);
            return context != null ? clazz.cast(bm.getReference(bean, clazz, context)) : null;
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns an {@link Optional} CDI bean instance that matches the given instance class and
   * qualifiers, ambiguous and unsatisfied return an empty {@link Optional}.
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T> the bean type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> Optional<T> find(Class<T> instanceClass, Annotation... qualifiers) {
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      return Optional.of(inst.get());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Returns an {@link Optional} CDI bean instance that matches the given instance type and
   * qualifiers, ambiguous and unsatisfied return an empty {@link Optional}.
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T> the bean type to be resolved
   * @param instanceType the bean instance type to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> Optional<T> find(TypeLiteral<T> instanceType, Annotation... qualifiers) {
    Instance<T> inst = select(instanceType, qualifiers);
    if (inst.isResolvable()) {
      return Optional.of(inst.get());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Returns an {@link Optional} bean instance from CDI or ServiceLoader that matches the given
   * instance class and qualifiers.
   *
   * Note: First lookup in CDI through the given instance class and qualifiers, if find a hit, it
   * will return immediately; If it is not found and the qualifiers is empty, it will be loaded from
   * {@link org.corant.shared.service.RequiredServiceLoader}. If there are multiple instances and
   * the given instance class is {@link Sortable}, then return the one with the highest priority.
   *
   * @param <T> the bean type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   *
   * @see #findService(Class)
   */
  public static <T> Optional<T> findAnyway(Class<T> instanceClass, Annotation... qualifiers) {
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      return Optional.of(inst.get());
    } else if (!inst.isUnsatisfied() && Sortable.class.isAssignableFrom(instanceClass)) {
      return forceCast(
          inst.stream().map(Sortable.class::cast).sorted(Sortable::compare).findFirst());
    } else {
      return isEmpty(qualifiers) ? findService(instanceClass) : Optional.empty();
    }
  }

  /**
   * Returns an {@link Optional} CDI bean instance that matches the given instance class and name,
   * ambiguous and unsatisfied return an empty {@link Optional}.
   *
   * <p>
   * <b>Note:</b> <br>
   * The Named Qualifier here extends the Named Qualifier mechanism of CDI. If the given name is not
   * null, use CDI's Named Qualifier for lookup first, if found, it will return immediately. If the
   * given name is empty, then directly do not add any qualifiers for lookup, if found, it will
   * return immediately, otherwise it will use {@link Unnamed} qualifier for lookup.
   *
   * @param <T> the bean type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param name the bean name for CDI bean lookup
   *
   * @see Unnamed
   */
  public static <T> Optional<T> findNamed(Class<T> instanceClass, String name) {
    Instance<T> inst = select(instanceClass, Any.Literal.INSTANCE);
    return findNamed(inst, name);
  }

  /**
   * Returns an {@link Optional} CDI bean instance that matches the given instance type and name,
   * ambiguous and unsatisfied return an empty {@link Optional}.
   *
   * <p>
   * <b>Note:</b> <br>
   * The Named Qualifier here extends the Named Qualifier mechanism of CDI. If the given name is not
   * null, use CDI's Named Qualifier for lookup first, if found, it will return immediately. If the
   * given name is empty, then directly do not add any qualifiers for lookup, if found, it will
   * return immediately, otherwise it will use {@link Unnamed} qualifier for lookup.
   *
   * @param <T> the bean type to be resolved
   * @param instanceType the bean instance type to be resolved
   * @param name the bean name for CDI bean lookup
   *
   * @see Unnamed
   */
  public static <T> Optional<T> findNamed(TypeLiteral<T> instanceType, String name) {
    Instance<T> inst = select(instanceType, Any.Literal.INSTANCE);
    return findNamed(inst, name);
  }

  /**
   * Returns an {@link Optional} instance that matches the given instance class from ServiceLoader
   * or return an empty {@link Optional} if not found.
   *
   * Note: If there are multiple service instances found by ServiceLoader and given instance class
   * is {@link Sortable} then return the highest priority instance.
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the instance class to be resolved
   *
   * @see Sortable#compare(Sortable, Sortable)
   */
  public static <T> Optional<T> findService(Class<T> instanceClass) {
    return Services.find(instanceClass);
  }

  public static boolean isManagedBean(Object object, Annotation... qualifiers) {
    return object != null && !select(getUserClass(object), qualifiers).isUnsatisfied();
  }

  /**
   * Make given object manageable, if given object is already managed in CDI then return it
   * directly.
   *
   * @param <T> the manageable bean type
   * @param obj the object that will be managed
   */
  public static <T> T manageable(T obj) {
    if (isManagedBean(obj)) {
      return obj;
    } else if (obj != null) {
      return UnmanageableBean.of(obj).produce().inject().postConstruct().get();
    }
    return null;
  }

  /**
   * Returns CDI bean instance or throws exception if can't resolve.
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> T resolve(Class<T> instanceClass, Annotation... qualifiers) {
    return select(instanceClass, qualifiers).get();
  }

  /**
   * Returns CDI bean instance or throws exception if can't resolve.
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T> the instance type to be resolved
   * @param instanceType the bean instance type to be resolved
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> T resolve(TypeLiteral<T> instanceType, Annotation... qualifiers) {
    return select(instanceType, qualifiers).get();
  }

  /**
   * Resolve CDI bean instance and consumer it or throws exception if can't resolve.
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the bean instance class to be resolved
   * @param consumer the consumer that consumer the bean instance
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> void resolveAccept(Class<T> instanceClass, Consumer<T> consumer,
      Annotation... qualifiers) {
    shouldNotNull(consumer).accept(resolve(instanceClass, qualifiers));
  }

  /**
   * Returns bean instance from CDI or Service Loader or throws exception if not found.
   *
   * <p>
   * Note: First lookup in CDI through the given instance class and qualifiers, if find a hit, it
   * will return immediately; If it is not found and the qualifiers is empty, it will be loaded from
   * {@link org.corant.shared.service.RequiredServiceLoader}. If there are multiple instances and
   * the given instance class is {@link Sortable}, then return the one with the highest priority; If
   * none of the above searches are found, an exception is thrown.
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the instance class to be resolved
   * @param qualifiers the instance qualifiers that use to resolve
   *
   * @see #findAnyway(Class, Annotation...)
   */
  public static <T> T resolveAnyway(Class<T> instanceClass, Annotation... qualifiers) {
    return findAnyway(instanceClass, qualifiers).orElseThrow(
        () -> new CorantRuntimeException("Can not resolve bean class %s.", instanceClass));
  }

  /**
   * Resolve CDI bean instance and returns the result using the given function interface, if the
   * bean instance can't resolve then throw exception.
   *
   * @param <T> the bean instance type to be resolved
   * @param <R> the function return type
   * @param instanceClass the bean instance class to be resolved
   * @param function the function that bean instance will be applied
   * @param qualifiers the bean qualifiers to be resolved
   */
  public static <T, R> R resolveApply(Class<T> instanceClass, Function<T, R> function,
      Annotation... qualifiers) {
    return shouldNotNull(function).apply(resolve(instanceClass, qualifiers));
  }

  /**
   * Returns an instance that matches the given instance class and qualifiers or throws an exception
   * if CDI is disabled.
   *
   * @param <T> the instance type to be selected
   * @param instanceClass the instance class to be selected
   * @param qualifiers the qualifiers to be selected
   */
  public static <T> Instance<T> select(Class<T> instanceClass, Annotation... qualifiers) {
    if (!CDIs.isEnabled()) {
      throw new IllegalStateException("Unable to access CDI, the CDI container may be closed.");
    }
    return CDI.current().select(shouldNotNull(instanceClass), qualifiers);
  }

  /**
   * Returns an instance that matches the given instance type and qualifiers or throws an exception
   * if CDI is disabled.
   *
   * @param <T> the instance type to be selected
   * @param <U> the instance type to be selected
   * @param subtype the required type
   * @param qualifiers the qualifiers to be selected
   */
  public static <T, U extends T> Instance<U> select(TypeLiteral<U> subtype,
      Annotation... qualifiers) {
    if (!CDIs.isEnabled()) {
      throw new IllegalStateException("Unable to access CDI, the CDI container may be closed.");
    }
    return CDI.current().select(shouldNotNull(subtype), qualifiers);
  }

  /**
   * Returns a bean instance from CDI that matches the given instance class and qualifiers, if not
   * matches or CDI isn't enabled returns null.
   *
   * @param <T> the instance type to be resolved
   * @param instanceClass the instance class to be resolved
   * @param qualifiers the qualifiers to be resolved
   */
  public static <T> T tryResolve(Class<T> instanceClass, Annotation... qualifiers) {
    return CDIs.isEnabled() ? find(instanceClass, qualifiers).orElse(null) : null;
  }

  public static <T> void tryResolveAccept(Class<T> instanceClass, Consumer<T> consumer,
      Annotation... qualifiers) {
    T instance = tryResolve(instanceClass, qualifiers);
    if (instance != null) {
      shouldNotNull(consumer).accept(instance);
    }
  }

  public static <T, R> R tryResolveApply(Class<T> instanceClass, Function<T, R> function,
      Annotation... qualifiers) {
    T instance = tryResolve(instanceClass, qualifiers);
    if (instance != null) {
      return shouldNotNull(shouldNotNull(function)).apply(instance);
    }
    return null;
  }

  static <T> Optional<T> findNamed(Instance<T> instance, String name) {
    Instance<T> inst = instance;
    if (inst.isUnsatisfied()) {
      return Optional.empty();
    }
    // supports the normal Named qualifier
    if (name != null) {
      Instance<T> normal = inst.select(NamedLiteral.of(name));
      if (normal.isResolvable()) {
        return Optional.of(normal.get());
      }
    }
    // supports the extended Named qualifier
    String useName = Qualifiers.resolveName(name);
    if (Qualifiers.EMPTY_NAME.equals(useName) && inst.isResolvable()
        || (inst = inst.select(Unnamed.INST)).isResolvable()) {
      return Optional.of(inst.get());
    }
    return Optional.empty();
  }
}

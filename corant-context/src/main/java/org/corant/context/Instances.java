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

import static org.corant.context.qualifier.Qualifiers.resolveNamedQualifiers;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.defaultTrim;
import static org.corant.shared.util.Strings.isBlank;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-context
 *
 * @author bingo 下午2:22:40
 *
 */
public class Instances {

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
   * Find CDI bean instance by given instance class and qualifiers, only return the resolved
   * instance, ambiguous and unsatisfied return Optional.empty().
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T> the bean type
   * @param instanceClass the bean instance class
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
   * Find bean instance from CDI or ServiceLoader by given instance class and qualifiers,
   *
   * @param <T> the bean type
   * @param instanceClass the bean instance class
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> Optional<T> findAnyway(Class<T> instanceClass, Annotation... qualifiers) {
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      return Optional.of(inst.get());
    } else {
      return isEmpty(qualifiers) ? findService(instanceClass) : Optional.empty();
    }
  }

  /**
   * Find named CDI bean instance by given instance class and qualifiers, only return the resolved
   * instance, ambiguous and unsatisfied return Optional.empty().
   *
   * @param <T>
   * @param instanceClass
   * @param name
   */
  public static <T> Optional<T> findNamed(Class<T> instanceClass, String name) {
    Instance<T> inst = select(instanceClass, Any.Literal.INSTANCE);
    if (inst.isUnsatisfied()) {
      return Optional.empty();
    }
    String useName = defaultTrim(name);
    if (isBlank(useName) && inst.isResolvable()
        || (inst = inst.select(resolveNamedQualifiers(useName))).isResolvable()) {
      return Optional.of(inst.get());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Returns bean instance from Service Loader or throws exception if not found.
   *
   * Note: If there are multiple service instances found by ServiceLoader and given instance class
   * is {@link Sortable} then return the highest priority instance, otherwise throw exception.
   *
   * @param <T> the instance type
   * @param instanceClass the instance class to be resolved
   *
   * @see Sortable#compare(Sortable, Sortable)
   */
  public static <T> Optional<T> findService(Class<T> instanceClass) {
    List<T> list = listOf(ServiceLoader.load(instanceClass, defaultClassLoader()));
    if (isNotEmpty(list)) {
      if (list.size() == 1) {
        Optional.of(list.get(0));
      } else if (Sortable.class.isAssignableFrom(instanceClass)) {
        Optional.of(forceCast(list.stream().map(t -> (Sortable) t).sorted(Sortable::compare)
            .findFirst().orElse(null)));
      }
    }
    return Optional.empty();
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
      return UnmanageableInstance.of(obj).produce().inject().postConstruct().get();
    }
    return null;
  }

  /**
   * Returns CDI bean instance or throws exception if can't resolve.
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T>
   * @param instanceClass the bean instance class
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> T resolve(Class<T> instanceClass, Annotation... qualifiers) {
    return select(instanceClass, qualifiers).get();
  }

  /**
   * Resolve CDI bean instance and consumer it or throws exception if can't resolve.
   *
   * @param <T>
   * @param instanceClass the bean instance class
   * @param consumer the consumer that consumer the bean instance
   * @param qualifiers the bean qualifiers that use to resolve
   */
  public static <T> void resolveAccept(Class<T> instanceClass, Consumer<T> consumer,
      Annotation... qualifiers) {
    Consumer<T> useConsumer = shouldNotNull(consumer);
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      useConsumer.accept(inst.get());
    } else {
      throw new CorantRuntimeException("Can not resolve bean class %s.", instanceClass);
    }
  }

  /**
   * Returns bean instance from CDI or Service Loader or throws exception if not found.
   *
   * <p>
   * <ul>
   * Resolve steps:
   * <li>First, we try to resolve the bean instance from the CDI environment, and return the
   * instance immediately if it can be resolved</li>
   * <li>Second, if given qualifiers is empty then try to look it up from the Service Loader, if
   * there are multiple instances found by Service Loader and the given instance class is
   * {@link Sortable} then return the highest priority instance.</li>
   * <li>throw an exception if ambiguous appears in CDI or can't load it from Service Loader.</li>
   * </ul>
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T>
   * @param instanceClass
   * @param qualifiers
   *
   * @see #select(Class, Annotation...)
   * @see #findService(Class)
   */
  public static <T> T resolveAnyway(Class<T> instanceClass, Annotation... qualifiers) {
    Instance<T> inst = select(instanceClass, qualifiers);
    if (!inst.isUnsatisfied()) {
      return inst.get();
    } else {
      T t = isEmpty(qualifiers) ? findService(instanceClass).orElse(null) : null;
      return shouldNotNull(t, "Can not resolve bean class %s.", instanceClass);
    }
  }

  /**
   * Resolve CDI bean instance and returns the result using the given function interface, if the
   * bean instance can't resolve then throw exception.
   *
   * @param <T>
   * @param <R>
   * @param instanceClass
   * @param function
   * @param qualifiers
   * @return resolveApply
   */
  public static <T, R> R resolveApply(Class<T> instanceClass, Function<T, R> function,
      Annotation... qualifiers) {
    Function<T, R> useFunction = shouldNotNull(function);
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      return useFunction.apply(inst.get());
    } else {
      throw new CorantRuntimeException("Can not resolve bean class %s.", instanceClass);
    }
  }

  public static <T> Instance<T> select(Class<T> instanceClass, Annotation... qualifiers) {
    if (!CDIs.isEnabled()) {
      throw new IllegalStateException("Unable to access CDI, the CDI container may be closed.");
    }
    return CDI.current().select(shouldNotNull(instanceClass), qualifiers);
  }

  public static <T> T tryResolve(Class<T> instanceClass, Annotation... qualifiers) {
    return CDIs.isEnabled() ? find(instanceClass, qualifiers).orElse(null) : null;
  }

  public static <T> T tryResolve(Instance<T> instance) {
    return instance != null && instance.isResolvable() ? instance.get() : null;
  }

  public static <T> void tryResolveAccept(Class<T> instanceClass, Consumer<T> consumer,
      Annotation... qualifiers) {
    Consumer<T> useConsumer = shouldNotNull(consumer);
    if (CDIs.isEnabled()) {
      Instance<T> inst = select(instanceClass, qualifiers);
      if (inst.isResolvable()) {
        useConsumer.accept(inst.get());
      }
    }
  }

  public static <T, R> R tryResolveApply(Class<T> instanceClass, Function<T, R> function,
      Annotation... qualifiers) {
    Function<T, R> useFunction = shouldNotNull(function);
    if (CDIs.isEnabled()) {
      Instance<T> inst = select(instanceClass, qualifiers);
      if (inst.isResolvable()) {
        return useFunction.apply(inst.get());
      }
    }
    return null;
  }
}

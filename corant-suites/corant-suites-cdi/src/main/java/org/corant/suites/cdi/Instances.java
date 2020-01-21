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
package org.corant.suites.cdi;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.defaultClassLoader;
import static org.corant.shared.util.ClassUtils.getUserClass;
import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.suites.cdi.Qualifiers.resolveNameds;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-cdi
 *
 * @author bingo 下午2:22:40
 *
 */
public class Instances {

  /**
   * Find CDI bean instance
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T>
   * @param instanceClass
   * @param qualifiers
   * @return find
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
   * Find CDI named bean instance
   *
   * @param <T>
   * @param instanceClass
   * @param name
   * @return findNamed
   */
  public static <T> Optional<T> findNamed(Class<T> instanceClass, String name) {
    Instance<T> inst = select(instanceClass);
    if (inst.isUnsatisfied()) {
      return Optional.empty();
    }
    String useName = defaultTrim(name);
    if (isBlank(useName) && inst.isResolvable()) {
      return Optional.of(inst.get());
    } else if ((inst = inst.select(resolveNameds(useName))).isResolvable()) {
      return Optional.of(inst.get());
    } else {
      return Optional.empty();
    }
  }

  public static boolean isManagedBean(Object object, Annotation... qualifiers) {
    return object != null && !select(getUserClass(object), qualifiers).isUnsatisfied();
  }

  /**
   * Resolve CDI bean instance
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T>
   * @param instanceClass
   * @param qualifiers
   * @return resolve
   */
  public static <T> T resolve(Class<T> instanceClass, Annotation... qualifiers) {
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      return inst.get();
    }
    throw new CorantRuntimeException("Can not resolve bean class %s", instanceClass);
  }

  /**
   * Resolve CDI bean instance and consumer it.
   *
   * @param <T>
   * @param instanceClass
   * @param consumer
   * @param qualifiers resolveAccept
   */
  public static <T> void resolveAccept(Class<T> instanceClass, Consumer<T> consumer,
      Annotation... qualifiers) {
    Consumer<T> useConsumer = shouldNotNull(consumer);
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      useConsumer.accept(inst.get());
    } else {
      throw new CorantRuntimeException("Can not resolve bean class %s", instanceClass);
    }
  }

  /**
   * Resolve bean instance from CDI or Service Loader
   *
   * First, we try to resolve the bean instance from the CDI environment, and return the resolved
   * instance immediately if it can be resolved; otherwise, try to look it up from the Service
   * Loader, and resolve it with UnmanageableInstance if it is not found in the Service Loader;
   * throw an exception if ambiguous appears in CDI.
   *
   * Use with care, there may be a memory leak.
   *
   * @param <T>
   * @param instanceClass
   * @param qualifiers
   * @return resolveAnyway
   */
  public static <T> T resolveAnyway(Class<T> instanceClass, Annotation... qualifiers) {
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      return inst.get();
    } else if (inst.isUnsatisfied()) {
      List<T> list = listOf(ServiceLoader.load(instanceClass, defaultClassLoader()));
      if (list.size() == 1) {
        return list.get(0);
      } else {
        return UnmanageableInstance.of(instanceClass).produce().inject().postConstruct().get();
      }
    } else {
      throw new CorantRuntimeException("Can not resolve bean class %s", instanceClass);
    }
  }

  public static <T> T resolveAnyway(T obj, Annotation... qualifiers) {
    if (isManagedBean(obj, qualifiers)) {
      return obj;
    } else if (obj != null) {
      return UnmanageableInstance.of(obj).produce().inject().postConstruct().get();
    }
    return null;
  }

  /**
   * Resolve CDI bean instance and returns the result using the function interface.
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
      throw new CorantRuntimeException("Can not resolve bean class %s", instanceClass);
    }
  }

  public static <T> Instance<T> select(Class<T> instanceClass, Annotation... qualifiers) {
    if (!CDIs.isEnabled()) {
      throw new IllegalStateException("Unable to access CDI, the CDI container may be closed.");
    }
    return CDI.current().select(shouldNotNull(instanceClass), qualifiers);
  }

  public static <T> T tryResolve(Class<T> instanceClass, Annotation... qualifiers) {
    return find(instanceClass, qualifiers).orElse(null);
  }

  public static <T> T tryResolve(Instance<T> instance) {
    return instance != null && instance.isResolvable() ? instance.get() : null;
  }

  public static <T> void tryResolveAccept(Class<T> instanceClass, Consumer<T> consumer,
      Annotation... qualifiers) {
    Consumer<T> useConsumer = shouldNotNull(consumer);
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      useConsumer.accept(inst.get());
    }
  }

  public static <T, R> R tryResolveApply(Class<T> instanceClass, Function<T, R> function,
      Annotation... qualifiers) {
    Function<T, R> useFunction = shouldNotNull(function);
    Instance<T> inst = select(instanceClass, qualifiers);
    if (inst.isResolvable()) {
      return useFunction.apply(inst.get());
    }
    return null;
  }
}

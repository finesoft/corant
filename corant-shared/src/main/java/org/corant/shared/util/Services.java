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
package org.corant.shared.util;

import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Functions.emptyPredicate;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.service.Required;
import org.corant.shared.service.RequiredClassNotPresent;
import org.corant.shared.service.RequiredClassPresent;
import org.corant.shared.service.RequiredConfiguration;
import org.corant.shared.service.SimpleRequired;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-shared
 *
 * <p>
 * A helper class of {@link ServiceLoader} that supports conditionally returning Service instances,
 * and supports returning the appropriate one from multiple Service instances under specific
 * conditions.
 *
 * <p>
 * The conditions include class name present or not, the value of system property / system
 * environment variable.
 *
 * <p>
 * If the service class to be loaded is {@link Sortable} or {@link Comparable}, it is processed
 * according to the respective sorting of {@link Sortable} or {@link Comparable}. If the service
 * class is both {@link Sortable} and {@link Comparable}, the sorting process will take precedence
 * over the {@link Sortable} implementation.
 *
 * @see RequiredClassPresent
 * @see RequiredClassNotPresent
 * @see RequiredConfiguration
 * @see Sortable#compare(Sortable, Sortable)
 * @see Comparable#compareTo(Object)
 *
 * @author bingo 下午10:48:27
 *
 */
public class Services {

  final static Required required =
      findPreferentially(Required.class).orElseGet(SimpleRequired::new);

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * from {@link ServiceLoader} or return an empty {@link Optional} if not found, throw exception if
   * found multiple instances.
   *
   * @param <S> the service type to be found
   * @param serviceClass the interface or abstract class representing the service
   * @return an appropriate service instance
   *
   * @see ServiceLoader#load(Class)
   * @see #findPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Optional<S> findPreferentially(Class<S> serviceClass) {
    return findPreferentially(ServiceLoader.load(serviceClass), serviceClass, emptyPredicate(true));
  }

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * from {@link ServiceLoader} which use the given class loader, or return an empty
   * {@link Optional} if not found, throw exception if found multiple instances.
   *
   * @param <S> the service type to be found
   * @param serviceClass the interface or abstract class representing the service
   * @param classLoader the class loader use in ServiceLoader
   * @return an appropriate service instance
   *
   * @see ServiceLoader#load(Class, ClassLoader)
   * @see #findPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Optional<S> findPreferentially(Class<S> serviceClass, ClassLoader classLoader) {
    return findPreferentially(ServiceLoader.load(serviceClass, classLoader), serviceClass,
        emptyPredicate(true));
  }

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * and the given module layer from {@link ServiceLoader} or return an empty {@link Optional} if
   * not found, throw exception if found multiple instances.
   *
   * @param <S> the service type to be resolved
   * @param layer the module layer
   * @param serviceClass the interface or abstract class representing the service
   *
   * @see ServiceLoader#load(ModuleLayer, Class)
   * @see #findPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Optional<S> findPreferentially(ModuleLayer layer, Class<S> serviceClass) {
    return findPreferentially(ServiceLoader.load(layer, serviceClass), serviceClass,
        emptyPredicate(true));
  }

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * and service loader and the given predicate testing or return an empty {@link Optional} if not
   * found, throw exception if found multiple instances.
   *
   * <p>
   * Note: If there are multiple service instances found by the given service loader and given
   * service class is {@link Sortable} or {@link Comparable} then return the highest priority
   * service instance, for some services that are implemented {@link Sortable} and
   * {@link Comparable}, the {@link Sortable} will be used for sorting preferred. if the given
   * service class is not {@link Sortable} and {@link Comparable} and there are multiple instances
   * found, then throw ServiceConfigurationError.
   *
   * @param <S> the service type to be found
   * @param serviceLoader the service loader
   * @param serviceClass the interface or abstract class representing the service
   * @param predicate use for filtering the service instance.
   * @return an appropriate service instance
   *
   * @see Sortable#compare(Sortable, Sortable)
   * @see Comparable#compareTo(Object)
   * @see #selectPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Optional<S> findPreferentially(ServiceLoader<S> serviceLoader,
      Class<S> serviceClass, Predicate<Provider<S>> predicate) {
    if (Sortable.class.isAssignableFrom(serviceClass)) {
      return selectPreferentially(serviceLoader, serviceClass, predicate).findFirst();
    } else if (Comparable.class.isAssignableFrom(serviceClass)) {
      return selectPreferentially(serviceLoader, serviceClass, predicate).findFirst();
    } else {
      Iterator<S> it = selectPreferentially(serviceLoader, serviceClass, predicate).iterator();
      if (it.hasNext()) {
        S p = it.next();
        if (!it.hasNext()) {
          return Optional.ofNullable(p);
        } else {
          throw new ServiceConfigurationError(
              "Ambiguous service instances for " + serviceClass.getCanonicalName());
        }
      }
      return Optional.empty();
    }
  }

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * from {@link ServiceLoader} or return an empty {@link Optional} if not found or not meet the
   * conditions, throw exception if found multiple instances.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class.
   *
   * @param <S> the service type to be resolved
   * @param serviceClass the interface or abstract class representing the service
   * @return a service instance
   *
   * @see ServiceLoader#load(Class)
   * @see Required#shouldVeto(Class)
   * @see #findPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Optional<S> findRequired(Class<S> serviceClass) {
    return findPreferentially(ServiceLoader.load(serviceClass), serviceClass, Services::required);
  }

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * and the given class loader from {@link ServiceLoader} or return an empty {@link Optional} if
   * not found or not meet the conditions, throw exception if found multiple instances.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class.
   *
   * @param <S> the service type to be resolved
   * @param serviceClass the interface or abstract class representing the service
   * @param classLoader the class loader to be used to load provider-configuration files and
   *        provider classes, or null if the system class loader (or, failing that, the bootstrap
   *        class loader) is to be used
   *
   * @see ServiceLoader#load(Class, ClassLoader)
   * @see Required#shouldVeto(Class)
   * @see #findPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Optional<S> findRequired(Class<S> serviceClass, ClassLoader classLoader) {
    return findPreferentially(ServiceLoader.load(serviceClass, classLoader), serviceClass,
        Services::required);
  }

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * and the given module layer from {@link ServiceLoader} or return an empty {@link Optional} if
   * not found or not meet the conditions, throw exception if found multiple instances.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class.
   *
   * @param <S> the service type to be resolved
   * @param layer the module layer
   * @param serviceClass the interface or abstract class representing the service
   *
   * @see ServiceLoader#load(ModuleLayer, Class)
   * @see Required#shouldVeto(Class)
   * @see #findPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Optional<S> findRequired(ModuleLayer layer, Class<S> serviceClass) {
    return findPreferentially(ServiceLoader.load(layer, serviceClass), serviceClass,
        Services::required);
  }

  public static Required getRequired() {
    return required;
  }

  /**
   * Returns an appropriate service instance that matches the given service class from
   * {@link ServiceLoader} or throw exception if not found or not meet the conditions or found
   * multiple instances.
   *
   * @param <S> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   *
   * @see #findRequired(Class)
   */
  public static <S> S resolve(Class<S> serviceClass) {
    return findRequired(serviceClass).orElseThrow(() -> new CorantRuntimeException(
        "Unable to resolve the service %s through the class loader %s.", serviceClass,
        defaultClassLoader()));
  }

  /**
   * Returns an appropriate service instance that matches the given service class and the given
   * class loader from {@link ServiceLoader} or throw exception if not found or not meet the
   * conditions or found multiple instances.
   *
   * @param <S> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   * @param classLoader The class loader to be used to load provider-configuration files and
   *        provider classes, or null if the system class loader (or, failing that, the bootstrap
   *        class loader) is to be used
   *
   * @see #findRequired(Class, ClassLoader)
   */
  public static <S> S resolve(Class<S> serviceClass, ClassLoader classLoader) {
    return findRequired(serviceClass, classLoader).orElseThrow(() -> new CorantRuntimeException(
        "Unable to resolve the service %s through the class loader %s.", serviceClass,
        classLoader));
  }

  /**
   * Returns an appropriate service instance that matches the given service class and the given
   * module layer from {@link ServiceLoader} or throw exception if not found or not meet the
   * conditions or found multiple instances.
   *
   * @param <S> the service type to be resolved
   * @param layer the module layer
   * @param serviceClass the service instance class to be resolved
   *
   * @see #findRequired(ModuleLayer, Class)
   */
  public static <S> S resolve(ModuleLayer layer, Class<S> serviceClass) {
    return findRequired(layer, serviceClass).orElseThrow(() -> new CorantRuntimeException(
        "Unable to resolve the service %s through the module layer %s.", serviceClass, layer));
  }

  /**
   * Returns the stream of service instances that use the given service loader to load the given
   * service class. If the service class is {@link Sortable} or {@link Comparable}, the returned
   * stream is sorted. If the service class is both {@link Sortable} and {@link Comparable}, the
   * sorting process will prioritize the implementation of {@link Sortable}. If the given filter is
   * not empty, the returned stream is filtered.
   *
   * @param <S> the service type to be resolved
   * @param loader the service loader
   * @param serviceClass the service class
   * @param predicate the predicate for filtering
   * @return a stream of service instances
   *
   * @see Sortable#compare(Sortable, Sortable)
   * @see Comparable#compareTo(Object)
   */
  @SuppressWarnings("unchecked")
  public static <S> Stream<S> selectPreferentially(ServiceLoader<S> loader, Class<S> serviceClass,
      Predicate<Provider<S>> predicate) {
    final Predicate<Provider<S>> required = defaultObject(predicate, emptyPredicate(true));
    if (Sortable.class.isAssignableFrom(serviceClass)) {
      return (Stream<S>) loader.stream().filter(required).map(Provider::get).map(p -> (Sortable) p)
          .sorted(Sortable::compare);
    } else if (Comparable.class.isAssignableFrom(serviceClass)) {
      return (Stream<S>) loader.stream().filter(required).map(Provider::get)
          .map(p -> (Comparable<?>) p).sorted();
    } else {
      return loader.stream().filter(required).map(Provider::get);
    }
  }

  /**
   * Returns a sorted service instance stream that matches the given service class and meet the
   * required conditions(if placed) from {@link ServiceLoader}.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class.
   *
   * @param <S> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   *
   * @see ServiceLoader#load(Class)
   * @see Required#shouldVeto(Class)
   * @see #selectPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Stream<S> selectRequired(Class<S> serviceClass) {
    return selectPreferentially(ServiceLoader.load(serviceClass), serviceClass, Services::required);
  }

  /**
   * Returns a sorted service instance stream that matches the given service class and meet the
   * required conditions(if placed) from {@link ServiceLoader} which use the given class loader.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class.
   *
   * @param <S> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   * @param classLoader The class loader to be used to load provider-configuration files and
   *        provider classes, or null if the system class loader (or, failing that, the bootstrap
   *        class loader) is to be used
   *
   * @see ServiceLoader#load(Class, ClassLoader)
   * @see Required#shouldVeto(Class)
   * @see #selectPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Stream<S> selectRequired(Class<S> serviceClass, ClassLoader classLoader) {
    return selectPreferentially(ServiceLoader.load(serviceClass, classLoader), serviceClass,
        Services::required);
  }

  /**
   * Returns an appropriate service instances stream that matches the given service class and the
   * given module layer from {@link ServiceLoader} or return an empty stream if not found or not
   * meet the conditions.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class.
   *
   * @param <S> the service type to be resolved
   * @param layer the module layer
   * @param service the interface or abstract class representing the service
   *
   * @see ServiceLoader#load(ModuleLayer, Class)
   * @see Required#shouldVeto(Class)
   * @see #selectPreferentially(ServiceLoader, Class, Predicate)
   */
  public static <S> Stream<S> selectRequired(ModuleLayer layer, Class<S> service) {
    return selectPreferentially(ServiceLoader.load(layer, service), service, Services::required);
  }

  public static boolean shouldVeto(Class<?> type) {
    return required.shouldVeto(type);
  }

  public static boolean shouldVeto(Provider<?> provider) {
    return shouldVeto(provider.type());
  }

  static boolean required(Provider<?> provider) {
    return !shouldVeto(provider);
  }
}

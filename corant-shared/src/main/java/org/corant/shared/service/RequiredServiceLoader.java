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
package org.corant.shared.service;

import static java.security.AccessController.doPrivileged;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Classes;

/**
 * corant-shared
 *
 * <p>
 * A helper class of {@link ServiceLoader} that supports conditionally returning Service instances,
 * and supports returning the appropriate one from multiple Service instances under specific
 * conditions.
 * <p>
 * The conditions include class name present or not, the value of system property / system
 * environment variable.
 *
 * @see RequiredClassPresent
 * @see RequiredClassNotPresent
 * @see RequiredConfiguration
 *
 * @author bingo 下午8:38:41
 *
 */
public class RequiredServiceLoader {

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * and {@link Classes#defaultClassLoader()} from {@link ServiceLoader} or return an empty
   * {@link Optional} if not found or not meet the conditions.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class. If there are
   * multiple service instances found by the {@link ServiceLoader} and given service class is
   * {@link Sortable} or {@link Comparable} then return the highest priority service instance, for
   * some services that are implemented {@link Sortable} and {@link Comparable}, the
   * {@link Sortable} will be used for sorting preferred.
   *
   * @param <S> the service type to be resolved
   * @param service the interface or abstract class representing the service
   *
   * @see Sortable#compare(Sortable, Sortable)
   * @see ServiceLoader#load(Class)
   */
  public static <S> Optional<S> find(Class<S> service) {
    return find(
        doPrivileged((PrivilegedAction<ServiceLoader<S>>) () -> ServiceLoader.load(service)),
        service);
  }

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * and the given class loader from {@link ServiceLoader} or return an empty {@link Optional} if
   * not found or not meet the conditions.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class. If there are
   * multiple service instances found by the {@link ServiceLoader} and given service class is
   * {@link Sortable} or {@link Comparable} then return the highest priority service instance, for
   * some services that are implemented {@link Sortable} and {@link Comparable}, the
   * {@link Sortable} will be used for sorting preferred.
   *
   * @param <S> the service type to be resolved
   * @param service the interface or abstract class representing the service
   * @param loader the class loader to be used to load provider-configuration files and provider
   *        classes, or null if the system class loader (or, failing that, the bootstrap class
   *        loader) is to be used
   *
   * @see Sortable#compare(Sortable, Sortable)
   * @see ServiceLoader#load(Class, ClassLoader)
   */
  public static <S> Optional<S> find(Class<S> service, ClassLoader loader) {
    return find(
        doPrivileged(
            (PrivilegedAction<ServiceLoader<S>>) () -> ServiceLoader.load(service, loader)),
        service);
  }

  /**
   * Returns an appropriate {@link Optional} service instance that matches the given service class
   * and the given module layer from {@link ServiceLoader} or return an empty {@link Optional} if
   * not found or not meet the conditions.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class. If there are
   * multiple service instances found by the {@link ServiceLoader} and given service class is
   * {@link Sortable} or {@link Comparable} then return the highest priority service instance, for
   * some services that are implemented {@link Sortable} and {@link Comparable}, the
   * {@link Sortable} will be used for sorting preferred.
   *
   * @param <S> the service type to be resolved
   * @param layer the module layer
   * @param service the interface or abstract class representing the service
   *
   * @see Sortable#compare(Sortable, Sortable)
   * @see ServiceLoader#load(ModuleLayer, Class)
   */
  public static <S> Optional<S> find(ModuleLayer layer, Class<S> service) {
    return find(
        doPrivileged((PrivilegedAction<ServiceLoader<S>>) () -> ServiceLoader.load(layer, service)),
        service);
  }

  /**
   * Returns an appropriate service instances stream that matches the given service class and
   * {@link Classes#defaultClassLoader()} from {@link ServiceLoader} or return an empty stream if
   * not found or not meet the conditions.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class. If the given
   * service class is {@link Sortable} or {@link Comparable} then return a sorted service instance
   * stream, for some services that are implemented {@link Sortable} and {@link Comparable}, the
   * {@link Sortable} will be used for sorting preferred.
   *
   * @param <S> the service type to be resolved
   * @param service the interface or abstract class representing the service
   *
   * @see Sortable#compare(Sortable, Sortable)
   * @see ServiceLoader#load(Class)
   * @see ServiceLoader#stream()
   */
  public static <S> Stream<S> load(Class<S> service) {
    return load(
        doPrivileged((PrivilegedAction<ServiceLoader<S>>) () -> ServiceLoader.load(service)),
        service);
  }

  /**
   * Returns an appropriate service instances stream that matches the given service class and the
   * given class loader from {@link ServiceLoader} or return an empty stream if not found or not
   * meet the conditions.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class. If the given
   * service class is {@link Sortable} or {@link Comparable} then return a sorted service instance
   * stream, for some services that are implemented {@link Sortable} and {@link Comparable}, the
   * {@link Sortable} will be used for sorting preferred.
   *
   * @param <S> the service type to be resolved
   * @param service the interface or abstract class representing the service
   * @param loader the class loader to be used to load provider-configuration files and provider
   *        classes, or null if the system class loader (or, failing that, the bootstrap class
   *        loader) is to be used
   *
   * @see Sortable#compare(Sortable, Sortable)
   * @see ServiceLoader#load(Class)
   * @see ServiceLoader#stream()
   */
  public static <S> Stream<S> load(Class<S> service, ClassLoader loader) {
    return load(
        doPrivileged(
            (PrivilegedAction<ServiceLoader<S>>) () -> ServiceLoader.load(service, loader)),
        service);
  }

  /**
   * Returns an appropriate service instances stream that matches the given service class and the
   * given module layer from {@link ServiceLoader} or return an empty stream if not found or not
   * meet the conditions.
   *
   * <p>
   * Note: The required conditions ({@link RequiredClassNotPresent},{@link RequiredClassPresent},
   * {@link RequiredConfiguration}) are placed upon on the implementation class. If the given
   * service class is {@link Sortable} or {@link Comparable} then return a sorted service instance
   * stream, for some services that are implemented {@link Sortable} and {@link Comparable}, the
   * {@link Sortable} will be used for sorting preferred.
   *
   * @param <S> the service type to be resolved
   * @param layer the module layer
   * @param service the interface or abstract class representing the service
   *
   * @see Sortable#compare(Sortable, Sortable)
   * @see ServiceLoader#load(ModuleLayer, Class)
   * @see ServiceLoader#stream()
   */
  public static <S> Stream<S> load(ModuleLayer layer, Class<S> service) {
    return load(
        doPrivileged((PrivilegedAction<ServiceLoader<S>>) () -> ServiceLoader.load(layer, service)),
        service);
  }

  @SuppressWarnings("unchecked")
  static <S> Optional<S> find(ServiceLoader<S> loader, Class<S> service) {
    if (Sortable.class.isAssignableFrom(service)) {
      return (Optional<S>) loader.stream().filter(RequiredServiceLoader::required)
          .map(Provider::get).map(p -> (Sortable) p).min(Sortable::compare);
    } else if (Comparable.class.isAssignableFrom(service)) {
      return (Optional<S>) loader.stream().filter(RequiredServiceLoader::required)
          .map(Provider::get).map(p -> (Comparable<?>) p).sorted().findFirst();
    } else {
      Iterator<Provider<S>> it = loader.stream().filter(RequiredServiceLoader::required).iterator();
      if (it.hasNext()) {
        Provider<S> p = it.next();
        if (!it.hasNext()) {
          return Optional.ofNullable(p.get());
        }
      }
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  static <S> Stream<S> load(ServiceLoader<S> loader, Class<S> service) {
    if (Sortable.class.isAssignableFrom(service)) {
      return (Stream<S>) loader.stream().filter(RequiredServiceLoader::required).map(Provider::get)
          .map(p -> (Sortable) p).sorted(Sortable::compare);
    } else if (Comparable.class.isAssignableFrom(service)) {
      return (Stream<S>) loader.stream().filter(RequiredServiceLoader::required).map(Provider::get)
          .map(p -> (Comparable<?>) p).sorted();
    } else {
      return loader.stream().filter(RequiredServiceLoader::required).map(Provider::get);
    }
  }

  static <S> boolean required(Provider<S> p) {
    return !Required.INSTANCE.shouldVeto(p.type());
  }
}

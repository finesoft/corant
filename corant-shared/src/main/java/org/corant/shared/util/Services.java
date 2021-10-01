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
import java.util.Optional;
import java.util.stream.Stream;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.service.RequiredServiceLoader;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-shared
 *
 * @author bingo 下午10:48:27
 *
 */
public class Services {

  /**
   * Returns an {@link Optional} service instance that matches the given service class and
   * {@link Classes#defaultClassLoader()} from {@link RequiredServiceLoader} or return an empty
   * {@link Optional} if not found.
   *
   * @param <T> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   *
   * @see RequiredServiceLoader#find(Class)
   * @see Sortable#compare(Sortable, Sortable)
   */
  public static <T> Optional<T> find(Class<T> serviceClass) {
    return RequiredServiceLoader.find(serviceClass);
  }

  /**
   * Returns an {@link Optional} service instance that matches the given service class and the given
   * class loader from {@link RequiredServiceLoader} or return an empty {@link Optional} if not
   * found.
   *
   * @param <T> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   * @param classLoader The class loader to be used to load provider-configuration files and
   *        provider classes, or null if the system class loader (or, failing that, the bootstrap
   *        class loader) is to be used
   *
   * @see RequiredServiceLoader#find(Class, ClassLoader)
   * @see Sortable#compare(Sortable, Sortable)
   */
  public static <T> Optional<T> find(Class<T> serviceClass, ClassLoader classLoader) {
    return RequiredServiceLoader.find(serviceClass, classLoader);
  }

  /**
   * Returns an {@link Optional} service instance that matches the given service class and the given
   * module layer and its ancestors from the {@link RequiredServiceLoader} or return an empty
   * {@link Optional} if not found.
   *
   * @param <T> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   * @param layer the module layer
   *
   * @see RequiredServiceLoader#find(ModuleLayer, Class)
   * @see Sortable#compare(Sortable, Sortable)
   */
  public static <T> Optional<T> find(ModuleLayer layer, Class<T> serviceClass) {
    return RequiredServiceLoader.find(layer, serviceClass);
  }

  /**
   * Returns a service instance that matches the given service class and
   * {@link Classes#defaultClassLoader()} from {@link RequiredServiceLoader} or throws exception if
   * the service not found.
   *
   * @param <T> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   *
   * @see RequiredServiceLoader#find(Class)
   * @see Sortable#compare(Sortable, Sortable)
   */
  public static <T> T resolve(Class<T> serviceClass) {
    return find(serviceClass).orElseThrow(() -> new CorantRuntimeException(
        "Unable to resolve the service %s through the class loader %s.", serviceClass,
        defaultClassLoader()));
  }

  /**
   * Returns a service instance that matches the given service class and
   * {@link Classes#defaultClassLoader()} from {@link RequiredServiceLoader} or throws exception if
   * the service not found.
   *
   * @param <T> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   * @param classLoader The class loader to be used to load provider-configuration files and
   *        provider classes, or null if the system class loader (or, failing that, the bootstrap
   *        class loader) is to be used
   *
   * @see RequiredServiceLoader#find(Class, ClassLoader)
   * @see Sortable#compare(Sortable, Sortable)
   */
  public static <T> T resolve(Class<T> serviceClass, ClassLoader classLoader) {
    return find(serviceClass, classLoader).orElseThrow(() -> new CorantRuntimeException(
        "Unable to resolve the service %s through the class loader %s.", serviceClass,
        classLoader));
  }

  /**
   * Returns a service instance that matches the given service class and the given module layer from
   * {@link RequiredServiceLoader} or throws exception if the service not found.
   *
   * @param <T> the service type to be resolved
   * @param layer the module layer
   * @param serviceClass the service instance class to be resolved
   *
   * @see RequiredServiceLoader#find(ModuleLayer, Class)
   * @see Sortable#compare(Sortable, Sortable)
   */
  public static <T> T resolve(ModuleLayer layer, Class<T> serviceClass) {
    return find(layer, serviceClass).orElseThrow(() -> new CorantRuntimeException(
        "Unable to resolve the service %s through the module layer %s.", serviceClass, layer));
  }

  /**
   * Returns a service instance stream that matches the given service class and
   * {@link Classes#defaultClassLoader()} from {@link RequiredServiceLoader}.
   *
   * @param <T> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   */
  public static <T> Stream<T> select(Class<T> serviceClass) {
    return select(serviceClass, defaultClassLoader());
  }

  /**
   * Returns a service instance stream that matches the given service class and the given class
   * loader from {@link RequiredServiceLoader}.
   *
   * @param <T> the service type to be resolved
   * @param serviceClass the service instance class to be resolved
   * @param classLoader The class loader to be used to load provider-configuration files and
   *        provider classes, or null if the system class loader (or, failing that, the bootstrap
   *        class loader) is to be used
   */
  public static <T> Stream<T> select(Class<T> serviceClass, ClassLoader classLoader) {
    return RequiredServiceLoader.load(serviceClass, classLoader);
  }

}

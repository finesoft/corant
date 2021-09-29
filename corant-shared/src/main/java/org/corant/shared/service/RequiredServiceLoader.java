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

import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Stream;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-shared
 *
 * @author bingo 下午8:38:41
 *
 */
public class RequiredServiceLoader {

  public static <S> Optional<S> find(Class<S> service) {
    return find(ServiceLoader.load(service), service);
  }

  public static <S> Optional<S> find(Class<S> service, ClassLoader loader) {
    return find(ServiceLoader.load(service, loader), service);
  }

  public static <S> Optional<S> find(ModuleLayer layer, Class<S> service) {
    return find(ServiceLoader.load(layer, service), service);
  }

  public static <S> Stream<S> load(Class<S> service) {
    return load(ServiceLoader.load(service), service);
  }

  public static <S> Stream<S> load(Class<S> service, ClassLoader loader) {
    return load(ServiceLoader.load(service, loader), service);
  }

  public static <S> Stream<S> load(ModuleLayer layer, Class<S> service) {
    return load(ServiceLoader.load(layer, service), service);
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

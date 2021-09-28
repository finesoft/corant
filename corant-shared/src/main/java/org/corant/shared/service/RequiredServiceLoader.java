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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * corant-shared
 *
 * @author bingo 下午8:38:41
 *
 */
public class RequiredServiceLoader {

  public static <S> List<S> load(Class<S> service) {
    return load(ServiceLoader.load(service));
  }

  public static <S> List<S> load(Class<S> service, ClassLoader loader) {
    return load(ServiceLoader.load(service, loader));
  }

  public static <S> List<S> load(ModuleLayer layer, Class<S> service) {
    return load(ServiceLoader.load(layer, service));
  }

  static <S> List<S> load(ServiceLoader<S> loader) {
    List<S> list = new ArrayList<>();
    if (loader != null) {
      Iterator<S> it = loader.iterator();
      while (it.hasNext()) {
        S service = it.next();
        if (!Required.shouldVeto(service.getClass())) {
          list.add(service);
        }
      }
    }
    return list;
  }
}

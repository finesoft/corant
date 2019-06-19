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
package org.corant.kernel.util;

import static org.corant.Corant.instance;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * corant-kernel
 *
 * @author bingo 下午2:22:40
 *
 */
public class Instances {

  public static <T> Optional<T> resolvable(Class<T> instanceClass) {
    if (instance().select(instanceClass).isResolvable()) {
      return Optional.of(instance().select(instanceClass).get());
    } else {
      return Optional.empty();
    }
  }

  public static <T> void resolvableAccept(Class<T> instanceClass, Consumer<T> consumer) {
    shouldNotNull(consumer).accept(instance().select(instanceClass).get());
  }

  public static <T> T resolvableAnyway(Class<T> instanceClass) {
    if (instance().select(instanceClass).isResolvable()) {
      return instance().select(instanceClass).get();
    } else if (instance().select(instanceClass).isUnsatisfied()) {
      return Unmanageables.create(instanceClass).get();
    } else {
      return null;
    }
  }

  public static <T> T resolvableAnyway(T obj) {
    if (Manageables.isManagedBean(obj)) {
      return obj;
    } else if (obj != null) {
      return Unmanageables.accept(obj).get();
    }
    return null;
  }

  public static <T, R> R resolvableApply(Class<T> instanceClass, Function<T, R> function) {
    return shouldNotNull(function).apply(instance().select(instanceClass).get());
  }

  public static <T> void tryResolvableAccept(Class<T> instanceClass, Consumer<T> consumer) {
    if (instance().select(instanceClass).isResolvable()) {
      shouldNotNull(consumer).accept(instance().select(instanceClass).get());
    }
  }

  public static <T, R> R tryResolvableApply(Class<T> instanceClass, Function<T, R> function) {
    if (instance().select(instanceClass).isResolvable()) {
      return shouldNotNull(function).apply(instance().select(instanceClass).get());
    }
    return null;
  }
}

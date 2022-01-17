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
package org.corant.shared.util;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Throwing.ThrowingBiConsumer;
import org.corant.shared.ubiquity.Throwing.ThrowingBiFunction;
import org.corant.shared.ubiquity.Throwing.ThrowingConsumer;
import org.corant.shared.ubiquity.Throwing.ThrowingFunction;
import org.corant.shared.ubiquity.Throwing.ThrowingRunnable;
import org.corant.shared.ubiquity.Throwing.ThrowingSupplier;

/**
 * corant-shared
 *
 * @author bingo 下午2:51:22
 *
 */
public class Functions {

  @SuppressWarnings("rawtypes")
  public static final Consumer EMPTY_CONSUMER = o -> {
  };

  @SuppressWarnings("rawtypes")
  public static final Supplier EMPTY_SUPPLIER = () -> null;

  @SuppressWarnings("rawtypes")
  public static final Function EMPTY_FUNCTION = p -> null;

  @SuppressWarnings("rawtypes")
  public static final Predicate EMPTY_PREDICATE_TRUE = p -> true;

  @SuppressWarnings("rawtypes")
  public static final Predicate EMPTY_PREDICATE_FALSE = p -> false;

  public static <T> Callable<T> asCallable(Runnable runnable) {
    return () -> {
      shouldNotNull(runnable).run();
      return null;
    };
  }

  public static <T> Callable<T> asCallable(Supplier<T> supplier) {
    return () -> supplier.get();
  }

  public static <T> Supplier<T> asSupplier(Callable<T> callable) {
    return () -> {
      try {
        return callable.call();
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    };
  }

  public static <T> Supplier<T> asSupplier(Runnable runnable) {
    return () -> {
      shouldNotNull(runnable).run();
      return null;
    };
  }

  @SuppressWarnings("unchecked")
  public static <T> Consumer<T> emptyConsumer() {
    return EMPTY_CONSUMER;
  }

  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> emptyPredicate(boolean bool) {
    return bool ? EMPTY_PREDICATE_TRUE : EMPTY_PREDICATE_FALSE;
  }

  @SuppressWarnings("unchecked")
  public static <T> Supplier<T> emptySupplier() {
    return EMPTY_SUPPLIER;
  }

  public static <T> Optional<T> optional(T obj) {
    return Optional.ofNullable(obj);
  }

  public static <T> T trySupplied(Supplier<T> supplier) {
    T supplied = null;
    if (supplier != null) {
      try {
        supplied = supplier.get();
      } catch (Exception e) {
        // Noop! just try...
      }
    }
    return supplied;
  }

  public static <K, V> BiConsumer<K, V> uncheckedBiConsumer(
      ThrowingBiConsumer<K, V, Exception> consumer) {
    return (k, v) -> {
      try {
        consumer.accept(k, v);
      } catch (Exception ex) {
        throw new CorantRuntimeException(ex);
      }
    };
  }

  public static <T, U, R> BiFunction<T, U, R> uncheckedBiFunction(
      ThrowingBiFunction<T, U, R, Exception> biFunction) {
    return (t, u) -> {
      try {
        return biFunction.apply(t, u);
      } catch (Exception ex) {
        throw new CorantRuntimeException(ex);
      }
    };
  }

  public static <T> Consumer<T> uncheckedConsumer(ThrowingConsumer<T, Exception> consumer) {
    return i -> {
      try {
        consumer.accept(i);
      } catch (Exception ex) {
        throw new CorantRuntimeException(ex);
      }
    };
  }

  public static <T, R, E extends Throwable> Function<T, R> uncheckedFunction(
      ThrowingFunction<T, R, E> computer) {
    return t -> {
      try {
        return shouldNotNull(computer).apply(t);
      } catch (Throwable e) {
        throw new CorantRuntimeException(e);
      }
    };
  }

  public static <E extends Throwable> Runnable uncheckedRunner(ThrowingRunnable<E> runner) {
    return () -> {
      try {
        shouldNotNull(runner).run();
      } catch (Throwable e) {
        throw new CorantRuntimeException(e);
      }
    };
  }

  public static <T, E extends Throwable> T uncheckedSupplied(ThrowingSupplier<T, E> supplier) {
    try {
      return shouldNotNull(supplier).get();
    } catch (Throwable e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T, E extends Throwable> Supplier<T> uncheckedSupplier(
      final ThrowingSupplier<T, E> supplier) {
    return () -> {
      try {
        return shouldNotNull(supplier).get();
      } catch (Throwable e) {
        throw new CorantRuntimeException(e);
      }
    };
  }
}

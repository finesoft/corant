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
package org.corant.shared.ubiquity;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Throwables.asUncheckedException;
import static org.corant.shared.util.Throwables.rethrow;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * corant-shared
 *
 * @author bingo 下午12:17:37
 */
public class Throwing {

  private Throwing() {}

  public static <K, V> BiConsumer<K, V> uncheckedBiConsumer(
      ThrowingBiConsumer<K, V, Exception> consumer) {
    return (k, v) -> {
      try {
        consumer.accept(k, v);
      } catch (Exception ex) {
        throw asUncheckedException(ex);
      }
    };
  }

  public static <T, U, R> BiFunction<T, U, R> uncheckedBiFunction(
      ThrowingBiFunction<T, U, R, Exception> biFunction) {
    return (t, u) -> {
      try {
        return biFunction.apply(t, u);
      } catch (Exception ex) {
        throw asUncheckedException(ex);
      }
    };
  }

  public static <T> Consumer<T> uncheckedConsumer(ThrowingConsumer<T, Exception> consumer) {
    return i -> {
      try {
        consumer.accept(i);
      } catch (Exception ex) {
        throw asUncheckedException(ex);
      }
    };
  }

  public static <T, R, E extends Exception> Function<T, R> uncheckedFunction(
      ThrowingFunction<T, R, E> computer) {
    return t -> {
      try {
        return shouldNotNull(computer).apply(t);
      } catch (Exception e) {
        throw asUncheckedException(e);
      }
    };
  }

  public static <E extends Exception> Runnable uncheckedRunner(ThrowingRunnable<E> runner) {
    return () -> {
      try {
        shouldNotNull(runner).run();
      } catch (Exception e) {
        rethrow(e);
      }
    };
  }

  public static <T, E extends Exception> T uncheckedSupplied(ThrowingSupplier<T, E> supplier) {
    try {
      return shouldNotNull(supplier).get();
    } catch (Exception e) {
      throw asUncheckedException(e);
    }
  }

  public static <T, E extends Exception> Supplier<T> uncheckedSupplier(
      final ThrowingSupplier<T, E> supplier) {
    return () -> {
      try {
        return shouldNotNull(supplier).get();
      } catch (Exception e) {
        throw asUncheckedException(e);
      }
    };
  }

  @FunctionalInterface
  public interface ThrowingBiConsumer<K, V, E extends Throwable> {
    void accept(K k, V v) throws E;

    default ThrowingBiConsumer<K, V, E> andThen(BiConsumer<? super K, ? super V> after) {
      shouldNotNull(after);
      return (l, r) -> {
        accept(l, r);
        after.accept(l, r);
      };
    }
  }

  @FunctionalInterface
  public interface ThrowingBiFunction<T, U, R, E extends Throwable> {

    default <V> ThrowingBiFunction<T, U, V, E> andThen(Function<? super R, ? extends V> after) {
      shouldNotNull(after);
      return (T t, U u) -> after.apply(apply(t, u));
    }

    R apply(T t, U u) throws E;

  }

  @FunctionalInterface
  public interface ThrowingConsumer<T, E extends Throwable> {
    void accept(T t) throws E;

    default ThrowingConsumer<T, E> andThen(Consumer<? super T> after) {
      shouldNotNull(after);
      return (T t) -> {
        accept(t);
        after.accept(t);
      };
    }
  }

  @FunctionalInterface
  public interface ThrowingFunction<T, R, E extends Throwable> {

    static <T, E extends Throwable> ThrowingFunction<T, T, E> identity() {
      return t -> t;
    }

    default <V> ThrowingFunction<T, V, E> andThen(Function<? super R, ? extends V> after) {
      shouldNotNull(after);
      return (T t) -> after.apply(apply(t));
    }

    R apply(T t) throws E;

    default <V> ThrowingFunction<V, R, E> compose(Function<? super V, ? extends T> before) {
      shouldNotNull(before);
      return (V v) -> apply(before.apply(v));
    }
  }

  @FunctionalInterface
  public interface ThrowingRunnable<E extends Throwable> {
    void run() throws E;
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;
  }
}

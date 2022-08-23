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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午12:17:37
 *
 */
public class Throwing {

  private Throwing() {}

  public static void rethrow(Throwable t) {
    if (t instanceof RuntimeException) {
      throw (RuntimeException) t;
    }
    if (t instanceof Error) {
      throw (Error) t;
    }
    throw new CorantRuntimeException(t);
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

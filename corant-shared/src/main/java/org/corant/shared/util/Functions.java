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
import static org.corant.shared.util.Throwables.asUncheckedException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
  public static final BiConsumer EMPTY_BICONSUMER = (a, b) -> {
  };

  @SuppressWarnings("rawtypes")
  public static final Supplier EMPTY_SUPPLIER = () -> null;

  @SuppressWarnings("rawtypes")
  public static final Function EMPTY_FUNCTION = p -> null;

  @SuppressWarnings("rawtypes")
  public static final Predicate EMPTY_PREDICATE_TRUE = p -> true;

  @SuppressWarnings("rawtypes")
  public static final Predicate EMPTY_PREDICATE_FALSE = p -> false;

  @SuppressWarnings("rawtypes")
  public static final BiPredicate EMPTY_BIPREDICATE_TRUE = (a, b) -> true;

  @SuppressWarnings("rawtypes")
  public static final BiPredicate EMPTY_BIPREDICATE_FALSE = (a, b) -> false;

  public static <T> Callable<T> asCallable(Runnable runnable) {
    return () -> {
      shouldNotNull(runnable).run();
      return null;
    };
  }

  public static <T> Callable<T> asCallable(Supplier<T> supplier) {
    return supplier::get;
  }

  public static <T> Supplier<T> asSupplier(Callable<T> callable) {
    return () -> {
      try {
        return callable.call();
      } catch (Exception e) {
        throw asUncheckedException(e);
      }
    };
  }

  public static <T> Supplier<T> asSupplier(Runnable runnable) {
    return () -> {
      shouldNotNull(runnable).run();
      return null;
    };
  }

  public static <T, U> BiPredicate<T, U> defaultBiPredicate(BiPredicate<T, U> predicate,
      boolean always) {
    return predicate != null ? predicate : emptyBiPredicate(always);
  }

  public static <T> Predicate<T> defaultPredicate(Predicate<T> predicate, boolean always) {
    return predicate != null ? predicate : emptyPredicate(always);
  }

  @SuppressWarnings("unchecked")
  public static <A, B> BiConsumer<A, B> emptyBiConsumer() {
    return EMPTY_BICONSUMER;
  }

  @SuppressWarnings("unchecked")
  public static <T, U> BiPredicate<T, U> emptyBiPredicate(boolean bool) {
    return bool ? EMPTY_BIPREDICATE_TRUE : EMPTY_BIPREDICATE_FALSE;
  }

  @SuppressWarnings("unchecked")
  public static <T> Consumer<T> emptyConsumer() {
    return EMPTY_CONSUMER;
  }

  @SuppressWarnings("unchecked")
  public static <P, R> Function<P, R> emptyFunction() {
    return EMPTY_FUNCTION;
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

}

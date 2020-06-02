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

import static org.corant.shared.util.StreamUtils.streamOf;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午11:26:00
 *
 */
public class ObjectUtils {

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

  protected ObjectUtils() {}

  public static String asString(Object o) {
    return Objects.toString(o);
  }

  public static String asString(Object o, String nullDefault) {
    return Objects.toString(o, nullDefault);
  }

  public static String[] asStrings(Iterable<?> it) {
    return asStrings(null, streamOf(it).map(Objects::toString).toArray(Object[]::new));
  }

  public static String[] asStrings(Object... objs) {
    return asStrings(null, objs);
  }

  public static String[] asStrings(UnaryOperator<String> uo, Object... objs) {
    if (uo == null) {
      return Arrays.stream(objs).map(o -> asString(o, "null")).toArray(String[]::new);
    } else {
      return Arrays.stream(objs).map(o -> uo.apply(asString(o, "null"))).toArray(String[]::new);
    }
  }

  public static <T> int compare(T a, T b, Comparator<? super T> c) {
    return Objects.compare(a, b, c);
  }

  public static <T> T defaultObject(T obj, Supplier<T> supplier) {
    return obj != null ? obj : supplier.get();
  }

  public static <T> T defaultObject(T obj, T altObj) {
    return obj != null ? obj : altObj;
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

  @SuppressWarnings("unchecked")
  public static <T> T forceCast(Object o) {
    return o != null ? (T) o : null;
  }

  public static int hash(Object... values) {
    return Objects.hash(values);
  }

  public static int hashCode(Object o) {
    return Objects.hashCode(o);
  }

  public static boolean isDeepEquals(Object a, Object b) {
    return Objects.deepEquals(a, b);
  }

  public static boolean isEquals(Object a, Object b) {
    return Objects.equals(a, b);
  }

  public static <T extends Number & Comparable<T>> boolean isEquals(T a, T b) {
    return Objects.equals(a, b) || a != null && b != null && a.compareTo(b) == 0;
  }

  public static boolean isNoneNull(Object... objs) {
    for (Object obj : objs) {
      if (isNull(obj)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isNotNull(Object obj) {
    return Objects.nonNull(obj);
  }

  public static boolean isNull(Object obj) {
    return Objects.isNull(obj);
  }

  /**
   * Return the max one, if the two parameters are the same, then return the first.
   *
   * @param <T>
   * @param a
   * @param b
   * @return max
   */
  public static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
  }

  /**
   * Return the min one, if the two parameters are the same, then return the first.
   *
   * @param <T>
   * @param a
   * @param b
   * @return min
   */
  public static <T extends Comparable<T>> T min(T a, T b) {
    return a.compareTo(b) <= 0 ? a : b;
  }

  public static <T> T newInstance(Class<T> cls) {
    try {
      return cls != null ? cls.newInstance() : null;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T> Optional<T> optional(T obj) {
    return Optional.ofNullable(obj);
  }

  public static <T> Optional<T> optionalCast(Object o, Class<T> cls) {
    return Optional.ofNullable(tryCast(o, cls));
  }

  public static <T> T tryCast(Object o, Class<T> cls) {
    return o != null && cls.isInstance(o) ? cls.cast(o) : null;
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

  public static void tryThreadSleep(Long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // Noop! just try...
    }
  }
}

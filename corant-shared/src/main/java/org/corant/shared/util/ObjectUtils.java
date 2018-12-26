/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.shared.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * @author bingo 下午2:51:32
 *
 */
public class ObjectUtils {

  protected ObjectUtils() {}

  public static String asString(Object o) {
    return Objects.toString(o);
  }

  public static String asString(Object o, String nullDefault) {
    return Objects.toString(o, nullDefault);
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
    return defaultObject(obj, supplier.get());
  }

  public static <T> T defaultObject(T obj, T altObj) {
    return obj == null ? altObj : obj;
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

  public static boolean isNotNull(Object obj) {
    return Objects.nonNull(obj);
  }

  public static boolean isNull(Object obj) {
    return Objects.isNull(obj);
  }

  public static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
  }

  public static <T extends Comparable<T>> T min(T a, T b) {
    return a.compareTo(b) < 0 ? a : b;
  }

  public static <T> Optional<T> optional(T obj) {
    return Optional.ofNullable(obj);
  }

  public static <T> Optional<T> optionalCast(Object o, Class<T> cls) {
    return Optional.ofNullable(tryCast(o, cls));
  }

  public static void shouldBeEquals(Object a, Object b) {
    shouldBeEquals(a, b, "The objects %s %s should be equal!", asString(a), asString(b));
  }

  public static void shouldBeEquals(Object a, Object b, String messageOrFormat, Object... args) {
    if (!isEquals(a, b)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  public static void shouldBeFalse(boolean condition) {
    shouldBeTrue(condition, "This shoud be false");
  }

  public static void shouldBeFalse(boolean condition, String messageOrFormat, Object... args) {
    if (condition) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  public static void shouldBeTrue(boolean condition) {
    shouldBeTrue(condition, "This shoud be true");
  }

  public static void shouldBeTrue(boolean condition, String messageOrFormat, Object... args) {
    if (!condition) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  public static <T> T shouldNotNull(T obj) {
    return shouldNotNull(obj, "The object should not null!");
  }

  public static <T> T shouldNotNull(T obj, String messageOrFormat, Object... args) {
    if (obj == null) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return obj;
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
      }
    }
    return supplied;
  }
}

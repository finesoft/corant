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

import static org.corant.shared.util.Streams.streamOf;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午11:26:00
 *
 */
public class Objects {

  protected Objects() {}

  public static boolean areEqual(Object a, Object b) {
    return java.util.Objects.equals(a, b);
  }

  public static <T extends Number & Comparable<T>> boolean areEqual(T a, T b) {
    return java.util.Objects.equals(a, b) || a != null && b != null && a.compareTo(b) == 0;
  }

  public static String asString(Object o) {
    return java.util.Objects.toString(o);
  }

  public static String asString(Object o, String nullDefault) {
    return java.util.Objects.toString(o, nullDefault);
  }

  public static String[] asStrings(Iterable<?> it) {
    return asStrings(null, streamOf(it).map(java.util.Objects::toString).toArray(Object[]::new));
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
    return java.util.Objects.compare(a, b, c);
  }

  public static <T> T defaultObject(T obj, Supplier<T> supplier) {
    return obj != null ? obj : supplier.get();
  }

  public static <T> T defaultObject(T obj, T altObj) {
    return obj != null ? obj : altObj;
  }

  @SuppressWarnings("unchecked")
  public static <T> T forceCast(Object o) {
    return o != null ? (T) o : null;
  }

  public static int hash(Object... values) {
    return java.util.Objects.hash(values);
  }

  public static int hashCode(Object o) {
    return java.util.Objects.hashCode(o);
  }

  public static boolean areDeepEquals(Object a, Object b) {
    return java.util.Objects.deepEquals(a, b);
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
    return java.util.Objects.nonNull(obj);
  }

  public static boolean isNull(Object obj) {
    return java.util.Objects.isNull(obj);
  }

  /**
   * Return the max one, if the two parameters are the same, then return the first. If any of the
   * comparables are null, return the greater of the non-null objects.
   *
   * @param <T>
   * @param comparables
   * @return max
   */
  @SuppressWarnings("unchecked")
  public static <T extends Comparable<? super T>> T max(final T... comparables) {
    T result = null;
    if (comparables != null) {
      for (final T value : comparables) {
        int c;
        if (value == result) {
          c = 0;
        } else if (value == null) {
          c = -1;
        } else if (result == null) {
          c = 1;
        } else {
          c = value.compareTo(result);
        }
        if (c > 0) {
          result = value;
        }
      }
    }
    return result;
  }

  /**
   * Return the min one, if the two parameters are the same, then return the first. If any of the
   * comparables are null, return the lesser of the non-null objects.
   *
   * @param <T>
   * @param comparables
   * @return min
   */
  @SuppressWarnings("unchecked")
  public static <T extends Comparable<? super T>> T min(final T... comparables) {
    T result = null;
    if (comparables != null) {
      for (final T value : comparables) {
        int c;
        if (value == result) {
          c = 0;
        } else if (value == null) {
          c = 1;
        } else if (result == null) {
          c = -1;
        } else {
          c = value.compareTo(result);
        }
        if (c < 0) {
          result = value;
        }
      }
    }
    return result;
  }

  public static <T> T newInstance(Class<T> cls) {
    try {
      return cls != null ? cls.newInstance() : null;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T> Optional<T> optionalCast(Object o, Class<T> cls) {
    return Optional.ofNullable(tryCast(o, cls));
  }

  public static <T> T tryCast(Object o, Class<T> cls) {
    return o != null && cls.isInstance(o) ? cls.cast(o) : null;
  }

}

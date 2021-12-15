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

import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Streams.streamOf;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
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

  public static final Object[] EMPTY_ARRAY = {};

  protected Objects() {}

  /**
   * @see java.util.Objects#deepEquals(Object, Object)
   */
  public static boolean areDeepEqual(Object a, Object b) {
    return java.util.Objects.deepEquals(a, b);
  }

  /**
   * @see java.util.Objects#equals(Object, Object)
   */
  public static boolean areEqual(Object a, Object b) {
    return java.util.Objects.equals(a, b);
  }

  /**
   * Returns true if the arguments are equal to each other or the result of the comparison is 0 and
   * false otherwise. Consequently, if both arguments are null, true is returned and if exactly one
   * argument is null, false is returned. Otherwise, equality is determined by using the equals or
   * compareTo method of the first argument. Therefore, this method can be used to determine the
   * equivalence of numeric values.
   *
   * @param <T> the object type
   * @param a a comparable object
   * @param b a comparable object to be compared with a for equality
   */
  public static <T extends Number & Comparable<T>> boolean areEqual(T a, T b) {
    return java.util.Objects.equals(a, b) || a != null && b != null && a.compareTo(b) == 0;
  }

  /**
   * @see java.util.Objects#toString(Object)
   */
  public static String asString(Object o) {
    return java.util.Objects.toString(o);
  }

  /**
   * @see java.util.Objects#toString(Object, String)
   */
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
      return Arrays.stream(objs).map(o -> asString(o, Strings.NULL)).toArray(String[]::new);
    } else {
      return Arrays.stream(objs).map(o -> uo.apply(asString(o, Strings.NULL)))
          .toArray(String[]::new);
    }
  }

  /**
   * Null safe comparison of Comparables. null is assumed to be less than a non-null value.
   *
   * Note: Code come from apache.
   *
   * @param <T> the comparable object type
   * @param c1 a comparable object
   * @param c2 a comparable object to be compared with a
   */
  public static <T extends Comparable<? super T>> int compare(final T c1, final T c2) {
    if (c1 == c2) {
      return 0;
    } else if (c1 == null) {
      return -1;
    } else if (c2 == null) {
      return 1;
    }
    return c1.compareTo(c2);
  }

  /**
   * @see java.util.Objects#compare(Object, Object, Comparator)
   */
  public static <T> int compare(T a, T b, Comparator<? super T> c) {
    return java.util.Objects.compare(a, b, c);
  }

  /**
   * If the first given parameter is not null, returns the value, otherwise returns the result of
   * the given supplier.
   *
   * @param <T> the object type
   * @param obj the object to return if not null
   * @param supplier the supplier for supporting the return value if the first given parameter is
   *        null
   */
  public static <T> T defaultObject(T obj, Supplier<? extends T> supplier) {
    return obj != null ? obj : supplier.get();
  }

  /**
   * If the first given parameter is not null, returns the value, otherwise returns the second given
   * parameter.
   *
   * @param <T> the object type
   * @param obj the object to return if not null
   * @param altObj the alternative object to return if the first given parameter is null
   */
  public static <T> T defaultObject(T obj, T altObj) {
    return obj != null ? obj : altObj;
  }

  /**
   * Simple cast object to expect type.
   *
   * @param <T> the object type
   * @param o the object to be cast
   * @return forceCast
   */
  @SuppressWarnings("unchecked")
  public static <T> T forceCast(Object o) {
    return o != null ? (T) o : null;
  }

  /**
   * @see java.util.Objects#hash(Object...)
   */
  public static int hash(Object... values) {
    return java.util.Objects.hash(values);
  }

  /**
   * @see java.util.Objects#hashCode(Object)
   */
  public static int hashCode(Object o) {
    return java.util.Objects.hashCode(o);
  }

  /**
   * Return true if all given parameter are not null and false otherwise.
   */
  public static boolean isNoneNull(Object... objs) {
    for (Object obj : objs) {
      if (isNull(obj)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return true if given parameter is not null and false otherwise.
   */
  public static boolean isNotNull(Object obj) {
    return java.util.Objects.nonNull(obj);
  }

  /**
   * Return false if given parameter is not null and true otherwise.
   */
  public static boolean isNull(Object obj) {
    return java.util.Objects.isNull(obj);
  }

  /**
   * Return the max one, if the two parameters are the same, then return the first. If any of the
   * comparable are null, return the greatest of the non-null objects.
   *
   * @param <T> the comparable type
   * @param comparables the given comparable object to find out the max one
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
   * comparables are null, return the least of the non-null objects.
   *
   * @param <T> the comparable type
   * @param comparables the given comparable object to find out the min one
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

  /**
   * Returns a new instance of the given class, use {@link Class#getDeclaredConstructor(Class...)}.
   *
   * @param <T> the instance type
   * @param cls the instance class
   */
  public static <T> T newInstance(Class<T> cls) {
    try {
      // return cls != null ? cls.newInstance() : null;// JDK8
      return cls != null ? cls.getDeclaredConstructor().newInstance() : null;
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T> Optional<T> optionalCast(Object o, Class<T> cls) {
    return Optional.ofNullable(tryCast(o, cls));
  }

  public static void setAccessible(AccessibleObject object, boolean flag) {
    if (System.getSecurityManager() == null) {
      object.setAccessible(flag);
    } else {
      AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
        try {
          object.setAccessible(flag);
        } catch (SecurityException ex) {
          // Noop!
        }
        return null;
      });
    }
  }

  public static <T> T tryCast(Object o, Class<T> cls) {
    return cls.isInstance(o) ? cls.cast(o) : null;
  }

  public static <T> T tryNewInstance(String className) {
    try {
      return forceCast(newInstance(tryAsClass(className)));
    } catch (Exception e) {
      // Noop!
    }
    return null;
  }

}

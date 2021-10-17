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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Range;
import org.corant.shared.ubiquity.Tuple.Triple;

/**
 * corant-shared
 *
 * @author bingo 上午12:30:19
 *
 */
public class Empties {

  private Empties() {}

  /**
   * <pre>
   * Empties.isEmpty(null)      = true
   * Empties.isEmpty("")        = true
   * Empties.isEmpty(" ")       = false
   * Empties.isEmpty("abc")     = false
   * Empties.isEmpty("  abc  ") = false
   * </pre>
   */
  public static boolean isEmpty(final CharSequence object) {
    return object == null || object.length() == 0;
  }

  /**
   * Return true if object is null or object.size() == 0
   *
   * @param object the collection object to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Collection<?> object) {
    return object == null || object.isEmpty();
  }

  /**
   * Return true if object is null or object.hasMoreElements() == false
   *
   * @param object the enumeration object to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Enumeration<?> object) {
    return object == null || !object.hasMoreElements();
  }

  /**
   * Return true if object is null or object.iterator().hasNext() == false
   *
   * @param object the iterable object to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Iterable<?> object) {
    return object == null || !object.iterator().hasNext();
  }

  /**
   * Return true if object is null and object.hasNext() == false
   *
   * @param object the iterator object to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Iterator<?> object) {
    return object == null || !object.hasNext();
  }

  /**
   * Return true if object is null and object.isEmpty() == true
   *
   * @param object the map object to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Map<?, ?> object) {
    return object == null || object.isEmpty();
  }

  public static boolean isEmpty(final Object object) {
    if (object == null) {
      return true;
    } else if (object instanceof Collection<?>) {
      return isEmpty((Collection<?>) object);
    } else if (object instanceof Map<?, ?>) {
      return isEmpty((Map<?, ?>) object);
    } else if (object instanceof Object[]) {
      return isEmpty((Object[]) object);
    } else if (object instanceof CharSequence) {
      return isEmpty((CharSequence) object);
    } else if (object instanceof Iterator<?>) {
      return isEmpty((Iterator<?>) object);
    } else if (object instanceof Iterable<?>) {
      return isEmpty((Iterable<?>) object);
    } else if (object instanceof Enumeration<?>) {
      return isEmpty((Enumeration<?>) object);
    } else if (object instanceof Pair<?, ?>) {
      return isEmpty((Pair<?, ?>) object);
    } else if (object instanceof Triple<?, ?, ?>) {
      return isEmpty((Triple<?, ?, ?>) object);
    } else if (object instanceof Range<?>) {
      return isEmpty((Range<?>) object);
    } else if (object instanceof Optional<?>) {
      return isEmpty((Optional<?>) object);
    } else {
      if (object.getClass().isArray()) {
        return Array.getLength(object) == 0;
      } else {
        Method m = Methods.getMatchingMethod(object.getClass(), "isEmpty");
        if (m != null && Modifier.isPublic(m.getModifiers())
            && (m.getReturnType().equals(Boolean.TYPE)
                || m.getReturnType().equals(Boolean.class))) {
          try {
            return (boolean) m.invoke(object);
          } catch (IllegalAccessException | IllegalArgumentException
              | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
          }
        }
      }
      throw new IllegalArgumentException("Unsupported object type: " + object.getClass().getName());
    }
  }

  /**
   * Return true if object is null and object.length == 0
   *
   * @param object the object array to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Object[] object) {
    return object == null || object.length == 0;
  }

  /**
   * Return true if object is null or object.iterator().hasNext() == false
   *
   * @param object the optional object to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Optional<?> object) {
    return object == null || object.isEmpty();
  }

  /**
   * Return true if object is null and object.isEmpty() == true
   *
   * @param object the tuple pair object to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Pair<?, ?> object) {
    return object == null || object.isEmpty();
  }

  /**
   * Return true if object is null and object.isEmpty() == true
   *
   * @param object the tuple range object to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Range<? extends Comparable<?>> object) {
    return object == null || object.isEmpty();
  }

  /**
   * Return true if object is null and object.isEmpty() == true
   *
   * @param object the tuple triple object to check
   * @return isEmpty
   */
  public static boolean isEmpty(final Triple<?, ?, ?> object) {
    return object == null || object.isEmpty();
  }

  /**
   * <pre>
   * Empties.isNotEmpty(null)      = false
   * Empties.isNotEmpty("")        = false
   * Empties.isNotEmpty(" ")       = true
   * Empties.isNotEmpty("abc")     = true
   * Empties.isNotEmpty("  abc  ") = true
   * </pre>
   *
   * @param object the char sequence (string) object to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final CharSequence object) {
    return !isEmpty(object);
  }

  /**
   * Return true if and only if object is not null and object.isEmpty() == false
   *
   * @param object the collection object to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final Collection<?> object) {
    return !isEmpty(object);
  }

  /**
   * Return true if and only if object is not null and object.hasMoreElements() == true
   *
   * @param object the enumeration object to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final Enumeration<?> object) {
    return !isEmpty(object);
  }

  /**
   * Return true if and only if object is not null and object.iterator().hasNext() == true
   *
   * @param object the iterable object to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final Iterable<?> object) {
    return !isEmpty(object);
  }

  /**
   * Return true if and only if object is not null and object.hasNext() == true
   *
   * @param object the iterator object to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final Iterator<?> object) {
    return !isEmpty(object);
  }

  /**
   * Return true if and only if object is not null and object.isEmpty() == false
   *
   * @param object the map object to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final Map<?, ?> object) {
    return !isEmpty(object);
  }

  public static boolean isNotEmpty(final Object object) {
    return !isEmpty(object);
  }

  /**
   * Return true if and only if object is not null and object.length > 0
   *
   * @param object the object array to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final Object[] object) {
    return !isEmpty(object);
  }

  public static boolean isNotEmpty(final Optional<?> object) {
    return !isEmpty(object);
  }

  /**
   * Return true if and only if object is not null and object.isEmpty() == false
   *
   * @param object the tuple pair object to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final Pair<?, ?> object) {
    return !isEmpty(object);
  }

  /**
   * Return true if and only if object is not null and object.isEmpty() == false
   *
   * @param object the tuple range object to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final Range<? extends Comparable<?>> object) {
    return !isEmpty(object);
  }

  /**
   * Return true if and only if object is not null and object.isEmpty() == false
   *
   * @param object the tuple triple object to check
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final Triple<?, ?, ?> object) {
    return !isEmpty(object);
  }

  /**
   * If object is null return 0 else return object.length;
   *
   * @param object the primitive boolean array to check
   * @return sizeOf
   */
  public static int sizeOf(final boolean[] object) {
    return object == null ? 0 : object.length;
  }

  /**
   * If object is null return 0 else return object.length;
   *
   * @param object the primitive byte array to check
   * @return sizeOf
   */
  public static int sizeOf(final byte[] object) {
    return object == null ? 0 : object.length;
  }

  /**
   * If object is null return 0 else return object.length;
   *
   * @param object the primitive char array to check
   * @return sizeOf
   */
  public static int sizeOf(final char[] object) {
    return object == null ? 0 : object.length;
  }

  /**
   * If object is null return 0 else return object.length();
   *
   * @param object the char sequence (string) object
   * @return sizeOf
   */
  public static int sizeOf(final CharSequence object) {
    return isEmpty(object) ? 0 : object.length();
  }

  /**
   * If object is null return 0 else return object.size();
   *
   * @param object the collection object
   * @return sizeOf
   */
  public static int sizeOf(final Collection<?> object) {
    return isEmpty(object) ? 0 : object.size();
  }

  /**
   * If object is null return 0 else return object.length;
   *
   * @param object the primitive double array
   * @return sizeOf
   */
  public static int sizeOf(final double[] object) {
    return object == null ? 0 : object.length;
  }

  /**
   * Returns the number of enumeration elements passed in
   *
   * @param enums the enumeration object
   * @return sizeOf
   */
  public static int sizeOf(final Enumeration<?> enums) {
    int size = 0;
    while (enums.hasMoreElements()) {
      size++;
      enums.nextElement();
    }
    return size;
  }

  /**
   * If object is null return 0 else return object.length;
   *
   * @param object the primitive float array
   * @return sizeOf
   */
  public static int sizeOf(final float[] object) {
    return object == null ? 0 : object.length;
  }

  /**
   * If object is null return 0 else return object.length;
   *
   * @param object the primitive int array
   * @return sizeOf
   */
  public static int sizeOf(final int[] object) {
    return object == null ? 0 : object.length;
  }

  /**
   * Returns the number of times the passed in iterable can iterate
   *
   * @param iterable the iterable object
   * @return sizeOf
   */
  public static int sizeOf(final Iterable<?> iterable) {
    if (iterable instanceof Collection) {
      return ((Collection<?>) iterable).size();
    } else if (iterable != null) {
      sizeOf(iterable.iterator());
    }
    return 0;
  }

  /**
   * Returns the number of times the passed in iterator can iterate
   *
   * @param iterator
   * @return sizeOf
   */
  @Deprecated
  public static int sizeOf(final Iterator<?> iterator) {
    int size = 0;
    if (iterator != null) {
      while (iterator.hasNext()) {
        iterator.next();
        size++;
      }
    }
    return size;
  }

  /**
   * If object is null return 0 else return object.length;
   *
   * @param object the primitive long array
   * @return sizeOf
   */
  public static int sizeOf(final long[] object) {
    return object == null ? 0 : object.length;
  }

  /**
   * If object is null return 0 else return object.size();
   *
   * @param object the map object
   * @return sizeOf
   */
  public static int sizeOf(final Map<?, ?> object) {
    return isEmpty(object) ? 0 : object.size();
  }

  /**
   * If object is null return 0 else return object.length;
   *
   * @param object the object array
   * @return sizeOf
   */
  public static int sizeOf(final Object[] object) {
    return isEmpty(object) ? 0 : object.length;
  }

  /**
   * If object is null return 0 else return object.length;
   *
   * @param object the primitive short array
   * @return sizeOf
   */
  public static int sizeOf(final short[] object) {
    return object == null ? 0 : object.length;
  }
}

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
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 上午12:30:19
 *
 */
public class Empties {

  private Empties() {
    super();
  }

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

  public static boolean isEmpty(final Collection<?> object) {
    return object == null || object.isEmpty();
  }

  public static boolean isEmpty(final Enumeration<?> object) {
    return object == null || !object.hasMoreElements();
  }

  public static boolean isEmpty(final Iterable<?> object) {
    return object == null || !object.iterator().hasNext();
  }

  public static boolean isEmpty(final Iterator<?> object) {
    return object == null || !object.hasNext();
  }

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
    } else if (object instanceof Iterator<?>) {
      return isEmpty((Iterator<?>) object);
    } else if (object instanceof Iterable<?>) {
      return isEmpty((Iterable<?>) object);
    } else if (object instanceof Enumeration<?>) {
      return isEmpty((Enumeration<?>) object);
    } else if (object instanceof CharSequence) {
      return isEmpty((CharSequence) object);
    } else {
      try {
        return Array.getLength(object) == 0;
      } catch (final IllegalArgumentException ex) {
        throw new IllegalArgumentException(
            "Unsupported object type: " + object.getClass().getName());
      }
    }
  }

  public static boolean isEmpty(final Object[] object) {
    return object == null || object.length == 0;
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
   * @param object
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final CharSequence object) {
    return !isEmpty(object);
  }

  public static boolean isNotEmpty(final Collection<?> object) {
    return !isEmpty(object);
  }

  public static boolean isNotEmpty(final Enumeration<?> object) {
    return !isEmpty(object);
  }

  public static boolean isNotEmpty(final Iterable<?> object) {
    return !isEmpty(object);
  }

  public static boolean isNotEmpty(final Iterator<?> object) {
    return !isEmpty(object);
  }

  public static boolean isNotEmpty(final Map<?, ?> object) {
    return !isEmpty(object);
  }

  public static boolean isNotEmpty(final Object object) {
    return !isEmpty(object);
  }

  public static boolean isNotEmpty(final Object[] object) {
    return !isEmpty(object);
  }

}

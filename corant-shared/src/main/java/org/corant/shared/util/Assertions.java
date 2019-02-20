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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.isBlank;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * Contract assertions
 *
 * @author bingo 下午11:26:29
 *
 */
public class Assertions {

  private Assertions() {}

  /**
   * Throw CorantRuntimeException if given two objects is not equals.
   *
   * @see ObjectUtils#isEquals(Object, Object)
   * @param a
   * @param b shouldBeEquals
   */
  public static void shouldBeEquals(Object a, Object b) {
    shouldBeEquals(a, b, "The objects %s %s should be equal!", asString(a), asString(b));
  }

  /**
   * Throw CorantRuntimeException if given two objects is not equals with given message and message
   * parameters.
   *
   * @param a
   * @param b
   * @param messageOrFormat
   * @param args
   */
  public static void shouldBeEquals(Object a, Object b, String messageOrFormat, Object... args) {
    if (!isEquals(a, b)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  public static void shouldBeFalse(boolean condition) {
    shouldBeFalse(condition, "This shoud be false");
  }

  public static void shouldBeFalse(boolean condition, String messageOrFormat, Object... args) {
    if (condition) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  public static <T> T shouldBeNull(T obj) {
    return shouldBeNull(obj, "The object should be null!");
  }

  public static <T> T shouldBeNull(T obj, String messageOrFormat, Object... args) {
    if (obj != null) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return null;
  }

  public static void shouldBeTrue(boolean condition) {
    shouldBeTrue(condition, "This shoud be true");
  }

  public static void shouldBeTrue(boolean condition, String messageOrFormat, Object... args) {
    if (!condition) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  public static <T extends CharSequence> T shouldNotBlank(T obj) {
    return shouldNotBlank(obj, "The object should not blank!");
  }

  public static <T extends CharSequence> T shouldNotBlank(T obj, String messageOrFormat,
      Object... args) {
    if (isBlank(obj)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return obj;
  }

  public static <T> T shouldNotEmpty(T object) {
    return shouldNotEmpty(object, "This shoud be true");
  }

  public static <T> T shouldNotEmpty(T object, String messageOrFormat, Object... args) {
    if (isEmpty(object)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return object;
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
}

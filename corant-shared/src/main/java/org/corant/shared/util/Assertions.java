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
   * Throw CorantRuntimeException if given two arguments are not equals.
   *
   * @see ObjectUtils#isEquals(Object, Object)
   * @param a
   * @param b shouldBeEquals
   */
  public static void shouldBeEquals(Object a, Object b) {
    shouldBeEquals(a, b, "The objects %s %s should be equal!", asString(a), asString(b));
  }

  /**
   * Throw CorantRuntimeException if given two arguments are not equals with given message and
   * message parameters.
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

  /**
   * Throw CorantRuntimeException if argument or expression is true
   *
   * @param condition shouldBeFalse
   */
  public static void shouldBeFalse(boolean condition) {
    shouldBeFalse(condition, "This shoud be false");
  }

  /**
   * Throw CorantRuntimeException with message and message parameter if argument or expression is
   * true
   *
   * @param condition
   * @param messageOrFormat
   * @param args shouldBeFalse
   */
  public static void shouldBeFalse(boolean condition, String messageOrFormat, Object... args) {
    if (condition) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  /**
   * Throw CorantRuntimeException if argument is not null
   *
   * @param obj
   * @return shouldBeNull
   */
  public static <T> T shouldBeNull(T obj) {
    return shouldBeNull(obj, "The object should be null!");
  }

  /**
   * Throw CorantRuntimeException if argument is not null
   *
   * @param obj the argument that must be null
   * @param messageOrFormat the exception message or message formatter
   * @param args the exception message parameter
   * @return shouldBeNull
   */
  public static <T> T shouldBeNull(T obj, String messageOrFormat, Object... args) {
    if (obj != null) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return null;
  }

  /**
   * Throw CorantRuntimeException if argument is not null
   *
   * @param condition shouldBeTrue
   */
  public static void shouldBeTrue(boolean condition) {
    shouldBeTrue(condition, "This shoud be true");
  }

  /**
   * Throw CorantRuntimeException with message and message parameter if argument or expression is
   * false
   *
   * @param condition the argument or expression
   * @param messageOrFormat the exception message or message formatter
   * @param args the exception message parameter
   */
  public static void shouldBeTrue(boolean condition, String messageOrFormat, Object... args) {
    if (!condition) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  /**
   * Throw CorantRuntimeException if argument is blank.
   *
   * @param obj
   * @see StringUtils#isNotBlank(CharSequence)
   * @return shouldNotBlank
   */
  public static <T extends CharSequence> T shouldNotBlank(T obj) {
    return shouldNotBlank(obj, "The object should not blank!");
  }

  /**
   * Throw CorantRuntimeException with message if argument is blank.
   *
   * @param obj the argument
   * @param messageOrFormat the exception message or message formatter
   * @param args the exception message parameters
   * @return shouldNotBlank
   */
  public static <T extends CharSequence> T shouldNotBlank(T obj, String messageOrFormat,
      Object... args) {
    if (isBlank(obj)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return obj;
  }

  /**
   * Throw CorantRuntimeException if argument is empty, usually the argument type is
   * Map/Collection/CharSequence/Iterable/Iterator/Enumeration/Array.
   *
   * @param obj the argument
   * @return shouldNotEmpty
   */
  public static <T> T shouldNotEmpty(T obj) {
    return shouldNotEmpty(obj, "This shoud be true");
  }

  /**
   * Throw CorantRuntimeException with message if argument is empty, usually the argument type is
   * Map/Collection/CharSequence/Iterable/Iterator/Enumeration/Array.
   *
   * @param obj the argument
   * @param messageOrFormat the exception message or message formatter
   * @param args the exception message parameters
   * @return shouldNotEmpty
   */
  public static <T> T shouldNotEmpty(T obj, String messageOrFormat, Object... args) {
    if (isEmpty(obj)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return obj;
  }

  /**
   * Throw CorantRuntimeException if argument is null
   *
   * @param obj the argument
   * @return shouldNotNull
   */
  public static <T> T shouldNotNull(T obj) {
    return shouldNotNull(obj, "The object should not null!");
  }

  /**
   * Throw CorantRuntimeException with message if argument is null
   *
   * @param obj the argument
   * @param messageOrFormat the exception message or message formatter
   * @param args the exception message parameter
   * @return shouldNotNull
   */
  public static <T> T shouldNotNull(T obj, String messageOrFormat, Object... args) {
    if (obj == null) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return obj;
  }
}

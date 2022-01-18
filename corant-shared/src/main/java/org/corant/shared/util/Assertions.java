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
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Strings.isBlank;
import java.util.function.Supplier;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * <p>
 * Contract assertions tool class, used for validity judgment, if the assertion fails, only throw a
 * runtime exception.
 * </p>
 *
 * @author bingo 下午11:26:29
 *
 */
public class Assertions {

  private Assertions() {}

  /**
   * Throw CorantRuntimeException if given two arguments are not equals.
   *
   * @see Objects#areEqual(Object, Object)
   * @param a an object
   * @param b an object to be compared with {@code a} for equality
   */
  public static void shouldBeEquals(Object a, Object b) {
    shouldBeEquals(a, b, "Objects should be equal!");
  }

  /**
   * Throw CorantRuntimeException if given two arguments are not equals with given message and
   * message parameters.
   *
   * @param a an object
   * @param b an object to be compared with {@code a} for equality
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message arguments
   */
  public static void shouldBeEquals(Object a, Object b, String messageOrFormat, Object... args) {
    if (!areEqual(a, b)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  /**
   * Throw a certain runtime exception if given two arguments are not equals
   *
   * @param a an object
   * @param b an object to be compared with {@code a} for equality
   * @param ex the supplying function that produces an exception to be thrown
   */
  public static void shouldBeEquals(Object a, Object b, Supplier<? extends RuntimeException> ex) {
    if (!areEqual(a, b)) {
      throw ex.get();
    }
  }

  /**
   * Throw CorantRuntimeException if the given argument or expression is true
   *
   * @param condition an argument or expression that should be false
   */
  public static void shouldBeFalse(boolean condition) {
    shouldBeFalse(condition, "Condition or expression should be false!");
  }

  /**
   * Throw CorantRuntimeException with message and message parameter if argument or expression is
   * true
   *
   * @param condition an argument or expression that should be false
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message arguments
   */
  public static void shouldBeFalse(boolean condition, String messageOrFormat, Object... args) {
    if (condition) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  /**
   * Throw a certain runtime exception if argument or expression is true
   *
   * @param condition an argument or expression that should be false
   * @param ex the supplying function that produces an exception to be thrown
   */
  public static void shouldBeFalse(boolean condition, Supplier<? extends RuntimeException> ex) {
    if (condition) {
      throw ex.get();
    }
  }

  /**
   * Throw CorantRuntimeException if the given object is less than or equal to the given comparison.
   *
   * <p>
   * Note: If any of the given objects is null then throw the exceptions.
   *
   * @param <T> the object type
   * @param obj the object to check
   * @param comparison the object of comparison
   * @return the given object
   */
  public static <T extends Comparable<T>> T shouldBeGreater(T obj, T comparison) {
    return shouldBeGreater(obj, comparison,
        "Object should not null and must be greater than the comparison");
  }

  /**
   * Throw CorantRuntimeException if the given object is less than or equal to the given comparison.
   *
   * <p>
   * Note: If any of the given objects is null then throw the exceptions.
   *
   * @param <T> the object type
   * @param obj the object to check
   * @param comparison the object of comparison
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   * @return the given object
   */
  public static <T extends Comparable<T>> T shouldBeGreater(T obj, T comparison,
      String messageOrFormat, Object... args) {
    if (obj != null && comparison != null && obj.compareTo(comparison) > 0) {
      return obj;
    }
    throw new CorantRuntimeException(messageOrFormat, args);
  }

  /**
   * Throw CorantRuntimeException if the given object is greater than or equal to the given
   * comparison.
   *
   * <p>
   * Note: If any of the given objects is null then throw the exceptions.
   *
   * @param <T> the object type
   * @param obj the object to check
   * @param comparison the object of comparison
   * @return the given object
   */
  public static <T extends Comparable<T>> T shouldBeLess(T obj, T comparison) {
    return shouldBeLess(obj, comparison,
        "Object should not null and must be less than the comparison");
  }

  /**
   * Throw CorantRuntimeException if the given object is greater than or equal to the given
   * comparison.
   *
   * <p>
   * Note: If any of the given objects is null then throw the exceptions.
   *
   * @param <T> the object type
   * @param obj the object to check
   * @param comparison the object of comparison
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   * @return the given object
   */
  public static <T extends Comparable<T>> T shouldBeLess(T obj, T comparison,
      String messageOrFormat, Object... args) {
    if (obj != null && comparison != null && obj.compareTo(comparison) < 0) {
      return obj;
    }
    throw new CorantRuntimeException(messageOrFormat, args);
  }

  /**
   * Throw CorantRuntimeException if argument is not null
   *
   * @param obj an object that should be null
   */
  public static void shouldBeNull(Object obj) {
    shouldBeNull(obj, "Object should be null!");
  }

  /**
   * Throw CorantRuntimeException if argument is not null
   *
   * @param obj an object that should be null
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   */
  public static void shouldBeNull(Object obj, String messageOrFormat, Object... args) {
    if (obj != null) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  /**
   * Throw a certain runtime exception if argument is not null
   *
   * @param obj an object that should be null
   * @param ex the supplying function that produces an exception to be thrown
   */
  public static void shouldBeNull(Object obj, Supplier<? extends RuntimeException> ex) {
    if (obj != null) {
      throw ex.get();
    }
  }

  /**
   * Throw CorantRuntimeException if argument is false
   *
   * @param condition an argument or expression that should be true
   */
  public static void shouldBeTrue(boolean condition) {
    shouldBeTrue(condition, "Condition or expression should be true!");
  }

  /**
   * Throw CorantRuntimeException with message and message parameter if given argument or expression
   * is false
   *
   * @param condition an argument or expression that should be true
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   */
  public static void shouldBeTrue(boolean condition, String messageOrFormat, Object... args) {
    if (!condition) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  /**
   * Throw a certain runtime exception if given argument or expression is false
   *
   * @param condition an argument or expression that should be true
   * @param ex the supplying function that produces an exception to be thrown
   */
  public static void shouldBeTrue(boolean condition, Supplier<? extends RuntimeException> ex) {
    if (!condition) {
      throw ex.get();
    }
  }

  /**
   * Throw a certain runtime exception if given object is not assignment-compatible with the object
   * represented by the given Class
   *
   * @param obj the object
   * @param clazz the class
   */
  @SuppressWarnings("unchecked")
  public static <T> T shouldInstanceOf(Object obj, Class<T> clazz) {
    shouldBeTrue(clazz.isInstance(obj), "The object must instanceof %s", clazz);
    return (T) obj;
  }

  /**
   * Throw a certain runtime exception if given object is not assignment-compatible with the object
   * represented by the given Class
   *
   * @param obj the object
   * @param clazz the class
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   */
  @SuppressWarnings("unchecked")
  public static <T> T shouldInstanceOf(Object obj, Class<T> clazz, String messageOrFormat,
      Object... args) {
    shouldBeTrue(clazz.isInstance(obj), messageOrFormat, args);
    return (T) obj;
  }

  /**
   * Throw CorantRuntimeException if one of the given arguments is null.
   *
   * @param args the objects that should none null
   */
  public static void shouldNoneNull(Object... args) {
    for (Object obj : args) {
      shouldNotNull(obj);
    }
  }

  /**
   * Throw CorantRuntimeException if the given object is greater than the given comparison.
   *
   * <p>
   * Note: If any of the given objects is null then throw the exceptions.
   *
   * @param <T> the object type
   * @param obj the object to check
   * @param comparison the object of comparison
   * @return the given object
   */
  public static <T extends Comparable<T>> T shouldNotBeGreater(T obj, T comparison) {
    return shouldNotBeGreater(obj, comparison,
        "Object should not null and must not greater than the comparad object");
  }

  /**
   * Throw CorantRuntimeException if the given object is greater than the given comparison.
   *
   * <p>
   * Note: If any of the given objects is null then throw the exceptions.
   *
   * @param <T> the object type
   * @param obj the object to check
   * @param comparison the object of comparison
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   * @return the given object
   */
  public static <T extends Comparable<T>> T shouldNotBeGreater(T obj, T comparison,
      String messageOrFormat, Object... args) {
    if (obj != null && comparison != null && obj.compareTo(comparison) <= 0) {
      return obj;
    }
    throw new CorantRuntimeException(messageOrFormat, args);
  }

  /**
   * Throw CorantRuntimeException if the given object is less than the given comparison.
   *
   * <p>
   * Note: If any of the given objects is null then throw the exceptions.
   *
   * @param <T> the object type
   * @param obj the object to check
   * @param comparison the object of comparison
   * @return the given object
   */
  public static <T extends Comparable<T>> T shouldNotBeLess(T obj, T comparison) {
    return shouldNotBeLess(obj, comparison,
        "Object should not null and must not less than the comparad object");
  }

  /**
   * Throw CorantRuntimeException if the given object is less than the given comparison.
   *
   * <p>
   * Note: If any of the given objects is null then throw the exceptions.
   *
   * @param <T> the object type
   * @param obj the object to check
   * @param comparison the object of comparison
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   * @return the given object
   */
  public static <T extends Comparable<T>> T shouldNotBeLess(T obj, T comparison,
      String messageOrFormat, Object... args) {
    if (obj != null && comparison != null && obj.compareTo(comparison) >= 0) {
      return obj;
    }
    throw new CorantRuntimeException(messageOrFormat, args);
  }

  /**
   * Throw CorantRuntimeException if the given char sequence is blank.
   *
   * @param <T> the type of the char sequence
   * @param obj the char sequence that should not blank
   * @see Strings#isNotBlank(CharSequence)
   * @return the given char sequence
   */
  public static <T extends CharSequence> T shouldNotBlank(T obj) {
    return shouldNotBlank(obj, "Object should not blank!");
  }

  /**
   * Throw CorantRuntimeException with message if the given char sequence is blank.
   *
   * @param <T> the type of the char sequence
   * @param obj the char sequence that should not blank
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   * @return the given char sequence
   */
  public static <T extends CharSequence> T shouldNotBlank(T obj, String messageOrFormat,
      Object... args) {
    if (isBlank(obj)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return obj;
  }

  /**
   * Throw a certain runtime exception if the given char sequence is blank.
   *
   * @param <T> the type of the char sequence
   * @param obj the char sequence that should not blank
   * @param ex the supplying function that produces an exception to be thrown
   * @return the given char sequence
   */
  public static <T extends CharSequence> T shouldNotBlank(T obj,
      Supplier<? extends RuntimeException> ex) {
    if (isBlank(obj)) {
      throw ex.get();
    }
    return obj;
  }

  /**
   * Throw CorantRuntimeException if the given object is empty or null, usually the type of the
   * given object is Map/Collection/CharSequence/Iterable/Iterator/Enumeration/Array.
   *
   * @param <T> the type of the given object, support
   *        Map/Collection/CharSequence/Iterable/Iterator/Enumeration/Array.
   * @param obj the object that should not empty or null
   * @return the given object
   */
  public static <T> T shouldNotEmpty(T obj) {
    return shouldNotEmpty(obj, "Object should not empty!");
  }

  /**
   * Throw CorantRuntimeException with message if the given object is empty or null, usually the
   * type of the given object Map/Collection/CharSequence/Iterable/Iterator/Enumeration/Array.
   *
   * @param <T> the type of the given object, support
   *        Map/Collection/CharSequence/Iterable/Iterator/Enumeration/Array.
   * @param obj the object that should not empty or null
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   * @return the given object
   */
  public static <T> T shouldNotEmpty(T obj, String messageOrFormat, Object... args) {
    if (isEmpty(obj)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return obj;
  }

  /**
   * Throw a certain runtime exception if the given object is empty or null, usually the type of the
   * given object is Map/Collection/CharSequence/Iterable/Iterator/Enumeration/Array.
   *
   * @param <T> the type of the given object, support
   *        Map/Collection/CharSequence/Iterable/Iterator/Enumeration/Array.
   * @param obj the object that should not empty or null
   * @param ex the supplying function that produces an exception to be thrown
   * @return the given object
   */
  public static <T> T shouldNotEmpty(T obj, Supplier<? extends RuntimeException> ex) {
    if (isEmpty(obj)) {
      throw ex.get();
    }
    return obj;
  }

  /**
   * Throw CorantRuntimeException if the given objects are equal.
   *
   * @param obj the object to check
   * @param other the other to compare
   * @return the given object
   */
  public static void shouldNotEquals(Object obj, Object other) {
    if (Objects.areEqual(obj, other)) {
      throw new CorantRuntimeException("Objects can't equal");
    }
  }

  /**
   * Throw CorantRuntimeException if given two arguments are equals with given message and message
   * parameters.
   *
   * @param a an object
   * @param b an object to be compared with {@code a} for equality
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message arguments
   */
  public static void shouldNotEquals(Object a, Object b, String messageOrFormat, Object... args) {
    if (areEqual(a, b)) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
  }

  /**
   * Throw a certain runtime exception if given two arguments are equals
   *
   * @param a an object
   * @param b an object to be compared with {@code a} for equality
   * @param ex the supplying function that produces an exception to be thrown
   */
  public static void shouldNotEquals(Object a, Object b, Supplier<? extends RuntimeException> ex) {
    if (areEqual(a, b)) {
      throw ex.get();
    }
  }

  /**
   * Throw CorantRuntimeException if the given object is null
   *
   * @param <T> the type of the given object
   * @param obj the object that should not null
   * @return the given object
   */
  public static <T> T shouldNotNull(T obj) {
    return shouldNotNull(obj, "Object should not null!");
  }

  /**
   * Throw CorantRuntimeException with message if the given object is null
   *
   * @param <T> the type of the given object
   * @param obj the object that should not null
   * @param messageOrFormat the exception message or message format, use for exception messaging
   * @param args the exception message parameters
   * @return the given object
   */
  public static <T> T shouldNotNull(T obj, String messageOrFormat, Object... args) {
    if (obj == null) {
      throw new CorantRuntimeException(messageOrFormat, args);
    }
    return obj;
  }

  /**
   * Throw a certain runtime exception if argument is null
   *
   * @param <T> the type of the given object
   * @param obj the object that should not null
   * @param ex the supplying function that produces an exception to be thrown
   * @return the given object
   */
  public static <T> T shouldNotNull(T obj, Supplier<? extends RuntimeException> ex) {
    if (obj == null) {
      throw ex.get();
    }
    return obj;
  }
}

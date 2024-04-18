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
import static org.corant.shared.util.Objects.areDeepEqual;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.contains;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.corant.shared.exception.GeneralRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 2013年6月24日
 */
public class Preconditions {

  private Preconditions() {}

  @SafeVarargs
  public static <T> T requireAllMatch(T obj, Object code, SinglePrecondition<T>... asts) {
    return requireAllMatch(obj, code, () -> Objects.EMPTY_ARRAY, asts);
  }

  @SafeVarargs
  public static <T> T requireAllMatch(T obj, Object code, Supplier<Object[]> mps,
      SinglePrecondition<T>... asts) {
    T r = obj;
    Object[] pms = mps == null ? Objects.EMPTY_ARRAY : mps.get();
    for (SinglePrecondition<T> ast : asts) {
      r = ast.testAndReturn(r, code, pms);
    }
    return r;
  }

  /**
   * Check if the given classes is an inheritance or implementation relationship
   *
   * @param superCls the superclass
   * @param subCls the subclass
   * @param code error message code
   * @param parameters error message parameters
   */
  public static void requireAssignable(Class<?> superCls, Class<?> subCls, Object code,
      Object... parameters) {
    requireNotNull(superCls, code, parameters);
    if (!superCls.isAssignableFrom(subCls)) {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * Returns the given {@code textToCheck} if it contains the given {@code substring}, otherwise
   * throws an exception with the given code and parameters.
   *
   * @param textToCheck the text string to be search
   * @param substring the substring
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireContains(String textToCheck, String substring, Object code,
      Object... parameters) {
    if (!contains(textToCheck, substring)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return textToCheck;
  }

  /**
   * Returns the first given {@code object1} if the given objects are deeply equal to each other and
   * throw an exception with the given code and parameters otherwise.Two null values are deeply
   * equal. If both given objects are arrays, the algorithm in Arrays.deepEquals is used to
   * determine equality.Otherwise, equality is determined by using the equals method of the first
   * given object.
   *
   * @param <T> the object type
   * @param object1 the object to be checked and returned
   * @param object2 the other object to be checked to
   * @param code the exception code or error message key
   * @param parameters the exception parameters
   */
  public static <T> T requireDeepEqual(T object1, T object2, Object code, Object... parameters) {
    if (areDeepEqual(object1, object2)) {
      return object1;
    } else {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * Returns the given {@code collection} if the elements of the given collection are distinct and
   * throw an exception with the given code and parameters otherwise.
   *
   * @param collection the collection to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T> Collection<T> requireDistinctEle(Collection<T> collection, Object code,
      Object... parameters) {
    if (collection instanceof Set) {
      return collection;
    }
    if (collection != null) {
      requireEqual(collection.size(), new HashSet<>(collection).size(), code, parameters);
    }
    return collection;
  }

  /**
   * Returns the first given {@code object1} if the given two objects are equal to each other and
   * throw an exception with the given code and parameters otherwise. If both objects are null, then
   * considered, they are equivalent. If the given two objects are {@link Comparable} and they are
   * not equivalent, the method will use {@link Comparable#compareTo(Object)} to check equivalent.
   *
   * @param object1 the object to be checked
   * @param object2 the object to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  @SuppressWarnings("unchecked")
  public static <T> T requireEqual(T object1, T object2, Object code, Object... parameters) {
    if (areEqual(object1, object2)) {
      return object1;
    } else if ((object1 instanceof Comparable no1 && object2 instanceof Comparable no2)
        && (Objects.compare(no1, no2) == 0)) {
      return object1;
    }
    throw new GeneralRuntimeException(code, parameters);

  }

  /**
   * Requires that the result of a given boolean {@code expression} must be false and throw an
   * exception by the given code and parameters otherwise.
   *
   * @param expression the expression to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  public static void requireFalse(boolean expression, Object code, Object... parameters) {
    if (expression) {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * Requires that the result of a given boolean {@code expression} must be false and throw an
   * exception by the given supplier otherwise.
   *
   * @param expression the expression to be checked
   * @param supplier the exception supplier
   */
  public static void requireFalse(boolean expression,
      Supplier<? extends RuntimeException> supplier) {
    if (expression) {
      throw supplier.get();
    }
  }

  /**
   * Returns the given {@code object} if it satisfies the given {@code predicate} and throw an
   * exception with the given code and parameters otherwise.
   *
   * @param object the object to be checked
   * @param predicate the check expression
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T> T requireFalse(T object, Predicate<? super T> predicate, Object code,
      Object... parameters) {
    if (!predicate.test(object)) {
      return object;
    } else {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * Returns the given {@code object} if it greater than the given {@code compareObject} and throw
   * an exception with the given code and parameters otherwise.
   *
   * @param object the object to be checked
   * @param compareObject the object to be compared
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T extends Comparable<T>> T requireGt(T object, T compareObject, Object code,
      Object... parameters) {
    if (object == null || compareObject == null) {
      throw new GeneralRuntimeException(code, parameters);
    }
    if (object.compareTo(compareObject) <= 0) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * Returns the given {@code object} if it greater than equals the given {@code compareObject} and
   * throw an exception with the given code and parameters otherwise.
   *
   * @param object the object to be checked
   * @param compareObject the object to be compared
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T extends Comparable<T>> T requireGte(T object, T compareObject, Object code,
      Object... parameters) {
    if (areEqual(object, compareObject)) {
      return object;
    }
    if (object == null || compareObject == null) {
      throw new GeneralRuntimeException(code, parameters);
    }
    if (object.compareTo(compareObject) < 0) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * Requires that the given image input stream {@code is} must be conforming to the given image
   * format {@code formatNames} and throw an exception by the given code and parameters otherwise.
   *
   * @param is the image input stream
   * @param code error message code
   * @param parameters error message parameters
   */
  public static void requireImage(InputStream is, String[] formatNames, Object code,
      Object... parameters) {
    requireTrue(Validates.isImage(is, formatNames), code, parameters);
  }

  /**
   * Returns the given {@code object} if it is instanced of the given class {@code klass} and throw
   * an exception with the given code and parameters otherwise.
   *
   * @param object the object to be checked
   * @param klass the required class
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T> T requireInstanceOf(Object object, Class<T> klass, Object code,
      Object... parameters) {
    requireNotNull(klass, code, parameters);
    if (!klass.isInstance(object)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return forceCast(object);
  }

  /**
   * Returns the given {@code ipAddress} if it is an IP v4 address and throw an exception with the
   * given code and parameters otherwise.
   *
   * @param ipAddress the address to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireIp4Address(String ipAddress, Object code, Object... parameters) {
    if (!Validates.isIp4Address(ipAddress)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return ipAddress;
  }

  /**
   * Returns the given {@code string} if its length is between the given minimum and maximum lengths
   * and throw an exception with the given code and parameters otherwise.
   *
   * @param string the string object to be checked
   * @param minLen the required min length
   * @param maxLen the required max length
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireLength(String string, int minLen, int maxLen, Object code,
      Object... parameters) {
    if (!Validates.minMaxLength(string, minLen, maxLen)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return string;
  }

  /**
   * Returns the given {@code object} if it less than the given {@code compareObject} and throw an
   * exception with the given code and parameters otherwise.
   *
   * @param object the object to be checked
   * @param compareObject the object to be compared
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T extends Comparable<T>> T requireLt(T object, T compareObject, Object code,
      Object... parameters) {
    if (object == null || compareObject == null) {
      throw new GeneralRuntimeException(code);
    }
    if (object.compareTo(compareObject) >= 0) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * Returns the given {@code object} if it less than equals the given {@code compareObject} and
   * throw an exception with the given code and parameters otherwise.
   *
   * @param object the object to be checked
   * @param compareObject the object to be compared
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T extends Comparable<T>> T requireLte(T object, T compareObject, Object code,
      Object... parameters) {
    if (areEqual(object, compareObject)) {
      return object;
    }
    if (object == null || compareObject == null) {
      throw new GeneralRuntimeException(code, parameters);
    }
    if (object.compareTo(compareObject) > 0) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * Returns the given {@code ipAddress} if it is a mail address and throw an exception with the
   * given code and parameters otherwise.
   *
   * @param mailAddress the address to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireMailAddress(String mailAddress, Object code, Object... parameters) {
    if (!Validates.isMailAddress(mailAddress)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return mailAddress;
  }

  /**
   * Returns the given {@code string} if it matches the given regular expression {@code pattern} and
   * throw an exception with the given code and parameters otherwise.
   *
   * @param string the string object to be checked
   * @param pattern the checked pattern
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireMatch(String string, String pattern, Object code,
      Object... parameters) {
    if (string == null || pattern == null) {
      throw new GeneralRuntimeException(code, parameters);
    }
    if (!Pattern.compile(pattern).matcher(string).matches()) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return string;
  }

  /**
   * Returns the given {@code string} if its length is less than equals the given {@code maxLen} and
   * throw an exception with the given code and parameters otherwise.
   *
   * @param string the string object to be checked
   * @param maxLen the required max length
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireMaxLength(String string, int maxLen, Object code,
      Object... parameters) {
    if (!Validates.maxLength(string, maxLen)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return string;
  }

  /**
   * Returns the given {@code string} if its length is greater than equals the given {@code minLen}
   * and throw an exception with the given code and parameters otherwise.
   *
   * @param string the string object to be checked
   * @param minLen the required min length
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireMinLength(String string, int minLen, Object code,
      Object... parameters) {
    if (!Validates.minLength(string, minLen)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return string;
  }

  /**
   * Returns the given {@code strings} array if all elements of the string array are not blank and
   * throw an exception with the given code and parameters otherwise.
   *
   * @param code error message code
   * @param parameters error message parameters
   * @param strings the string objects to be checked
   */
  public static String[] requireNoneBlank(String code, Object[] parameters, String... strings) {
    if (strings != null) {
      for (String obj : strings) {
        if (!isNotBlank(obj)) {
          throw new GeneralRuntimeException(code, parameters);
        }
      }
    }
    return strings;
  }

  /**
   * Returns the given {@code strings} array if all elements of the string array are not blank and
   * throw an exception with the given code otherwise.
   *
   * @param code error message code
   * @param strings the string objects to be checked
   */
  public static String[] requireNoneBlank(String code, String... strings) {
    return requireNoneBlank(code, Objects.EMPTY_ARRAY, strings);
  }

  /**
   * Returns the given {@code objects} array if all elements of the object array are not null and
   * throw an exception with the given code otherwise.
   *
   * @param code error message code
   * @param objects the string objects to be checked
   */
  public static Object[] requireNoneNull(String code, Object... objects) {
    return requireNoneNull(code, Objects.EMPTY_ARRAY, objects);
  }

  /**
   * Returns the given {@code objects} array if all elements of the object array are not null and
   * throw an exception with the given code and parameters otherwise.
   *
   * @param code error message code
   * @param parameters error message parameters
   * @param objects the objects to be checked
   */
  public static Object[] requireNoneNull(String code, Object[] parameters, Object... objects) {
    if (objects != null) {
      for (Object obj : objects) {
        if (obj == null) {
          throw new GeneralRuntimeException(code, parameters);
        }
      }
    }
    return objects;
  }

  /**
   * Returns the given {@code string} if it is not blank and throw an exception with the given code
   * otherwise.
   *
   * @param string the string object to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireNotBlank(String string, Object code, Object... parameters) {
    if (isBlank(string)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return string;
  }

  /**
   * Returns the given {@code string} if it is not blank and throw an exception by the given
   * exception supplier.
   *
   * @param string the string object to be checked
   * @param supplier exception supplier
   */
  public static String requireNotBlank(String string,
      Supplier<? extends RuntimeException> supplier) {
    if (isBlank(string)) {
      throw supplier.get();
    }
    return string;
  }

  /**
   * Returns the given {@code textToCheck} if it not contains the given {@code substring}, otherwise
   * throws an exception with the given code and parameters.
   *
   * @param textToCheck the string to be checked
   * @param substring the substring to search
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireNotContains(String textToCheck, String substring, Object code,
      Object... parameters) {
    if (contains(textToCheck, substring)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return textToCheck;
  }

  /**
   * Returns the given {@code collection} if it is not null and not empty and the elements of the
   * given collection are not null, throw an exception with the given code and parameters otherwise.
   *
   * @param collection the collection to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T> Collection<T> requireNotEmptOrNullEle(Collection<T> collection, Object code,
      Object... parameters) {
    if (isEmpty(collection)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    for (Object obj : collection) {
      if (obj == null) {
        throw new GeneralRuntimeException(code, parameters);
      }
    }
    return collection;
  }

  /**
   * Returns the given {@code collection} if it is not null and not empty, throw an exception with
   * the given code and parameters otherwise.
   *
   * @param collection the collection to check
   * @param code the exception code to use if the validation fails
   * @param parameters the exception info params
   * @throws GeneralRuntimeException if the collection is <code>null</code> or has no elements
   */
  public static <C extends Collection<T>, T> C requireNotEmpty(C collection, Object code,
      Object... parameters) {
    if (isEmpty(collection)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return collection;
  }

  /**
   * Returns the given {@code map} if it is not null and not empty, throw an exception with the
   * given code and parameters otherwise.
   *
   * @param map the map to check
   * @param code the exception code to use if the validation fails
   * @param parameters the exception info parameters
   * @throws GeneralRuntimeException if the map is <code>null</code> or has no entries
   */
  public static <K, V> Map<K, V> requireNotEmpty(Map<K, V> map, Object code, Object... parameters) {
    if (isEmpty(map)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return map;
  }

  /**
   * Returns the given {@code array} if it is not null and not empty, throw an exception with the
   * given code and parameters otherwise.
   *
   * @param array the array to check
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   * @throws GeneralRuntimeException if the object array is <code>null</code> or has no elements
   */
  public static <T> T[] requireNotEmpty(T[] array, Object code, Object... parameters) {
    if (isEmpty(array)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return array;
  }

  /**
   * Returns the given {@code object} if it is not null, throw an exception with the given code and
   * parameters otherwise.
   *
   * @param object the object to be checked
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   */
  public static <T> T requireNotNull(T object, Object code, Object... parameters) {
    if (object == null) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * Returns the given {@code object} if it is not null, throw an exception by the given supplier
   * otherwise.
   *
   * @param object the object to be checked
   * @param supplier the exception supplier
   */
  public static <T> T requireNotNull(T object, Object code,
      Supplier<? extends RuntimeException> supplier) {
    if (object == null) {
      throw supplier.get();
    }
    return object;
  }

  /**
   * Requires that the given left and right must be equals and throw an exception by the given code
   * and parameters otherwise.
   *
   * @param left the object to be checked
   * @param right another object to be checked
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   */
  public static void requireNotNullAndEq(Object left, Object right, Object code,
      Object... parameters) {
    requireNotNull(right, code, parameters);
    if (!left.equals(right)) {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * Returns the given {@code array} if it is null or the elements of the given collection are not
   * null, throw an exception with the given code and parameters otherwise.
   *
   * @param array the array to check
   * @param code the exception code to use if the validation fails
   * @param parameters the exception info params
   * @throws GeneralRuntimeException if the object array contains a <code>null</code> element
   */
  public static <T> T[] requireNotNullEle(T[] array, Object code, Object... parameters) {
    if (array != null) {
      for (T element : array) {
        if (element == null) {
          throw new GeneralRuntimeException(code, parameters);
        }
      }
    }
    return array;
  }

  /**
   * Returns the given {@code object} if it is null, throw an exception with the given code and
   * parameters otherwise.
   *
   * @param object the object to be checked
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   */
  public static void requireNull(Object object, Object code, Object... parameters) {
    if (object != null) {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * Requires that the result of a given boolean {@code expression} must be true and throw an
   * exception by the given code and parameters otherwise.
   *
   * @param expression the expression to be checked
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   */
  public static void requireTrue(boolean expression, Object code, Object... parameters) {
    if (!expression) {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * Requires that the result of a given boolean {@code expression} must be false and throw an
   * exception by the given supplier.
   *
   * @param expression the expression to be checked
   * @param supplier the exception supplier
   */
  public static void requireTrue(boolean expression,
      Supplier<? extends RuntimeException> supplier) {
    if (!expression) {
      throw supplier.get();
    }
  }

  /**
   * Returns the given {@code object} if it satisfies the given {@code predicate} and throw an
   * exception with the given code and parameters otherwise.
   *
   * @param object the object to be checked
   * @param predicate the check expression
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   */
  public static <T> T requireTrue(T object, Predicate<? super T> predicate, Object code,
      Object... parameters) {
    if (predicate.test(object)) {
      return object;
    } else {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * Returns the given {@code mobileNumber} if it satisfies the China mobile number rule and throw
   * an exception with the given code and parameters otherwise.
   *
   * @param mobileNumber the China mobile number to be checked
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   */
  public static String requireZhMobileNumber(String mobileNumber, Object code,
      Object... parameters) {
    if (!Validates.isZhMobileNumber(mobileNumber)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return mobileNumber;
  }

  /**
   * Returns the given {@code phoneNumber} if it satisfies the China phone number rule and throw an
   * exception with the given code and parameters otherwise.
   *
   * @param phoneNumber the China phone number to be checked
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   */
  public static String requireZhPhoneNumber(String phoneNumber, Object code, Object... parameters) {
    if (!Validates.isZhPhoneNumber(phoneNumber)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return phoneNumber;
  }

  @FunctionalInterface
  public interface BiPrecondition<T> {
    T testAndReturn(T obj, T x, Object code, Object... variants);
  }

  @FunctionalInterface
  public interface BoolPrecondition<T> {
    T testAndReturn(T obj, Predicate<? super T> p, Object code, Object... variants);
  }

  @FunctionalInterface
  public interface ComparablePrecondition<T extends Comparable<T>> {
    T testAndReturn(T obj, T cmprObj, Object code, Object... mps);
  }

  @FunctionalInterface
  public interface SinglePrecondition<T> {
    T testAndReturn(T obj, Object code, Object... variants);
  }
}

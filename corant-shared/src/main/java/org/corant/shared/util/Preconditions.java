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
   * 必须是继承或实现关系
   *
   * @param superCls the super class
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
   * 必须包含某个字符
   *
   * @param textToSearch the text string to be search
   * @param substring the substring
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireContain(String textToSearch, String substring, Object code,
      Object... parameters) {
    if (!contains(textToSearch, substring)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return textToSearch;
  }

  public static <T> T requireDeepEqual(T obj1, T obj2, Object code, Object... parameters) {
    if (areDeepEqual(obj1, obj2)) {
      return obj1;
    } else {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * 不能有相同的元素
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
   * 必须相同
   *
   * @param obj1 the object to be checked
   * @param obj2 the object to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T> T requireEqual(T obj1, T obj2, Object code, Object... parameters) {
    if (areEqual(obj1, obj2)) {
      return obj1;
    } else {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * 参数必须为false
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
   * 参数必须为true
   *
   * @param obj the object to be checked
   * @param p the check expression
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T> T requireFalse(T obj, Predicate<? super T> p, Object code,
      Object... parameters) {
    if (!p.test(obj)) {
      return obj;
    } else {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * 必须大于或等于某个值
   *
   * @param obj the object to be checked
   * @param compareObj the object to be compared
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T extends Comparable<T>> T requireGaet(T obj, T compareObj, Object code,
      Object... parameters) {
    if (areEqual(obj, compareObj)) {
      return obj;
    }
    if (obj == null || compareObj == null) {
      throw new GeneralRuntimeException(code, parameters);
    }
    if (obj.compareTo(compareObj) < 0) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return obj;
  }

  /**
   * 必须大于某个值
   *
   * @param obj the object to be checked
   * @param compareObj the object to be compared
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T extends Comparable<T>> T requireGt(T obj, T compareObj, Object code,
      Object... parameters) {
    if (obj == null || compareObj == null) {
      throw new GeneralRuntimeException(code, parameters);
    }
    if (obj.compareTo(compareObj) <= 0) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return obj;
  }

  /**
   * 输入必须是图片流
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
   * 必须是某个类型的对象
   *
   * @param cls the required class
   * @param obj the object to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T> T requireInstanceOf(Class<T> cls, Object obj, Object code,
      Object... parameters) {
    requireNotNull(cls, code, parameters);
    if (!cls.isInstance(obj)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return forceCast(obj);
  }

  /**
   * ip地址校验
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
   * 必须小于或等于某个值
   *
   * @param obj the object to be checked
   * @param compareObj the object to be compared
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T extends Comparable<T>> T requireLaet(T obj, T compareObj, Object code,
      Object... parameters) {
    if (areEqual(obj, compareObj)) {
      return obj;
    }
    if (obj == null || compareObj == null) {
      throw new GeneralRuntimeException(code, parameters);
    }
    if (obj.compareTo(compareObj) > 0) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return obj;
  }

  /**
   * 字符串长度必须小于 MaxLen 且大于MinLen
   *
   * @param object the string object to be checked
   * @param minLen the required min length
   * @param maxLen the required max length
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireLength(String object, int minLen, int maxLen, Object code,
      Object... parameters) {
    if (!Validates.minMaxLength(object, minLen, maxLen)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * 必须小于某个值
   *
   * @param obj the object to be checked
   * @param compareObj the object to be compared
   * @param code error message code
   * @param parameters error message parameters
   */
  public static <T extends Comparable<T>> T requireLt(T obj, T compareObj, Object code,
      Object... parameters) {
    if (obj == null || compareObj == null) {
      throw new GeneralRuntimeException(code);
    }
    if (obj.compareTo(compareObj) >= 0) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return obj;
  }

  /**
   * 邮件地址校验
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
   * 必须满足正则表达式
   *
   * @param object the string object to be checked
   * @param pattern the checked pattern
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireMatch(String object, String pattern, Object code,
      Object... parameters) {
    if (object == null || pattern == null) {
      throw new GeneralRuntimeException(code, parameters);
    }
    if (!Pattern.compile(pattern).matcher(object).matches()) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * 字符串长度必须小于 len
   *
   * @param object the string object to be checked
   * @param maxLen the required max length
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireMaxLength(String object, int maxLen, Object code,
      Object... parameters) {
    if (!Validates.maxLength(object, maxLen)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * 字符串长度必须大于 len
   *
   * @param object the string object to be checked
   * @param minLen the required min length
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireMinLength(String object, int minLen, Object code,
      Object... parameters) {
    if (!Validates.minLength(object, minLen)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * 必须全部满足包含非空格字符
   *
   * @param code error message code
   * @param parameters error message parameters
   * @param objects the string objects to be checked
   */
  public static String[] requireNoneBlank(String code, Object[] parameters, String... objects) {
    if (objects != null) {
      for (String obj : objects) {
        if (!isNotBlank(obj)) {
          throw new GeneralRuntimeException(code, parameters);
        }
      }
    }
    return objects;
  }

  /**
   * 必须全部满足包含非空格字符
   *
   * @param code error message code
   * @param objects the string objects to be checked
   */
  public static String[] requireNoneBlank(String code, String... objects) {
    return requireNoneBlank(code, Objects.EMPTY_ARRAY, objects);
  }

  /**
   * 全部不能为空
   *
   * @param code error message code
   * @param objects the string objects to be checked
   */
  public static Object[] requireNoneNull(String code, Object... objects) {
    return requireNoneNull(code, Objects.EMPTY_ARRAY, objects);
  }

  /**
   * 全部不能为空
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
   * 字符串必须有包含非空格字符
   *
   * @param object the string object to be checked
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireNotBlank(String object, Object code, Object... parameters) {
    if (isBlank(object)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return object;
  }

  /**
   * 不能包含某个字符
   *
   * @param textToSearch the string to be checked
   * @param substring the substring to search
   * @param code error message code
   * @param parameters error message parameters
   */
  public static String requireNotContain(String textToSearch, String substring, Object code,
      Object... parameters) {
    if (contains(textToSearch, substring)) {
      throw new GeneralRuntimeException(code, parameters);
    }
    return textToSearch;
  }

  /**
   * 不能为空且元素必须不能为空
   *
   * @param collection the collection to be che
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
   * 集合不能为空，且必须至少包含一个元素
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
   * Map不能为空，且必须至少包含一个分录
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
   * 数组不能为空，且必须至少包含一个元素
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
   * 必须不能有空元素，用于map或collection
   *
   * @param bag the Map/Collection object to be checked
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> T requireNotEmptyBag(T bag, Object code, Object... parameters) {
    if (bag instanceof Map) {
      return (T) requireNotEmpty((Map) bag, code, parameters);
    } else if (bag instanceof Collection) {
      return (T) requireNotEmpty((Collection) bag, code, parameters);
    } else {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * 参数不能为null
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
   * 不能为空且必须相同
   *
   * @param left the object to be checked
   * @param right the another object to be checked
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
   * 数组中的元素不能为空
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
   * 参数只能为null
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
   * 参数必须为true
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

  public static <T> T requireTrue(T obj, Predicate<? super T> p, Object code) {
    if (p.test(obj)) {
      return obj;
    } else {
      throw new GeneralRuntimeException(code);
    }
  }

  /**
   * 参数必须为true
   *
   * @param obj the object to be checked
   * @param p the check expression
   * @param code the exception code to use if the validation fails
   * @param parameters the exception parameters
   */
  public static <T> T requireTrue(T obj, Predicate<? super T> p, Object code,
      Object... parameters) {
    if (p.test(obj)) {
      return obj;
    } else {
      throw new GeneralRuntimeException(code, parameters);
    }
  }

  /**
   * 手机号码校验
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
   * 电话号码校验 正确格式 012-87654321、0123-87654321、0123－7654321
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

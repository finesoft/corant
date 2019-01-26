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

import static org.corant.shared.util.CollectionUtils.asList;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.ObjectUtils.shouldBeFalse;
import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author bingo 下午10:08:22
 *
 */
public class MethodUtils {

  private MethodUtils() {
    super();
  }

  public static Method getMatchingMethod(final Class<?> cls, final String methodName,
      final Class<?>... parameterTypes) {
    shouldNotNull(cls, "Null class not allowed.");
    shouldBeFalse(isEmpty(methodName), "Null or blank methodName not allowed.");
    List<Method> methods = asList(cls.getDeclaredMethods());
    ClassUtils.getAllSuperClasses(cls).stream().map(Class::getDeclaredMethods)
        .map(CollectionUtils::asList).forEach(methods::addAll);
    Method inexactMatch = null;
    for (final Method method : methods) {
      if (methodName.equals(method.getName())
          && Objects.deepEquals(parameterTypes, method.getParameterTypes())) {
        return method;
      } else if (methodName.equals(method.getName())
          && isParameterTypesMatching(parameterTypes, method.getParameterTypes(), true)) {
        if (inexactMatch == null) {
          inexactMatch = method;
        } else if (distance(parameterTypes, method.getParameterTypes()) < distance(parameterTypes,
            inexactMatch.getParameterTypes())) {
          inexactMatch = method;
        }
      }
    }
    return inexactMatch;
  }

  public static boolean isParameterTypesMatching(Class<?>[] classArray, Class<?>[] toClassArray,
      final boolean autoboxing) {
    Class<?>[] useClsArr = classArray == null ? new Class<?>[0] : classArray;
    Class<?>[] useToClsArr = toClassArray == null ? new Class<?>[0] : toClassArray;
    if (useClsArr.length != useToClsArr.length) {
      return false;
    }
    for (int i = 0; i < useClsArr.length; i++) {
      if (!ClassUtils.isAssignable(useClsArr[i], useToClsArr[i], autoboxing)) {
        return false;
      }
    }
    return true;
  }

  public static MethodSignature signature(Method method) {
    return MethodSignature.of(method);
  }

  public static void traverseLocalMethods(Class<?> clazz, Function<Method, Boolean> visitor) {
    if (clazz != null) {
      for (Method method : clazz.getMethods()) {
        if (!visitor.apply(method)) {
          break;
        }
      }
    }
  }

  public static void traverseMethods(Class<?> clazz, Consumer<Method> visitor) {
    if (clazz != null) {
      for (Method method : clazz.getMethods()) {
        visitor.accept(method);
      }
      if (clazz.getSuperclass() != null) {
        traverseMethods(clazz.getSuperclass(), visitor);
      } else if (clazz.isInterface()) {
        for (Class<?> superIfc : clazz.getInterfaces()) {
          traverseMethods(superIfc, visitor);
        }
      }
    }
  }

  static int distance(final Class<?>[] classArray, final Class<?>[] toClassArray) {
    int answer = 0;
    if (!isParameterTypesMatching(classArray, toClassArray, true)) {
      return -1;
    }
    for (int offset = 0; offset < classArray.length; offset++) {
      // Note InheritanceUtils.distance() uses different scoring system.
      if (classArray[offset].equals(toClassArray[offset])) {
        continue;
      } else if (ClassUtils.isAssignable(classArray[offset], toClassArray[offset], true)
          && !ClassUtils.isAssignable(classArray[offset], toClassArray[offset], false)) {
        answer++;
      } else {
        answer = answer + 2;
      }
    }
    return answer;
  }

  public static class MethodSignature implements Serializable {

    private static final long serialVersionUID = 2424253135982857193L;

    private final String methodName;

    private final String[] parameterTypes;

    public MethodSignature(Method method) {
      methodName = method.getName();
      parameterTypes = new String[method.getParameterTypes().length];
      for (int i = 0; i < method.getParameterTypes().length; i++) {
        parameterTypes[i] = method.getParameterTypes()[i].getName();
      }
    }

    public MethodSignature(String methodName, String... parameterTypes) {
      this.methodName = methodName;
      this.parameterTypes = parameterTypes;
    }

    public static MethodSignature of(Method method) {
      return new MethodSignature(method);
    }

    public static MethodSignature of(String methodName, String... parameterTypes) {
      return new MethodSignature(methodName, parameterTypes);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof MethodSignature)) {
        return false;
      }
      MethodSignature other = (MethodSignature) obj;
      if (methodName == null) {
        if (other.methodName != null) {
          return false;
        }
      } else if (!methodName.equals(other.methodName)) {
        return false;
      }
      return Arrays.equals(parameterTypes, other.parameterTypes);
    }

    public String getMethodName() {
      return methodName;
    }

    public String[] getParameterTypes() {
      return Arrays.copyOf(parameterTypes, parameterTypes.length);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + methodName.hashCode();
      result = prime * result + Arrays.hashCode(parameterTypes);
      return result;
    }

    public boolean matches(Method method) {
      if (!methodName.equals(method.getName())) {
        return false;
      }
      final Class<?>[] methodParameterTypes = method.getParameterTypes();
      if (methodParameterTypes.length != parameterTypes.length) {
        return false;
      }
      for (int i = 0; i < parameterTypes.length; i++) {
        if (!parameterTypes[i].equals(methodParameterTypes[i].getName())) {
          return false;
        }
      }
      return true;
    }

    @Override
    public String toString() {
      return new StringBuffer().append("method ").append(getMethodName())
          .append(Arrays.toString(parameterTypes).replace('[', '(').replace(']', ')')).toString();
    }

  }
}

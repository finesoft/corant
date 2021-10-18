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

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Lists.listOf;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * corant-shared
 *
 * @author bingo 下午10:08:22
 */
public class Methods {

  private static final Pattern SETTER_PTN = Pattern.compile("^set[A-Z].*");
  private static final Pattern GETTER_PTN = Pattern.compile("^get[A-Z].*");
  private static final Pattern IS_PTN = Pattern.compile("^is[A-Z].*");

  private Methods() {}

  public static Method getMatchingMethod(final Class<?> cls, final String methodName,
      final Class<?>... parameterTypes) {
    shouldBeFalse(isEmpty(methodName), "Null or blank methodName not allowed.");
    List<Method> methods = getAllMethods(shouldNotNull(cls, "Null class not allowed."));
    Method inexactMatch = null;
    for (final Method method : methods) {
      if (methodName.equals(method.getName())
          && Objects.deepEquals(parameterTypes, method.getParameterTypes())) {
        return method;
      } else if (methodName.equals(method.getName())
          && isParameterTypesMatching(parameterTypes, method.getParameterTypes(), true)
          && (inexactMatch == null
              || distance(parameterTypes, method.getParameterTypes()) < distance(parameterTypes,
                  inexactMatch.getParameterTypes()))) {
        inexactMatch = method;
      }
    }
    methods.clear();
    return inexactMatch;
  }

  public static boolean isGetter(Method method) {
    if (Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 0) {
      if (GETTER_PTN.matcher(method.getName()).matches()
          && !method.getReturnType().equals(void.class)) {
        return true;
      }
      return IS_PTN.matcher(method.getName()).matches()
          && method.getReturnType().equals(boolean.class);
    }
    return false;
  }

  public static boolean isParameterTypesMatching(Class<?>[] classArray, Class<?>[] toClassArray,
      final boolean autoboxing) {
    Class<?>[] useClsArr = classArray == null ? Classes.EMPTY_ARRAY : classArray;
    Class<?>[] useToClsArr = toClassArray == null ? Classes.EMPTY_ARRAY : toClassArray;
    if (useClsArr.length != useToClsArr.length) {
      return false;
    }
    for (int i = 0; i < useClsArr.length; i++) {
      if (!Classes.isAssignable(useClsArr[i], useToClsArr[i], autoboxing)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isSetter(Method method) {
    return Modifier.isPublic(method.getModifiers()) && method.getReturnType().equals(void.class)
        && method.getParameterTypes().length == 1 && SETTER_PTN.matcher(method.getName()).matches();
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

  /**
   * Code base from from apache.org
   * <p>
   * Returns the aggregate number of inheritance hops between assignable argument class types.
   * Returns -1 if the arguments aren't assignable. Fills a specific purpose for getMatchingMethod
   * and is not generalized.
   * </p>
   *
   * @param classArray
   * @param toClassArray
   * @return the aggregate number of inheritance hops between assignable argument class types.
   *
   */
  static int distance(final Class<?>[] classArray, final Class<?>[] toClassArray) {
    int answer = 0;
    if (!isParameterTypesMatching(classArray, toClassArray, true)) {
      return -1;
    }
    for (int offset = 0; offset < classArray.length; offset++) {
      if (classArray[offset].equals(toClassArray[offset])) {
        continue;
      } else if (Classes.isAssignable(classArray[offset], toClassArray[offset], true)
          && !Classes.isAssignable(classArray[offset], toClassArray[offset], false)) {
        answer++;
      } else {
        answer = answer + 2;
      }
    }
    return answer;
  }

  static List<Method> getAllMethods(Class<?> cls) {
    List<Method> methods = listOf(cls.getDeclaredMethods());
    Classes.getAllSuperClasses(cls).stream().map(Class::getDeclaredMethods)
        .forEach(ms -> Collections.addAll(methods, ms));
    Classes.getAllInterfaces(cls).stream().map(Class::getDeclaredMethods).flatMap(Arrays::stream)
        .forEach(im -> {
          if (im.isDefault() && methods.stream().noneMatch(m -> m.getName().equals(im.getName())
              && isParameterTypesMatching(m.getParameterTypes(), im.getParameterTypes(), true))) {
            methods.add(im);
          }
        });
    return methods;
  }

  /**
   * corant-shared
   *
   * @author bingo 下午5:14:56
   *
   */
  public static class InvokerBuilder {
    Class<?> clazz;
    String methodName;
    Class<?>[] parameterTypes;
    Object object;
    boolean forceAccess;

    private InvokerBuilder(Class<?> clazz) {
      this.clazz = clazz;
    }

    private InvokerBuilder(Object object) {
      clazz = shouldNotNull(object).getClass();
      this.object = object;
    }

    public static InvokerBuilder from(Object object) {
      return new InvokerBuilder(object);
    }

    public static InvokerBuilder of(Class<?> clazz) {
      return new InvokerBuilder(clazz);
    }

    public static InvokerBuilder of(String className) {
      return of(tryAsClass(className));
    }

    public InvokerBuilder forceAccess(boolean forceAccess) {
      this.forceAccess = forceAccess;
      return this;
    }

    public Object invoke(Object... parameters)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      Method method = getMatchingMethod(clazz, methodName, parameterTypes);
      if (forceAccess) {
        AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
          method.setAccessible(true);
          return null;
        });
      }
      return method.invoke(object, parameters);
    }

    public InvokerBuilder methodName(String methodName) {
      this.methodName = methodName;
      return this;
    }

    public InvokerBuilder on(Object object) {
      this.object = object;
      return this;
    }

    public InvokerBuilder parameterTypes(Class<?>... parameterTypes) {
      this.parameterTypes = parameterTypes;
      return this;
    }

    public Object tryInvoke(Object... parameters) {
      try {
        return invoke(parameters);
      } catch (Exception e) {
        return null;
      }
    }
  }

  public static class MethodSignature implements Serializable {

    private static final long serialVersionUID = 2424253135982857193L;

    private final String methodName;

    private final String[] parameterTypes;

    public MethodSignature(Method method) {
      methodName = method.getName();
      Class<?>[] methodParameterTypes = method.getParameterTypes();
      parameterTypes = new String[methodParameterTypes.length];
      for (int i = 0; i < methodParameterTypes.length; i++) {
        parameterTypes[i] = methodParameterTypes[i].getName();
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
      return new StringBuilder().append("method ").append(getMethodName())
          .append(Arrays.toString(parameterTypes).replace('[', '(').replace(']', ')')).toString();
    }

  }
}

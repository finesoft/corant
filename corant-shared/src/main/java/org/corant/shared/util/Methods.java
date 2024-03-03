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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    shouldNotNull(cls, "Null class not allowed.");
    shouldBeTrue(isNotBlank(methodName), "Null or blank methodName not allowed.");
    try {
      return cls.getMethod(methodName, parameterTypes);
    } catch (NoSuchMethodException e) {
      // ignore no ops
    }
    List<Method> methods = getAllMethods(cls, methodName);
    for (final Method method : methods) {
      Class<?>[] methodParamTypes = resolveVarArgTypes(parameterTypes, method);
      if (methodParamTypes != null && Arrays.deepEquals(methodParamTypes, parameterTypes)) {
        return method;
      }
    }

    Method inexactMatch = null;
    for (final Method method : methods) {
      if (isParameterTypesMatching(parameterTypes, method.getParameterTypes(), true,
          method.isVarArgs())
          && (inexactMatch == null
              || distance(parameterTypes, method) < distance(parameterTypes, inexactMatch))) {
        inexactMatch = method;
      }
    }
    methods.clear();
    return inexactMatch;
  }

  public static Object invokeMethod(Object instance, Method method, Object[] params)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    shouldNotNull(method, "The method to be invoked can't null");
    shouldNotNull(params, "The params to be invoked can't null");
    if (method.isVarArgs()) {
      final Class<?>[] methodParamTypes = method.getParameterTypes();
      final int normalMethodParamCount = methodParamTypes.length - 1;
      if (params.length < normalMethodParamCount) {
        throw new IllegalArgumentException(
            "Invoke method error, the given method parameter is illegal!");
      }
      final Class<?> varMethodParamType = methodParamTypes[normalMethodParamCount].componentType();
      final Object[] newParam = new Object[methodParamTypes.length];
      if (params.length < methodParamTypes.length) {
        System.arraycopy(params, 0, newParam, 0, params.length);
        newParam[normalMethodParamCount] = Array.newInstance(varMethodParamType, 0);
      } else {
        System.arraycopy(params, 0, newParam, 0, normalMethodParamCount);
        int varParamsLen = params.length - normalMethodParamCount;
        Object varParams = Array.newInstance(varMethodParamType, varParamsLen);
        System.arraycopy(params, normalMethodParamCount, varParams, 0, varParamsLen);
        newParam[normalMethodParamCount] = varParams;
      }
      return method.invoke(instance, newParam);
    } else {
      return method.invoke(instance, params);
    }
  }

  public static InvokerBuilder invokerBuilder(Class<?> clazz) {
    return new InvokerBuilder(clazz);
  }

  public static InvokerBuilder invokerBuilder(Object object) {
    return new InvokerBuilder(object);
  }

  public static InvokerBuilder invokerBuilder(String className) {
    return new InvokerBuilder(tryAsClass(className));
  }

  public static boolean isGetter(Method method) {
    if (Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 0) {
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
      boolean autoboxing, boolean varArgs) {
    if (varArgs) {
      int normalParamLength = toClassArray.length - 1;
      if (classArray.length < normalParamLength) {
        return false;
      }
      int i;
      for (i = 0; i < normalParamLength && i < classArray.length; i++) {
        if (!Classes.isAssignable(classArray[i], toClassArray[i], true)) {
          return false;
        }
      }
      final Class<?> varArgParameterType = toClassArray[normalParamLength].getComponentType();
      for (; i < classArray.length; i++) {
        if (!Classes.isAssignable(classArray[i], varArgParameterType, true)) {
          return false;
        }
      }
      return true;
    }
    return Classes.isAssignable(classArray, toClassArray, autoboxing);
  }

  public static boolean isSetter(Method method) {
    return Modifier.isPublic(method.getModifiers()) && method.getReturnType().equals(void.class)
        && method.getParameterCount() == 1 && SETTER_PTN.matcher(method.getName()).matches();
  }

  public static MethodSignature signature(Method method) {
    return MethodSignature.of(method);
  }

  public static void traverseLocalMethods(Class<?> clazz, Function<Method, Boolean> visitor) {
    if (clazz != null) {
      for (Method method : clazz.getDeclaredMethods()) {
        if (!visitor.apply(method)) {
          break;
        }
      }
    }
  }

  public static void traverseMethods(Class<?> clazz, Consumer<Method> visitor) {
    if (clazz != null) {
      for (Method method : clazz.getDeclaredMethods()) {
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
   * Code base from apache.org
   * <p>
   * Returns the aggregate number of inheritance hops between assignable argument class types.
   * Returns -1 if the arguments aren't assignable. Fills a specific purpose for getMatchingMethod
   * and is not generalized.
   * </p>
   *
   * @param classArray the Class array to calculate the distance from.
   * @param toClassArray the Class array to calculate the distance to.
   * @return the aggregate number of inheritance hops between assignable argument class types.
   *
   */
  static int distance(final Class<?>[] classArray, final Class<?>[] toClassArray) {
    int answer = 0;
    if (!Classes.isAssignable(classArray, toClassArray, true)) {
      return -1;
    }
    for (int offset = 0; offset < classArray.length; offset++) {
      final Class<?> aClass = classArray[offset];
      final Class<?> toClass = toClassArray[offset];
      if (aClass == null || aClass.equals(toClass)) {
        continue;
      } else if (Classes.isAssignable(aClass, toClass, true)
          && !Classes.isAssignable(aClass, toClass, false)) {
        answer++;
      } else {
        answer = answer + 2;
      }
    }

    return answer;
  }

  /**
   * Returns the aggregate number of inheritance hops between assignable argument class types.
   * Returns -1 if the arguments aren't assignable. Fills a specific purpose for getMatchingMethod
   * and is not generalized.
   *
   * @param classArray the Class array to calculate the distance from.
   * @param method the method to calculate the parameter types distance to.
   * @return the aggregate number of inheritance hops between assignable argument class types.
   */
  static int distance(final Class<?>[] classArray, final Method method) {
    Class<?>[] toClassArray = resolveVarArgTypes(classArray, method);
    if (toClassArray == null) {
      return -1;
    }
    return distance(classArray, toClassArray);
  }

  static List<Method> getAllMethods(Class<?> cls, String name) {
    List<Method> methods = Arrays.stream(cls.getDeclaredMethods())
        .filter(m -> m.getName().equals(name)).collect(Collectors.toList());
    Classes.getAllSuperClasses(cls).stream().map(Class::getDeclaredMethods).flatMap(Arrays::stream)
        .filter(m -> m.getName().equals(name)).forEach(methods::add);
    Classes.getAllInterfaces(cls).stream().map(Class::getDeclaredMethods).flatMap(Arrays::stream)
        .filter(m -> m.getName().equals(name)).forEach(im -> {
          if (im.isDefault() && methods.stream()
              .noneMatch(m -> m.getName().equals(im.getName()) && isParameterTypesMatching(
                  m.getParameterTypes(), im.getParameterTypes(), true, false))) {
            methods.add(im);
          }
        });
    return methods;
  }

  static Class<?>[] resolveVarArgTypes(final Class<?>[] classArray, final Method method) {
    Class<?>[] toClassArray = method.getParameterTypes();
    if (method.isVarArgs()) {
      int normalMethodParamLength = toClassArray.length - 1;
      if (classArray.length < normalMethodParamLength) {
        return null;
      } else if (classArray.length == normalMethodParamLength) {
        toClassArray = Arrays.copyOf(toClassArray, normalMethodParamLength);
      } else {
        Class<?> componentType = toClassArray[normalMethodParamLength].getComponentType();
        Class<?>[] temp = new Class<?>[classArray.length];
        int i = normalMethodParamLength;
        System.arraycopy(toClassArray, 0, temp, 0, i);
        for (; i < classArray.length; i++) {
          temp[i] = componentType;
        }
        toClassArray = temp;
      }
    }
    return toClassArray;
  }

  /**
   * corant-shared
   *
   * @author bingo 下午5:14:56
   *
   */
  public static class InvokerBuilder {
    protected final Class<?> clazz;
    protected final Object object;
    protected String methodName;
    protected Class<?>[] parameterTypes;
    protected boolean forceAccess;
    protected volatile Method method;

    protected InvokerBuilder(Class<?> clazz) {
      this.clazz = clazz;
      object = null;
    }

    protected InvokerBuilder(Object object) {
      clazz = shouldNotNull(object).getClass();
      this.object = object;
    }

    public InvokerBuilder forceAccess(boolean forceAccess) {
      this.forceAccess = forceAccess;
      return this;
    }

    public <T> T invoke(Object... parameters)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      return invoke(shouldNotNull(object), parameters);
    }

    @SuppressWarnings("unchecked")
    public <T> T invoke(Object instance, Object[] parameters)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      resolveMethod();
      if (forceAccess) {
        method.setAccessible(true);
      }
      return (T) invokeMethod(instance, method, parameters);
    }

    public InvokerBuilder methodName(String methodName) {
      this.methodName = methodName;
      return this;
    }

    public InvokerBuilder parameterTypes(Class<?>... parameterTypes) {
      if (parameterTypes.length == 0) {
        this.parameterTypes = Classes.EMPTY_ARRAY;
      } else {
        this.parameterTypes = parameterTypes.clone();
      }
      return this;
    }

    public <T> T tryInvoke(Object... parameters) {
      try {
        return invoke(parameters);
      } catch (Exception e) {
        return null;
      }
    }

    public <T> T tryInvoke(Object instance, Object... parameters) {
      try {
        return invoke(instance, parameters);
      } catch (Exception e) {
        return null;
      }
    }

    protected void resolveMethod() {
      if (method == null) {
        synchronized (this) {
          if (method == null) {
            method = shouldNotNull(getMatchingMethod(clazz, methodName, parameterTypes),
                "Can't find the method!");
          }
        }
      }
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 下午5:20:37
   *
   */
  public static class MethodSignature implements Serializable {

    private static final long serialVersionUID = 2424253135982857193L;

    protected final String methodName;

    protected final String[] parameterTypes;

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
      this.parameterTypes = parameterTypes.clone();
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
      if (!(obj instanceof MethodSignature other)) {
        return false;
      }
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
      return prime * result + Arrays.hashCode(parameterTypes);
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
      return "method " + methodName
          + Arrays.toString(parameterTypes).replace('[', '(').replace(']', ')');
    }

  }

}

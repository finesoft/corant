/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.shared.util;

import static org.corant.shared.util.MapUtils.asImmutableMap;
import static org.corant.shared.util.ObjectUtils.trySupplied;
import static org.corant.shared.util.StringUtils.isEmpty;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author bingo 下午10:31:37
 *
 */
public class ClassUtils {

  public static final String CGLIB_CLASS_SEPARATOR = "$$";
  public static final char PACKAGE_SEPARATOR_CHAR = '.';
  public static final String PACKAGE_SEPARATOR = String.valueOf(PACKAGE_SEPARATOR_CHAR);
  public static final char INNER_CLASS_SEPARATOR_CHAR = '$';
  public static final String INNER_CLASS_SEPARATOR = String.valueOf(INNER_CLASS_SEPARATOR_CHAR);
  public static final String CLASS_FILE_NAME_EXTENSION = ".class";

  public static final Map<String, Class<?>> NAME_PRIMITIVE_MAP =
      asImmutableMap("boolean", Boolean.TYPE, "byte", Byte.TYPE, "char", Character.TYPE, "short",
          Short.TYPE, "int", Integer.TYPE, "long", Long.TYPE, "double", Double.TYPE, "float",
          Float.TYPE, "void", Void.TYPE);

  public static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP =
      asImmutableMap(Boolean.TYPE, Boolean.class, Byte.TYPE, Byte.class, Character.TYPE,
          Character.class, Short.TYPE, Short.class, Integer.TYPE, Integer.class, Long.TYPE,
          Long.class, Double.TYPE, Double.class, Float.TYPE, Float.class, Void.TYPE, Void.TYPE);

  public static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP =
      Collections.unmodifiableMap(PRIMITIVE_WRAPPER_MAP.entrySet().stream()
          .collect(Collectors.toMap(Entry::getValue, Entry::getKey)));

  public static Class<?> asClass(final ClassLoader classLoader, final String className)
      throws ClassNotFoundException {
    return asClass(classLoader, className, true);
  }

  public static Class<?> asClass(final ClassLoader classLoader, final String className,
      final boolean initialize) throws ClassNotFoundException {
    try {
      return NAME_PRIMITIVE_MAP.getOrDefault(className,
          Class.forName(className, initialize, classLoader));
    } catch (ClassNotFoundException ex) {
      throw ex;
    }
  }

  public static Class<?> asClass(String className) throws ClassNotFoundException {
    return asClass(defaultClassLoader(), className, true);
  }

  public static void checkPackageAccess(Class<?> clazz) {
    checkPackageAccess(clazz.getName());
    if (Proxy.isProxyClass(clazz)) {
      checkProxyPackageAccess(clazz);
    }
  }

  public static void checkPackageAccess(String name) {
    SecurityManager s = System.getSecurityManager();
    if (s != null) {
      String cname = name.replace('/', '.');
      if (cname.startsWith("[")) {
        int b = cname.lastIndexOf('[') + 2;
        if (b > 1 && b < cname.length()) {
          cname = cname.substring(b);
        }
      }
      int i = cname.lastIndexOf('.');
      if (i != -1) {
        s.checkPackageAccess(cname.substring(0, i));
      }
    }
  }

  public static void checkProxyPackageAccess(Class<?> clazz) {
    SecurityManager s = System.getSecurityManager();
    if (s != null && Proxy.isProxyClass(clazz)) {
      // check proxy interfaces if the given class is a proxy class
      for (Class<?> intf : clazz.getInterfaces()) {
        checkPackageAccess(intf);
      }
    }
  }

  public static ClassLoader defaultClassLoader() {
    ClassLoader classLoader = trySupplied(Thread.currentThread()::getContextClassLoader);
    if (classLoader == null) {
      classLoader = ClassUtils.class.getClassLoader();
      if (classLoader == null) {
        classLoader = trySupplied(ClassLoader::getSystemClassLoader);
      }
    }
    return classLoader;
  }

  public static List<Class<?>> getAllInterfaces(final Class<?> clazz) {
    Set<Class<?>> interfaces = new LinkedHashSet<>();
    if (clazz != null) {
      Class<?> usedClazz = clazz.isArray() ? clazz.getComponentType() : clazz;
      if (usedClazz.isInterface()) {
        interfaces.add(usedClazz);
      }
      Class<?> current = usedClazz;
      while (current != null) {
        Class<?>[] ifcs = current.getInterfaces();
        for (Class<?> ifc : ifcs) {
          interfaces.addAll(getAllInterfaces(ifc));
        }
        current = current.getSuperclass();
      }
    }
    return new ArrayList<>(interfaces);
  }


  public static List<Class<?>> getAllInterfaces(final Object object) {
    return object == null ? new ArrayList<>() : getAllInterfaces(object.getClass());
  }

  public static List<Class<?>> getAllSuperClasses(final Class<?> clazz) {
    List<Class<?>> superClasses = new ArrayList<>();
    if (clazz != null) {
      Class<?> usedClazz = clazz.isArray() ? clazz.getComponentType() : clazz;
      if (!usedClazz.isInterface() && usedClazz != Object.class) {
        Class<?> current = usedClazz;
        while (current.getSuperclass() != null) {
          superClasses.add(current.getSuperclass());
          current = current.getSuperclass();
        }
      }
    }
    return superClasses;
  }

  public static List<Class<?>> getAllSuperClasses(final Object object) {
    return object == null ? new ArrayList<>() : getAllSuperClasses(object.getClass());
  }

  public static List<Class<?>> getAllSuperclassesAndInterfaces(final Class<?> clazz) {
    List<Class<?>> list = new ArrayList<>();
    list.addAll(getAllInterfaces(clazz));
    list.addAll(getAllSuperClasses(clazz));
    return list;
  }

  public static Class<?> getComponentClass(Object object) {
    Class<?> clazz = null;
    if (object instanceof Iterable<?>) {
      Iterator<?> it = ((Iterable<?>) object).iterator();
      while (clazz == null && it.hasNext()) {
        Object cmp = it.next();
        if (cmp != null) {
          clazz = cmp.getClass();
        }
      }
    } else if (object != null && object.getClass().isArray()) {
      clazz = object.getClass().getComponentType();
    } else if (object instanceof Iterator<?>) {
      Iterator<?> it = (Iterator<?>) object;
      while (clazz == null && it.hasNext()) {
        Object cmp = it.next();
        if (cmp != null) {
          clazz = cmp.getClass();
        }
      }
    } else if (object instanceof Enumeration<?>) {
      Enumeration<?> enums = (Enumeration<?>) object;
      while (clazz == null && enums.hasMoreElements()) {
        Object cmp = enums.nextElement();
        if (cmp != null) {
          clazz = cmp.getClass();
        }
      }
    } else if (object != null) {
      clazz = object.getClass();
    } else {
      clazz = Object.class;
    }
    return getUserClass(clazz);
  }

  public static String getPackageName(String fullClassName) {
    if (isEmpty(fullClassName)) {
      return StringUtils.EMPTY;
    }
    int lastDot = fullClassName.lastIndexOf('.');
    return lastDot < 0 ? "" : fullClassName.substring(0, lastDot);
  }

  public static String getShortClassName(String className) {
    String shortClassName = className;
    if (isEmpty(shortClassName)) {
      return StringUtils.EMPTY;
    }
    final StringBuilder arrayPrefix = new StringBuilder();
    if (shortClassName.startsWith("[")) {
      while (shortClassName.charAt(0) == '[') {
        shortClassName = shortClassName.substring(1);
        arrayPrefix.append("[]");
      }
      if (shortClassName.charAt(0) == 'L'
          && shortClassName.charAt(shortClassName.length() - 1) == ';') {
        shortClassName = shortClassName.substring(1, shortClassName.length() - 1);
      }
    }
    final int lastDotIdx = shortClassName.lastIndexOf(PACKAGE_SEPARATOR_CHAR);
    final int innerIdx =
        shortClassName.indexOf(INNER_CLASS_SEPARATOR_CHAR, lastDotIdx == -1 ? 0 : lastDotIdx + 1);
    String out = shortClassName.substring(lastDotIdx + 1);
    if (innerIdx != -1) {
      out = out.replace(INNER_CLASS_SEPARATOR_CHAR, PACKAGE_SEPARATOR_CHAR);
    }
    return out + arrayPrefix;
  }

  public static Class<?> getUserClass(Class<?> clazz) {
    if (clazz != null && clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
      Class<?> superclass = clazz.getSuperclass();
      if (superclass != null && Object.class != superclass) {
        return superclass;
      }
    }
    return clazz;
  }

  public static Class<?> getUserClass(Object instance) {
    return getUserClass(instance.getClass());
  }

  public static boolean isAssignable(Class<?> cls, final Class<?> toClass,
      final boolean autoboxing) {
    if (toClass == null) {
      return false;
    }
    if (cls == null) {
      return !toClass.isPrimitive();
    }
    if (autoboxing) {
      if (cls.isPrimitive() && !toClass.isPrimitive()) {
        cls = primitiveToWrapper(cls);
        if (cls == null) {
          return false;
        }
      }
      if (toClass.isPrimitive() && !cls.isPrimitive()) {
        cls = wrapperToPrimitive(cls);
        if (cls == null) {
          return false;
        }
      }
    }
    if (cls.equals(toClass)) {
      return true;
    }
    if (cls.isPrimitive()) {
      if (!toClass.isPrimitive()) {
        return false;
      }
      if (Integer.TYPE.equals(cls)) {
        return Long.TYPE.equals(toClass) || Float.TYPE.equals(toClass)
            || Double.TYPE.equals(toClass);
      }
      if (Long.TYPE.equals(cls)) {
        return Float.TYPE.equals(toClass) || Double.TYPE.equals(toClass);
      }
      if (Boolean.TYPE.equals(cls)) {
        return false;
      }
      if (Double.TYPE.equals(cls)) {
        return false;
      }
      if (Float.TYPE.equals(cls)) {
        return Double.TYPE.equals(toClass);
      }
      if (Character.TYPE.equals(cls)) {
        return Integer.TYPE.equals(toClass) || Long.TYPE.equals(toClass)
            || Float.TYPE.equals(toClass) || Double.TYPE.equals(toClass);
      }
      if (Short.TYPE.equals(cls)) {
        return Integer.TYPE.equals(toClass) || Long.TYPE.equals(toClass)
            || Float.TYPE.equals(toClass) || Double.TYPE.equals(toClass);
      }
      if (Byte.TYPE.equals(cls)) {
        return Short.TYPE.equals(toClass) || Integer.TYPE.equals(toClass)
            || Long.TYPE.equals(toClass) || Float.TYPE.equals(toClass)
            || Double.TYPE.equals(toClass);
      }
      // should never get here
      return false;
    }
    return toClass.isAssignableFrom(cls);
  }

  public static boolean isConcrete(Class<?> clazz) {
    return clazz != null && !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers());
  }

  public static boolean isEnum(Class<?> clazz) {
    return clazz != null
        && (clazz.isEnum() || clazz.isArray() && clazz.getComponentType().isEnum());
  }

  public static boolean isPrimitiveArray(Class<?> clazz) {
    return clazz.isArray() && clazz.getComponentType().isPrimitive();
  }

  public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
    return clazz != null && (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
  }

  public static boolean isPrimitiveWrapper(Class<?> clazz) {
    return WRAPPER_PRIMITIVE_MAP.containsKey(clazz);
  }

  public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
    return clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType());
  }

  public static Class<?>[] primitivesToWrappers(final Class<?>... classes) {
    final Class<?>[] convertedClasses = new Class[classes.length];
    for (int i = 0; i < classes.length; i++) {
      convertedClasses[i] = primitiveToWrapper(classes[i]);
    }
    return convertedClasses;
  }

  public static Class<?> primitiveToWrapper(final Class<?> clazz) {
    if (clazz != null && clazz.isPrimitive()) {
      return PRIMITIVE_WRAPPER_MAP.get(clazz);
    } else {
      return clazz;
    }
  }

  public static void traverseAllInterfaces(final Class<?> clazz,
      Function<Class<?>, Boolean> visitor) {
    if (clazz != null) {
      Class<?> usedClazz = clazz.isArray() ? clazz.getComponentType() : clazz;
      if (usedClazz.isInterface() && !visitor.apply(usedClazz)) {
        return;
      }
      Class<?> current = usedClazz;
      stop: while (current != null) {
        Class<?>[] ifcs = current.getInterfaces();
        for (Class<?> ifc : ifcs) {
          for (Class<?> cls : getAllInterfaces(ifc)) {
            if (!visitor.apply(cls)) {
              break stop;
            }
          }
        }
        current = current.getSuperclass();
      }
    }
  }

  public static void traverseAllSuperClasses(final Class<?> clazz,
      Function<Class<?>, Boolean> visitor) {
    if (clazz != null) {
      Class<?> usedClazz = clazz.isArray() ? clazz.getComponentType() : clazz;
      if (!usedClazz.isInterface() && usedClazz != Object.class) {
        Class<?> current = usedClazz;
        while (current.getSuperclass() != null) {
          if (!visitor.apply(current.getSuperclass())) {
            break;
          }
          current = current.getSuperclass();
        }
      }
    }
  }


  public static Class<?> tryAsClass(String className) {
    try {
      return asClass(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public static Class<?>[] wrappersToPrimitives(final Class<?>... classes) {
    final Class<?>[] convertedClasses = new Class[classes.length];
    for (int i = 0; i < classes.length; i++) {
      convertedClasses[i] = wrapperToPrimitive(classes[i]);
    }
    return convertedClasses;
  }

  public static Class<?> wrapperToPrimitive(final Class<?> cls) {
    return WRAPPER_PRIMITIVE_MAP.get(cls);
  }
}

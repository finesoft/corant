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

import static org.corant.shared.util.Maps.immutableMapOf;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * corant-shared
 *
 * @author bingo 下午2:52:21
 *
 */
public class Primitives {

  public static final Map<String, Class<?>> NAME_PRIMITIVE_MAP =
      immutableMapOf("boolean", Boolean.TYPE, "byte", Byte.TYPE, "char", Character.TYPE, "short",
          Short.TYPE, "int", Integer.TYPE, "long", Long.TYPE, "double", Double.TYPE, "float",
          Float.TYPE, "void", Void.TYPE);

  public static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP =
      immutableMapOf(Boolean.TYPE, Boolean.class, Byte.TYPE, Byte.class, Character.TYPE,
          Character.class, Short.TYPE, Short.class, Integer.TYPE, Integer.class, Long.TYPE,
          Long.class, Double.TYPE, Double.class, Float.TYPE, Float.class, Void.TYPE, Void.TYPE);

  public static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP =
      Collections.unmodifiableMap(PRIMITIVE_WRAPPER_MAP.entrySet().stream()
          .collect(Collectors.toMap(Entry::getValue, Entry::getKey)));

  public static boolean isPrimitiveArray(final Class<?> clazz) {
    return clazz.isArray() && clazz.getComponentType().isPrimitive();
  }

  public static boolean isPrimitiveOrWrapper(final Class<?> clazz) {
    return clazz != null && (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
  }

  public static boolean isPrimitiveWrapper(final Class<?> clazz) {
    return WRAPPER_PRIMITIVE_MAP.containsKey(clazz);
  }

  public static boolean isPrimitiveWrapperArray(final Class<?> clazz) {
    return clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType());
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> unwrap(final Class<T> clazz) {
    if (clazz != null && !clazz.isPrimitive()) {
      return (Class<T>) WRAPPER_PRIMITIVE_MAP.get(clazz);
    } else {
      return clazz;
    }
  }

  public static Class<?>[] unwrapAll(final Class<?>... classes) {
    if (classes == null || classes.length == 0) {
      return classes;
    }
    final Class<?>[] convertedClasses = new Class[classes.length];
    for (int i = 0; i < classes.length; i++) {
      convertedClasses[i] = unwrap(classes[i]);
    }
    return convertedClasses;
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> wrap(final Class<T> clazz) {
    if (clazz != null && clazz.isPrimitive()) {
      return (Class<T>) PRIMITIVE_WRAPPER_MAP.get(clazz);
    } else {
      return clazz;
    }
  }

  public static Class<?>[] wrapAll(final Class<?>... classes) {
    if (classes == null || classes.length == 0) {
      return classes;
    }
    final Class<?>[] convertedClasses = new Class[classes.length];
    for (int i = 0; i < classes.length; i++) {
      convertedClasses[i] = wrap(classes[i]);
    }
    return convertedClasses;
  }
}

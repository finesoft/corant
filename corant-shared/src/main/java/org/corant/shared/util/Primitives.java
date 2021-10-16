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
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 下午2:52:21
 *
 */
public class Primitives {

  public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];
  public static final byte[] EMPTY_BYTE_ARRAY = Bytes.EMPTY_ARRAY;
  public static final char[] EMPTY_CHAR_ARRAY = Chars.EMPTY_ARRAY;
  public static final short[] EMPTY_SHORT_ARRAY = new short[0];
  public static final int[] EMPTY_INTEGER_ARRAY = new int[0];
  public static final long[] EMPTY_LONG_ARRAY = new long[0];
  public static final float[] EMPTY_FLOAT_ARRAY = new float[0];
  public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

  public static final Boolean[] EMPTY_WRAP_BOOLEAN_ARRAY = new Boolean[0];
  public static final Byte[] EMPTY_WRAP_BYTE_ARRAY = new Byte[0];
  public static final Character[] EMPTY_WRAP_CHAR_ARRAY = new Character[0];
  public static final Short[] EMPTY_WRAP_SHORT_ARRAY = new Short[0];
  public static final Integer[] EMPTY_WRAP_INTEGER_ARRAY = new Integer[0];
  public static final Long[] EMPTY_WRAP_LONG_ARRAY = new Long[0];
  public static final Float[] EMPTY_WRAP_FLOAT_ARRAY = new Float[0];
  public static final Double[] EMPTY_WRAP_DOUBLE_ARRAY = new Double[0];

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

  public static boolean[] unwrap(final Boolean[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_BOOLEAN_ARRAY;
    }
    final boolean[] result = new boolean[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static byte[] unwrap(final Byte[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_BYTE_ARRAY;
    }
    final byte[] result = new byte[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> unwrap(final Class<T> clazz) {
    if (clazz != null && !clazz.isPrimitive()) {
      return (Class<T>) WRAPPER_PRIMITIVE_MAP.get(clazz);
    } else {
      return clazz;
    }
  }

  public static double[] unwrap(final Double[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_DOUBLE_ARRAY;
    }
    final double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static float[] unwrap(final Float[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_FLOAT_ARRAY;
    }
    final float[] result = new float[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static int[] unwrap(final Integer[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_INTEGER_ARRAY;
    }
    final int[] result = new int[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static long[] unwrap(final Long[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_LONG_ARRAY;
    }
    final long[] result = new long[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static short[] unwrap(final Short[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_SHORT_ARRAY;
    }
    final short[] result = new short[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
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

  public static Boolean[] wrap(final boolean[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_WRAP_BOOLEAN_ARRAY;
    }
    final Boolean[] result = new Boolean[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] ? Boolean.TRUE : Boolean.FALSE;
    }
    return result;
  }

  public static Byte[] wrap(final byte[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_WRAP_BYTE_ARRAY;
    }
    final Byte[] result = new Byte[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> wrap(final Class<T> clazz) {
    if (clazz != null && clazz.isPrimitive()) {
      return (Class<T>) PRIMITIVE_WRAPPER_MAP.get(clazz);
    } else {
      return clazz;
    }
  }

  public static Double[] wrap(final double[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_WRAP_DOUBLE_ARRAY;
    }
    final Double[] result = new Double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static Float[] wrap(final float[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_WRAP_FLOAT_ARRAY;
    }
    final Float[] result = new Float[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static Integer[] wrap(final int[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_WRAP_INTEGER_ARRAY;
    }
    final Integer[] result = new Integer[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static Long[] wrap(final long[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_WRAP_LONG_ARRAY;
    }
    final Long[] result = new Long[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static Short[] wrap(final short[] array) {
    if (array == null) {
      return null;
    } else if (array.length == 0) {
      return EMPTY_WRAP_SHORT_ARRAY;
    }
    final Short[] result = new Short[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i];
    }
    return result;
  }

  public static Type wrap(final Type type) {
    if (type instanceof Class) {
      return wrap((Class<?>) type);
    } else {
      return type;
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

  public static Object[] wrapArray(Object array) {
    if (array == null) {
      return null;
    } else if (array instanceof Object[]) {
      return (Object[]) array;
    } else if (array.getClass().isArray()) {
      Class<?> eleType = array.getClass().getComponentType();
      if (eleType.equals(Integer.TYPE)) {
        return wrap((int[]) array);
      } else if (eleType.equals(Short.TYPE)) {
        return wrap((short[]) array);
      } else if (eleType.equals(Byte.TYPE)) {
        return wrap((byte[]) array);
      } else if (eleType.equals(Boolean.TYPE)) {
        return wrap((boolean[]) array);
      } else if (eleType.equals(Float.TYPE)) {
        return wrap((float[]) array);
      } else if (eleType.equals(Double.TYPE)) {
        return wrap((double[]) array);
      } else if (eleType.equals(Long.TYPE)) {
        return wrap((long[]) array);
      } else {
        final int len = Array.getLength(array);
        Object[] result = new Object[len];
        for (int i = 0; i < len; i++) {
          result[i] = Array.get(array, i);
        }
        return result;
      }
    }
    throw new CorantRuntimeException("The given parameter must be an array!");
  }
}

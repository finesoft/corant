/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.conversion.converter.factory;

import static org.corant.shared.util.Lists.immutableListOf;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Primitives.unwrap;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.corant.shared.conversion.Conversion;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;

/**
 * corant-shared
 *
 * <p>
 * Note: The internal implementation of the converter factory is achieved through
 * {@link Conversion}, and recursive loops should be avoided.
 *
 * @author bingo 上午11:59:51
 */
public class ObjectPrimitiveArrayConverterFactories {

  public static final List<ObjectPrimitiveArrayConverterFactory<Object, ?>> FACTORIES =
  //@formatter:off
      immutableListOf(
        new ObjectBooleanArrayConverterFactory(),
        new ObjectByteArrayConverterFactory(),
        new ObjectShortArrayConverterFactory(),
        new ObjectIntArrayConverterFactory(),
        new ObjectLongArrayConverterFactory(),
        new ObjectFloatArrayConverterFactory(),
        new ObjectDoubleArrayConverterFactory()
      );
  //@formatter:on

  static <T> T[] convert(Object s, Class<T> targetClass, Map<String, ?> hints) {
    T[] result;
    final Class<?> sourceClass = s.getClass();
    if (sourceClass.isArray()) {
      int length = Array.getLength(s);
      result = createArray(targetClass, length);
      for (int i = 0; i < length; i++) {
        result[i] = Conversion.convert(Array.get(s, i), targetClass, hints);
      }
      return result;
    } else if (Collection.class.isAssignableFrom(sourceClass)) {
      Collection<?> collection = (Collection<?>) s;
      result = createArray(targetClass, collection.size());
      int i = 0;
      for (final Object object : collection) {
        result[i] = Conversion.convert(object, targetClass, hints);
      }
      return result;
    } else if (Iterable.class.isAssignableFrom(sourceClass)) {
      List<Object> list = listOf((Iterable<?>) s);
      result = createArray(targetClass, list.size());
      int i = 0;
      for (final Object object : list) {
        result[i] = Conversion.convert(object, targetClass, hints);
      }
      return result;
    } else {
      throw new ConversionException(
          "The primitives array converter only support Array or Iterable objects");
    }
  }

  @SuppressWarnings("unchecked")
  static <T> T[] createArray(final Class<?> targetComponentType, final int length) {
    return (T[]) Array.newInstance(targetComponentType, length);
  }

  public static class ObjectBooleanArrayConverterFactory
      extends ObjectPrimitiveArrayConverterFactory<Object, boolean[]> {
    private ObjectBooleanArrayConverterFactory() {}

    @Override
    public Converter<Object, boolean[]> create(Class<boolean[]> targetClass, boolean[] defaultValue,
        boolean throwException) {
      return (s, h) -> unwrap(convert(s, Boolean.class, h));
    }

    @Override
    public boolean isSupportTargetClass(Class<?> targetClass) {
      return boolean[].class.equals(targetClass);
    }
  }

  public static class ObjectByteArrayConverterFactory
      extends ObjectPrimitiveArrayConverterFactory<Object, byte[]> {
    private ObjectByteArrayConverterFactory() {}

    @Override
    public Converter<Object, byte[]> create(Class<byte[]> targetClass, byte[] defaultValue,
        boolean throwException) {
      return (s, h) -> unwrap(convert(s, Byte.class, h));
    }

    @Override
    public boolean isSupportTargetClass(Class<?> targetClass) {
      return byte[].class.equals(targetClass);
    }
  }

  public static class ObjectDoubleArrayConverterFactory
      extends ObjectPrimitiveArrayConverterFactory<Object, double[]> {
    private ObjectDoubleArrayConverterFactory() {}

    @Override
    public Converter<Object, double[]> create(Class<double[]> targetClass, double[] defaultValue,
        boolean throwException) {
      return (s, h) -> unwrap(convert(s, Double.class, h));
    }

    @Override
    public boolean isSupportTargetClass(Class<?> targetClass) {
      return double[].class.equals(targetClass);
    }
  }

  public static class ObjectFloatArrayConverterFactory
      extends ObjectPrimitiveArrayConverterFactory<Object, float[]> {
    private ObjectFloatArrayConverterFactory() {}

    @Override
    public Converter<Object, float[]> create(Class<float[]> targetClass, float[] defaultValue,
        boolean throwException) {
      return (s, h) -> unwrap(convert(s, Float.class, h));
    }

    @Override
    public boolean isSupportTargetClass(Class<?> targetClass) {
      return float[].class.equals(targetClass);
    }
  }

  public static class ObjectIntArrayConverterFactory
      extends ObjectPrimitiveArrayConverterFactory<Object, int[]> {
    private ObjectIntArrayConverterFactory() {}

    @Override
    public Converter<Object, int[]> create(Class<int[]> targetClass, int[] defaultValue,
        boolean throwException) {
      return (s, h) -> unwrap(convert(s, Integer.class, h));
    }

    @Override
    public boolean isSupportTargetClass(Class<?> targetClass) {
      return int[].class.equals(targetClass);
    }
  }

  public static class ObjectLongArrayConverterFactory
      extends ObjectPrimitiveArrayConverterFactory<Object, long[]> {
    private ObjectLongArrayConverterFactory() {}

    @Override
    public Converter<Object, long[]> create(Class<long[]> targetClass, long[] defaultValue,
        boolean throwException) {
      return (s, h) -> unwrap(convert(s, Long.class, h));
    }

    @Override
    public boolean isSupportTargetClass(Class<?> targetClass) {
      return long[].class.equals(targetClass);
    }
  }

  public static abstract class ObjectPrimitiveArrayConverterFactory<S, T>
      implements ConverterFactory<S, T> {
    @Override
    public boolean isSupportSourceClass(Class<?> sourceClass) {
      return sourceClass.isArray() || Iterable.class.isAssignableFrom(sourceClass);
    }
  }

  public static class ObjectShortArrayConverterFactory
      extends ObjectPrimitiveArrayConverterFactory<Object, short[]> {
    private ObjectShortArrayConverterFactory() {}

    @Override
    public Converter<Object, short[]> create(Class<short[]> targetClass, short[] defaultValue,
        boolean throwException) {
      return (s, h) -> unwrap(convert(s, Short.class, h));
    }

    @Override
    public boolean isSupportTargetClass(Class<?> targetClass) {
      return short[].class.equals(targetClass);
    }
  }
}

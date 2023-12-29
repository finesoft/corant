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
package org.corant.shared.conversion.converter.factory;

import static org.corant.shared.util.Objects.defaultObject;
import java.util.Map;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;

/**
 * corant-shared
 *
 * @author bingo 下午8:01:05
 */
public class NumberPrimitiveConverterFactory implements ConverterFactory<Number, Object> {

  public static Object convert(Number value, Class<?> targetClass, Map<String, ?> hints) {
    if (value == null) {
      return null;
    }
    if (targetClass.equals(Integer.TYPE)) {
      return value.intValue();
    } else if (targetClass.equals(Short.TYPE)) {
      return value.shortValue();
    } else if (targetClass.equals(Byte.TYPE)) {
      return value.byteValue();
    } else if (targetClass.equals(Long.TYPE)) {
      return value.longValue();
    } else if (targetClass.equals(Double.TYPE)) {
      return value.doubleValue();
    } else if (targetClass.equals(Float.TYPE)) {
      return value.floatValue();
    }
    throw new ConversionException("Can't convert Number %s to %s!", value, targetClass.getName());
  }

  @Override
  public Converter<Number, Object> create(Class<Object> targetClass, Object defaultValue,
      boolean throwException) {
    return (s, h) -> defaultObject(convert(s, targetClass, h), defaultValue);
  }

  @Override
  public boolean isSupportSourceClass(Class<?> sourceClass) {
    return Number.class.isAssignableFrom(sourceClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return targetClass.equals(Integer.TYPE) || targetClass.equals(Short.TYPE)
        || targetClass.equals(Byte.TYPE) || targetClass.equals(Long.TYPE)
        || targetClass.equals(Double.TYPE) || targetClass.equals(Float.TYPE);
  }

}

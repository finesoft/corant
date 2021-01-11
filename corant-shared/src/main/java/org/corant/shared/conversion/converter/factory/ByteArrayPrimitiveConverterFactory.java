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

import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.util.Bytes;

/**
 * corant-shared
 *
 * @author bingo 下午2:51:15
 *
 */
public class ByteArrayPrimitiveConverterFactory implements ConverterFactory<byte[], Object> {

  final Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public Converter<byte[], Object> create(Class<Object> targetClass, Object defaultValue,
      boolean throwException) {

    return (t, h) -> {
      Object result = null;
      final boolean strict = isStrict(h);
      try {
        if (targetClass.equals(Long.class) || targetClass.equals(Long.TYPE)) {
          result = Bytes.toLong(t, strict);
        } else if (targetClass.equals(Integer.class) || targetClass.equals(Integer.TYPE)) {
          result = Bytes.toInt(t, strict);
        } else if (targetClass.equals(Short.class) || targetClass.equals(Short.TYPE)) {
          result = Bytes.toShort(t, strict);
        } else if (targetClass.equals(Character.class) || targetClass.equals(Character.TYPE)) {
          result = Bytes.toChar(t, strict);
        } else if (targetClass.equals(Float.class) || targetClass.equals(Float.TYPE)) {
          result = Bytes.toFloat(t, strict);
        } else {
          result = Bytes.toDouble(t, strict);
        }
      } catch (Exception e) {
        if (throwException) {
          throw new ConversionException(e);
        } else {
          logger.log(Level.WARNING, e, () -> String.format("Can not convert %s.", asString(t)));
        }
      }
      return defaultObject(result, defaultValue);
    };

  }

  @Override
  public boolean isSupportSourceClass(Class<?> sourceClass) {
    return byte[].class.equals(sourceClass) || Byte[].class.equals(sourceClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return targetClass.equals(Long.TYPE) || targetClass.equals(Integer.TYPE)
        || targetClass.equals(Short.TYPE) || targetClass.equals(Character.TYPE)
        || targetClass.equals(Float.TYPE) || targetClass.equals(Double.TYPE)
        || targetClass.equals(Long.class) || targetClass.equals(Integer.class)
        || targetClass.equals(Short.class) || targetClass.equals(Character.class)
        || targetClass.equals(Float.class) || targetClass.equals(Double.class);
  }

  protected boolean isStrict(Map<String, ?> hints) {
    if (ConverterHints.containsKey(hints, ConverterHints.CVT_BYTES_PRIMITIVE_STRICTLY_KEY)) {
      return ConverterHints.getHint(hints, ConverterHints.CVT_BYTES_PRIMITIVE_STRICTLY_KEY);
    }
    return true;
  }

}

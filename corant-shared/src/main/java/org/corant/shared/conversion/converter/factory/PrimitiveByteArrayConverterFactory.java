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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;
import org.corant.shared.util.Bytes;

/**
 * corant-shared
 *
 * @author bingo 下午2:51:15
 *
 */
public class PrimitiveByteArrayConverterFactory implements ConverterFactory<Object, byte[]> {

  final Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public Converter<Object, byte[]> create(Class<byte[]> targetClass, byte[] defaultValue,
      boolean throwException) {
    return (t, h) -> {
      byte[] result = null;
      try {
        if (t instanceof Long) {
          result = Bytes.toBytes((Long) t);
        } else if (t instanceof Integer) {
          result = Bytes.toBytes((Integer) t);
        } else if (t instanceof Short) {
          result = Bytes.toBytes((Short) t);
        } else if (t instanceof Character) {
          result = Bytes.toBytes((Character) t);
        } else if (t instanceof Float) {
          result = Bytes.toBytes((Float) t);
        } else if (t instanceof Double) {
          result = Bytes.toBytes((Double) t);
        }
      } catch (Exception e) {
        if (throwException) {
          throw new ConversionException(e);
        } else {
          logger.log(Level.WARNING, e, () -> String.format("Can not convert %s", asString(t)));
        }
      }
      return defaultObject(result, defaultValue);
    };
  }

  @Override
  public boolean isSupportSourceClass(Class<?> sourceClass) {
    return sourceClass.equals(Long.TYPE) || sourceClass.equals(Integer.TYPE)
        || sourceClass.equals(Short.TYPE) || sourceClass.equals(Character.TYPE)
        || sourceClass.equals(Float.TYPE) || sourceClass.equals(Double.TYPE)
        || sourceClass.equals(Long.class) || sourceClass.equals(Integer.class)
        || sourceClass.equals(Short.class) || sourceClass.equals(Character.class)
        || sourceClass.equals(Float.class) || sourceClass.equals(Double.class);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return byte[].class.equals(targetClass);
  }

}

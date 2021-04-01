package org.corant.modules.json.converter;
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

import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.defaultObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonNumber;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;

/**
 * corant-modules-json
 *
 * @author bingo 下午6:37:13
 *
 */
public class JsonNumberNumberConverterFactory implements ConverterFactory<JsonNumber, Object> {

  final Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public Converter<JsonNumber, Object> create(Class<Object> targetClass, Object defaultValue,
      boolean throwException) {
    return (t, h) -> {
      Object result = null;
      try {
        if (targetClass.equals(Long.class) || targetClass.equals(Long.TYPE)) {
          result = t.longValueExact();
        } else if (targetClass.equals(Integer.class) || targetClass.equals(Integer.TYPE)) {
          result = t.intValueExact();
        } else if (targetClass.equals(Short.class) || targetClass.equals(Short.TYPE)) {
          result = (short) t.intValue();
        } else if (targetClass.equals(Float.class) || targetClass.equals(Float.TYPE)) {
          result = Double.valueOf(t.doubleValue()).floatValue();
        } else if (targetClass.equals(BigDecimal.class)) {
          result = t.bigDecimalValue();
        } else if (targetClass.equals(BigInteger.class)) {
          result = t.bigIntegerValueExact();
        } else if (targetClass.equals(Number.class)) {
          result = t.numberValue();
        } else {
          result = t.doubleValue();
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
    return JsonNumber.class.isAssignableFrom(sourceClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return targetClass.equals(Long.TYPE) || targetClass.equals(Integer.TYPE)
        || targetClass.equals(Short.TYPE) || targetClass.equals(Character.TYPE)
        || targetClass.equals(Float.TYPE) || targetClass.equals(Double.TYPE)
        || targetClass.equals(Long.class) || targetClass.equals(Integer.class)
        || targetClass.equals(Short.class) || targetClass.equals(BigDecimal.class)
        || targetClass.equals(Number.class) || targetClass.equals(BigInteger.class)
        || targetClass.equals(Float.class) || targetClass.equals(Double.class);
  }

}

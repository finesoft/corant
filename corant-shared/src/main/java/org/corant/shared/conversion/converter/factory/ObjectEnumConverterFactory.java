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

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Sets.immutableSetOf;
import static org.corant.shared.util.Strings.split;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 上午10:15:06
 *
 */
public class ObjectEnumConverterFactory implements ConverterFactory<Object, Enum<?>> {

  final Logger logger = Logger.getLogger(this.getClass().getName());
  final Set<Class<?>> supportedSourceClass = immutableSetOf(Enum.class, Number.class,
      CharSequence.class, Integer.TYPE, Long.TYPE, Short.TYPE, Byte.TYPE);

  @SuppressWarnings("rawtypes")
  public static <T extends Enum<?>> T convert(Object value, Class<T> targetClass,
      Map<String, ?> hints) throws Exception {
    if (value == null) {
      return null;
    }
    if (targetClass.isAssignableFrom(value.getClass())) {
      return targetClass.cast(value);
    } else if (value.getClass().equals(Integer.TYPE)) {
      return targetClass.getEnumConstants()[(int) value];
    } else if (value instanceof Integer) {
      return targetClass.getEnumConstants()[(Integer) value];
    } else {
      String name = null;
      if (value instanceof Map) {
        name = getMapString((Map) value, "name");
      } else {
        String[] values = split(value.toString(), ".", true, true);
        if (!isEmpty(values)) {
          name = values[values.length - 1];
        }
      }
      if (name != null) {
        boolean cs = ConverterHints.getHint(hints, ConverterHints.CVT_CASE_SENSITIVE, false);
        for (T t : targetClass.getEnumConstants()) {
          if (cs) {
            if (t.name().equals(name)) {
              return t;
            }
          } else if (t.name().equalsIgnoreCase(name)) {
            return t;
          }
        }
      }
      throw new ConversionException("Can not convert %s -> %s.", value.getClass(), targetClass);
    }
  }

  @Override
  public Converter<Object, Enum<?>> create(Class<Enum<?>> targetClass, Enum<?> defaultValue,
      boolean throwException) {
    return (t, h) -> {
      Enum<?> result = null;
      try {
        result = convert(t, targetClass, h);
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
    return supportedSourceClass.contains(sourceClass)
        || supportedSourceClass.stream().anyMatch(c -> c.isAssignableFrom(sourceClass));
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return Enum.class.isAssignableFrom(targetClass);
  }
}

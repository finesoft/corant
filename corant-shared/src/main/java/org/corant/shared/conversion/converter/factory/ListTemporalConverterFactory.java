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

import static org.corant.shared.util.Empties.sizeOf;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.List;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;

/**
 * corant-shared
 *
 * @author bingo 下午8:01:05
 *
 */
@SuppressWarnings("rawtypes")
public class ListTemporalConverterFactory implements ConverterFactory<List, Temporal> {

  public static LocalDate convertLocalDate(List value, LocalDate defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      if (sizeOf(value) == 3) {
        return LocalDate.of((Integer) value.get(0), (Integer) value.get(1), (Integer) value.get(2));
      }
      throw new ConversionException("Can't convert value to LocalDate from List object.");
    }
  }

  public static LocalDateTime convertLocalDateTime(List value, LocalDateTime defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      int size = sizeOf(value);
      if (size == 5) {
        return LocalDateTime.of((Integer) value.get(0), (Integer) value.get(1),
            (Integer) value.get(2), (Integer) value.get(3), (Integer) value.get(4));
      } else if (size == 6) {
        return LocalDateTime.of((Integer) value.get(0), (Integer) value.get(1),
            (Integer) value.get(2), (Integer) value.get(3), (Integer) value.get(4),
            (Integer) value.get(5));
      } else if (size == 7) {
        return LocalDateTime.of((Integer) value.get(0), (Integer) value.get(1),
            (Integer) value.get(2), (Integer) value.get(3), (Integer) value.get(4),
            (Integer) value.get(5), (Integer) value.get(6));
      }
      throw new ConversionException("Can't convert value to LocalDateTime from List object.");
    }
  }

  static LocalTime convertLocalTime(List value, LocalTime defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      int size = sizeOf(value);
      if (size == 3) {
        return LocalTime.of((Integer) value.get(0), (Integer) value.get(1), (Integer) value.get(2));
      } else if (size == 4) {
        return LocalTime.of((Integer) value.get(0), (Integer) value.get(1), (Integer) value.get(2),
            (Integer) value.get(3));
      }
      throw new ConversionException("Can't convert value to LocalTime from List object.");
    }
  }

  @Override
  public Converter<List, Temporal> create(Class<Temporal> targetClass, Temporal defaultValue,
      boolean throwException) {
    return (t, h) -> {
      if (targetClass.equals(LocalTime.class)) {
        return convertLocalTime(t, (LocalTime) defaultValue);
      } else if (targetClass.equals(LocalDate.class)) {
        return convertLocalDate(t, (LocalDate) defaultValue);
      } else {
        return convertLocalDateTime(t, (LocalDateTime) defaultValue);
      }
    };
  }

  @Override
  public boolean isSupportSourceClass(Class<?> sourceClass) {
    return List.class.isAssignableFrom(sourceClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return targetClass.equals(LocalDate.class) || targetClass.equals(LocalDateTime.class)
        || targetClass.equals(LocalTime.class);
  }

}

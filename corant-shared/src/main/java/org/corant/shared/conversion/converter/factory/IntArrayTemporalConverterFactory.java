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
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;

/**
 * corant-shared
 *
 * @author bingo 下午8:01:05
 *
 */
public class IntArrayTemporalConverterFactory implements ConverterFactory<int[], Temporal> {

  public static LocalDate convertLocalDate(int[] value, LocalDate defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      if (sizeOf(value) == 3) {
        return LocalDate.of(value[0], value[1], value[2]);
      }
      throw new ConversionException("Can't convert value to LocalDate from int array.");
    }
  }

  public static LocalDateTime convertLocalDateTime(int[] value, LocalDateTime defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      int size = value.length;
      if (size == 5) {
        return LocalDateTime.of(value[0], value[1], value[2], value[3], value[4]);
      } else if (size == 6) {
        return LocalDateTime.of(value[0], value[1], value[2], value[3], value[4], value[5]);
      } else if (size == 7) {
        return LocalDateTime.of(value[0], value[1], value[2], value[3], value[4], value[5],
            value[6]);
      }
      throw new ConversionException("Can't convert value to LocalDateTime from int array.");
    }
  }

  public static LocalTime convertLocalTime(int[] value, LocalTime defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      int size = value.length;
      if (size == 3) {
        return LocalTime.of(value[0], value[1], value[2]);
      } else if (size == 4) {
        return LocalTime.of(value[0], value[1], value[2], value[3]);
      }
      throw new ConversionException("Can't convert value to LocalTime from int array.");
    }
  }

  @Override
  public Converter<int[], Temporal> create(Class<Temporal> targetClass, Temporal defaultValue,
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
    return int[].class.isAssignableFrom(sourceClass) || Integer[].class.equals(sourceClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return targetClass.equals(LocalDate.class) || targetClass.equals(LocalDateTime.class)
        || targetClass.equals(LocalTime.class);
  }

}

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
import static org.corant.shared.util.Strings.asDefaultString;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.Map;
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
public class MapTemporalConverterFactory implements ConverterFactory<Map, Temporal> {

  @Override
  public Converter<Map, Temporal> create(Class<Temporal> targetClass, Temporal defaultValue,
      boolean throwException) {
    return (t, h) -> {
      if (targetClass.equals(Instant.class)) {
        return convertInstant(t, defaultValue);
      } else if (targetClass.equals(LocalDate.class)) {
        return convertLocalDate(t, defaultValue);
      } else if (targetClass.equals(LocalDateTime.class)) {
        return convertLocalDateTime(t, defaultValue);
      } else {
        return convertLocalTime(t, defaultValue);
      }
    };
  }

  @Override
  public boolean isSupportSourceClass(Class<?> sourceClass) {
    return Map.class.isAssignableFrom(sourceClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return targetClass.equals(LocalDate.class) || targetClass.equals(LocalDateTime.class)
        || targetClass.equals(LocalTime.class) || targetClass.equals(Instant.class);
  }

  Temporal convertInstant(Map value, Temporal defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      if (value.containsKey("epochSecond") && value.containsKey("nano")) {
        return Instant.ofEpochSecond(resolveLong(value.get("epochSecond")),
            resolveLong(value.get("nano")));
      } else if (value.containsKey("epochSecond")) {
        return Instant.ofEpochSecond(resolveLong(value.get("epochSecond")));
      }
      throw new ConversionException("Can't found the value of 'epochSecond'");
    }
  }

  Temporal convertLocalDate(Map value, Temporal defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      if (value.containsKey("year") && value.containsKey("month")
          && (value.containsKey("day") || value.containsKey("dayOfMonth"))) {
        return LocalDate.of(resolveInteger(value.get("year")), resolveInteger(value.get("month")),
            defaultObject(resolveInteger(value.get("dayOfMonth")),
                resolveInteger(value.get("day"))));
      } else if (value.containsKey("year") && value.containsKey("dayOfYear")) {
        return LocalDate.ofYearDay(resolveInteger(value.get("year")),
            resolveInteger(value.get("dayOfYear")));
      } else if (value.containsKey("epochDay")) {
        return LocalDate.ofEpochDay(resolveLong(value.get("epochDay")));
      }
      throw new ConversionException("Can't convert value to LocalDate from Map object.");
    }
  }

  Temporal convertLocalDateTime(Map value, Temporal defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      if (value.containsKey("year") && value.containsKey("month")
          && (value.containsKey("day") || value.containsKey("dayOfMonth"))
          && value.containsKey("hour") && value.containsKey("minute")) {
        if (value.containsKey("second")) {
          if (value.containsKey("nanoOfSecond") || value.containsKey("nano")) {
            return LocalDateTime.of(resolveInteger(value.get("year")),
                resolveInteger(value.get("month")),
                defaultObject(resolveInteger(value.get("dayOfMonth")),
                    resolveInteger(value.get("day"))),
                resolveInteger(value.get("hour")), resolveInteger(value.get("minute")),
                resolveInteger(value.get("second")), defaultObject(
                    resolveInteger(value.get("nanoOfSecond")), resolveInteger(value.get("nano"))));
          } else {
            return LocalDateTime.of(resolveInteger(value.get("year")),
                resolveInteger(value.get("month")),
                defaultObject(resolveInteger(value.get("dayOfMonth")),
                    resolveInteger(value.get("day"))),
                resolveInteger(value.get("hour")), resolveInteger(value.get("minute")),
                resolveInteger(value.get("second")));
          }
        } else {
          return LocalDateTime.of(resolveInteger(value.get("year")),
              resolveInteger(value.get("month")),
              defaultObject(resolveInteger(value.get("dayOfMonth")),
                  resolveInteger(value.get("day"))),
              resolveInteger(value.get("hour")), resolveInteger(value.get("minute")));
        }
      } else if (value.containsKey("epochSecond") && value.containsKey("nanoOfSecond")
          && value.containsKey("offsetId")) {
        return LocalDateTime.ofEpochSecond(resolveLong(value.get("epochSecond")),
            resolveInteger(value.get("nanoOfSecond")),
            ZoneOffset.of(asDefaultString(value.get("offsetId"))));
      }
      throw new ConversionException("Can't convert value to LocalDateTime from Map object.");
    }
  }

  Temporal convertLocalTime(Map value, Temporal defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      if (value.containsKey("hour") && value.containsKey("minute")) {
        if (value.containsKey("second")) {
          if (value.containsKey("nanoOfSecond") || value.containsKey("nano")) {
            return LocalTime.of(resolveInteger(value.get("hour")),
                resolveInteger(value.get("minute")), resolveInteger(value.get("second")),
                defaultObject(resolveInteger(value.get("nanoOfSecond")),
                    resolveInteger(value.get("nano"))));
          } else {
            return LocalTime.of(resolveInteger(value.get("hour")),
                resolveInteger(value.get("minute")), resolveInteger(value.get("second")));
          }
        } else {
          return LocalTime.of(resolveInteger(value.get("hour")),
              resolveInteger(value.get("minute")));
        }
      }
      throw new ConversionException("Can't convert value to LocalDate from Map object.");
    }
  }

  Integer resolveInteger(Object obj) {
    if (obj != null) {
      if (obj instanceof Integer) {
        return (Integer) obj;
      } else if (obj.getClass().equals(Integer.TYPE)) {
        return (int) obj;
      } else if (obj instanceof Number) {
        return ((Number) obj).intValue();
      } else if (obj instanceof String) {
        return Integer.valueOf(obj.toString());
      }
    } else {
      return null;
    }
    throw new ConversionException("Can't convert %s to integer!", obj);
  }

  Long resolveLong(Object obj) {
    if (obj != null) {
      if (obj instanceof Long) {
        return (Long) obj;
      } else if (obj.getClass().equals(Long.TYPE)) {
        return (long) obj;
      } else if (obj instanceof Number) {
        return ((Number) obj).longValue();
      } else if (obj instanceof String) {
        return Long.valueOf(obj.toString());
      }
    } else {
      return null;
    }
    throw new ConversionException("Can't convert %s to long!", obj);
  }

}

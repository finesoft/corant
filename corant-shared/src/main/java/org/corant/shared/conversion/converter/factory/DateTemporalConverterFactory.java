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

import static org.corant.shared.util.Objects.defaultObject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Map;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.Converter;
import org.corant.shared.conversion.ConverterFactory;
import org.corant.shared.conversion.converter.AbstractConverter;
import org.corant.shared.conversion.converter.AbstractTemporalConverter;

/**
 * corant-shared
 *
 * @author bingo 上午10:59:48
 */
public class DateTemporalConverterFactory implements ConverterFactory<Date, Temporal> {

  @SuppressWarnings("unchecked")
  public static <T> T convert(Date value, Class<T> targetClass, Map<String, ?> hints) {
    if (value == null) {
      return null;
    }

    if (targetClass.equals(Instant.class)) {
      return (T) value.toInstant();
    } else {
      ZoneId zoneId = AbstractTemporalConverter.resolveHintZoneId(hints)
          .orElseGet(() -> !AbstractConverter.isStrict(hints) ? ZoneId.systemDefault() : null);
      if (zoneId == null) {
        throw new ConversionException("Can't convert Date %s to %s, zone id not find!", value,
            targetClass.getName());
      }
      if (targetClass.equals(ZonedDateTime.class)) {
        return (T) ZonedDateTime.ofInstant(value.toInstant(), zoneId);
      } else if (targetClass.equals(OffsetDateTime.class)) {
        return (T) OffsetDateTime.ofInstant(value.toInstant(), zoneId);
      } else if (targetClass.equals(LocalDateTime.class)) {
        return (T) LocalDateTime.ofInstant(value.toInstant(), zoneId);
      } else if (targetClass.equals(LocalDate.class)) {
        return (T) LocalDate.ofInstant(value.toInstant(), zoneId);
      }
    }
    throw new ConversionException("Can't convert Date %s to %s!", value, targetClass.getName());
  }

  @Override
  public Converter<Date, Temporal> create(Class<Temporal> targetClass, Temporal defaultValue,
      boolean throwException) {
    return (s, h) -> defaultObject(convert(s, targetClass, h), defaultValue);
  }

  @Override
  public boolean isSupportSourceClass(Class<?> sourceClass) {
    return Date.class.equals(sourceClass) || java.sql.Date.class.equals(sourceClass);
  }

  @Override
  public boolean isSupportTargetClass(Class<?> targetClass) {
    return targetClass.equals(Instant.class) || targetClass.equals(ZonedDateTime.class)
        || targetClass.equals(OffsetDateTime.class) || targetClass.equals(LocalDateTime.class)
        || targetClass.equals(LocalDate.class);
  }

}

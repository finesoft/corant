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
package org.corant.shared.conversion.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.Optional;

/**
 * corant-shared
 *
 * @author bingo 上午10:47:31
 *
 */
public class TemporalLocalDateTimeConverter
    extends AbstractTemporalConverter<Temporal, LocalDateTime> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public TemporalLocalDateTimeConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public TemporalLocalDateTimeConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public TemporalLocalDateTimeConverter(LocalDateTime defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public TemporalLocalDateTimeConverter(LocalDateTime defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected LocalDateTime convert(Temporal value, Map<String, ?> hints) throws Exception {
    Optional<ZoneId> zoneId = resolveHintZoneId(hints);
    if (zoneId.isPresent()) {
      // violate JSR-310
      if (value instanceof Instant) {
        return ((Instant) value).atZone(zoneId.get()).toLocalDateTime();
      } else if (value instanceof LocalDate) {
        warn(LocalDateTime.class, value);
        return LocalDateTime.of((LocalDate) value, LocalTime.ofNanoOfDay(0L));
      }
    } else if (!isStrict(hints)) {
      // violate JSR-310
      warn(LocalDateTime.class, value);
      if (value instanceof Instant) {
        return ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDateTime();
      } else if (value instanceof LocalDate) {
        return LocalDateTime.of((LocalDate) value, LocalTime.ofNanoOfDay(0L));
      }
    }
    return LocalDateTime.from(value);
  }

}

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

/**
 * corant-shared
 *
 * @author bingo 上午10:47:31
 *
 */
public class TemporalInstantConverter extends AbstractTemporalConverter<Temporal, Instant> {

  public TemporalInstantConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public TemporalInstantConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public TemporalInstantConverter(Instant defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public TemporalInstantConverter(Instant defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Instant convert(Temporal value, Map<String, ?> hints) throws Exception {
    if (value == null) {
      return getDefaultValue();
    }
    ZoneId zoneId = resolveHintZoneId(hints).orElse(null);
    if (zoneId != null) {
      // violate JSR-310
      if (value instanceof LocalDateTime) {
        return ((LocalDateTime) value).atZone(zoneId).toInstant();
      } else if (value instanceof LocalDate) {
        return ((LocalDate) value).atTime(LocalTime.ofNanoOfDay(0L)).atZone(zoneId).toInstant();
      }
    }
    return Instant.from(value);
  }

}

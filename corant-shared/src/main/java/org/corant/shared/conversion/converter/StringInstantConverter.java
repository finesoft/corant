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

import static org.corant.shared.util.Strings.isDecimalNumber;
import static org.corant.shared.util.Strings.split;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.Optional;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringInstantConverter extends AbstractTemporalConverter<String, Instant> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public StringInstantConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public StringInstantConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public StringInstantConverter(Instant defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public StringInstantConverter(Instant defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected Instant doConvert(String value, Map<String, ?> hints) throws Exception {
    if (value.isEmpty()) {
      return getDefaultValue();
    }
    String val = value.trim();
    if (isDecimalNumber(val)) {
      return Instant.ofEpochMilli(Long.parseLong(val));
    } else if (val.contains(",")) {
      String[] arr = split(val, ",", true, true);
      if (arr.length == 2 && isDecimalNumber(arr[0]) && isDecimalNumber(arr[1])) {
        return Instant.ofEpochSecond(Long.parseLong(arr[0]), Long.parseLong(arr[1]));
      }
    }
    Optional<DateTimeFormatter> hintDtf = resolveHintFormatter(hints);
    Optional<ZoneId> ozoneId = resolveHintZoneId(hints);
    boolean strictly = isStrict(hints);
    if (hintDtf.isPresent()) {
      return hintDtf.get().parse(val, Instant::from);// strictly
    } else {
      TemporalFormatter m = decideFormatter(val).orElse(null);
      if (m != null) {
        if (m.withTime) {
          TemporalAccessor ta = m.formatter.parseBest(val, Instant::from, ZonedDateTime::from,
              OffsetDateTime::from, LocalDateTime::from);
          if (ta instanceof Instant) {
            return (Instant) ta;
          } else if (ta instanceof ZonedDateTime) {
            return ((ZonedDateTime) ta).toInstant();
          } else if (ta instanceof OffsetDateTime) {
            return ((OffsetDateTime) ta).toInstant();
          } else if (ta instanceof LocalDateTime) {
            if (ozoneId.isPresent()) {
              return ((LocalDateTime) ta).atZone(ozoneId.get()).toInstant();
            } else if (!strictly) {
              warn(Instant.class, val);
              return ((LocalDateTime) ta).atZone(ZoneId.systemDefault()).toInstant();
            }
          }
        } else if (!strictly) {
          warn(Instant.class, val);
          LocalDate ta = m.formatter.parse(val, LocalDate::from);
          return ta.atStartOfDay(ozoneId.orElse(ZoneId.systemDefault())).toInstant();
        }
      }
      return Instant.parse(val);
    }
  }
}

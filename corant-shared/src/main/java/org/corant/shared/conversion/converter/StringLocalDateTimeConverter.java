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

import static org.corant.shared.util.Chars.isDecimalNumber;
import static org.corant.shared.util.Empties.isEmpty;
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
public class StringLocalDateTimeConverter extends AbstractTemporalConverter<String, LocalDateTime> {

  public StringLocalDateTimeConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public StringLocalDateTimeConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public StringLocalDateTimeConverter(LocalDateTime defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public StringLocalDateTimeConverter(LocalDateTime defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected LocalDateTime convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    String val = value.trim();
    Optional<DateTimeFormatter> hintDtf = resolveHintFormatter(hints);
    Optional<ZoneId> ozoneId = resolveHintZoneId(hints);
    boolean strictly = isStrict(hints);
    if (val.contains(",")) {
      // violate JSR-310
      String[] arr = split(val, ",", true, true);
      if (arr.length == 2 && isDecimalNumber(arr[0]) && isDecimalNumber(arr[1])) {
        if (ozoneId.isPresent()) {
          return Instant.ofEpochSecond(Long.parseLong(arr[0]), Long.parseLong(arr[1]))
              .atZone(ozoneId.get()).toLocalDateTime();
        } else if (!strictly) {
          warn(LocalDateTime.class, val);
          return Instant.ofEpochSecond(Long.parseLong(arr[0]), Long.parseLong(arr[1]))
              .atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
      }
    }

    if (hintDtf.isPresent()) {
      return hintDtf.get().parse(val, LocalDateTime::from);// strictly
    } else {
      TemporalFormatter m = decideFormatter(val).orElse(null);
      if (m != null) {
        if (m.withTime) {
          TemporalAccessor ta = m.formatter.parseBest(val, LocalDateTime::from, ZonedDateTime::from,
              OffsetDateTime::from, Instant::from);
          if (ta instanceof LocalDateTime || ta instanceof ZonedDateTime
              || ta instanceof OffsetDateTime) {
            return LocalDateTime.from(ta);
          } else if (ta instanceof Instant) {
            if (ozoneId.isPresent()) {
              return ((Instant) ta).atZone(ozoneId.get()).toLocalDateTime();
            } else if (!strictly) {
              warn(LocalDateTime.class, val);
              return ((Instant) ta).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
          }
        } else if (!strictly) {
          warn(LocalDateTime.class, val);
          return m.formatter.parse(val, LocalDate::from).atStartOfDay();
        }
      }
      return LocalDateTime.parse(val);
    }
  }
}

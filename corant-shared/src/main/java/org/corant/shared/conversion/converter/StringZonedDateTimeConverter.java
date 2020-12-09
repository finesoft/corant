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
public class StringZonedDateTimeConverter extends AbstractTemporalConverter<String, ZonedDateTime> {

  public StringZonedDateTimeConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public StringZonedDateTimeConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public StringZonedDateTimeConverter(ZonedDateTime defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public StringZonedDateTimeConverter(ZonedDateTime defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected ZonedDateTime convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    String val = value.trim();
    boolean strictly = isStrict(hints);
    Optional<ZoneId> ozoneId = resolveHintZoneId(hints);
    if (val.contains(",")) {
      String[] arr = split(val, ",", true, true);
      if (arr.length == 2 && isDecimalNumber(arr[0]) && isDecimalNumber(arr[1])) {
        if (ozoneId.isPresent()) {
          return ZonedDateTime.ofInstant(
              Instant.ofEpochSecond(Long.parseLong(arr[0]), Long.parseLong(arr[1])), ozoneId.get());
        } else if (!strictly) {
          warn(ZonedDateTime.class, val);
          return ZonedDateTime.ofInstant(
              Instant.ofEpochSecond(Long.parseLong(arr[0]), Long.parseLong(arr[1])),
              ZoneId.systemDefault());
        }
      }
    }
    Optional<DateTimeFormatter> hintDtf = resolveHintFormatter(hints);
    if (hintDtf.isPresent()) {
      return hintDtf.get().parse(val, ZonedDateTime::from);// strictly
    } else {
      TemporalFormatter m = decideFormatter(val).orElse(null);
      if (m != null) {
        if (m.withTime) {
          TemporalAccessor ta = m.formatter.parseBest(val, ZonedDateTime::from,
              OffsetDateTime::from, LocalDateTime::from, Instant::from);
          if (ta instanceof Instant) {
            if (ozoneId.isPresent()) {
              return ((Instant) ta).atZone(ozoneId.get());
            } else if (!strictly) {
              warn(ZonedDateTime.class, val);
              return ((Instant) ta).atZone(ZoneId.systemDefault());
            }
          } else if (ta instanceof ZonedDateTime) {
            return (ZonedDateTime) ta;
          } else if (ta instanceof OffsetDateTime) {
            return ((OffsetDateTime) ta).toZonedDateTime();
          } else if (ta instanceof LocalDateTime) {
            if (ozoneId.isPresent()) {
              return ((LocalDateTime) ta).atZone(ozoneId.get());
            } else if (!strictly) {
              warn(ZonedDateTime.class, val);
              return ((LocalDateTime) ta).atZone(ZoneId.systemDefault());
            }
          }
        } else if (!strictly) {
          warn(ZonedDateTime.class, val);
          LocalDate ta = m.formatter.parse(val, LocalDate::from);
          return ta.atStartOfDay(ozoneId.orElse(ZoneId.systemDefault()));
        }
      }
      return ZonedDateTime.parse(val);
    }
  }

}

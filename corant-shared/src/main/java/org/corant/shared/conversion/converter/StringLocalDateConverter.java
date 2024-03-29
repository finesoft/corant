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
public class StringLocalDateConverter extends AbstractTemporalConverter<String, LocalDate> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public StringLocalDateConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public StringLocalDateConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public StringLocalDateConverter(LocalDate defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public StringLocalDateConverter(LocalDate defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected LocalDate doConvert(String value, Map<String, ?> hints) throws Exception {
    if (value.isEmpty()) {
      return getDefaultValue();
    }
    String val = value.trim();
    Optional<DateTimeFormatter> hintDtf = resolveHintFormatter(hints);
    Optional<ZoneId> ozoneId = resolveHintZoneId(hints);
    boolean strictly = isStrict(hints);
    if (hintDtf.isPresent()) {
      return hintDtf.get().parse(val, LocalDate::from);// strictly
    } else {
      TemporalFormatter m = decideFormatter(val).orElse(null);
      if (m != null) {
        if (!m.withTime) {
          return m.formatter.parse(val, LocalDate::from);
        } else {
          TemporalAccessor ta = m.formatter.parseBest(val, LocalDateTime::from, ZonedDateTime::from,
              OffsetDateTime::from, Instant::from);
          if (ta instanceof Instant) {
            if (ozoneId.isPresent()) {
              return ((Instant) ta).atZone(ozoneId.get()).toLocalDate();
            } else if (!strictly) {
              warn(LocalDate.class, val);
              return ((Instant) ta).atZone(ZoneId.systemDefault()).toLocalDate();
            }
          } else if (ta instanceof ZonedDateTime) {
            return ((ZonedDateTime) ta).toLocalDate();
          } else if (ta instanceof OffsetDateTime) {
            return ((OffsetDateTime) ta).toLocalDate();
          } else if (ta instanceof LocalDateTime) {
            return ((LocalDateTime) ta).toLocalDate();
          }
        }
      }
      return LocalDate.parse(val);
    }

  }

}

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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 下午5:35:33
 *
 */
public class NumberLocalDateConverter extends AbstractTemporalConverter<Number, LocalDate> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public NumberLocalDateConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public NumberLocalDateConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public NumberLocalDateConverter(LocalDate defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public NumberLocalDateConverter(LocalDate defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected LocalDate convert(Number value, Map<String, ?> hints) throws Exception {
    ChronoUnit cu = ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_EPOCH_KEY);
    Optional<ZoneId> ozoneId = resolveHintZoneId(hints);
    if (ozoneId.isPresent()) {
      if (ChronoUnit.MILLIS.equals(cu)) {
        return Instant.ofEpochMilli(value.longValue()).atZone(ozoneId.get()).toLocalDate();
      } else if (ChronoUnit.SECONDS.equals(cu)) {
        return Instant.ofEpochSecond(value.longValue()).atZone(ozoneId.get()).toLocalDate();
      }
    } else if (!isStrict(hints)) {
      warn(LocalDate.class, value);
      if (ChronoUnit.MILLIS.equals(cu)) {
        return Instant.ofEpochMilli(value.longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
      } else if (ChronoUnit.SECONDS.equals(cu)) {
        return Instant.ofEpochSecond(value.longValue()).atZone(ZoneId.systemDefault())
            .toLocalDate();
      }
    }
    return LocalDate.ofEpochDay(value.longValue());
  }
}

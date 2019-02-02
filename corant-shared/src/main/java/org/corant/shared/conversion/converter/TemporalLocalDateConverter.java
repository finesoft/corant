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
import java.time.temporal.Temporal;
import java.util.Map;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 上午10:47:31
 *
 */
public class TemporalLocalDateConverter extends AbstractConverter<Temporal, LocalDate> {

  public TemporalLocalDateConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public TemporalLocalDateConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public TemporalLocalDateConverter(LocalDate defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public TemporalLocalDateConverter(LocalDate defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected LocalDate convert(Temporal value, Map<String, ?> hints) throws Exception {
    ZoneId zoneId = null;
    Object hintZoneId = ConverterHints.getHint(hints, ConverterHints.CVT_ZONE_ID_KEY);
    if (hintZoneId instanceof ZoneId) {
      zoneId = (ZoneId) hintZoneId;
    } else if (hintZoneId instanceof String) {
      zoneId = ZoneId.of(hintZoneId.toString());
    }
    if (zoneId != null && value instanceof Instant) {
      // violate JSR-310
      return Instant.class.cast(value).atZone(zoneId).toLocalDate();
    }
    return LocalDate.from(value);
  }

}

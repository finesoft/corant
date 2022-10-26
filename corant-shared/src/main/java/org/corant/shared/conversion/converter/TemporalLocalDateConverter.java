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
import java.util.Optional;

/**
 * corant-shared
 *
 * @author bingo 上午10:47:31
 *
 */
public class TemporalLocalDateConverter extends AbstractTemporalConverter<Temporal, LocalDate> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public TemporalLocalDateConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public TemporalLocalDateConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public TemporalLocalDateConverter(LocalDate defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public TemporalLocalDateConverter(LocalDate defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected LocalDate doConvert(Temporal value, Map<String, ?> hints) throws Exception {
    Optional<ZoneId> zoneId = resolveHintZoneId(hints);
    if (value instanceof Instant) {
      // violate JSR-310
      if (zoneId.isPresent()) {
        return ((Instant) value).atZone(zoneId.get()).toLocalDate();
      } else if (!isStrict(hints)) {
        warn(LocalDate.class, value);
        return ((Instant) value).atZone(ZoneId.systemDefault()).toLocalDate();
      }
    }
    return LocalDate.from(value);
  }

}

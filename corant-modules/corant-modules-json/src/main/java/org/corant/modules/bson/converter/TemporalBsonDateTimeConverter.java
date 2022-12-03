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
package org.corant.modules.bson.converter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.Temporal;
import java.util.Map;
import org.bson.BsonDateTime;
import org.corant.shared.conversion.converter.AbstractConverter;
import org.corant.shared.conversion.converter.AbstractTemporalConverter;

/**
 * corant-modules-json
 *
 * @author bingo 上午10:47:31
 *
 */
public class TemporalBsonDateTimeConverter extends AbstractConverter<Temporal, BsonDateTime> {

  public TemporalBsonDateTimeConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public TemporalBsonDateTimeConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public TemporalBsonDateTimeConverter(BsonDateTime defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public TemporalBsonDateTimeConverter(BsonDateTime defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected BsonDateTime doConvert(Temporal value, Map<String, ?> hints) throws Exception {
    // violate JSR-310
    ZoneId zoneId =
        AbstractTemporalConverter.resolveHintZoneId(hints).orElseGet(ZoneId::systemDefault);
    if (value instanceof LocalDateTime) {
      return new BsonDateTime(((LocalDateTime) value).atZone(zoneId).toInstant().toEpochMilli());
    } else if (value instanceof LocalDate) {
      return new BsonDateTime(((LocalDate) value).atTime(LocalTime.ofNanoOfDay(0L)).atZone(zoneId)
          .toInstant().toEpochMilli());
    } else if (value instanceof OffsetDateTime) {
      return new BsonDateTime(
          ((OffsetDateTime) value).atZoneSameInstant(zoneId).toInstant().toEpochMilli());
    }
    return new BsonDateTime(Instant.from(value).toEpochMilli());
  }

}

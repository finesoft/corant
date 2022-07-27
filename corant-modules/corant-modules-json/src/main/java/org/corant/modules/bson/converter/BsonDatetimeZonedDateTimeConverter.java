/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import org.bson.BsonDateTime;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.converter.AbstractConverter;
import org.corant.shared.conversion.converter.AbstractTemporalConverter;

/**
 * corant-modules-json
 *
 * @author bingo 上午10:14:32
 *
 */
public class BsonDatetimeZonedDateTimeConverter
    extends AbstractConverter<BsonDateTime, ZonedDateTime> {

  @Override
  protected ZonedDateTime convert(BsonDateTime value, Map<String, ?> hints) throws Exception {
    if (value == null) {
      return null;
    }
    Instant instant = Instant.ofEpochMilli(value.getValue());
    Optional<ZoneId> zoneId = AbstractTemporalConverter.resolveHintZoneId(hints);
    // violate JSR-310
    if (zoneId.isPresent()) {
      return instant.atZone(zoneId.get());
    } else if (!isStrict(hints)) {
      warn(LocalDate.class, value);
      return instant.atZone(ZoneId.systemDefault());
    }
    throw new ConversionException("Can't convert bson date time to instant.");
  }

}

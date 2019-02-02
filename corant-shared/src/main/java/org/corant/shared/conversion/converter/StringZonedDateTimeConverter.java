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

import static org.corant.shared.util.Empties.isEmpty;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringZonedDateTimeConverter extends AbstractConverter<String, ZonedDateTime> {

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
    DateTimeFormatter dtf = ConverterHints.getHint(hints, ConverterHints.CVT_DATE_FMT_KEY);
    if (dtf == null
        && ConverterHints.containsKeyWithNnv(hints, ConverterHints.CVT_DATE_FMT_PTN_KEY)) {
      dtf = DateTimeFormatter
          .ofPattern(ConverterHints.getHint(hints, ConverterHints.CVT_DATE_FMT_PTN_KEY));
    }
    ZoneId zoneId = ZoneId.systemDefault();
    Object hintZoneId = ConverterHints.getHint(hints, ConverterHints.CVT_ZONE_ID_KEY);
    if (hintZoneId instanceof ZoneId) {
      zoneId = (ZoneId) hintZoneId;
    } else if (hintZoneId instanceof String) {
      zoneId = ZoneId.of(hintZoneId.toString());
    }

    if (dtf != null) {
      return ZonedDateTime.parse(value, dtf);
    } else if (value.indexOf('[') != -1 && value.indexOf(']') != -1) {
      return ZonedDateTime.parse(value);
    } else {
      StringBuilder zone = new StringBuilder("[").append(zoneId.toString()).append("]");
      return ZonedDateTime.parse(value + zone);
    }
  }
}

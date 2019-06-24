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

import static org.corant.shared.util.MapUtils.getMapInteger;
import static org.corant.shared.util.MapUtils.getMapLong;
import static org.corant.shared.util.MapUtils.getMapString;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.corant.shared.conversion.ConversionException;

/**
 * corant-shared
 *
 * @author bingo 上午10:47:31
 *
 */
@SuppressWarnings("rawtypes")
public class MapLocalDateTimeConverter extends AbstractConverter<Map, LocalDateTime> {

  public MapLocalDateTimeConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public MapLocalDateTimeConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public MapLocalDateTimeConverter(LocalDateTime defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public MapLocalDateTimeConverter(LocalDateTime defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected LocalDateTime convert(Map value, Map<String, ?> hints) throws Exception {
    if (value != null && value.containsKey("year") && value.containsKey("month")
        && (value.containsKey("day") || value.containsKey("dayOfMonth"))
        && value.containsKey("hour") && value.containsKey("minute")) {
      if (value.containsKey("second")) {
        if (value.containsKey("nanoOfSecond") || value.containsKey("nano")) {
          return LocalDateTime.of(getMapInteger(value, "year"), getMapInteger(value, "month"),
              defaultObject(getMapInteger(value, "dayOfMonth"), getMapInteger(value, "day")),
              getMapInteger(value, "hour"), getMapInteger(value, "minute"),
              getMapInteger(value, "second"),
              defaultObject(getMapInteger(value, "nanoOfSecond"), getMapInteger(value, "nano")));
        } else {
          return LocalDateTime.of(getMapInteger(value, "year"), getMapInteger(value, "month"),
              defaultObject(getMapInteger(value, "dayOfMonth"), getMapInteger(value, "day")),
              getMapInteger(value, "hour"), getMapInteger(value, "minute"),
              getMapInteger(value, "second"));
        }
      } else {
        return LocalDateTime.of(getMapInteger(value, "year"), getMapInteger(value, "month"),
            defaultObject(getMapInteger(value, "dayOfMonth"), getMapInteger(value, "day")),
            getMapInteger(value, "hour"), getMapInteger(value, "minute"));
      }
    } else if (value != null && value.containsKey("epochSecond")
        && value.containsKey("nanoOfSecond") && value.containsKey("offsetId")) {
      return LocalDateTime.ofEpochSecond(getMapLong(value, "epochSecond"),
          getMapInteger(value, "nanoOfSecond"), ZoneOffset.of(getMapString(value, "offsetId")));
    }
    throw new ConversionException("Can't convert value to LocalDateTime from Map object.");
  }
}
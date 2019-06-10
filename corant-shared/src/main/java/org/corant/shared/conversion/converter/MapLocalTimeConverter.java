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
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.time.LocalTime;
import java.util.Map;
import org.corant.shared.conversion.ConversionException;

/**
 * corant-shared
 *
 * @author bingo 上午10:47:31
 *
 */
@SuppressWarnings("rawtypes")
public class MapLocalTimeConverter extends AbstractConverter<Map, LocalTime> {

  public MapLocalTimeConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public MapLocalTimeConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public MapLocalTimeConverter(LocalTime defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public MapLocalTimeConverter(LocalTime defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected LocalTime convert(Map value, Map<String, ?> hints) throws Exception {
    // int hour, int minute, int second, int nanoOfSecond
    if (value != null && value.containsKey("hour") && value.containsKey("minute")) {
      if (value.containsKey("second")) {
        if (value.containsKey("nanoOfSecond") || value.containsKey("nano")) {
          return LocalTime.of(getMapInteger(value, "hour"), getMapInteger(value, "minute"),
              getMapInteger(value, "second"),
              defaultObject(getMapInteger(value, "nanoOfSecond"), getMapInteger(value, "nano")));
        } else {
          return LocalTime.of(getMapInteger(value, "hour"), getMapInteger(value, "minute"),
              getMapInteger(value, "second"));
        }
      } else {
        return LocalTime.of(getMapInteger(value, "hour"), getMapInteger(value, "minute"));
      }
    }
    throw new ConversionException("Can't convert value to LocalDate from Map object.");
  }
}

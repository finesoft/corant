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
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.time.LocalDate;
import java.util.Map;
import org.corant.shared.conversion.ConversionException;

/**
 * corant-shared
 *
 * @author bingo 上午10:47:31
 *
 */
@SuppressWarnings("rawtypes")
public class MapLocalDateConverter extends AbstractConverter<Map, LocalDate> {

  public MapLocalDateConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public MapLocalDateConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public MapLocalDateConverter(LocalDate defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public MapLocalDateConverter(LocalDate defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected LocalDate convert(Map value, Map<String, ?> hints) throws Exception {
    if (value != null && value.containsKey("year") && value.containsKey("month")
        && (value.containsKey("day") || value.containsKey("dayOfMonth"))) {
      return LocalDate.of(getMapInteger(value, "year"), getMapInteger(value, "month"),
          defaultObject(getMapInteger(value, "dayOfMonth"), getMapInteger(value, "day")));
    } else if (value != null && value.containsKey("year") && value.containsKey("dayOfYear")) {
      return LocalDate.ofYearDay(getMapInteger(value, "year"), getMapInteger(value, "dayOfYear"));
    } else if (value != null && value.containsKey("epochDay")) {
      return LocalDate.ofEpochDay(getMapLong(value, "epochDay"));
    }
    throw new ConversionException("Can't convert value to LocalDate from Map object.");
  }
}

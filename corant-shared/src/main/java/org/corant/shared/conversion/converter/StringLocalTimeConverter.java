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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringLocalTimeConverter extends AbstractConverter<String, LocalTime> {

  public StringLocalTimeConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public StringLocalTimeConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public StringLocalTimeConverter(LocalTime defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public StringLocalTimeConverter(LocalTime defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected LocalTime convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    DateTimeFormatter dtf = ConverterHints.getHint(hints, ConverterHints.CVT_DATE_FMT_KEY);
    if (dtf == null) {
      String hintPtn = ConverterHints.getHint(hints, ConverterHints.CVT_DATE_FMT_PTN_KEY);
      if (hintPtn != null) {
        dtf = DateTimeFormatter.ofPattern(hintPtn);
      }
    }
    if (dtf != null) {
      return LocalTime.parse(value, dtf);
    } else {
      return LocalTime.parse(value, DateTimeFormatter.ISO_TIME);
    }
  }

}

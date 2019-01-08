/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.shared.conversion.converter;

import static org.corant.shared.util.StringUtils.isEmpty;
import static org.corant.shared.util.StringUtils.split;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.corant.shared.conversion.ConversionException;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringLocalDateConverter extends AbstractConverter<String, LocalDate> {

  public StringLocalDateConverter() {
    super();
  }

  /**
   * @param defaultValue
   */
  public StringLocalDateConverter(LocalDate defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param useNullValueIfErr
   * @param useDefaultValueIfErr
   */
  public StringLocalDateConverter(LocalDate defaultValue, boolean useNullValueIfErr,
      boolean useDefaultValueIfErr) {
    super(defaultValue, useNullValueIfErr, useDefaultValueIfErr);
  }

  @Override
  protected LocalDate convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    DateTimeFormatter dtf = ConverterHints.getHint(hints, ConverterHints.CVT_DATE_FMT_KEY);
    if (dtf == null
        && ConverterHints.containsKeyWithNnv(hints, ConverterHints.CVT_DATE_FMT_PTN_KEY)) {
      dtf = DateTimeFormatter
          .ofPattern(ConverterHints.getHint(hints, ConverterHints.CVT_DATE_FMT_PTN_KEY));
    }
    if (dtf != null) {
      LocalDate.parse(value, dtf);
    } else if (value.chars().allMatch(Character::isDigit)) {
      return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
    } else {
      String[] values = split(value, c -> c == '-' || c == ' ');
      String fixedValue = String.join("-", values);
      if (values.length == 3) {
        if (values[1].chars().allMatch(Character::isDigit)) {
          if (values[2].contains("+")) {
            return LocalDate.parse(fixedValue, DateTimeFormatter.ISO_OFFSET_DATE);
          } else {
            return LocalDate.parse(fixedValue, DateTimeFormatter.ISO_LOCAL_DATE);
          }
        } else {
          return LocalDate.parse(fixedValue, DateTimeFormatter.ISO_WEEK_DATE);
        }
      } else if (values.length == 2) {
        return LocalDate.parse(fixedValue, DateTimeFormatter.ISO_ORDINAL_DATE);
      }
    }
    throw new ConversionException(new NotSupportedException(),
        "Can not convert String value '%s' to LocalDate!", value);
  }

}

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

import static org.corant.shared.util.StringUtils.isEmpty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringLocalDateTimeConverter extends AbstractConverter<String, LocalDateTime> {

  /**
   * @param defaultValue
   */
  public StringLocalDateTimeConverter(LocalDateTime defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param useNullValueIfErr
   * @param useDefaultValueIfErr
   */
  public StringLocalDateTimeConverter(LocalDateTime defaultValue, boolean useNullValueIfErr,
      boolean useDefaultValueIfErr) {
    super(defaultValue, useNullValueIfErr, useDefaultValueIfErr);
  }

  @Override
  protected LocalDateTime convert(String value, Map<String, ?> hints) throws Exception {
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
      return LocalDateTime.parse(value, dtf);
    } else if (value.contains("T")) {
      return LocalDateTime.parse(value);
    } else {
      return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
  }
}

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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 */
public class StringLocalTimeConverter extends AbstractTemporalConverter<String, LocalTime> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public StringLocalTimeConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public StringLocalTimeConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public StringLocalTimeConverter(LocalTime defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public StringLocalTimeConverter(LocalTime defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected LocalTime doConvert(String value, Map<String, ?> hints) throws Exception {
    if (value.isEmpty()) {
      return getDefaultValue();
    }
    String val = value.trim();
    Optional<DateTimeFormatter> dtf = resolveHintFormatter(hints);
    if (dtf.isPresent()) {
      return dtf.get().parse(val, LocalTime::from);
    } else {
      return LocalTime.parse(val, DateTimeFormatter.ISO_TIME);
    }
  }

}

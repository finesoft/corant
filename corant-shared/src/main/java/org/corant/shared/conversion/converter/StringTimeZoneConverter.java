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
import java.util.Map;
import java.util.TimeZone;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringTimeZoneConverter extends AbstractConverter<String, TimeZone> {

  /**
   * @param defaultValue
   */
  public StringTimeZoneConverter(TimeZone defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param useNullValueIfErr
   * @param useDefaultValueIfErr
   */
  public StringTimeZoneConverter(TimeZone defaultValue, boolean useNullValueIfErr,
      boolean useDefaultValueIfErr) {
    super(defaultValue, useNullValueIfErr, useDefaultValueIfErr);
  }

  @Override
  protected TimeZone convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    return TimeZone.getTimeZone(value);
  }

}

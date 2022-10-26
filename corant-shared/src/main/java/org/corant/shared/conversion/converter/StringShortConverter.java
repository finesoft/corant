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

import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringShortConverter extends AbstractNumberConverter<String, Short> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public StringShortConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public StringShortConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public StringShortConverter(Short defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public StringShortConverter(Short defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Short doConvert(String value, Map<String, ?> hints) throws Exception {
    if (value.isEmpty()) {
      return getDefaultValue();
    } else {
      String val = stripTrailingZeros(value);
      if (isDecodable(val, hints)) {
        return Short.decode(val);
      } else {
        return Short.valueOf(val, getHintsRadix(hints));
      }
    }
  }

}

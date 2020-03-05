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
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringIntegerConverter extends AbstractNumberConverter<String, Integer> {

  public StringIntegerConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public StringIntegerConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public StringIntegerConverter(Integer defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public StringIntegerConverter(Integer defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Integer convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    } else if (hasHex(value)) {
      return Integer.decode(value);
    } else {
      Integer radix = getHintsRadix(hints);
      if (isHintsUnsigned(hints)) {
        if (radix != null) {
          return Integer.parseUnsignedInt(value, radix);
        } else {
          return Integer.parseUnsignedInt(value);
        }
      } else {
        if (radix != null) {
          return Integer.valueOf(value, radix);
        } else {
          return Integer.valueOf(value);
        }
      }
    }
  }

}

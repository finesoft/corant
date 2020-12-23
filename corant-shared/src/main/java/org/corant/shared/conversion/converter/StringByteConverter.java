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
public class StringByteConverter extends AbstractNumberConverter<String, Byte> {

  public StringByteConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public StringByteConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public StringByteConverter(Byte defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public StringByteConverter(Byte defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Byte convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    } else {
      String val = value.trim();
      if (hasHex(val)) {
        return Byte.decode(val);
      } else {
        Integer radix = getHintsRadix(hints);
        return radix != null ? Byte.valueOf(val, radix) : Byte.valueOf(val);
      }
    }
  }

}

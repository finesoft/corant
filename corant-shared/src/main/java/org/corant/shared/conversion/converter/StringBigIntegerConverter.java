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
import java.math.BigInteger;
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 下午6:10:26
 *
 */
public class StringBigIntegerConverter extends AbstractNumberConverter<String, BigInteger> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public StringBigIntegerConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public StringBigIntegerConverter(BigInteger defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public StringBigIntegerConverter(BigInteger defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public StringBigIntegerConverter(boolean throwException) {
    super(throwException);
  }

  @Override
  protected BigInteger convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    } else {
      String val = stripTrailingZeros(value);
      if (hasPrefix(val)) {
        return BigInteger.valueOf(Long.decode(val));
      }
      return new BigInteger(val, getHintsRadix(hints));
    }
  }

}

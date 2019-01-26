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
import java.math.BigDecimal;
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 下午6:10:26
 *
 */
public class StringBigDecimalConverter extends AbstractConverter<String, BigDecimal> {

  public StringBigDecimalConverter() {
    super();
  }

  /**
   * @param defaultValue
   */
  public StringBigDecimalConverter(BigDecimal defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public StringBigDecimalConverter(BigDecimal defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  /**
   * @param throwException
   */
  public StringBigDecimalConverter(boolean throwException) {
    super(throwException);
  }

  @Override
  protected BigDecimal convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    return new BigDecimal(value);
  }

}

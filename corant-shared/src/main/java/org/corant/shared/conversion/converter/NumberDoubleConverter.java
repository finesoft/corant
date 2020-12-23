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
 * @author bingo 下午5:35:33
 *
 */
public class NumberDoubleConverter extends AbstractConverter<Number, Double> {

  public NumberDoubleConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public NumberDoubleConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public NumberDoubleConverter(Double defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public NumberDoubleConverter(Double defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Double convert(Number value, Map<String, ?> hints) throws Exception {
    if (value instanceof Double) {
      return (Double) value;
    } else if (value == null) {
      return getDefaultValue();
    }
    return value.doubleValue();
  }

}

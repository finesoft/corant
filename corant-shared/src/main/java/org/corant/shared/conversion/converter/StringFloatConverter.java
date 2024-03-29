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
 * @author bingo 下午6:21:39
 *
 */
public class StringFloatConverter extends AbstractNumberConverter<String, Float> {

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public StringFloatConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public StringFloatConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public StringFloatConverter(Float defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public StringFloatConverter(Float defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Float doConvert(String value, Map<String, ?> hints) throws Exception {
    if (value.isEmpty()) {
      return getDefaultValue();
    }
    return Float.valueOf(value.trim());
  }
}

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
public class NumberLongConverter extends AbstractConverter<Number, Long> {

  private static final long serialVersionUID = 1890230341337567700L;

  public NumberLongConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public NumberLongConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public NumberLongConverter(Long defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public NumberLongConverter(Long defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Long convert(Number value, Map<String, ?> hints) throws Exception {
    if (value instanceof Long) {
      return (Long) value;
    } else if (value == null) {
      return getDefaultValue();
    }
    return Long.valueOf(value.longValue());
  }

}

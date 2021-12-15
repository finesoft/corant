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

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public NumberLongConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public NumberLongConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public NumberLongConverter(Long defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public NumberLongConverter(Long defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Long convert(Number value, Map<String, ?> hints) throws Exception {
    if (value instanceof Long) {
      return (Long) value;
    }
    return value.longValue();
  }

}

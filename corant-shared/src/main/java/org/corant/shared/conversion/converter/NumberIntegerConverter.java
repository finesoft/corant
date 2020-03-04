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
import org.corant.shared.conversion.ConversionException;

/**
 * corant-shared
 *
 * @author bingo 下午5:35:33
 *
 */
public class NumberIntegerConverter extends AbstractConverter<Number, Integer> {

  public NumberIntegerConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public NumberIntegerConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public NumberIntegerConverter(Integer defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public NumberIntegerConverter(Integer defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Integer convert(Number value, Map<String, ?> hints) throws Exception {
    if (value instanceof Integer) {
      return (Integer) value;
    } else if (value == null) {
      return getDefaultValue();
    }
    final long longValue = value.longValue();
    if (longValue > Integer.MAX_VALUE) {
      throw new ConversionException("Can not convert, the source value is to big for integer!");
    }
    if (longValue < Integer.MIN_VALUE) {
      throw new ConversionException("Can not convert, the source value is to small for integer!");
    }
    return Integer.valueOf(value.intValue());
  }

}

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
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 上午11:03:47
 *
 */
public abstract class AbstractNumberConverter<S, T extends Number> extends AbstractConverter<S, T> {

  static final String[] HEX_PREFIXES =
      {"0x", "0X", "-0x", "-0X", "+0x", "+0X", "#", "-#", "+#", "0", "-0", "+0"};

  /**
   *
   */
  protected AbstractNumberConverter() {}

  /**
   * @param throwException
   */
  protected AbstractNumberConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  protected AbstractNumberConverter(T defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  protected AbstractNumberConverter(T defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  public boolean hasHex(String value) {
    if (value == null) {
      return false;
    } else {
      for (String hp : HEX_PREFIXES) {
        if (value.startsWith(hp)) {
          return true;
        }
      }
    }
    return false;
  }

  protected Integer getHintsRadix(Map<String, ?> hints) {
    return ConverterHints.getHint(hints, ConverterHints.CVT_NUMBER_RADIX_KEY);
  }

  protected boolean isHintsUnsigned(Map<String, ?> hints) {
    return ConverterHints.getHint(hints, ConverterHints.CVT_NUMBER_UNSIGNED_KEY, false);
  }
}

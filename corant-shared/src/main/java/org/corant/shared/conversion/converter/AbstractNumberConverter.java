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
import org.corant.shared.util.Chars;

/**
 * corant-shared
 *
 * @author bingo 上午11:03:47
 *
 */
public abstract class AbstractNumberConverter<S, T extends Number> extends AbstractConverter<S, T> {

  static final String[] NUMERIC_PREFIXES =
      {"0x", "0X", "-0x", "-0X", "+0x", "+0X", "#", "-#", "+#", "0", "-0", "+0"};

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  protected AbstractNumberConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  protected AbstractNumberConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  protected AbstractNumberConverter(T defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  protected AbstractNumberConverter(T defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  /**
   * Check if the string starts with the prefix of the number.
   *
   * @param value the string to check
   */
  public static boolean hasPrefix(String value) {
    if (value != null) {
      for (String hp : NUMERIC_PREFIXES) {
        if (value.startsWith(hp)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the radix contained in hints, or 10 if it does not exist.
   *
   * @param hints the hints may contain radix
   */
  protected static Integer getHintsRadix(Map<String, ?> hints) {
    return ConverterHints.getHint(hints, ConverterHints.CVT_NUMBER_RADIX_KEY, 10);
  }

  /**
   * Returns whether the hint contains unsigned
   *
   * @param hints the hints may contain unsigned
   */
  protected static boolean isHintsUnsigned(Map<String, ?> hints) {
    return ConverterHints.getHint(hints, ConverterHints.CVT_NUMBER_UNSIGNED_KEY, false);
  }

  /**
   * Returns a stripped decimal point and trailing zero string for integer type conversion. If it
   * cannot be stripped or it is empty after stripping, the original string is returned.
   *
   * @param value the value to strip
   */
  protected static String stripTrailingZeros(String value) {
    String sval = value.strip();
    String rval = sval;
    int di;
    if ((di = rval.indexOf(Chars.DOT)) != -1
        && rval.substring(di + 1).chars().allMatch(c -> c == Chars.ZERO)) {
      rval = rval.substring(0, di);
    }
    return rval.isEmpty() ? sval : rval;
  }
}

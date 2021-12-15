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
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 下午5:23:17
 *
 */
public class StringBooleanConverter extends AbstractConverter<String, Boolean> {

  private String[] trues = {"true", "yes", "y", "on", "1", "是"};
  // private String[] falses = {"false", "no", "n", "off", "0", "否"};

  public StringBooleanConverter() {
  }

  public StringBooleanConverter(Boolean defaultValue, String[] trues/* , String[] falses */) {
    super(defaultValue);
    if (trues != null) {
      this.trues = Arrays.stream(trues).map(String::toLowerCase).toArray(String[]::new);
    }
    // if (falses != null) {
    // this.falses = Arrays.stream(falses).map(String::toLowerCase).toArray(String[]::new);
    // }
  }

  @Override
  protected Boolean convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    final String stringValue = value.toLowerCase(Locale.getDefault()).trim();
    for (String trueString : trues) {
      if (trueString.equals(stringValue)) {
        return Boolean.TRUE;
      }
    }
    return Boolean.FALSE;
    // for (String falseString : falses) {
    // if (falseString.equals(stringValue)) {
    // return Boolean.FALSE;
    // }
    // }
    // throw new ConversionException("Can not convert from '%s' to boolean", value);
  }
}

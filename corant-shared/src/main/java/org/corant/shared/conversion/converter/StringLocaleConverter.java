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
import java.util.Locale;
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringLocaleConverter extends AbstractConverter<String, Locale> {

  public final static char IETF_SEPARATOR = '-';
  public final static String EMPTY_STRING = "";
  public final static Locale defaultLocale = Locale.getDefault();

  public StringLocaleConverter() {
    super();
  }

  /**
   * @param throwException
   */
  public StringLocaleConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  public StringLocaleConverter(Locale defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  public StringLocaleConverter(Locale defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  protected Locale convert(String value, Map<String, ?> hints) throws Exception {
    if (isEmpty(value)) {
      return getDefaultValue();
    }
    String language = EMPTY_STRING;
    String country = EMPTY_STRING;
    String variant = EMPTY_STRING;
    int i1 = value.indexOf(IETF_SEPARATOR);
    if (i1 < 0) {
      language = value;
    } else {
      language = value.substring(0, i1);
      ++i1;
      int i2 = value.indexOf(IETF_SEPARATOR, i1);
      if (i2 < 0) {
        country = value.substring(i1);
      } else {
        country = value.substring(i1, i2);
        variant = value.substring(i2 + 1);
      }
    }
    if (language.length() == 2) {
      language = language.toLowerCase(defaultLocale);
    } else {
      language = EMPTY_STRING;
    }
    if (country.length() == 2) {
      country = country.toUpperCase(defaultLocale);
    } else {
      country = EMPTY_STRING;
    }
    if (variant.length() > 0 && (language.length() == 2 || country.length() == 2)) {
      variant = variant.toUpperCase(defaultLocale);
    } else {
      variant = EMPTY_STRING;
    }
    return new Locale(language, country, variant);
  }

}

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
package org.corant.modules.bundle;

import static org.corant.shared.util.Strings.EMPTY;
import java.util.Locale;

/**
 * corant-modules-bundle
 *
 * @author bingo 下午10:26:42
 *
 */
public class LocaleUtils {

  /**
   * IETF RFC 1766 tag separator
   */
  public static final char IETF_SEPARATOR = '-';

  public static Locale langToLocale(String lang) {
    return langToLocale(lang, IETF_SEPARATOR);
  }

  public static Locale langToLocale(String lang, char separator) {
    if (lang == null || EMPTY.equals(lang)) { // not specified => getDefault
      return Locale.getDefault();
    }
    String language = EMPTY;
    String country = EMPTY;
    String variant = EMPTY;

    int i1 = lang.indexOf(separator);
    if (i1 < 0) {
      language = lang;
    } else {
      language = lang.substring(0, i1);
      ++i1;
      int i2 = lang.indexOf(separator, i1);
      if (i2 < 0) {
        country = lang.substring(i1);
      } else {
        country = lang.substring(i1, i2);
        variant = lang.substring(i2 + 1);
      }
    }

    if (language.length() == 2) {
      language = language.toLowerCase(Locale.ENGLISH);
    } else {
      language = EMPTY;
    }

    if (country.length() == 2) {
      country = country.toUpperCase(Locale.ENGLISH);
    } else {
      country = EMPTY;
    }

    if (variant.length() > 0 && (language.length() == 2 || country.length() == 2)) {
      variant = variant.toUpperCase(Locale.ENGLISH);
    } else {
      variant = EMPTY;
    }

    return new Locale(language, country, variant);
  }
}

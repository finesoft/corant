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
package org.corant.shared.conversion;

import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 下午5:49:59
 *
 */
public class ConverterHints {

  public static final int CVT_MAX_NEST_DEPT = 3;

  public static final String CVT_NEST_DEPT_KEY = "converter.max.nest.dept";

  public static final String CVT_DATE_FMT_PTN_KEY = "converter.date.formater.pattern";

  public static final String CVT_DATE_FMT_KEY = "converter.date.formater";

  public static final String CVT_ZONE_ID_KEY = "converter.zoneId";

  public static final String CVT_CLS_LOADER_KEY = "converter.classLoader";

  public static boolean containsKey(Map<String, ?> hints, String key) {
    return hints != null && hints.containsKey(key);
  }

  public static boolean containsKeyWithNnv(Map<String, ?> hints, String key) {
    return hints != null && hints.containsKey(key) && hints.get(key) != null;
  }

  public static <T> T getHint(Map<String, ?> hints, String key) {
    return getHint(hints, key, null);
  }

  @SuppressWarnings("unchecked")
  public static <T> T getHint(Map<String, ?> hints, String key, T altVal) {
    T hint = null;
    if (key != null && hints != null && hints.containsKey(key)) {
      Object obj = hints.get(key);
      if (obj != null) {
        hint = (T) obj;
      }
    }
    return hint == null ? altVal : hint;
  }

}

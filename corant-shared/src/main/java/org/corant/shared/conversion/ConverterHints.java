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

import static org.corant.shared.util.Objects.defaultObject;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import org.corant.shared.normal.Names;

/**
 * corant-shared
 *
 * @author bingo 下午5:49:59
 *
 */
public class ConverterHints {

  public static final int CVT_MAX_NEST_DEPT = 3;

  public static final String CVT_NEST_DEPT_KEY = "converter.max.nest.dept";

  public static final String CVT_NUMBER_RADIX_KEY = "converter.number.radix";

  public static final String CVT_NUMBER_UNSIGNED_KEY = "converter.number.unsigned";

  public static final String CVT_TEMPORAL_FMT_PTN_KEY = "converter.temporal.formater.pattern";

  public static final String CVT_TEMPORAL_FMT_KEY = "converter.temporal.formater";

  public static final String CVT_TEMPORAL_EPOCH_KEY = "converter.temporal.of-epoch";

  public static final String CVT_TEMPORAL_STRICTLY_KEY = "converter.temporal.strictly";

  public static final String CVT_BYTES_PRIMITIVE_STRICTLY_KEY =
      "converter.bytes-primitive.strictly";

  public static final String CVT_ZONE_ID_KEY = "converter.zone-id";

  public static final String CVT_LOCAL_KEY = "converter.local";

  public static final String CVT_CLS_LOADER_KEY = "converter.class-loader";

  private static final Map<String, ?> sys_hints = Collections.unmodifiableMap(resolveSysProHints());// static?

  public static boolean containsKey(Map<String, ?> hints, String key) {
    return hints != null && hints.containsKey(key) || sys_hints.containsKey(key);
  }

  public static <T> T getHint(Map<String, ?> hints, String key) {
    return getHint(hints, key, null);
  }

  @SuppressWarnings("unchecked")
  public static <T> T getHint(Map<String, ?> hints, String key, T altVal) {
    T hint = null;
    if (key != null && hints != null) {
      Object obj = hints.get(key);
      if (obj != null) {
        hint = (T) obj;
      }
    }
    return (T) defaultObject(hint, () -> defaultObject(sys_hints.get(key), altVal));
  }

  static Map<String, Object> resolveSysProHints() {
    Map<String, Object> map = new HashMap<>();
    resolveSysProHints(map, CVT_NEST_DEPT_KEY, Integer::valueOf);
    resolveSysProHints(map, CVT_ZONE_ID_KEY, ZoneId::of);
    resolveSysProHints(map, CVT_LOCAL_KEY, Locale::forLanguageTag);
    resolveSysProHints(map, CVT_TEMPORAL_FMT_PTN_KEY, s -> s);
    resolveSysProHints(map, CVT_TEMPORAL_FMT_KEY,
        s -> map.get(CVT_LOCAL_KEY) != null
            ? DateTimeFormatter.ofPattern(s, (Locale) map.get(CVT_LOCAL_KEY))
            : DateTimeFormatter.ofPattern(s));
    resolveSysProHints(map, CVT_TEMPORAL_EPOCH_KEY, ChronoUnit::valueOf);
    resolveSysProHints(map, CVT_TEMPORAL_STRICTLY_KEY, Boolean::valueOf);
    resolveSysProHints(map, CVT_BYTES_PRIMITIVE_STRICTLY_KEY, Boolean::valueOf);
    return map;
  }

  static void resolveSysProHints(Map<String, Object> map, String key,
      Function<String, Object> func) {
    Properties p = System.getProperties();
    String useKey = Names.CORANT_PREFIX.concat(key);
    if (p.get(useKey) != null) {
      map.put(key, func.apply(p.get(useKey).toString()));
    }
  }
}

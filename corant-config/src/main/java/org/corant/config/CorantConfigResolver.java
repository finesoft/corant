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
package org.corant.config;

import static org.corant.shared.normal.Names.NAME_SPACE_SEPARATOR;
import static org.corant.shared.normal.Names.NAME_SPACE_SEPARATORS;
import static org.corant.shared.normal.Names.ConfigNames.CFG_ADJUST_PREFIX;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Strings.aggregate;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.defaultStrip;
import static org.corant.shared.util.Strings.escapedPattern;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.replace;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.corant.config.CorantConfigExpander.CorantConfigRawValueProvider;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;
import org.eclipse.microprofile.config.Config;

/**
 * corant-config
 *
 * @author bingo 下午3:23:28
 *
 */
public class CorantConfigResolver {

  public static final String ESCAPE = "\\";
  public static final char ESCAPE_CHAR = '\\';
  public static final char KEY_DELIMITER_CHAR = NAME_SPACE_SEPARATOR;
  public static final String KEY_DELIMITER = NAME_SPACE_SEPARATORS;
  public static final int KEY_DELIMITER_LEN = KEY_DELIMITER.length();
  public static final String VAL_DELIMITER = ",";
  public static final String KEY_DELIMITER_ESCAPES = ESCAPE + KEY_DELIMITER;
  public static final String VAL_DELIMITER_ESCAPES = ESCAPE + VAL_DELIMITER;
  public static final Pattern VAL_SPLITTER = escapedPattern(ESCAPE, VAL_DELIMITER);
  public static final Pattern KEY_SPLITTER = escapedPattern(ESCAPE, KEY_DELIMITER);

  public static void adjust(Object... props) {
    Map<String, String> map = mapOf(props);
    map.forEach((k, v) -> Systems.setProperty(CFG_ADJUST_PREFIX + defaultString(k), v));
  }

  public static String concatKey(String... keys) {
    StringBuilder concats = new StringBuilder();
    for (String key : keys) {
      String useKey = defaultString(key);
      if (!useKey.isBlank()) {
        concats.append(removeSplitor(key)).append(KEY_DELIMITER);
      }
    }
    return removeSplitor(concats.toString());
  }

  public static String dashify(String substring) {
    StringBuilder sb = new StringBuilder();
    boolean puc = false;
    boolean cuc = false;
    boolean suc = false;
    for (char i : defaultStrip(substring).toCharArray()) {
      cuc = i >= 'A' && i <= 'Z';
      if (cuc && !puc || puc && suc && !cuc) {
        sb.append('-');
      }
      sb.append(Character.toLowerCase(i));
      suc = puc == cuc;
      puc = cuc;
    }
    return sb.toString();
  }

  public static Map<String, List<String>> getGroupConfigKeys(Config config,
      Predicate<String> filter, int keyIndex) {
    return getGroupConfigKeys(config.getPropertyNames(), filter, keyIndex);
  }

  public static Map<String, List<String>> getGroupConfigKeys(Config config, String prefix,
      int keyIndex) {
    return getGroupConfigKeys(config.getPropertyNames(), prefix, keyIndex);
  }

  public static Map<String, List<String>> getGroupConfigKeys(Iterable<String> configs,
      Predicate<String> filter, int keyIndex) {
    shouldBeTrue(keyIndex >= 0);
    return aggregate(configs, filter, s -> {
      String[] arr = splitKey(s);
      if (arr.length > keyIndex) {
        return new String[] {arr[keyIndex], s};
      }
      return Strings.EMPTY_ARRAY;
    });
  }

  public static Map<String, List<String>> getGroupConfigKeys(Iterable<String> configs,
      String prefix, int keyIndex) {
    return getGroupConfigKeys(configs, s -> defaultString(s).startsWith(prefix), keyIndex);
  }

  public static String regulateKeyPrefix(String prefix) {
    String rs = defaultStrip(prefix);
    if (rs.length() == 0) {
      return rs;
    }
    return removeSplitor(prefix).concat(KEY_DELIMITER);
  }

  public static String removeSplitor(final String str) {
    String rs = defaultStrip(str);
    int length;
    if ((length = rs.length()) == 0 || rs.indexOf(KEY_DELIMITER_CHAR) == -1) {
      return rs;
    }
    int s = 0;
    while (s < length) {
      if (rs.charAt(s) != KEY_DELIMITER_CHAR) {
        break;
      }
      s++;
    }
    int e = length;
    while (e > s) {
      if (rs.charAt(e - 1) != KEY_DELIMITER_CHAR || e - 2 >= 0 && rs.charAt(e - 2) == ESCAPE_CHAR) {
        break;
      }
      e--;
    }
    // while (rs.endsWith(KEY_DELIMITER) && !rs.endsWith(KEY_DELIMITER_ESCAPES)) {
    // rs = defaultStrip(rs.substring(0, rs.length() - KEY_DELIMITER_LEN));
    // }
    // while (rs.startsWith(KEY_DELIMITER)) {
    // rs = defaultStrip(rs.substring(KEY_DELIMITER_LEN));
    // }
    return defaultStrip(rs.substring(s, e));
  }

  /**
   * Returns resolved value, if the given value contains an expression, it will be expanded and the
   * result after the expansion will be returned, otherwise the given value will be returned
   * directly.
   *
   * @param configValue the value to resolve
   * @param provider the raw configuration properties provider
   * @return the expanded value or the original given value if it can't expand
   */
  public static String resolveValue(String configValue, CorantConfigRawValueProvider provider) {
    return CorantConfigExpander.expand(configValue, provider);
  }

  public static String[] splitKey(String text) {
    return split(text, KEY_SPLITTER, true);
  }

  public static String[] splitValue(String text) {
    return split(text, VAL_SPLITTER, false);
  }

  static String[] split(String text, Pattern pattern, boolean key) {
    String[] array;
    if (text == null || (array = pattern.split(text)).length == 0) {
      return Strings.EMPTY_ARRAY;
    }
    String target = key ? KEY_DELIMITER_ESCAPES : VAL_DELIMITER_ESCAPES;
    String replace = key ? KEY_DELIMITER : VAL_DELIMITER;
    int length = array.length;
    String[] result = new String[length];
    int resultLength = 0;
    for (int i = 0; i < length; i++) {
      if (key && isNotBlank(array[i])) {
        result[resultLength] = replace(array[i], target, replace).trim();
        resultLength++;
      } else if (!array[i].isEmpty()) {
        result[resultLength] = replace(array[i], target, replace);
        resultLength++;
      }
    }
    return Arrays.copyOf(result, resultLength);
  }

}

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

import static org.corant.shared.normal.Names.NAME_SPACE_SEPARATORS;
import static org.corant.shared.normal.Names.ConfigNames.CFG_ADJUST_PREFIX;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Strings.aggregate;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.defaultTrim;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.corant.shared.util.Strings;
import org.eclipse.microprofile.config.Config;

/**
 * corant-config
 *
 * @author bingo 下午3:23:28
 *
 */
public class ConfigUtils {

  public static final int SEPARATOR_LEN = NAME_SPACE_SEPARATORS.length();
  public static final String VALUE_DELIMITER = "(?<!\\\\),";
  public static final String KEY_DELIMITER = "(?<!\\\\)\\.";
  private static final Pattern ENV_KEY_PATTERN = Pattern.compile("[^a-zA-Z0-9_]");

  public static void adjust(Object... props) {
    Map<String, String> map = mapOf(props);
    map.forEach((k, v) -> System.setProperty(CFG_ADJUST_PREFIX + defaultString(k), v));
  }

  public static String concatKey(String... keys) {
    StringBuilder concats = new StringBuilder();
    for (String key : keys) {
      String useKey = defaultString(key);
      if (isNotBlank(useKey)) {
        concats.append(removeSplitor(key)).append(NAME_SPACE_SEPARATORS);
      }
    }
    return removeSplitor(concats.toString());
  }

  public static String dashify(String substring) {
    StringBuilder sb = new StringBuilder();
    boolean puc = false;
    boolean cuc = false;
    boolean suc = false;
    for (char i : defaultTrim(substring).toCharArray()) {
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

  public static String extractSysEnv(Map<String, String> sysEnv, String propertyName) {
    if (propertyName == null) {
      return null;
    }
    String value = sysEnv.get(propertyName);
    if (value != null) {
      return value;
    }
    String sanitizedName = ENV_KEY_PATTERN.matcher(propertyName).replaceAll("_");
    value = sysEnv.get(sanitizedName);
    if (value != null) {
      return value;
    }
    return sysEnv.get(sanitizedName.toUpperCase(Locale.ROOT));
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

  public static String regulerKeyPrefix(String prefix) {
    String rs = defaultTrim(prefix);
    if (rs.length() == 0) {
      return rs;
    }
    return removeSplitor(prefix).concat(NAME_SPACE_SEPARATORS);
  }

  public static String removeSplitor(final String str) {
    String rs = defaultTrim(str);
    if (rs.length() == 0) {
      return rs;
    }
    while (rs.endsWith(NAME_SPACE_SEPARATORS) && !rs.endsWith("\\\\.")) {
      rs = defaultTrim(rs.substring(0, rs.length() - SEPARATOR_LEN));
    }
    while (rs.startsWith(NAME_SPACE_SEPARATORS)) {
      rs = defaultTrim(rs.substring(SEPARATOR_LEN));
    }
    return rs;
  }

  public static String[] splitKey(String text) {
    return splitProperties(text, KEY_DELIMITER, true);
  }

  public static String[] splitValue(String text) {
    return splitProperties(text, VALUE_DELIMITER, false);
  }

  static String[] splitProperties(String text, String regex, boolean trim) {
    if (text == null) {
      return Strings.EMPTY_ARRAY;
    }
    String splitor = regex.substring(regex.length() - 1);
    String[] split = text.split(regex);
    int spLen = split.length;
    String[] result = new String[spLen];
    int reLen = 0;
    for (int i = 0; i < spLen; i++) {
      split[i] = split[i].replace("\\\\" + splitor, splitor);
      if (isNotBlank(split[i])) {
        result[reLen] = trim ? split[i].trim() : split[i];
        reLen++;
      }
    }
    return Arrays.copyOf(result, reLen);
  }
}

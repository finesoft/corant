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

import static org.corant.kernel.normal.Names.NAME_SPACE_SEPARATORS;
import static org.corant.kernel.normal.Names.ConfigNames.CFG_ADJUST_PREFIX;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.group;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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

  public static void adjust(Object... props) {
    Map<String, String> map = mapOf(props);
    map.forEach((k, v) -> System.setProperty(CFG_ADJUST_PREFIX + defaultString(k), v));
  }

  public static String concatKey(String... keys) {
    String concats = "";
    for (String key : keys) {
      concats = removeSplitor(concats).concat(NAME_SPACE_SEPARATORS)
          .concat(removeSplitor(defaultTrim(key)));
    }
    return removeSplitor(concats);
  }

  public static String dashify(String substring) {
    /*
     * StringBuilder ret = new StringBuilder(); for (char i : substring.toCharArray()) { if (i >=
     * 'A' && i <= 'Z') { ret.append('-'); } ret.append(Character.toLowerCase(i)); }
     */
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
    return sb.toString();// FIXME
  }

  public static Class<?> getFieldActualTypeArguments(Field field, int index) {
    return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[index];
  }

  public static Map<String, List<String>> getGroupConfigNames(Config config,
      Predicate<String> filter, int keyIndex) {
    return getGroupConfigNames(config.getPropertyNames(), filter, keyIndex);
  }

  public static Map<String, List<String>> getGroupConfigNames(Config config, String prefix,
      int keyIndex) {
    return getGroupConfigNames(config.getPropertyNames(), prefix, keyIndex);
  }

  public static Map<String, List<String>> getGroupConfigNames(Iterable<String> configs,
      Predicate<String> filter, int keyIndex) {
    shouldBeTrue(keyIndex >= 0);
    return group(configs, s -> filter.test(s), s -> {
      String[] arr = splitKey(s);
      if (arr.length > keyIndex) {
        return new String[] {arr[keyIndex], s};
      }
      return new String[0];
    });
  }

  public static Map<String, List<String>> getGroupConfigNames(Iterable<String> configs,
      String prefix, int keyIndex) {
    return getGroupConfigNames(configs, s -> defaultString(s).startsWith(prefix), keyIndex);
  }

  public static String hanleInfixKey(String key) {
    return key.contains(NAME_SPACE_SEPARATORS) ? key.replaceAll("\\.", "\\\\.") : key;
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
    while (rs.endsWith(NAME_SPACE_SEPARATORS) && !rs.endsWith("\\.")) {
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
      return new String[0];
    }
    String splitor = regex.substring(regex.length() - 1);
    String[] split = text.split(regex);
    int spLen = split.length;
    String[] result = new String[spLen];
    int reLen = 0;
    for (int i = 0; i < spLen; i++) {
      split[i] = split[i].replace("\\" + splitor, splitor);
      if (isNotBlank(split[i])) {
        result[reLen] = trim ? split[i].trim() : split[i];
        reLen++;
      }
    }
    return Arrays.copyOf(result, reLen);
  }
}

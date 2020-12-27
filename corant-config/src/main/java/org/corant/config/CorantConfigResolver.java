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
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Strings.aggregate;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.defaultTrim;
import static org.corant.shared.util.Strings.escapedPattern;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.left;
import static org.corant.shared.util.Strings.replace;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.corant.shared.ubiquity.Mutable.MutableInteger;
import org.corant.shared.util.Strings;
import org.eclipse.microprofile.config.Config;

/**
 * corant-config
 *
 * @author bingo 下午3:23:28
 *
 */
public class CorantConfigResolver {

  public static final String ESCAPE = "\\";

  public static final String KEY_DELIMITER = NAME_SPACE_SEPARATORS;
  public static final int KEY_DELIMITER_LEN = KEY_DELIMITER.length();
  public static final String VAL_DELIMITER = ",";
  public static final String KEY_DELIMITER_ESCAPES = ESCAPE + KEY_DELIMITER;
  public static final String VAL_DELIMITER_ESCAPES = ESCAPE + VAL_DELIMITER;
  public static final Pattern VAL_SPLITTER = escapedPattern(ESCAPE, VAL_DELIMITER);
  public static final Pattern KEY_SPLITTER = escapedPattern(ESCAPE, KEY_DELIMITER);
  public static final Pattern ENV_KEY_PATTERN = Pattern.compile("[^a-zA-Z0-9_]");

  public static final String EXPAND_ENABLED_KEY = "mp.config.property.expressions.enabled";
  public static final String VAR_PREFIX = "${";
  public static final String EXP_PREFIX = "#{";
  public static final String VNE_SUFFIX = "}";
  public static final String VAR_DEFAULT = ":";
  public static final int VE_SUFFIX_LEN = 1;
  public static final int VE_PREFIX_LEN = 2;
  public static final int EXPANDED_LIMITED = 16;

  public static final String VAR_REP = ESCAPE + VAR_PREFIX;
  public static final String EXP_REP = ESCAPE + EXP_PREFIX;

  public static final Pattern VAR_SPTN = escapedPattern(ESCAPE, VAR_PREFIX);
  public static final Pattern EXP_SPTN = escapedPattern(ESCAPE, EXP_PREFIX);
  public static final Pattern VNE_EPTN = escapedPattern(ESCAPE, VNE_SUFFIX);
  public static final Pattern VAR_DPTN = escapedPattern(ESCAPE, VAR_DEFAULT);

  public static void adjust(Object... props) {
    Map<String, String> map = mapOf(props);
    map.forEach((k, v) -> System.setProperty(CFG_ADJUST_PREFIX + defaultString(k), v));
  }

  public static String concatKey(String... keys) {
    StringBuilder concats = new StringBuilder();
    for (String key : keys) {
      String useKey = defaultString(key);
      if (isNotBlank(useKey)) {
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
    String rs = defaultTrim(prefix);
    if (rs.length() == 0) {
      return rs;
    }
    return removeSplitor(prefix).concat(KEY_DELIMITER);
  }

  public static String removeSplitor(final String str) {
    String rs = defaultTrim(str);
    if (rs.length() == 0) {
      return rs;
    }
    while (rs.endsWith(KEY_DELIMITER) && !rs.endsWith(KEY_DELIMITER_ESCAPES)) {
      rs = defaultTrim(rs.substring(0, rs.length() - KEY_DELIMITER_LEN));
    }
    while (rs.startsWith(KEY_DELIMITER)) {
      rs = defaultTrim(rs.substring(KEY_DELIMITER_LEN));
    }
    return rs;
  }

  public static String resolveSysEnvValue(Map<String, String> sysEnv, String propertyName) {
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

  public static String resolveValue(String key, CorantConfigRawValueProvider provider) {
    if (key == null || isBlank(key)) {
      return key;
    }
    String value = provider.get(false, key);
    if (toBoolean(provider.get(false, EXPAND_ENABLED_KEY))) {
      List<String> stacks = new ArrayList<>(EXPANDED_LIMITED);
      if (value.contains(EXP_PREFIX)) {
        value = expandValue(true, value, provider, stacks);
        value = replace(value, EXP_REP, EXP_PREFIX);
      }
      if (value.contains(VAR_PREFIX)) {
        value = expandValue(false, value, provider, stacks);
        value = replace(value, VAR_REP, VAR_PREFIX);
      }
      stacks.clear();
    }
    return value;
  }

  public static String[] splitKey(String text) {
    return split(text, KEY_SPLITTER, true);
  }

  public static String[] splitValue(String text) {
    return split(text, VAL_SPLITTER, false);
  }

  static String expandValue(boolean eval, String value, CorantConfigRawValueProvider provider,
      Collection<String> stacks) {
    Pattern pattern = eval ? EXP_SPTN : VAR_SPTN;
    Matcher matcher = pattern.matcher(value);
    if (matcher.find()) {
      MutableInteger start = new MutableInteger(matcher.start());
      matcher.results().map(MatchResult::start).forEach(start::set);
      String content = value.substring(start.intValue() + VE_PREFIX_LEN);
      Optional<MatchResult> contents = VNE_EPTN.matcher(content).results().findFirst();
      if (contents.isPresent()) {
        // TODO replace \\} to }
        int end = contents.get().start();
        String extracted = content.substring(0, end);
        System.out.printf("stack%d -> %s \n", stacks.size(), extracted);
        if (isNotBlank(extracted)) {
          if (!eval && extracted.contains(VAR_DEFAULT)) {
            Optional<MatchResult> defaults = VAR_DPTN.matcher(extracted).results().findFirst();
            if (defaults.isPresent()) {
              // TODO replace \\: to :
              String defaultValue = extracted.substring(defaults.get().start() + VE_SUFFIX_LEN);
              extracted = extracted.substring(0, defaults.get().start());
              extracted =
                  defaultString(resolveValue(eval, extracted, provider, stacks), defaultValue);
            }
          } else {
            extracted = resolveValue(eval, extracted, provider, stacks);
          }
        }
        if (isNotBlank(extracted)) {
          return expandValue(eval, left(value, start.intValue()).concat(extracted)
              .concat(content.substring(end + VE_SUFFIX_LEN)), provider, stacks);
        } else {
          throw new NoSuchElementException(
              String.format("Can not expanded value, the extracted not found, the expanded path %s",
                  String.join(", ", stacks)));
        }
      }
    }
    return value;
  }

  static String resolveValue(boolean eval, String key, CorantConfigRawValueProvider provider,
      Collection<String> stacks) {
    if (stacks.size() > EXPANDED_LIMITED) {
      throw new IllegalArgumentException(String.format(
          "Can not expanded value, lookups exceeds limit(max: %d), the expanded path %s",
          EXPANDED_LIMITED, String.join(", ", stacks)));
    }
    stacks.add(key);
    String result = provider.get(eval, key);
    if (result != null) {
      if (result.contains(EXP_PREFIX)) {
        return expandValue(false, result, provider, stacks);
      }
      if (result.contains(VAR_PREFIX)) {
        return expandValue(true, result, provider, stacks);
      }
    }
    return result;
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

  @FunctionalInterface
  public interface CorantConfigRawValueProvider {

    String get(boolean eval, String key);
  }
}

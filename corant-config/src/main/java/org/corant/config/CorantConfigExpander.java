/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.escapedPattern;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.left;
import static org.corant.shared.util.Strings.replace;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.corant.shared.util.Strings;

/**
 * corant-config
 *
 * @author bingo 下午12:57:49
 *
 */
public class CorantConfigExpander {
  public static final String ESCAPE = "\\";
  public static final String MACRO_EXP_PREFIX = "#{";
  public static final String MACRO_VAR_PREFIX = "${";
  public static final String MACRO_SUFFIX = "}";
  public static final String MACRO_DEFAULT = ":";
  public static final String ESCAPED_MACRO_EXP_PREFIX = ESCAPE + MACRO_EXP_PREFIX;
  public static final String ESCAPED_MACRO_VAR_PREFIX = ESCAPE + MACRO_VAR_PREFIX;
  public static final String ESCAPED_MACRO_SUFFIX = ESCAPE + MACRO_SUFFIX;
  public static final String ESCAPED_MACRO_DEFAULT = ESCAPE + MACRO_DEFAULT;

  public static final int MACRO_PREFIX_LENGTH = 2;
  public static final int MACRO_SUFFIX_LENGTH = 1;
  // "(?<!" + Pattern.quote(ESCAPE) + ")" + "[\\$,\\#]" + Pattern.quote("{");
  public static final String MACRO_PREFIX_REGEX = "(?<!\\Q\\\\E)[$,#]\\Q{\\E";
  public static final Pattern MACRO_PREFIX_PATTERN = Pattern.compile(MACRO_PREFIX_REGEX);
  public static final Pattern MACRO_SUFFIX_PATTERN = escapedPattern(ESCAPE, MACRO_SUFFIX);
  public static final Pattern MACRO_DEFAULT_PATTERN = escapedPattern(ESCAPE, MACRO_DEFAULT);

  public static final int EXPANDED_LIMITED = 16;

  private static final Logger logger = Logger.getLogger(CorantConfigResolver.class.getName());

  public static String expand(String configValue, CorantConfigRawValueProvider provider) {
    String value = configValue;
    if (containsMacro(value) && provider != null) {
      List<String> stacks = new LinkedList<>();
      value = resolveEscape(resolve(value, provider, stacks));
      stacks.clear();
    }
    return value;
  }

  static boolean containsMacro(String value) {
    return isNotBlank(value)
        && (value.contains(MACRO_EXP_PREFIX) || value.contains(MACRO_VAR_PREFIX));
  }

  // TODO Use AST?
  static String resolve(String template, CorantConfigRawValueProvider provider,
      Collection<String> stacks) {
    int[] position = resolvePosition(template);
    if (position[1] >= 0) {
      boolean eval = position[0] == 1;
      String extracted = template.substring(position[1] + MACRO_PREFIX_LENGTH, position[2]);
      final String logExtracted = extracted;
      logger.finer(() -> String.format("%s stack%d -> %s %n", "-".repeat(stacks.size()),
          stacks.size(), logExtracted));
      if (isNotBlank(extracted)) {
        Optional<MatchResult> defaults;
        if (!eval && (defaults = MACRO_DEFAULT_PATTERN.matcher(extracted).results().findFirst())
            .isPresent()) {
          String defaultValue = extracted.substring(defaults.get().start() + MACRO_SUFFIX_LENGTH);
          extracted = extracted.substring(0, defaults.get().start());
          extracted = defaultString(resolveValue(eval, extracted, provider, stacks), defaultValue);
        } else if (extracted.endsWith(MACRO_DEFAULT) && extracted.length() > 1) {
          extracted = defaultString(resolveValue(eval, extracted, provider, stacks), Strings.EMPTY);
        } else {
          extracted = resolveValue(eval, extracted, provider, stacks);
        }
      }
      if (extracted != null) {
        return resolve(left(template, position[1]).concat(extracted)
            .concat(template.substring(position[2] + 1)), provider, stacks);
      } else {
        throw new NoSuchElementException(String.format(
            "Can not expanded the variable value, the extracted not found, the expanded path [%s].",
            String.join(" -> ", stacks)));
      }
    }
    return template;
  }

  static String resolveEscape(String value) {
    String resolved = replace(value, ESCAPED_MACRO_EXP_PREFIX, MACRO_EXP_PREFIX);
    resolved = replace(resolved, ESCAPED_MACRO_VAR_PREFIX, MACRO_VAR_PREFIX);
    resolved = replace(resolved, ESCAPED_MACRO_SUFFIX, MACRO_SUFFIX);
    return replace(resolved, ESCAPED_MACRO_DEFAULT, MACRO_DEFAULT);
  }

  static int[] resolvePosition(String value) {
    Matcher matcher = MACRO_PREFIX_PATTERN.matcher(value);
    if (matcher.find()) {
      int start = matcher.start();
      start = matcher.results().map(MatchResult::start).max(Integer::compareTo).orElse(start);
      String content = value.substring(start + MACRO_PREFIX_LENGTH);
      Optional<MatchResult> result = MACRO_SUFFIX_PATTERN.matcher(content).results().findFirst();
      if (result.isPresent()) {
        return new int[] {value.charAt(start) == '$' ? 0 : 1, start,
            start + MACRO_PREFIX_LENGTH + result.get().start()};
      } else {
        return resolvePosition(value.substring(0, start));
      }
    }
    return new int[] {-1, -1, -1};
  }

  static String resolveValue(boolean eval, String key, CorantConfigRawValueProvider provider,
      Collection<String> stacks) {
    if (stacks.size() > EXPANDED_LIMITED) {
      throw new IllegalArgumentException(String.format(
          "Can not expanded the variable value, lookups exceeds limit(max: %d), the expanded path [%s].",
          EXPANDED_LIMITED, String.join(" -> ", stacks)));
    }
    String actualKey = eval ? key : replace(key, ESCAPED_MACRO_DEFAULT, MACRO_DEFAULT);
    stacks.add(actualKey);
    String result = provider.get(eval, actualKey);
    if (containsMacro(result)) {
      return resolve(result, provider, stacks);
    }
    return result;
  }

  @FunctionalInterface
  public interface CorantConfigRawValueProvider {

    String get(boolean eval, String key);
  }
}

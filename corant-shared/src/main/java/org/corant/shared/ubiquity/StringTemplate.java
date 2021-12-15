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
package org.corant.shared.ubiquity;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Strings.defaultBlank;
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
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.corant.shared.util.Strings;

/**
 * corant-shared
 *
 * @author bingo 下午7:37:15
 *
 */
public class StringTemplate {

  public static final String DEFAULT_MACRO_PREFIX = "${";
  public static final String DEFAULT_MACRO_SUFFIX = "}";
  public static final String DEFAULT_MACRO_DEFAULT = ":";
  public static final String DEFAULT_ESCAPE = "\\";

  public static final StringTemplate DEFAULT = new StringTemplate(DEFAULT_ESCAPE,
      DEFAULT_MACRO_PREFIX, DEFAULT_MACRO_SUFFIX, DEFAULT_MACRO_DEFAULT, 16);

  final String escape;
  final String macroPrefix;
  final String macroSuffix;
  final String macroDefault;
  final String escapedMacroPrefix;
  final String escapedMacroSuffix;
  final String escapedMacroDefault;
  final int macroPrefixLength;
  final int macroSuffixLength;
  final Pattern macroPrefixPattern;
  final Pattern macroSuffixPattern;
  final Pattern macroDefaultPattern;
  final int expandedLimit;

  public StringTemplate(String escape, String macroPrefix, String macroSuffix, String macroDefault,
      int expandedLimit) {
    this.escape = defaultBlank(escape, Strings.BACK_SLASH);
    this.macroPrefix = shouldNotBlank(macroPrefix);
    this.macroSuffix = shouldNotBlank(macroSuffix);
    this.macroDefault = macroDefault;
    escapedMacroPrefix = escape + macroPrefix;
    escapedMacroSuffix = escape + macroSuffix;
    if (macroDefault != null) {
      escapedMacroDefault = escape + macroDefault;
      macroDefaultPattern = escapedPattern(escape, macroDefault);
    } else {
      escapedMacroDefault = null;
      macroDefaultPattern = null;
    }
    macroPrefixLength = macroPrefix.length();
    macroSuffixLength = macroSuffix.length();
    macroPrefixPattern = escapedPattern(escape, macroPrefix);
    macroSuffixPattern = escapedPattern(escape, macroSuffix);

    this.expandedLimit = expandedLimit;
  }

  public static String parseSimpleDollar(String propertyName, Function<String, String> provider) {
    int startVar = 0;
    String resolvedValue = propertyName;
    while ((startVar = resolvedValue.indexOf(DEFAULT_MACRO_PREFIX, startVar)) >= 0) {
      int endVar = resolvedValue.indexOf(DEFAULT_MACRO_SUFFIX, startVar);
      if (endVar <= 0) {
        break;
      }
      String varName = resolvedValue.substring(startVar + 2, endVar);
      if (varName.isEmpty()) {
        break;
      }
      String varVal = provider.apply(varName);
      if (varVal != null) {
        resolvedValue = parseSimpleDollar(
            resolvedValue.replace(DEFAULT_MACRO_PREFIX + varName + DEFAULT_MACRO_SUFFIX, varVal),
            provider);
      }
      startVar++;
    }
    return resolvedValue;
  }

  public String getEscape() {
    return escape;
  }

  public int getExpandedLimit() {
    return expandedLimit;
  }

  public String getMacroDefault() {
    return macroDefault;
  }

  public String getMacroPrefix() {
    return macroPrefix;
  }

  public String getMacroSuffix() {
    return macroSuffix;
  }

  public String parse(String template, Function<String, Object> provider) {
    String value = template;
    if (isNotBlank(value) && provider != null && value.contains(macroPrefix)) {
      List<String> stacks = new LinkedList<>();
      value = resolveEscape(resolve(value, provider, stacks));
      stacks.clear();
    }
    return value;
  }

  // TODO Use AST?
  protected String resolve(String template, Function<String, Object> provider,
      Collection<String> stacks) {
    int[] position = resolvePosition(template);
    if (position[0] >= 0) {
      String extracted = template.substring(position[0] + macroPrefixLength, position[1]);
      if (isNotBlank(extracted)) {
        Optional<MatchResult> defaults;
        if (macroDefaultPattern != null
            && (defaults = macroDefaultPattern.matcher(extracted).results().findFirst())
                .isPresent()) {
          String defaultValue = extracted.substring(defaults.get().start() + macroSuffixLength);
          extracted = extracted.substring(0, defaults.get().start());
          extracted = defaultString(resolveValue(extracted, provider, stacks), defaultValue);
        } else if (macroDefault != null && extracted.endsWith(macroDefault)
            && extracted.length() > 1) {
          extracted = defaultString(resolveValue(extracted, provider, stacks), Strings.EMPTY);
        } else {
          extracted = resolveValue(extracted, provider, stacks);
        }
      }
      if (extracted != null) {
        return resolve(left(template, position[0]).concat(extracted)
            .concat(template.substring(position[1] + 1)), provider, stacks);
      } else {
        throw new NoSuchElementException(String.format(
            "Can not expanded the variable value, the extracted not found, the expanded path [%s].",
            String.join(" -> ", stacks)));
      }
    }
    return template;
  }

  protected String resolveEscape(String value) {
    String resolved = replace(value, escapedMacroPrefix, macroPrefix);
    resolved = replace(resolved, escapedMacroSuffix, macroSuffix);
    return replace(resolved, escapedMacroDefault, macroDefault);
  }

  protected int[] resolvePosition(String value) {
    Matcher matcher = macroPrefixPattern.matcher(value);
    if (matcher.find()) {
      int start = matcher.start();
      start = matcher.results().map(MatchResult::start).max(Integer::compareTo).orElse(start);
      String content = value.substring(start + macroPrefixLength);
      Optional<MatchResult> result = macroSuffixPattern.matcher(content).results().findFirst();
      if (result.isPresent()) {
        return new int[] {start, start + macroPrefixLength + result.get().start()};
      } else {
        return resolvePosition(value.substring(0, start));
      }
    }
    return new int[] {-1, -1};
  }

  protected String resolveValue(String key, Function<String, Object> provider,
      Collection<String> stacks) {
    if (stacks.size() > expandedLimit) {
      throw new IllegalArgumentException(String.format(
          "Can not expanded the variable value, lookups exceeds limit(max: %d), the expanded path [%s].",
          expandedLimit, String.join(" -> ", stacks)));
    }
    String actualKey = macroDefault == null ? key : replace(key, escapedMacroDefault, macroDefault);
    stacks.add(actualKey);
    Object value = provider.apply(actualKey);
    String result = value == null ? null : value.toString();
    if (result != null && result.contains(macroPrefix)) {
      return resolve(result, provider, stacks);
    }
    return result;
  }
}

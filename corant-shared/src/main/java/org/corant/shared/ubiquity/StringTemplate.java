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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.corant.shared.ubiquity.Mutable.MutableInteger;
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
    this.macroDefault = defaultBlank(macroDefault, Strings.COLON);
    escapedMacroPrefix = escape + macroPrefix;
    macroPrefixLength = macroPrefix.length();
    macroSuffixLength = macroSuffix.length();
    macroPrefixPattern = escapedPattern(escape, macroPrefix);
    macroSuffixPattern = escapedPattern(escape, macroSuffix);
    macroDefaultPattern = escapedPattern(escape, macroDefault);
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
    if (isNotBlank(value) && provider != null) {
      boolean vars = value.contains(macroPrefix);
      if (vars) {
        List<String> stacks = new ArrayList<>(expandedLimit);
        value = expandValue(value, provider, stacks);
        value = replace(value, escapedMacroPrefix, macroPrefix);
        stacks.clear();
      }
    }
    return value;
  }

  // TODO Use AST?
  String expandValue(String value, Function<String, Object> provider, Collection<String> stacks) {
    Matcher matcher = macroPrefixPattern.matcher(value);
    if (matcher.find()) {
      MutableInteger start = new MutableInteger(matcher.start());
      matcher.results().map(MatchResult::start).forEach(start::set);
      String content = value.substring(start.intValue() + macroPrefixLength);
      Optional<MatchResult> contents = macroSuffixPattern.matcher(content).results().findFirst();
      if (contents.isPresent()) {
        int end = contents.get().start();
        String extracted = content.substring(0, end);
        if (isNotBlank(extracted)) {
          if (extracted.contains(macroDefault)) {
            Optional<MatchResult> defaults =
                macroDefaultPattern.matcher(extracted).results().findFirst();
            if (defaults.isPresent()) {
              String defaultValue = extracted.substring(defaults.get().start() + macroSuffixLength);
              extracted = extracted.substring(0, defaults.get().start());
              extracted = defaultString(resolveValue(extracted, provider, stacks), defaultValue);
            } else if (extracted.endsWith(macroDefault) && extracted.length() > 1) {
              // default value not exist return Strings.EMPTY
              extracted = defaultString(resolveValue(extracted, provider, stacks), Strings.EMPTY);
            }
          } else {
            extracted = resolveValue(extracted, provider, stacks);
          }
        }
        if (extracted != null) {
          return expandValue(left(value, start.intValue()).concat(extracted)
              .concat(content.substring(end + macroSuffixLength)), provider, stacks);
        } else {
          throw new NoSuchElementException(String.format(
              "Can not expanded the variable value, the extracted not found, the expanded path [%s].",
              String.join(" -> ", stacks)));
        }
      }
    }
    return value;
  }

  String resolveValue(String key, Function<String, Object> provider, Collection<String> stacks) {
    if (stacks.size() > expandedLimit) {
      throw new IllegalArgumentException(String.format(
          "Can not expanded the variable value, lookups exceeds limit(max: %d), the expanded path [%s].",
          expandedLimit, String.join(" -> ", stacks)));
    }
    stacks.add(key);
    Object value = provider.apply(key);
    String result = value == null ? null : value.toString();
    if (result != null && result.contains(macroPrefix)) {
      return expandValue(result, provider, stacks);
    }
    return result;
  }
}

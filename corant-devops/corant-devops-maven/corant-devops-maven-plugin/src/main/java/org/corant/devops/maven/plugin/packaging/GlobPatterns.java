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
package org.corant.devops.maven.plugin.packaging;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * corant-devops-maven-plugin
 *
 * @author bingo 下午2:12:35
 *
 */
public class GlobPatterns {
  public static final String REG_CHARS = ".^$+{[]|()";
  public static final String GLO_CHARS = "\\*?[{";
  private static final char EOL = 0;

  public static Pattern build(String globExpress, boolean isDos, boolean ignoreCase) {
    if (globExpress == null || globExpress.isBlank()) {
      return null;
    }
    if (isDos) {
      if (ignoreCase) {
        return Pattern.compile(toWindowsRegexPattern(globExpress), Pattern.UNICODE_CASE);
      } else {
        return Pattern.compile(toWindowsRegexPattern(globExpress),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
      }
    } else if (ignoreCase) {
      return Pattern.compile(toUnixRegexPattern(globExpress), Pattern.UNICODE_CASE);
    } else {
      return Pattern.compile(toUnixRegexPattern(globExpress),
          Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
  }

  public static List<Pattern> buildAll(String globExpresses, boolean isDos, boolean ignoreCase) {
    if (globExpresses == null || globExpresses.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(globExpresses.split(",")).map(p -> GlobPatterns.build(p, false, true))
        .filter(Objects::nonNull).collect(Collectors.toList());
  }

  public static boolean isGlobChar(char c) {
    return GLO_CHARS.indexOf(c) != -1;
  }

  public static boolean isGlobChar(int c) {
    return GLO_CHARS.indexOf(c) != -1;
  }

  public static boolean isRegexChar(char c) {
    return REG_CHARS.indexOf(c) != -1;
  }

  public static boolean isRegexChar(int c) {
    return REG_CHARS.indexOf(c) != -1;
  }

  public static boolean matchPath(String path, List<Pattern> patterns) {
    if (patterns == null || patterns.isEmpty()) {
      return false;
    }
    String usePath = path.replace('\\', '/');
    return patterns.stream().anyMatch(p -> p.matcher(usePath).matches());
  }

  public static String toUnixRegexPattern(String globExpress) {
    return toRegexPattern(globExpress, false);
  }

  public static String toWindowsRegexPattern(String globExpress) {
    return toRegexPattern(globExpress, true);
  }

  private static char next(String glob, int i) {
    if (i < glob.length()) {
      return glob.charAt(i);
    }
    return EOL;
  }

  /**
   * Creates a regex pattern from the given glob expression.
   *
   * @throws PatternSyntaxException
   */
  private static String toRegexPattern(String globPattern, boolean isDos) {
    boolean inGroup = false;
    StringBuilder regex = new StringBuilder("^");

    int i = 0;
    int length = globPattern.length();
    while (i < length) {
      char c = globPattern.charAt(i++);
      switch (c) {
        case '\\':
          // escape special characters
          if (i == length) {
            throw new PatternSyntaxException("No character to escape", globPattern, i - 1);
          }
          char next = globPattern.charAt(i++);
          if (isGlobChar(next) || isRegexChar(next)) {
            regex.append('\\');
          }
          regex.append(next);
          break;
        case '/':
          if (isDos) {
            regex.append("\\\\");
          } else {
            regex.append(c);
          }
          break;
        case '[':
          // don't match name separator in class
          if (isDos) {
            regex.append("[[^\\\\]&&[");
          } else {
            regex.append("[[^/]&&[");
          }
          if (next(globPattern, i) == '^') {
            // escape the regex negation char if it appears
            regex.append("\\^");
            i++;
          } else {
            // negation
            if (next(globPattern, i) == '!') {
              regex.append('^');
              i++;
            }
            // hyphen allowed at start
            if (next(globPattern, i) == '-') {
              regex.append('-');
              i++;
            }
          }
          boolean hasRangeStart = false;
          char last = 0;
          while (i < length) {
            c = globPattern.charAt(i++);
            if (c == ']') {
              break;
            }
            if (c == '/' || isDos && c == '\\') {
              throw new PatternSyntaxException("Explicit 'name separator' in class", globPattern,
                  i - 1);
            }
            // TBD: how to specify ']' in a class?
            if (c == '\\' || c == '[' || c == '&' && next(globPattern, i) == '&') {
              // escape '\', '[' or "&&" for regex class
              regex.append('\\');
            }
            regex.append(c);

            if (c == '-') {
              if (!hasRangeStart) {
                throw new PatternSyntaxException("Invalid range", globPattern, i - 1);
              }
              if ((c = next(globPattern, i++)) == EOL || c == ']') {
                break;
              }
              if (c < last) {
                throw new PatternSyntaxException("Invalid range", globPattern, i - 3);
              }
              regex.append(c);
              hasRangeStart = false;
            } else {
              hasRangeStart = true;
              last = c;
            }
          }
          if (c != ']') {
            throw new PatternSyntaxException("Missing ']", globPattern, i - 1);
          }
          regex.append("]]");
          break;
        case '{':
          if (inGroup) {
            throw new PatternSyntaxException("Cannot nest groups", globPattern, i - 1);
          }
          regex.append("(?:(?:");
          inGroup = true;
          break;
        case '}':
          if (inGroup) {
            regex.append("))");
            inGroup = false;
          } else {
            regex.append('}');
          }
          break;
        case ',':
          if (inGroup) {
            regex.append(")|(?:");
          } else {
            regex.append(',');
          }
          break;
        case '*':
          if (next(globPattern, i) == '*') {
            // crosses directory boundaries
            regex.append(".*");
            i++;
          } else // within directory boundary
          if (isDos) {
            regex.append("[^\\\\]*");
          } else {
            regex.append("[^/]*");
          }
          break;
        case '?':
          if (isDos) {
            regex.append("[^\\\\]");
          } else {
            regex.append("[^/]");
          }
          break;

        default:
          if (isRegexChar(c)) {
            regex.append('\\');
          }
          regex.append(c);
      }
    }

    if (inGroup) {
      throw new PatternSyntaxException("Missing '}", globPattern, i - 1);
    }

    return regex.append('$').toString();
  }
}

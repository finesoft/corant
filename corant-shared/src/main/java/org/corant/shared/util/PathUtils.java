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
package org.corant.shared.util;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * corant-shared
 *
 * @author bingo 下午8:25:03
 *
 */
public class PathUtils {

  public static boolean matchClassPath(String path, String globExpress) {
    return matchUnixPath(path, globExpress);
  }

  public static boolean matchUnixPath(String path, String globExpress) {
    return GlobPatterns.build(GlobPatterns.toUnixRegexPattern(globExpress), false).matcher(path)
        .matches();
  }

  public static boolean matchWinPath(String path, String globExpress) {
    return GlobPatterns.build(GlobPatterns.toWindowsRegexPattern(globExpress), false).matcher(path)
        .matches();
  }

  /**
   * corant-shared
   *
   * Use Glob wildcards for filtering.
   *
   * @author bingo 下午8:32:50
   *
   */
  public static class GlobMatcher implements Predicate<String> {

    private final boolean ignoreCase;
    private final String globExpress;
    private final Pattern pattern;

    /**
     * @param ignoreCase
     * @param globExpress
     */
    protected GlobMatcher(boolean ignoreCase, String globExpress) {
      super();
      this.ignoreCase = ignoreCase;
      this.globExpress = globExpress;
      pattern = GlobPatterns.build(globExpress, ignoreCase);
    }

    public static boolean hasGlobChar(String str) {
      return str != null && str.chars().anyMatch(GlobPatterns::isGlobChar);
    }

    public static GlobMatcher of(boolean ignoreCase, String globExpress) {
      return new GlobMatcher(ignoreCase, globExpress);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      GlobMatcher other = (GlobMatcher) obj;
      if (globExpress == null) {
        if (other.globExpress != null) {
          return false;
        }
      } else if (!globExpress.equals(other.globExpress)) {
        return false;
      }
      if (ignoreCase != other.ignoreCase) {
        return false;
      }
      return true;
    }

    public String getGlobExpress() {
      return globExpress;
    }

    public Pattern getPattern() {
      return pattern;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (globExpress == null ? 0 : globExpress.hashCode());
      result = prime * result + (ignoreCase ? 1231 : 1237);
      return result;
    }

    public boolean isIgnoreCase() {
      return ignoreCase;
    }

    @Override
    public boolean test(String t) {
      return pattern.matcher(t).matches();
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午4:30:24
   *
   */
  public static class GlobPatterns {

    private static final String REG_CHARS = ".^$+{[]|()";
    private static final String GLO_CHARS = "\\*?[{";
    private static final char EOL = 0;

    public static Pattern build(String globExpress, boolean ignoreCase) {
      if (ignoreCase) {
        return Pattern.compile(toUnixRegexPattern(globExpress), Pattern.UNICODE_CASE);
      } else {
        return Pattern.compile(toUnixRegexPattern(globExpress),
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
      }
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
      while (i < globPattern.length()) {
        char c = globPattern.charAt(i++);
        switch (c) {
          case '\\':
            // escape special characters
            if (i == globPattern.length()) {
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
            while (i < globPattern.length()) {
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
            } else {
              // within directory boundary
              if (isDos) {
                regex.append("[^\\\\]*");
              } else {
                regex.append("[^/]*");
              }
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
}

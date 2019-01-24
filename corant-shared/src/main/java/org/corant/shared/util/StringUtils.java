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

import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.StreamUtils.asStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author bingo 上午12:31:35
 *
 */
public class StringUtils {

  public static final String EMPTY = "";

  private StringUtils() {
    super();
  }

  /**
   *
   * @param seq
   * @param flags
   * @param patterns
   * @return allMatch
   */
  public static boolean allMatch(final CharSequence seq, final int flags,
      final String... patterns) {
    if (seq == null || patterns.length == 0) {
      return false;
    } else {
      return asStream(patterns).map(ps -> Pattern.compile(ps, 0))
          .allMatch(p -> p.matcher(seq).find());
    }
  }

  /**
   *
   * @param seq
   * @param flags
   * @param patterns
   * @return anyMatch
   */
  public static boolean anyMatch(final CharSequence seq, final int flags,
      final String... patterns) {
    if (seq == null || patterns.length == 0) {
      return false;
    } else {
      return asStream(patterns).map(ps -> Pattern.compile(ps, flags))
          .anyMatch(p -> p.matcher(seq).find());
    }
  }

  /**
   * <pre>
   * StringUtils.defaultString(null)  = ""
   * StringUtils.defaultString("")    = ""
   * StringUtils.defaultString("abc") = "abc"
   * StringUtils.defaultString(NonNull) = NonNull.toString()
   * </pre>
   */
  public static String asDefaultString(final Object obj) {
    return asString(obj, EMPTY);
  }

  /**
   * <pre>
   * StringUtils.contains(null, *)    = false
   * StringUtils.contains("", *)      = false
   * StringUtils.contains("abc", 'a') = true
   * StringUtils.contains("abc", 'z') = false
   * </pre>
   *
   * @param str
   * @param searchStr
   * @return contains
   */
  public static boolean contains(String str, String searchStr) {
    return str == null || searchStr == null ? false : str.contains(searchStr);
  }

  /**
   * <pre>
   * StringUtils.defaultString(null)  = ""
   * StringUtils.defaultString("")    = ""
   * StringUtils.defaultString("abc") = "abc"
   * </pre>
   */
  public static String defaultString(final String str) {
    return defaultString(str, EMPTY);
  }

  /**
   * <pre>
   * StringUtils.defaultString(null, "DFLT")  = "DFLT"
   * StringUtils.defaultString("", "DFLT")    = ""
   * StringUtils.defaultString("abc", "DFLT") = "abc"
   * </pre>
   *
   * @param str
   * @param dfltStr
   * @return defaultString
   */
  public static String defaultString(final String str, final String dfltStr) {
    return str == null ? dfltStr : str;
  }

  /**
   * <pre>
   * StringUtils.trimToEmpty(null)          = ""
   * StringUtils.trimToEmpty("")            = ""
   * StringUtils.trimToEmpty("     ")       = ""
   * StringUtils.trimToEmpty("abc")         = "abc"
   * StringUtils.trimToEmpty("    abc    ") = "abc"
   * </pre>
   *
   * @param str
   * @return defaultTrim
   */
  public static String defaultTrim(String str) {
    return str == null ? EMPTY : str.trim();
  }

  /**
   * @param str
   * @param ignoreCase
   * @param globExpress
   * @return globMatch
   */
  public static boolean globMatch(final CharSequence str, final boolean ignoreCase,
      final String globExpress) {
    if (str == null || isEmpty(globExpress)) {
      return false;
    } else {
      return GlobPatterns.build(globExpress, ignoreCase).matcher(str).matches();
    }
  }

  /**
   * ["prefix.1","prefix.2","prefix.3","unmatch.4"] = {key="prefix",value=["1","2","3"]}
   *
   * @param iterable
   * @param prefix
   * @return group
   */
  public static Map<String, List<String>> group(Iterable<String> iterable, Predicate<String> filter,
      Function<String, String[]> func) {
    Map<String, List<String>> map = new LinkedHashMap<>();
    if (iterable != null && filter != null) {
      asStream(iterable).filter(filter).sorted().map(func).forEach(s -> {
        if (s.length > 1) {
          map.computeIfAbsent(s[0], (k) -> new ArrayList<>()).add(s[1]);
        }
      });
    }
    return map;
  }

  /**
   * <pre>
   * StringUtils.ifBlank(null, "DFLT")  = "DFLT"
   * StringUtils.ifBlank("", "DFLT")    = "DFLT"
   * StringUtils.ifBlank(" ", "DFLT")   = "DFLT"
   * StringUtils.ifBlank("abc", "DFLT") = "abc"
   * StringUtils.ifBlank("", null)      = null
   * </pre>
   *
   * @param str
   * @param dfltStr
   * @return ifBlank
   */
  public static <T extends CharSequence> T ifBlank(final T str, final T dfltStr) {
    return isBlank(str) ? dfltStr : str;
  }

  /**
   * <pre>
   * StringUtils.isBlank(null)      = true
   * StringUtils.isBlank("")        = true
   * StringUtils.isBlank(" ")       = true
   * StringUtils.isBlank("abc")     = false
   * StringUtils.isBlank("  abc  ") = false
   * </pre>
   *
   * @param cs
   * @return isBlank
   */
  public static boolean isBlank(final CharSequence cs) {
    int len;
    if (cs == null || (len = cs.length()) == 0) {
      return true;
    }
    for (int i = 0; i < len; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * <pre>
   * StringUtils.isEmpty(null)      = true
   * StringUtils.isEmpty("")        = true
   * StringUtils.isEmpty(" ")       = false
   * StringUtils.isEmpty("abc")     = false
   * StringUtils.isEmpty("  abc  ") = false
   * </pre>
   */
  public static boolean isEmpty(final CharSequence cs) {
    return cs == null || cs.length() == 0;
  }

  /**
   * <pre>
   * StringUtils.isNoneBlank((String) null)    = false
   * StringUtils.isNoneBlank((String[]) null)  = true
   * StringUtils.isNoneBlank(null, "abc")      = false
   * StringUtils.isNoneBlank(null, null)       = false
   * StringUtils.isNoneBlank("", "123")        = false
   * StringUtils.isNoneBlank("xyz", "")        = false
   * StringUtils.isNoneBlank("  xyz  ", null)  = false
   * StringUtils.isNoneBlank(" ", "123")       = false
   * StringUtils.isNoneBlank(new String[] {})  = true
   * StringUtils.isNoneBlank(new String[]{""}) = false
   * StringUtils.isNoneBlank("abc", "123")     = true
   * </pre>
   *
   * @param css
   * @return
   */
  public static boolean isNoneBlank(final CharSequence... css) {
    if (css == null || css.length == 0) {
      return true;
    }
    for (final CharSequence cs : css) {
      if (isBlank(cs)) {
        return false;
      }
    }
    return true;
  }

  /**
   * <pre>
   * StringUtils.isNotBlank(null)      = false
   * StringUtils.isNotBlank("")        = false
   * StringUtils.isNotBlank(" ")       = false
   * StringUtils.isNotBlank("abc")     = true
   * StringUtils.isNotBlank("  abc  ") = true
   * </pre>
   */
  public static boolean isNotBlank(final CharSequence cs) {
    return !isBlank(cs);
  }

  /**
   * <pre>
   * StringUtils.isNotEmpty(null)      = false
   * StringUtils.isNotEmpty("")        = false
   * StringUtils.isNotEmpty(" ")       = true
   * StringUtils.isNotEmpty("abc")     = true
   * StringUtils.isNotEmpty("  abc  ") = true
   * </pre>
   *
   * @param cs
   * @return isNotEmpty
   */
  public static boolean isNotEmpty(final CharSequence cs) {
    return !isEmpty(cs);
  }

  /**
   * <pre>
   * StringUtils.left(null, *)    = null
   * StringUtils.left(*, -ve)     = ""
   * StringUtils.left("", *)      = ""
   * StringUtils.left("abc", 0)   = ""
   * StringUtils.left("abc", 2)   = "ab"
   * StringUtils.left("abc", 4)   = "abc"
   * </pre>
   *
   * @param str
   * @param len
   * @return left
   */
  public static String left(final String str, final int len) {
    if (str == null) {
      return null;
    }
    if (len < 0) {
      return EMPTY;
    }
    if (str.length() <= len) {
      return str;
    }
    return str.substring(0, len);
  }

  /**
   * <pre>
   * StringUtils.mid(null, *, *)    = null
   * StringUtils.mid(*, *, -ve)     = ""
   * StringUtils.mid("", 0, *)      = ""
   * StringUtils.mid("abc", 0, 2)   = "ab"
   * StringUtils.mid("abc", 0, 4)   = "abc"
   * StringUtils.mid("abc", 2, 4)   = "c"
   * StringUtils.mid("abc", 4, 2)   = ""
   * StringUtils.mid("abc", -2, 2)  = "ab"
   * </pre>
   *
   * @param str
   * @param pos
   * @param len
   * @return mid
   */
  public static String mid(final String str, int start, final int len) {
    if (str == null) {
      return null;
    }
    int strLen, pos = start;
    if (len < 0 || pos > (strLen = str.length())) {
      return EMPTY;
    }
    if (pos < 0) {
      pos = 0;
    }
    if (strLen <= pos + len) {
      return str.substring(pos);
    }
    return str.substring(pos, pos + len);
  }

  /**
   * Replace string use for short string not regex.
   *
   * @param source
   * @param orginal
   * @param replace
   * @return replaced
   */
  public static String replace(String source, String orginal, String replace) {
    if (source == null || isEmpty(orginal)) {
      return source;
    }
    String replaced = source;
    int i = 0;
    if ((i = replaced.indexOf(orginal, i)) >= 0) {
      char[] srcArray = replaced.toCharArray(), nsArray = replace.toCharArray();
      int olen = orginal.length(), srclen = srcArray.length;
      StringBuilder buf = new StringBuilder(srclen);
      buf.append(srcArray, 0, i).append(nsArray);
      i += olen;
      int j = i;
      while ((i = replaced.indexOf(orginal, i)) > 0) {
        buf.append(srcArray, j, i - j).append(nsArray);
        i += olen;
        j = i;
      }
      buf.append(srcArray, j, srclen - j);
      replaced = buf.toString();
      buf.setLength(0);
    }
    return replaced;
  }

  /**
   * <pre>
   * StringUtils.right(null, *)    = null
   * StringUtils.right(*, -ve)     = ""
   * StringUtils.right("", *)      = ""
   * StringUtils.right("abc", 0)   = ""
   * StringUtils.right("abc", 2)   = "bc"
   * StringUtils.right("abc", 4)   = "abc"
   * </pre>
   *
   * @param str
   * @param len
   * @return right
   */
  public static String right(final String str, final int len) {
    if (str == null) {
      return null;
    }
    if (len < 0) {
      return EMPTY;
    }
    int strLen;
    if ((strLen = str.length()) <= len) {
      return str;
    }
    return str.substring(strLen - len);
  }

  /**
   * Split string with Predicate<Character>
   *
   * @param str
   * @param splitor
   * @return split
   */
  public static String[] split(final String str, final Predicate<Character> splitor) {
    int len;
    if (str == null || (len = str.length()) == 0) {
      return new String[0];
    }
    if (splitor == null) {
      return new String[] {str};
    }
    int i = 0, s = 0, g = len > 16 ? 16 : (len >> 1) + 1, ai = 0;
    String[] array = new String[g];
    boolean match = false;
    while (i < len) {
      if (splitor.test(str.charAt(i))) {
        if (match) {
          if (ai == g) {
            array = Arrays.copyOf(array, g += g);
          }
          array[ai++] = str.substring(s, i);
          match = false;
        }
        s = ++i;
        continue;
      }
      match = true;
      i++;
    }
    if (match) {
      array = Arrays.copyOf(array, ai + 1);
      array[ai] = str.substring(s, i);
      return array;
    } else {
      return Arrays.copyOf(array, ai);
    }
  }

  /**
   * Split string with whole spreator string not regex.
   *
   * @param str
   * @param wholeSpreator
   * @return
   */
  public static String[] split(final String str, final String wholeSpreator) {
    int len, slen;
    if (str == null || (len = str.length()) == 0) {
      return new String[0];
    }
    if (wholeSpreator == null || (slen = wholeSpreator.length()) == 0) {
      return new String[] {str};
    }
    if (slen == 1) {
      char wholeChar = wholeSpreator.charAt(0);
      return split(str, c -> c.charValue() == wholeChar);
    }
    int s = 0, e = 0, i = 0, g = len > 16 ? 16 : (len >> 1) + 1;
    String[] array = new String[g];
    while (e < len) {
      e = str.indexOf(wholeSpreator, s);
      if (e > -1) {
        if (e > s) {
          if (i == g) {
            array = Arrays.copyOf(array, g += g);
          }
          array[i++] = str.substring(s, e);
        }
        s = e + slen;
      } else {
        if (s < len) {
          array = Arrays.copyOf(array, i + 1);
          array[i++] = str.substring(s);
        }
        e = len;
      }
    }
    return Arrays.copyOf(array, i);
  }

  /**
   * Return not blank elements
   *
   * @param str
   * @param wholeSpreator
   * @param trim
   * @return split
   */
  public static String[] split(final String str, final String wholeSpreator,
      final boolean removeBlank, final boolean trim) {
    String[] splits = split(str, wholeSpreator);
    String[] result = new String[splits.length];
    if (splits.length > 0) {
      int i = 0;
      for (String e : splits) {
        if (isNotBlank(e) || isBlank(e) && !removeBlank) {
          result[i++] = trim ? trim(e) : e;
        }
      }
      return Arrays.copyOf(result, i);
    } else {
      return result;
    }
  }

  /**
   * <pre>
   * StringUtils.trim(null)          = null
   * StringUtils.trim("")            = ""
   * StringUtils.trim("     ")       = ""
   * StringUtils.trim("abc")         = "abc"
   * StringUtils.trim("    abc    ") = "abc"
   * </pre>
   *
   * @param str
   * @return trim
   */
  public static String trim(String str) {
    return str == null ? null : str.trim();
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

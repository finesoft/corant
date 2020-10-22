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

import static org.corant.shared.util.Streams.streamOf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.corant.shared.util.PathMatcher.GlobPatterns;

/**
 *
 * @author bingo 上午12:31:35
 *
 */
public class Strings {

  public static final String EMPTY = "";

  private Strings() {
    super();
  }

  /**
   * ["prefix.1","prefix.2","prefix.3","unmatch.4"] = {key="prefix",value=["1","2","3"]}
   *
   * @param iterable
   * @param filter
   * @param func
   * @return group
   */
  public static Map<String, List<String>> aggregate(Iterable<String> iterable,
      Predicate<String> filter, Function<String, String[]> func) {
    Map<String, List<String>> map = new LinkedHashMap<>();
    if (iterable != null && filter != null) {
      streamOf(iterable).filter(filter).sorted().map(func).forEach(s -> {
        if (s.length > 1) {
          map.computeIfAbsent(s[0], k -> new ArrayList<>()).add(s[1]);
        }
      });
    }
    return map;
  }

  /**
   * <pre>
   * Strings.defaultString(null)  = ""
   * Strings.defaultString("")    = ""
   * Strings.defaultString("abc") = "abc"
   * Strings.defaultString(NonNull) = NonNull.toString()
   * </pre>
   */
  public static String asDefaultString(final Object obj) {
    return obj != null ? obj.toString() : EMPTY;
  }

  /**
   * <pre>
   * Strings.contains(null, *)    = false
   * Strings.contains("", *)      = false
   * Strings.contains("abc", 'a') = true
   * Strings.contains("abc", 'z') = false
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
   * Strings.ifBlank(null, ()->"DFLT")  = "DFLT"
   * Strings.ifBlank("", ()->"DFLT")    = "DFLT"
   * Strings.ifBlank(" ", ()->"DFLT")   = "DFLT"
   * Strings.ifBlank("abc", ()->"DFLT") = "abc"
   * Strings.ifBlank("", null)      = null
   * </pre>
   *
   * @param str
   * @param supplier
   * @return ifBlank
   */
  public static <T extends CharSequence> T defaultBlank(final T str, final Supplier<T> supplier) {
    return isBlank(str) ? supplier == null ? null : supplier.get() : str;
  }

  /**
   * <pre>
   * Strings.ifBlank(null, "DFLT")  = "DFLT"
   * Strings.ifBlank("", "DFLT")    = "DFLT"
   * Strings.ifBlank(" ", "DFLT")   = "DFLT"
   * Strings.ifBlank("abc", "DFLT") = "abc"
   * Strings.ifBlank("", null)      = null
   * </pre>
   *
   * @param str
   * @param dfltStr
   * @return ifBlank
   */
  public static <T extends CharSequence> T defaultBlank(final T str, final T dfltStr) {
    return isBlank(str) ? dfltStr : str;
  }

  /**
   * <pre>
   * Strings.defaultString(null)  = ""
   * Strings.defaultString("")    = ""
   * Strings.defaultString("abc") = "abc"
   * </pre>
   */
  public static String defaultString(final String str) {
    return defaultString(str, EMPTY);
  }

  /**
   * <pre>
   * Strings.defaultString(null, "DFLT")  = "DFLT"
   * Strings.defaultString("", "DFLT")    = ""
   * Strings.defaultString("abc", "DFLT") = "abc"
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
   * Strings.trimToEmpty(null)          = ""
   * Strings.trimToEmpty("")            = ""
   * Strings.trimToEmpty("     ")       = ""
   * Strings.trimToEmpty("abc")         = "abc"
   * Strings.trimToEmpty("    abc    ") = "abc"
   * </pre>
   *
   * @param str
   * @return defaultTrim
   */
  public static String defaultTrim(String str) {
    return str == null ? EMPTY : str.trim();
  }

  /**
   * <pre>
   * Strings.isBlank(null)      = true
   * Strings.isBlank("\t")      = true
   * Strings.isBlank("")        = true
   * Strings.isBlank(" ")       = true
   * Strings.isBlank("abc")     = false
   * Strings.isBlank("  abc  ") = false
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
   * Strings.isNoneBlank((String) null)    = false
   * Strings.isNoneBlank((String[]) null)  = true
   * Strings.isNoneBlank(null, "abc")      = false
   * Strings.isNoneBlank(null, null)       = false
   * Strings.isNoneBlank("", "123")        = false
   * Strings.isNoneBlank("xyz", "")        = false
   * Strings.isNoneBlank("  xyz  ", null)  = false
   * Strings.isNoneBlank(" ", "123")       = false
   * Strings.isNoneBlank(new String[] {})  = true
   * Strings.isNoneBlank(new String[]{""}) = false
   * Strings.isNoneBlank("abc", "123")     = true
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
   * Strings.isNotBlank(null)      = false
   * Strings.isNotBlank("")        = false
   * Strings.isNotBlank(" ")       = false
   * Strings.isNotBlank("abc")     = true
   * Strings.isNotBlank("  abc  ") = true
   * </pre>
   */
  public static boolean isNotBlank(final CharSequence cs) {
    return !isBlank(cs);
  }

  /**
   * <pre>
   * Strings.left(null, *)    = null
   * Strings.left(*, -ve)     = ""
   * Strings.left("", *)      = ""
   * Strings.left("abc", 0)   = ""
   * Strings.left("abc", 2)   = "ab"
   * Strings.left("abc", 4)   = "abc"
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
   * Checks if the passed string matches all passed regular expressions.
   *
   * @param seq
   * @param flags
   * @param regex
   * @return matchAllRegex
   */
  public static boolean matchAllRegex(final CharSequence seq, final int flags,
      final String... regex) {
    if (seq == null || regex.length == 0) {
      return false;
    } else {
      return streamOf(regex).map(ps -> Pattern.compile(ps, flags))
          .allMatch(p -> p.matcher(seq).find());
    }
  }

  /**
   * Checks if the passed string matches any passed regular expressions.
   *
   * @param seq
   * @param flags
   * @param regex
   * @return matchAnyRegex
   */
  public static boolean matchAnyRegex(final CharSequence seq, final int flags,
      final String... regex) {
    if (seq == null || regex.length == 0) {
      return false;
    } else {
      return streamOf(regex).map(ps -> Pattern.compile(ps, flags))
          .anyMatch(p -> p.matcher(seq).find());
    }
  }

  /**
   * Checks if the passed string matches Glob expressions.
   *
   * @param str
   * @param ignoreCase
   * @param isDos
   * @param globExpress
   * @return matchGlob
   */
  public static boolean matchGlob(final CharSequence str, final boolean ignoreCase,
      final boolean isDos, final String globExpress) {
    if (str == null || globExpress == null || globExpress.length() == 0) {
      return false;
    } else {
      return GlobPatterns.build(globExpress, isDos, ignoreCase).matcher(str).matches();
    }
  }

  /**
   * Checks if the passed string matches Wildcard expressions.
   *
   * @param str
   * @param ignoreCase
   * @param wildcardExpress
   * @return
   * @see WildcardMatcher
   */
  public static boolean matchWildcard(final String str, final boolean ignoreCase,
      final String wildcardExpress) {
    if (str == null || wildcardExpress == null || wildcardExpress.length() == 0) {
      return false;
    } else {
      return WildcardMatcher.of(ignoreCase, wildcardExpress).test(str);
    }
  }

  /**
   * <pre>
   * Strings.mid(null, *, *)    = null
   * Strings.mid(*, *, -ve)     = ""
   * Strings.mid("", 0, *)      = ""
   * Strings.mid("abc", 0, 2)   = "ab"
   * Strings.mid("abc", 0, 4)   = "abc"
   * Strings.mid("abc", 2, 4)   = "c"
   * Strings.mid("abc", 4, 2)   = ""
   * Strings.mid("abc", -2, 2)  = "ab"
   * </pre>
   *
   * @param str
   * @param start
   * @param len
   * @return mid
   */
  public static String mid(final String str, int start, final int len) {
    if (str == null) {
      return null;
    }
    int strLen;
    int pos = start;
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
   * Remove all the given substring in the given string, any one of the parameters is null or the
   * length is 0 then return the given string.
   *
   * <pre>
   * Strings.remove("123", null)                         = 123
   * Strings.remove("123abc123abc", "abc")               = "123123"
   * Strings.remove(null, null)                          = null
   * Strings.remove("", "123")                           = ""
   * Strings.remove("123", "")                           = "123"
   * Strings.remove("123", null)                         = "123"
   * </pre>
   *
   * @param str
   * @param remove
   * @return remove
   */
  public static String remove(final String str, final String remove) {
    int rlen;
    if (str == null || remove == null || (rlen = remove.length()) == 0) {
      return str;
    }
    int s = 0;
    int i = str.indexOf(remove, s);
    if (i == -1) {
      return str;
    }
    StringBuilder buf = new StringBuilder(str.length());
    do {
      buf.append(str, s, i);
      s = i + rlen;
    } while ((i = str.indexOf(remove, s)) != -1);
    if (s < str.length()) {
      buf.append(str, s, str.length());
    }
    return buf.toString();
  }

  /**
   * Use the given character filter to remove characters that meet the filter criteria.
   *
   * @param str
   * @param filter
   * @return removeCharIf
   */
  public static String removeCharIf(final String str, final Predicate<Character> filter) {
    int len;
    if (str == null || (len = str.length()) == 0 || filter == null) {
      return str;
    }
    StringBuilder buf = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      if (!filter.test(str.charAt(i))) {
        buf.append(str.charAt(i));
      }
    }
    return buf.toString();
  }

  /**
   * Replace string use for short string, not regex.
   *
   * @param source
   * @param orginal
   * @param replace
   * @return replaced
   */
  public static String replace(String source, String orginal, String replace) {
    if (source == null || orginal == null || orginal.length() == 0) {
      return source;
    }
    String replaced = source;
    int i = 0;
    if ((i = replaced.indexOf(orginal, i)) >= 0) {
      char[] srcArray = replaced.toCharArray();
      char[] nsArray = replace.toCharArray();
      int olen = orginal.length();
      int srclen = srcArray.length;
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
   * Strings.right(null, *)    = null
   * Strings.right(*, -ve)     = ""
   * Strings.right("", *)      = ""
   * Strings.right("abc", 0)   = ""
   * Strings.right("abc", 2)   = "bc"
   * Strings.right("abc", 4)   = "abc"
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
   * Segment string into a string array with Predicate, delete blank elements or trim elements as
   * needed. The difference with {@link #split(String, Predicate)} is that this method does not
   * delete any characters.
   *
   * @see Strings#segment(String, Predicate)
   *
   * @param str
   * @param removeBlank
   * @param trim
   * @param predicate
   * @return segments
   */
  public static String[] segment(final String str, final boolean removeBlank, final boolean trim,
      final Predicate<Character> predicate) {
    return regulateSplits(segment(str, predicate), removeBlank, trim);
  }

  /**
   * Segment string into a string array with Predicate, the difference with
   * {@link #split(String, Predicate)} is that this method does not delete any characters.
   *
   * <pre>
   * Strings.group("abc123efg456", Character::isDigit)        =       ["abc","123","efg","456"]
   * Strings.group("abc,efg", c -> c==',')                    =       ["abc",",","efg"]
   * Strings.group("123", Character::isDigit)                 =       ["123"]
   * Strings.group(null, Character::isDigit)                  =       []
   * Strings.group("abc", Character::isDigit)                 =       ["abc"]
   * Strings.group("abc", null)                               =       ["abc"]
   * Strings.group(null, null)                                =       []
   * </pre>
   *
   * @param str
   * @param predicate
   * @return segments
   */
  public static String[] segment(final String str, final Predicate<Character> predicate) {
    int len;
    if (str == null || (len = str.length()) == 0) {
      return new String[0];
    }
    if (predicate == null) {
      return new String[] {str};
    }
    int i = 0;
    int s = 0;
    int g = len > 16 ? 16 : (len >> 1) + 1;
    int ai = 0;
    String[] array = new String[g];
    boolean match = false;
    for (; i < len; i++) {
      if (predicate.test(str.charAt(i))) {
        if (!match && i > 0) {
          if (ai == g) {
            array = Arrays.copyOf(array, g += g);
          }
          array[ai++] = str.substring(s, i);
          s = i;
        }
        match = true;
      } else {
        if (match) {
          if (ai == g) {
            array = Arrays.copyOf(array, g += g);
          }
          array[ai++] = str.substring(s, i);
          s = i;
        }
        match = false;
      }
    }
    if (s < len) {
      array = Arrays.copyOf(array, ai + 1);
      array[ai] = str.substring(s);
      return array;
    } else {
      return Arrays.copyOf(array, ai);
    }
  }

  /**
   * Split the string into a string array, delete blank elements or trim elements as needed.
   *
   * @param str
   * @param removeBlank
   * @param trim
   * @param p
   * @return split
   */
  public static String[] split(final String str, final boolean removeBlank, final boolean trim,
      Predicate<Character> p) {
    return regulateSplits(split(str, p), removeBlank, trim);
  }

  /**
   * Split the string into a string array with Predicate.
   *
   * <pre>
   * Strings.split("abc123efg456", Character::isDigit)        =       ["abc","efg"]
   * Strings.split("abc,efg", c -> c==',')                    =       ["abc","efg"]
   * Strings.split("123", Character::isDigit)                 =       []
   * Strings.split(null, Character::isDigit)                  =       []
   * Strings.split("abc", Character::isDigit)                 =       ["abc"]
   * Strings.split("abc", null)                               =       ["abc"]
   * Strings.split(null, null)                                =       []
   * </pre>
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
    int i = 0;
    int s = 0;
    int g = len > 16 ? 16 : (len >> 1) + 1;
    int ai = 0;
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
   * Split the string into a string array with whole spreator string, not regex.
   *
   * @param str
   * @param wholeSpreator
   * @return
   */
  public static String[] split(final String str, final String wholeSpreator) {
    int len;
    int slen;
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
    int s = 0;
    int e = 0;
    int i = 0;
    int g = len > 16 ? 16 : (len >> 1) + 1;
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
   * Split the string into a string array, delete blank elements or trim elements as needed.
   *
   * @param str
   * @param wholeSpreator
   * @param removeBlank
   * @param trim
   * @return split
   */
  public static String[] split(final String str, final String wholeSpreator,
      final boolean removeBlank, final boolean trim) {
    return regulateSplits(split(str, wholeSpreator), removeBlank, trim);
  }

  /**
   * <pre>
   * Strings.trim(null)          = null
   * Strings.trim("")            = ""
   * Strings.trim("     ")       = ""
   * Strings.trim("abc")         = "abc"
   * Strings.trim("    abc    ") = "abc"
   * </pre>
   *
   * @param str
   * @return trim
   */
  public static String trim(String str) {
    return str == null ? null : str.trim();
  }

  private static String[] regulateSplits(String[] splits, final boolean removeBlank,
      final boolean trim) {
    if (!removeBlank && !trim) {
      return splits;
    } else {
      String[] result = new String[splits.length];
      int i = 0;
      for (String e : splits) {
        if (isNotBlank(e) || isBlank(e) && !removeBlank) {
          result[i++] = trim ? trim(e) : e;
        }
      }
      return Arrays.copyOf(result, i);
    }
  }

  /**
   * corant-shared
   *
   * Use wildcards for filtering, algorithm from apache.org.
   *
   * @author bingo 下午8:32:50
   *
   */
  public static class WildcardMatcher implements Predicate<String> {

    private final boolean ignoreCase;
    private final String[] tokens;
    private final String wildcardExpress;

    /**
     * @param ignoreCase
     * @param wildcardExpress
     */
    protected WildcardMatcher(boolean ignoreCase, String wildcardExpress) {
      super();
      this.ignoreCase = ignoreCase;
      this.wildcardExpress = wildcardExpress;
      tokens = splitOnTokens(wildcardExpress);
    }

    public static boolean hasWildcard(String text) {
      return text.indexOf('?') != -1 || text.indexOf('*') != -1;
    }

    public static WildcardMatcher of(boolean ignoreCase, String wildcardExpress) {
      return new WildcardMatcher(ignoreCase, wildcardExpress);
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
      WildcardMatcher other = (WildcardMatcher) obj;
      if (ignoreCase != other.ignoreCase) {
        return false;
      }
      if (wildcardExpress == null) {
        if (other.wildcardExpress != null) {
          return false;
        }
      } else if (!wildcardExpress.equals(other.wildcardExpress)) {
        return false;
      }
      return true;
    }

    public String[] getTokens() {
      return Arrays.copyOf(tokens, tokens.length);
    }

    public String getWildcardExpress() {
      return wildcardExpress;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (ignoreCase ? 1231 : 1237);
      result = prime * result + (wildcardExpress == null ? 0 : wildcardExpress.hashCode());
      return result;
    }

    public boolean isIgnoreCase() {
      return ignoreCase;
    }

    @Override
    public boolean test(final String text) {
      boolean anyChars = false;
      int textIdx = 0;
      int tokenIdx = 0;
      final Stack<int[]> backtrack = new Stack<>();
      do {
        if (!backtrack.isEmpty()) {
          final int[] array = backtrack.pop();
          tokenIdx = array[0];
          textIdx = array[1];
          anyChars = true;
        }
        while (tokenIdx < tokens.length) {
          if (tokens[tokenIdx].equals("?")) {
            textIdx++;
            if (textIdx > text.length()) {
              break;
            }
            anyChars = false;
          } else if (tokens[tokenIdx].equals("*")) {
            anyChars = true;
            if (tokenIdx == tokens.length - 1) {
              textIdx = text.length();
            }
          } else {
            if (anyChars) {
              textIdx = checkIndexOf(text, textIdx, tokens[tokenIdx]);
              if (textIdx == -1) {
                break;
              }
              final int repeat = checkIndexOf(text, textIdx + 1, tokens[tokenIdx]);
              if (repeat >= 0) {
                backtrack.push(new int[] {tokenIdx, repeat});
              }
            } else {
              if (!checkRegionMatches(text, textIdx, tokens[tokenIdx])) {
                break;
              }
            }
            textIdx += tokens[tokenIdx].length();
            anyChars = false;
          }
          tokenIdx++;
        }
        if (tokenIdx == tokens.length && textIdx == text.length()) {
          return true;
        }
      } while (!backtrack.isEmpty());

      return false;
    }

    int checkIndexOf(final String str, final int strStartIndex, final String search) {
      final int endIndex = str.length() - search.length();
      if (endIndex >= strStartIndex) {
        for (int i = strStartIndex; i <= endIndex; i++) {
          if (checkRegionMatches(str, i, search)) {
            return i;
          }
        }
      }
      return -1;
    }

    boolean checkRegionMatches(final String str, final int strStartIndex, final String search) {
      return str.regionMatches(ignoreCase, strStartIndex, search, 0, search.length());
    }

    String[] splitOnTokens(final String wildcardExpress) {
      if (!hasWildcard(wildcardExpress)) {
        return new String[] {wildcardExpress};
      }
      final char[] array = wildcardExpress.toCharArray();
      final ArrayList<String> list = new ArrayList<>();
      final StringBuilder buffer = new StringBuilder();
      char prevChar = 0;
      for (final char ch : array) {
        if (ch == '?' || ch == '*') {
          if (buffer.length() != 0) {
            list.add(buffer.toString());
            buffer.setLength(0);
          }
          if (ch == '?') {
            list.add("?");
          } else if (prevChar != '*') {
            list.add("*");
          }
        } else {
          buffer.append(ch);
        }
        prevChar = ch;
      }
      if (buffer.length() != 0) {
        list.add(buffer.toString());
      }
      return list.toArray(new String[list.size()]);
    }

  }
}

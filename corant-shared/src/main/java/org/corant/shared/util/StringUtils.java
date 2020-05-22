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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.StreamUtils.streamOf;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.PathUtils.GlobPatterns;

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
  public static <T extends CharSequence> T defaultBlank(final T str, final T dfltStr) {
    return isBlank(str) ? dfltStr : str;
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
   * Convert input stream to string
   *
   * @param is
   * @return
   * @throws IOException fromInputStream
   */
  public static String fromInputStream(InputStream is) throws IOException {
    StringBuilder sb = new StringBuilder();
    try (Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      int c = 0;
      while ((c = reader.read()) != -1) {
        sb.append((char) c);
      }
    }
    return sb.toString();
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
   * Determine if a string is decimal number. Hexadecimal and scientific notations and octal number
   * are not considered
   *
   * <pre>
   * StringUtils.isDecimalNumber(null)      =false
   * StringUtils.isDecimalNumber("")        =false
   * StringUtils.isDecimalNumber(" ")       =false
   * StringUtils.isDecimalNumber("123")     =true
   * StringUtils.isDecimalNumber("0123")    =true
   * StringUtils.isDecimalNumber("1.23")    =true
   * StringUtils.isDecimalNumber(".123")    =true
   * StringUtils.isDecimalNumber("-123")    =true
   * StringUtils.isDecimalNumber("-.123")   =true
   * StringUtils.isDecimalNumber("+123")    =true
   * StringUtils.isDecimalNumber("+.123")   =true
   * StringUtils.isDecimalNumber("-.")      =false
   * StringUtils.isDecimalNumber(".0")      =true
   * StringUtils.isDecimalNumber("12 3")    =false
   * StringUtils.isDecimalNumber(" 123")    =false
   * StringUtils.isDecimalNumber("a123")    =false
   * </pre>
   *
   * @param obj
   * @return isDecimalNumber
   */
  public static boolean isDecimalNumber(final String obj) {
    int len;
    if ((len = sizeOf(obj)) == 0) {
      return false;
    }
    if (obj.charAt(len - 1) == '.') {
      return false;
    }
    int idx = 0;
    if (obj.charAt(0) == '-' || obj.charAt(0) == '+') {
      if (len == 1) {
        return false;
      }
      idx = 1;
    }
    int point = 0;
    for (int i = idx; i < len; i++) {
      if (obj.charAt(i) == '.') {
        if (++point > 1) {
          return false;
        }
        continue;
      }
      if (!Character.isDigit(obj.charAt(i))) {
        return false;
      }
    }
    return true;
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
   * String lines from file, use for read txt file line by line.
   *
   * @param file
   * @return lines
   */
  public static Stream<String> lines(final File file) {
    FileInputStream fis;
    try {
      fis = new FileInputStream(shouldNotNull(file));
    } catch (FileNotFoundException e1) {
      throw new CorantRuntimeException(e1);
    }
    return lines(fis, -1, -1).onClose(() -> {
      try {
        fis.close();
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
    });
  }

  /**
   * String lines from file input stream, use for read txt file line by line.
   *
   * @param fis
   * @param offset the offset start from 0
   * @param limit the number of lines returned
   */
  public static Stream<String> lines(final FileInputStream fis, int offset, int limit) {
    return lines(new InputStreamReader(fis, StandardCharsets.UTF_8), offset, limit);
  }

  /**
   * String lines from input stream reader, use for read txt file line by line.
   *
   * @param isr the input stream reader
   * @param offset the offset start from 0
   * @param limit the number of lines returned
   */
  public static Stream<String> lines(final InputStreamReader isr, int offset, int limit) {

    return streamOf(new Iterator<String>() {

      BufferedReader reader = new BufferedReader(isr);
      String nextLine = null;
      int readLines = 0;
      boolean valid = true;
      { // skip lines if necessary
        try {
          if (offset > 0) {
            for (int i = 0; i < offset; i++) {
              if (reader.readLine() == null) {
                valid = false;
                break;
              }
            }
          }
        } catch (Exception e) {
          throw new CorantRuntimeException(e);
        }
      }

      @Override
      public boolean hasNext() {
        if (!valid) {
          return false;
        }
        if (nextLine != null) {
          return true;
        } else {
          try {
            nextLine = reader.readLine();
            return nextLine != null && (limit <= 0 || readLines++ < limit);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }
      }

      @Override
      public String next() {
        if (nextLine != null || hasNext()) {
          String line = nextLine;
          nextLine = null;
          return line;
        } else {
          throw new NoSuchElementException();
        }
      }
    });
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
    if (str == null || isEmpty(globExpress)) {
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
   * @param globExpress
   * @return
   * @see WildcardMatcher
   */
  public static boolean matchWildcard(final String str, final boolean ignoreCase,
      final String globExpress) {
    if (str == null || isEmpty(globExpress)) {
      return false;
    } else {
      return WildcardMatcher.of(ignoreCase, globExpress).test(str);
    }
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
   * Split string with whole spreator string not regex.
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
   * Convert input stream to String
   *
   * @param is
   * @return tryFromInputStream
   */
  public static String tryFromInputStream(InputStream is) {
    try {
      return fromInputStream(is);
    } catch (IOException e) {
      return null;
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

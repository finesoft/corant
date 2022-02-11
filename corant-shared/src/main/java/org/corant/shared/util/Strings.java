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

import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.areEqual;
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
import org.corant.shared.ubiquity.StringTemplate;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.PathMatcher.GlobPatterns;

/**
 * corant-shared
 *
 * @author bingo 上午12:31:35
 *
 */
public class Strings {

  //@formatter:off
  public static final String[] EMPTY_ARRAY    = {};
  public static final String EMPTY            = "";
  public static final String AMPERSAND        = "&";
  public static final String AT               = "@";
  public static final String ASTERISK         = "*";
  public static final String BACK_SLASH       = "\\";
  public static final String COLON            = ":";
  public static final String COMMA            = ",";
  public static final String DASH             = "-";
  public static final String DOLLAR           = "$";
  public static final String DOT              = ".";
  public static final String EQUALS           = "=";
  public static final String FALSE            = "false";
  public static final String SLASH            = "/";
  public static final String HASH             = "#";
  public static final String HAT              = "^";
  public static final String LEFT_BRACE       = "{";
  public static final String LEFT_BRACKET     = "(";
  public static final String LEFT_CHEV        = "<";
  public static final String NEWLINE          = "\n";
  public static final String NULL             = "null";
  public static final String PERCENT          = "%";
  public static final String PIPE             = "|";
  public static final String PLUS             = "+";
  public static final String QUESTION_MARK    = "?";
  public static final String EXCLAMATION_MARK = "!";
  public static final String QUOTE            = "\"";
  public static final String RETURN           = "\r";
  public static final String TAB              = "\t";
  public static final String RIGHT_BRACE      = "}";
  public static final String RIGHT_BRACKET    = ")";
  public static final String RIGHT_CHEV       = ">";
  public static final String SEMICOLON        = ";";
  public static final String SINGLE_QUOTE     = "'";
  public static final String BACKTICK         = "`";
  public static final String SPACE            = " ";
  public static final String TILDA            = "~";
  public static final String LEFT_SQ_BRACKET  = "[";
  public static final String RIGHT_SQ_BRACKET = "]";
  public static final String TRUE             = "true";
  public static final String UNDERSCORE       = "_";
  public static final String ONE              = "1";
  public static final String ZERO             = "0";
  public static final String DOLLAR_LEFT_BRACE= "${";
  public static final String HASH_LEFT_BRACE  = "#{";
  public static final String CRLF             = "\r\n";

  public static final String NUMBERS = "0123456789";
  public static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String UPPER_CASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String LOWER_CASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
  public static final String NUMBERS_AND_LETTERS =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String NUMBERS_AND_UPPER_CASE_LETTERS =
      "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String NUMBERS_AND_LOWER_CASE_LETTERS =
      "0123456789abcdefghijklmnopqrstuvwxyz";
  public static final int SPLIT_ARRAY_LENGTH = 8;
  //@formatter:on

  private Strings() {}

  /**
   * Returns a grouping given elements of string, according to a classification function and filter
   * function, and returning the results in a {@code Map}.
   *
   * <pre>
   * ["prefix.1","prefix.2","prefix.3","unmatch.4"] = {key="prefix",value=["1","2","3"]}
   * </pre>
   *
   * @param iterable the string elements for grouping
   * @param filter filter function use for select the elements for grouping
   * @param func the classification function
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
   * Returns either a string representation of the given object, or if the given object is
   * {@code null}, the value of {@link #EMPTY}.
   *
   * <pre>
   * Strings.asDefaultString(null)  = ""
   * Strings.asDefaultString("")    = ""
   * Strings.asDefaultString("abc") = "abc"
   * Strings.asDefaultString(NonNull) = NonNull.toString()
   * </pre>
   */
  public static String asDefaultString(final Object obj) {
    return obj != null ? defaultString(obj.toString()) : EMPTY;
  }

  /**
   * Returns true if and only if the given parameters are not null and the given {@code str}
   * contains the given specified string {@code searchStr}. The difference with
   * {@link String#contains(CharSequence)}, this method is null safe.
   *
   * <pre>
   * Strings.contains(null, *)    = false
   * Strings.contains("", *)      = false
   * Strings.contains("abc", 'a') = true
   * Strings.contains("abc", 'z') = false
   * </pre>
   *
   * @param str the string being searched
   * @param searchStr the string to search for
   */
  public static boolean contains(String str, String searchStr) {
    return str != null && searchStr != null && str.contains(searchStr);
  }

  /**
   * Returns true if and only if the given parameters are not null and the given {@code str}
   * contains any char from the given specified string {@code searchStr}. This method is null safe.
   *
   * <pre>
   * Strings.containsAnyChars(null, *)      = false
   * Strings.containsAnyChars("", *)        = false
   * Strings.containsAnyChars(" ", "a b")   = true
   * Strings.containsAnyChars("abc", "ab")  = true
   * Strings.containsAnyChars("abc", "z")   = false
   * </pre>
   *
   * @param str the string being searched
   * @param searchStr the string that contains chars to search for
   */
  public static boolean containsAnyChars(String str, String searchStr) {
    return str != null && searchStr != null
        && str.chars().anyMatch(c -> searchStr.indexOf(c) != -1);
  }

  /**
   * Returns either the given {@code str}, or if the given {@code str} is whitespace, empty ("") or
   * {@code null}, the default value provided by the given {@code supplier}.
   *
   * <pre>
   * Strings.defaultBlank(null, ()->"DFLT")  = "DFLT"
   * Strings.defaultBlank("", ()->"DFLT")    = "DFLT"
   * Strings.defaultBlank(" ", ()->"DFLT")   = "DFLT"
   * Strings.defaultBlank("abc", ()->"DFLT") = "abc"
   * Strings.defaultBlank("", null)      = null
   * </pre>
   *
   * @param str the string
   * @param supplier the supplier used to provide the default value
   */
  public static <T extends CharSequence> T defaultBlank(final T str, final Supplier<T> supplier) {
    return isBlank(str) ? supplier == null ? null : supplier.get() : str;
  }

  /**
   * Returns either the given {@code str}, or if the given {@code str} is whitespace, empty ("") or
   * {@code null}, the default value of the given {@code defaultStr}.
   *
   * <pre>
   * Strings.defaultBlank(null, "DFLT")  = "DFLT"
   * Strings.defaultBlank("", "DFLT")    = "DFLT"
   * Strings.defaultBlank(" ", "DFLT")   = "DFLT"
   * Strings.defaultBlank("abc", "DFLT") = "abc"
   * Strings.defaultBlank("", null)      = null
   * </pre>
   *
   * @param str the string
   * @param dfltStr the default string
   */
  public static <T extends CharSequence> T defaultBlank(final T str, final T dfltStr) {
    return isBlank(str) ? dfltStr : str;
  }

  /**
   * Returns either the given {@code str}, or if {@code null}, the {@link #EMPTY}. The difference
   * with {@link #defaultBlank(CharSequence, CharSequence)}, this method doesn't consider the
   * whitespace.
   *
   * <pre>
   * Strings.defaultString(null)  = ""
   * Strings.defaultString("")    = ""
   * Strings.defaultString("abc") = "abc"
   * </pre>
   *
   * @param str the string
   */
  public static String defaultString(final String str) {
    return defaultString(str, EMPTY);
  }

  /**
   * Returns either the given {@code str}, or if {@code null}, the default value of the given
   * {@code defaultStr}. The difference with {@link #defaultBlank(CharSequence, CharSequence)}, this
   * method doesn't consider the whitespace.
   *
   * <pre>
   * Strings.defaultString(null, "DFLT")  = "DFLT"
   * Strings.defaultString("", "DFLT")    = ""
   * Strings.defaultString("abc", "DFLT") = "abc"
   * </pre>
   *
   * @param str the string
   * @param dfltStr the default string
   */
  public static String defaultString(final String str, final String dfltStr) {
    return str == null ? dfltStr : str;
  }

  /**
   * Returns either the stripped given {@code str}, or if {@code null}, the {@link #EMPTY}
   *
   * <pre>
   * Strings.defaultStrip(null)          = ""
   * Strings.defaultStrip("")            = ""
   * Strings.defaultStrip("     ")       = ""
   * Strings.defaultStrip("abc")         = "abc"
   * Strings.defaultStrip("    abc    ") = "abc"
   * </pre>
   *
   * @param str the string for stripping
   * @see String#strip()
   */
  public static String defaultStrip(String str) {
    return str == null ? EMPTY : str.strip();
  }

  /**
   * Returns either the trimmed given {@code str}, or if {@code null}, the {@link #EMPTY}
   *
   * <pre>
   * Strings.defaultTrim(null)          = ""
   * Strings.defaultTrim("")            = ""
   * Strings.defaultTrim("     ")       = ""
   * Strings.defaultTrim("abc")         = "abc"
   * Strings.defaultTrim("    abc    ") = "abc"
   * </pre>
   *
   * @param str the string for trimming
   * @see String#trim()
   */
  public static String defaultTrim(String str) {
    return str == null ? EMPTY : str.trim();
  }

  /**
   * Returns the escaped string pattern.
   *
   * @param escapes the escape characters
   * @param quote the quote string
   */
  public static Pattern escapedPattern(final String escapes, final String quote) {
    if (isNotEmpty(escapes) && isNotEmpty(quote)) {
      if (!areEqual(escapes, quote)) {
        return Pattern.compile("(?<!" + Pattern.quote(escapes) + ")" + Pattern.quote(quote));
      } else {
        return Pattern.compile("(?<!" + Pattern.quote(quote) + ")" + Pattern.quote(quote) + "(?!"
            + Pattern.quote(quote) + ")");
      }
    } else if (isNotEmpty(quote)) {
      return Pattern.compile(Pattern.quote(quote));
    }
    return null;
  }

  /**
   * Split string supports escape characters
   *
   * @param str the string to be separated
   * @param escapes the separator escape characters
   * @param separator the separator string
   * @return String[]
   */
  public static String[] escapedSplit(final String str, final String escapes,
      final String separator) {
    return escapedSplit(str, escapes, separator, 0);
  }

  /**
   * Split string supports escape characters
   *
   * @param str the string to be separated
   * @param escapes the separator escape characters
   * @param separator the separator string
   * @param limit the result threshold
   *
   * @see String#split(String, int)
   */
  public static String[] escapedSplit(final String str, final String escapes,
      final String separator, final int limit) {
    if (isEmpty(str)) {
      return EMPTY_ARRAY;
    }
    if (isEmpty(separator)) {
      return new String[] {str};
    }
    if (isEmpty(escapes)) {
      return str.split(separator, limit);
    }
    final String target = escapes.concat(separator);
    final String[] result = escapedPattern(escapes, separator).split(str, limit);
    Arrays.setAll(result, i -> replace(result[i], target, separator));
    return result;
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
   */
  public static boolean isBlank(final CharSequence cs) {
    int len;
    if (cs == null || (len = cs.length()) == 0) {
      return true;
    }
    for (int i = 0, j = len - 1; i <= j; i++, j--) {
      if (!Character.isWhitespace(cs.charAt(i)) || !Character.isWhitespace(cs.charAt(j))) {
        return false;
      }
    }
    return true;
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
   * @since 1.6.2
   */
  public static boolean isBlank(final String cs) {
    return cs == null || cs.isBlank();
  }

  /**
   * Determine if a string is decimal number. Hexadecimal and scientific notations and octal number
   * are not considered
   *
   * <pre>
   * Strings.isDecimalNumber(null)      =false
   * Strings.isDecimalNumber("")        =false
   * Strings.isDecimalNumber(" ")       =false
   * Strings.isDecimalNumber("123")     =true
   * Strings.isDecimalNumber("0123")    =true
   * Strings.isDecimalNumber("1.23")    =true
   * Strings.isDecimalNumber(".123")    =true
   * Strings.isDecimalNumber("-123")    =true
   * Strings.isDecimalNumber("-.123")   =true
   * Strings.isDecimalNumber("+123")    =true
   * Strings.isDecimalNumber("+.123")   =true
   * Strings.isDecimalNumber("-.")      =false
   * Strings.isDecimalNumber(".0")      =true
   * Strings.isDecimalNumber("12 3")    =false
   * Strings.isDecimalNumber(" 123")    =false
   * Strings.isDecimalNumber("a123")    =false
   * </pre>
   */
  public static boolean isDecimalNumber(final CharSequence obj) {
    int len;
    if (obj == null || (len = obj.length()) == 0) {
      return false;
    }
    if (obj.charAt(len - 1) == Chars.DOT) {
      return false;
    }
    int idx = 0;
    if (obj.charAt(0) == Chars.DASH || obj.charAt(0) == Chars.PLUS) {
      if (len == 1) {
        return false;
      }
      idx = 1;
    }
    int point = 0;
    for (int i = idx; i < len; i++) {
      char ch = obj.charAt(i);
      if (ch == Chars.DOT) {
        if (++point > 1) {
          return false;
        }
        continue;
      }
      if (!Character.isDigit(ch)) {
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
   */
  public static boolean isNoneBlank(final CharSequence... css) {
    if (css == null || css.length == 0) {
      return false;
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
   * Strings.isNotBlank(null)      = false
   * Strings.isNotBlank("")        = false
   * Strings.isNotBlank(" ")       = false
   * Strings.isNotBlank("abc")     = true
   * Strings.isNotBlank("  abc  ") = true
   * </pre>
   */
  public static boolean isNotBlank(final String cs) {
    return cs != null && !cs.isBlank();
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
   * @param seq the char sequence to test matching
   * @param flags Match flags, a bit mask that may include {@link Pattern#CASE_INSENSITIVE},
   *        {@link Pattern#MULTILINE}, {@link Pattern#DOTALL}, {@link Pattern#UNICODE_CASE},
   *        {@link Pattern#CANON_EQ}, {@link Pattern#UNIX_LINES}, {@link Pattern#LITERAL},
   *        {@link Pattern#UNICODE_CHARACTER_CLASS} and {@link Pattern#COMMENTS}
   * @param regex the regex for testing
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
   * @param seq the char sequence to test matching
   * @param flags Match flags, a bit mask that may include {@link Pattern#CASE_INSENSITIVE},
   *        {@link Pattern#MULTILINE}, {@link Pattern#DOTALL}, {@link Pattern#UNICODE_CASE},
   *        {@link Pattern#CANON_EQ}, {@link Pattern#UNIX_LINES}, {@link Pattern#LITERAL},
   *        {@link Pattern#UNICODE_CHARACTER_CLASS} and {@link Pattern#COMMENTS}
   * @param regex the regex for testing
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
   * @param str the char sequence to test matching
   * @param ignoreCase whether to ignore case when test matching
   * @param isDos whether to follow the path rules of windows
   * @param globExpress the glob expression for testing
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
   * @param str the char sequence to test matching
   * @param ignoreCase whether to ignore case when test matching
   * @param wildcardExpress the wildcard expression for testing
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
   * Parses string template and replaces macros with resolved values. if the given template does not
   * contain the macro element of the string template, it will return directly. Supports default
   * value and escape and nested.
   * <p>
   *
   * <pre>
   * The string macro templates: ${...}
   * Example:
   *   <b>1. Normal</b>: ${firstName} ${lastName}, the firstName and the lastName will be resolved
   *   from the given provider, if not found throw NoSuchElementException.
   *
   *   <b>2. With default value</b>: ${firstName} ${lastName:unknown}, if the lastName can't be
   *   resolved from the given provider, then use 'unknown' as the retrieve result.
   *
   *   <b>3. With escape</b>: ${firstName} ${lastName} \\${escape}, the \\${escape} will be restore
   *   to ${escape}.
   *
   *   <b>4. Nested</b>: ${first${key}}, If the provider provides the value of the variable named 'key' as
   *   Name and at the same time provides the value of the variable named 'firstName', the value
   *   corresponding to firstName will be returned.
   * </pre>
   *
   * @param template the given template that may contain the macro element of the template.
   * @param provider the template named variable value provider
   *
   */
  public static String parseDollarTemplate(String template, Function<String, Object> provider) {
    return StringTemplate.DEFAULT.parse(template, provider);
  }

  /**
   * Split the string into fixed length chunks array.
   *
   * @param str the string to be separated
   * @param chunkLength the fixed chunk length
   */
  public static String[] partition(final String str, int chunkLength) {
    if (chunkLength < 1 || str == null) {
      return EMPTY_ARRAY;
    }
    return str.split("(?<=\\G.{" + chunkLength + "})");
  }

  /**
   * Split the string into a string array by whole separator string, not regex.
   * <p>
   * Note: The difference from {@link #split(String, String)} is that the split does not discard
   * empty string in two consecutive delimiters, thus can be used to split a format-specific row
   * string into an array of column strings.
   *
   * @param str the string to be separated
   * @param delimiter the delimiting string
   * @return string array
   */
  public static String[] partition(final String str, final String delimiter) {
    int len;
    int slen;
    if (str == null || (len = str.length()) == 0) {
      return EMPTY_ARRAY;
    }
    if (delimiter == null || (slen = delimiter.length()) == 0) {
      return new String[] {str};
    }
    int s = 0;
    int e = 0;
    int i = 0;
    int g = len > SPLIT_ARRAY_LENGTH ? SPLIT_ARRAY_LENGTH : (len >> 1) + 1;
    String[] array = new String[g];
    while (e < len) {
      e = str.indexOf(delimiter, s);
      if (e > -1) {
        if (e >= s) {
          if (i == g) {
            array = Arrays.copyOf(array, g += g);
          }
          array[i++] = str.substring(s, e);
        }
        s = e + slen;
      } else {
        if (s <= len) {
          array = Arrays.copyOf(array, i + 1);
          array[i++] = str.substring(s);
        }
        e = len;
      }
    }
    return Arrays.copyOf(array, i);
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
   * @param str the given string to be adjusted
   * @param filter the filter used to test whether characters should be deleted
   * @return removeCharIf
   */
  public static String removeCharIf(final String str, final Predicate<Character> filter) {
    int len;
    if (str == null || (len = str.length()) == 0 || filter == null) {
      return str;
    }
    StringBuilder buf = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);
      if (!filter.test(ch)) {
        buf.append(ch);
      }
    }
    return buf.toString();
  }

  /**
   * Remove characters that contains in the given sample chars sequence.
   *
   * @param str the given string to be adjusted
   * @param sample a string, all characters in the string will be deleted.
   * @return removeCharIf
   */
  public static String removeCharIf(final String str, final String sample) {
    if (str == null || sample == null) {
      return str;
    }
    return removeCharIf(str, s -> sample.indexOf(s) != -1);
  }

  /**
   * Replace string use for short string, not regex.
   *
   * @param original the original string
   * @param replaced the sequence of char values to be replaced
   * @param replacement the replacement sequence of char values
   * @return replaced
   */
  public static String replace(String original, String replaced, String replacement) {
    if (original == null || replaced == null || replaced.length() == 0) {
      return original;
    }
    String temp = original;
    int i = 0;
    if ((i = temp.indexOf(replaced, i)) >= 0) {
      char[] srcArray = temp.toCharArray();
      char[] nsArray = replacement.toCharArray();
      int olen = replaced.length();
      int srclen = srcArray.length;
      StringBuilder buf = new StringBuilder(srclen);
      buf.append(srcArray, 0, i).append(nsArray);
      i += olen;
      int j = i;
      while ((i = temp.indexOf(replaced, i)) > 0) {
        buf.append(srcArray, j, i - j).append(nsArray);
        i += olen;
        j = i;
      }
      buf.append(srcArray, j, srclen - j);
      temp = buf.toString();
      buf.setLength(0);
    }
    return temp;
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
   * Segment string into a string array with Predicate, delete blank elements or strip elements as
   * needed. The difference with {@link #split(String, Predicate)} is that this method does not
   * delete any characters.
   *
   * @see Strings#segment(String, Predicate)
   *
   * @param str the string to be separated
   * @param removeBlank whether to remove the blank element in return array
   * @param strip whether to strip the element in return array
   * @param predicate the delimiting predicate
   * @return segments
   */
  public static String[] segment(final String str, final boolean removeBlank, final boolean strip,
      final Predicate<Character> predicate) {
    return regulateSplits(segment(str, predicate), removeBlank, strip);
  }

  /**
   * Segment string into a string array with Predicate, the difference with
   * {@link #split(String, Predicate)} is that this method does not delete any characters.
   *
   * <pre>
   * Strings.segment("abc123efg456", Character::isDigit)        =       ["abc","123","efg","456"]
   * Strings.segment("abc,efg", c -> c==',')                    =       ["abc",",","efg"]
   * Strings.segment("123", Character::isDigit)                 =       ["123"]
   * Strings.segment(null, Character::isDigit)                  =       []
   * Strings.segment("abc", Character::isDigit)                 =       ["abc"]
   * Strings.segment("abc", null)                               =       ["abc"]
   * Strings.segment(null, null)                                =       []
   * </pre>
   *
   * @param str the string to be separated
   * @param predicate the delimiting predicate
   * @return segments
   */
  public static String[] segment(final String str, final Predicate<Character> predicate) {
    int len;
    if (str == null || (len = str.length()) == 0) {
      return EMPTY_ARRAY;
    }
    if (predicate == null) {
      return new String[] {str};
    }
    int i = 0;
    int s = 0;
    int g = len > SPLIT_ARRAY_LENGTH ? SPLIT_ARRAY_LENGTH : (len >> 1) + 1;
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
   * @param str the string to be separated
   * @param removeBlank whether to remove the blank element in return array
   * @param strip whether to strip the element in return array
   * @param p the delimiting predicate
   * @return split
   */
  public static String[] split(final String str, final boolean removeBlank, final boolean strip,
      Predicate<Character> p) {
    return regulateSplits(split(str, p), removeBlank, strip);
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
   * @param str the string to be separated
   * @param splitter the delimiting predicate
   * @return split
   */
  public static String[] split(final String str, final Predicate<Character> splitter) {
    return split(str, splitter, -1);
  }

  /**
   * Split the string into a string array with the predicate.
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
   * @param str the string to be separated
   * @param splitter the delimiting predicate
   * @param limit a non-negative integer specifying a limit on the number of substrings to be
   *        included in the array. If provided, splits the string at each occurrence of the
   *        specified separator, but stops when limit entries have been placed in the array. Any
   *        leftover text is not included in the array at all.
   * @return split
   */
  public static String[] split(final String str, final Predicate<Character> splitter, int limit) {
    int len;
    if (str == null || (len = str.length()) == 0) {
      return EMPTY_ARRAY;
    }
    if (splitter == null) {
      return new String[] {str};
    }
    int i = 0;
    int s = 0;
    int g = len > SPLIT_ARRAY_LENGTH ? SPLIT_ARRAY_LENGTH : (len >> 1) + 1;
    int l = limit < 1 ? Integer.MAX_VALUE : limit;
    int ai = 0;
    String[] array = new String[g];
    boolean match = false;
    while (i < len) {
      if (splitter.test(str.charAt(i))) {
        if (match) {
          if (ai == g) {
            array = Arrays.copyOf(array, g += g);
          }
          array[ai++] = str.substring(s, i);
          match = false;
          // limit size
          if (l == ai) {
            break;
          }
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
   * Split the string into a string array by whole separator string, not regex.
   *
   * @param str the string to be separated
   * @param wholeSeparator the delimiting string
   * @return string array
   */
  public static String[] split(final String str, final String wholeSeparator) {
    return split(str, wholeSeparator, -1);
  }

  /**
   * Split the string into a string array, delete blank elements or strip elements as needed.
   *
   * @param str the string to be separated
   * @param wholeSeparator the delimiting string
   * @param removeBlank whether to remove the blank element in return array
   * @param strip whether to strip the element in return array
   * @return split
   */
  public static String[] split(final String str, final String wholeSeparator,
      final boolean removeBlank, final boolean strip) {
    return regulateSplits(split(str, wholeSeparator), removeBlank, strip);
  }

  /**
   * Split the string into a string array by whole separator string, not regex.
   *
   * @param str the string to be separated
   * @param wholeSeparator the delimiting string
   * @param limit a non-negative integer specifying a limit on the number of substrings to be
   *        included in the array. If provided, splits the string at each occurrence of the
   *        specified separator, but stops when limit entries have been placed in the array. Any
   *        leftover text is not included in the array at all.
   * @return string array
   */
  public static String[] split(final String str, final String wholeSeparator, int limit) {
    int len;
    int slen;
    if (str == null || (len = str.length()) == 0) {
      return EMPTY_ARRAY;
    }
    if (wholeSeparator == null || (slen = wholeSeparator.length()) == 0) {
      return new String[] {str};
    }
    if (slen == 1) {
      char wholeChar = wholeSeparator.charAt(0);
      return split(str, c -> c == wholeChar, limit);
    }
    int s = 0;
    int e = 0;
    int i = 0;
    int g = len > SPLIT_ARRAY_LENGTH ? SPLIT_ARRAY_LENGTH : (len >> 1) + 1;
    int l = limit < 1 ? Integer.MAX_VALUE : limit;
    String[] array = new String[g];
    while (e < len) {
      e = str.indexOf(wholeSeparator, s);
      if (e > -1) {
        if (e > s) {
          if (i == g) {
            array = Arrays.copyOf(array, g += g);
          }
          array[i++] = str.substring(s, e);
          // limit size
          if (i == l) {
            break;
          }
        }
        s = e + slen;
      } else {
        if (s < len) {
          array = Arrays.copyOf(array, i + 1);
          array[i++] = str.substring(s);
          // limit size
          if (i == l) {
            break;
          }
        }
        e = len;
      }
    }
    return Arrays.copyOf(array, i);
  }

  /**
   * Split the string into a list of objects of the given class with the given whole separator
   * string.
   *
   * @param <T> the type of element of the return list
   * @param str the string to be separated
   * @param wholeSeparator the delimiting string
   * @param clazz the class of element of the return list
   *
   * @see Strings#split(String, String)
   */
  public static <T> List<T> splitAs(final String str, final String wholeSeparator, Class<T> clazz) {
    String[] array = split(str, wholeSeparator);
    List<T> list = new ArrayList<>();
    for (String ele : array) {
      list.add(toObject(ele, clazz));
    }
    return list;
  }

  /**
   * Null-safe strip string
   *
   *
   * from JDK-11
   *
   * @see String#strip()
   */
  public static String strip(final String str) {
    return str == null ? null : str.strip();
  }

  /**
   * Null-safe strip leading
   */
  public static String stripLeading(final String str) {
    return str == null ? null : str.stripLeading();
  }

  /**
   * Null-safe strip trainling
   *
   * from JDK-11
   *
   * @see String#strip()
   */
  public static String stripTrailing(final String str) {
    return str == null ? null : str.stripTrailing();
  }

  /**
   * Returns a string that is a substring of this string. The substring begins with the character at
   * the specified index and extends to the end of this string.
   * <p>
   * Note: If the given from index < 0 it represents the reverse order index position of the given
   * string.
   *
   * <pre>
   * Strings.substring(null, 2)             = null
   * Strings.substring("", 0)               = ""
   * Strings.substring("abc", 1)            = "bc"
   * Strings.substring("abc", 2)            = "c"
   * Strings.substring("abc", -1)           = "c"
   * Strings.substring("abc", -2)           = "bc"
   * </pre>
   *
   * @param str the string to gain the substring
   * @param fromIndex the beginning index, inclusive
   * @see String#substring(int)
   */
  public static String substring(final String str, final int fromIndex) {
    if (str == null || fromIndex == 0) {
      return str;
    }
    return fromIndex < 0 ? str.substring(str.length() + fromIndex) : str.substring(fromIndex);
  }

  /**
   *
   *
   * Returns a string that is a substring of this string. The substring begins at the specified
   * beginIndex and extends to the character at index endIndex - 1. Thus, the length of the
   * substring is endIndex-beginIndex.
   *
   * <p>
   * Note: If the given index < 0 it represents the reverse order index position of the given
   * string.
   *
   * <pre>
   * Strings.substring(null, *, *)                  = null
   * Strings.substring("abcefg", 0, 1)              = "a"
   * Strings.substring("abcefg", 0, -1)             = "abcef"
   * Strings.substring("abcefg", 3, 6)              = "efg"
   * Strings.substring("abcefg", 3, -1)             = "ef""
   * Strings.substring("abcefg", 2, -2)             = "ce"
   * </pre>
   *
   * @param str the string to gain the substring
   * @param fromIndex the beginning index, inclusive.
   * @param toIndex the ending index, exclusive.
   * @see String#substring(int, int)
   */
  public static String substring(final String str, final int fromIndex, final int toIndex) {
    if (str == null) {
      return null;
    }
    int length = str.length();
    int beginIndex = fromIndex < 0 ? length + fromIndex : fromIndex;
    int endIndex = toIndex < 0 ? length + toIndex : toIndex;
    return str.substring(beginIndex, endIndex);
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
   * @param str the string to trim
   */
  public static String trim(String str) {
    return str == null ? null : str.trim();
  }

  static List<Pair<Boolean, String>> segment(final String str, final String wholeSeparator) {
    int len;
    int slen;
    if (str == null || (len = str.length()) == 0) {
      return listOf(Pair.of(false, EMPTY));
    }
    if (wholeSeparator == null || (slen = wholeSeparator.length()) == 0) {
      return listOf(Pair.of(false, str));
    }
    int s = 0;
    int e = 0;
    List<Pair<Boolean, String>> list = new ArrayList<>();
    while (e < len) {
      e = str.indexOf(wholeSeparator, s);
      if (e > -1) {
        if (e > s) {
          list.add(Pair.of(false, str.substring(s, e)));
        }
        list.add(Pair.of(true, wholeSeparator));
        s = e + slen;
      } else {
        if (s < len) {
          list.add(Pair.of(false, str.substring(s)));
        }
        e = len;
      }
    }
    return list;
  }

  private static String[] regulateSplits(String[] splits, final boolean removeBlank,
      final boolean strip) {
    if (!removeBlank && !strip) {
      return splits;
    } else {
      String[] result = new String[splits.length];
      int i = 0;
      for (String e : splits) {
        if (isNotBlank(e) || isBlank(e) && !removeBlank) {
          result[i++] = strip ? strip(e) : e;
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
     * @param ignoreCase whether to ignore case
     * @param wildcardExpress the wildcard expression
     */
    protected WildcardMatcher(boolean ignoreCase, String wildcardExpress) {
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
        return other.wildcardExpress == null;
      } else {
        return wildcardExpress.equals(other.wildcardExpress);
      }
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
      return prime * result + (wildcardExpress == null ? 0 : wildcardExpress.hashCode());
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
          if ("?".equals(tokens[tokenIdx])) {
            textIdx++;
            if (textIdx > text.length()) {
              break;
            }
            anyChars = false;
          } else if ("*".equals(tokens[tokenIdx])) {
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
            } else if (!checkRegionMatches(text, textIdx, tokens[tokenIdx])) {
              break;
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
      return list.toArray(new String[0]);
    }

  }
}

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

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.StringUtils.defaultTrim;
import static org.corant.shared.util.StringUtils.isBlank;
import java.util.Locale;
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

  private PathUtils() {
    super();
  }

  /**
   * Decide {@code PathMatcher} from path expressions. Returns a {@code PathMatcher} that performs
   * match operations on the {@code String} representation of path by interpreting a given pattern.
   *
   * The pathExp parameter identifies the syntax and the pattern and takes the form: <blockquote>
   *
   * <pre>
   * <i>syntax</i><b>:</b><i>pattern</i>
   * </pre>
   *
   * </blockquote> where {@code ':'} stands for itself.
   *
   * <p>
   * Supports the "{@code glob}" and "{@code regex}" syntaxes.
   *
   * <p>
   * NOTE: If syntax is not found, use the following processes
   * <ul>
   * <li>1. If the path expression is empty, it is regarded as {@code GlobMatcher}, and the pattern
   * is {@code **}</li>
   * <li>2. If the path expression ends with the path separator, it is also regarded as
   * {@code GlobMatcher}, and the pattern is {@code xxxx/**}</li>
   * <li>3. If {@code *} or {@code ?} appears in the path expression, it is also regarded as
   * {@code GlobMatcher}</li>
   * <li>4. In addition to the above description, it is regarded as {@code CaseMatcher}</li>
   * </ul>
   *
   * @param pathExp
   * @param dos
   * @param ignoreCase
   * @return decidePathMatcher
   */
  public static PathMatcher decidePathMatcher(String pathExp, boolean dos, boolean ignoreCase) {
    String path = defaultTrim(pathExp);
    if (path.startsWith("regex:")) {
      path = shouldNotBlank(path.substring("regex:".length()));
      return new RegexMatcher(ignoreCase, path);
    } else {
      boolean glob = false;
      String pathSpr = dos ? "\\" : "/";
      if (isBlank(path) || path.endsWith(pathSpr)) {
        path += "**";
        glob = true;
      }
      if (path.startsWith("glob:")) {
        path = shouldNotBlank(path.substring("glob:".length()));
        glob = true;
      } else {
        glob = path.chars().anyMatch(c -> c == '?' || c == '*');
      }
      if (glob) {
        return new GlobMatcher(dos, ignoreCase, path);
      } else {
        return new CaseMatcher(path, ignoreCase);
      }
    }
  }

  public static boolean matchClassPath(String path, String globExpress) {
    return matchUnixPath(path, globExpress);
  }

  public static boolean matchPath(String path, String globExpress) {
    return System.getProperty("os.name").toLowerCase(Locale.getDefault()).startsWith("window")
        ? matchWinPath(path, globExpress)
        : matchUnixPath(path, globExpress);
  }

  public static boolean matchUnixPath(String path, String globExpress) {
    return GlobPatterns.build(globExpress, false, false).matcher(path).matches();
  }

  public static boolean matchWinPath(String path, String globExpress) {
    return GlobPatterns.build(globExpress, true, true).matcher(path).matches();
  }

  private static String resolvePlainParent(String patternChars, String pathSeparator,
      String express) {
    if (isBlank(patternChars)) {
      return express;
    }
    int idx = -1;
    for (int i = 0; i < express.length(); i++) {
      char c = express.charAt(i);
      if (patternChars.indexOf(c) != -1) {
        idx = i;
        break;
      }
    }
    if (idx == -1) {
      return express;
    } else if (idx == 0) {
      return StringUtils.EMPTY;
    } else {
      String path = express.substring(0, idx);
      if (path.indexOf(pathSeparator) != -1) {
        return path.substring(0, path.lastIndexOf(pathSeparator));
      } else {
        return path;
      }
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 下午3:48:58
   *
   */
  public static class CaseMatcher implements PathMatcher {

    private final String express;
    private final boolean ignoreCase;

    /**
     * @param express
     * @param ignoreCase
     */
    protected CaseMatcher(String express, boolean ignoreCase) {
      super();
      this.express = express;
      this.ignoreCase = ignoreCase;
    }

    @Override
    public String getExpress() {
      return express;
    }

    @Override
    public String getPlainParent(String pathSeparator) {
      return PathUtils.resolvePlainParent(null, pathSeparator, express);
    }

    @Override
    public boolean test(String t) {
      return ignoreCase ? express.equalsIgnoreCase(t) : express.equals(t);
    }
  }

  /**
   * corant-shared
   *
   * Use Glob wildcards for filtering.
   *
   * @author bingo 下午8:32:50
   *
   */
  public static class GlobMatcher implements PathMatcher {

    private final boolean isDos;
    private final boolean ignoreCase;
    private final String globExpress;
    private final Pattern pattern;

    /**
     * @param isDos
     * @param ignoreCase
     * @param globExpress
     */
    protected GlobMatcher(boolean isDos, boolean ignoreCase, String globExpress) {
      super();
      this.isDos = isDos;
      this.ignoreCase = ignoreCase;
      this.globExpress = globExpress;
      pattern = GlobPatterns.build(globExpress, isDos, ignoreCase);
    }

    public static boolean hasGlobChar(String str) {
      return str != null && str.chars().anyMatch(GlobPatterns::isGlobChar);
    }

    public static GlobMatcher of(boolean isDos, String globExpress, boolean ignoreCase) {
      return new GlobMatcher(isDos, ignoreCase, globExpress);
    }

    public static GlobMatcher of(String globExpress) {
      return new GlobMatcher(false, false, globExpress);
    }

    public static GlobMatcher of(String globExpress, boolean ignoreCase) {
      return of(System.getProperty("os.name").toLowerCase(Locale.getDefault()).startsWith("window"),
          globExpress, ignoreCase);
    }

    public static GlobMatcher ofDos(String globExpress) {
      return new GlobMatcher(true, true, globExpress);
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
      return isDos == other.isDos && ignoreCase == other.ignoreCase;
    }

    @Override
    public String getExpress() {
      return globExpress;
    }

    public Pattern getPattern() {
      return pattern;
    }

    @Override
    public String getPlainParent(String pathSeparator) {
      return PathUtils.resolvePlainParent(GlobPatterns.DIST_GLO_CHARS, pathSeparator, globExpress);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (globExpress == null ? 0 : globExpress.hashCode());
      result = prime * result + (ignoreCase ? 1231 : 1237);
      result = prime * result + (isDos ? 1231 : 1237);
      return result;
    }

    public boolean isDos() {
      return isDos;
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
   * <p>
   * NOTE: code base from sun.nio.fs
   *
   * <p>
   * The glob patterns:
   *
   * <ul>
   * <li>
   * <p>
   * The {@code *} character matches zero or more {@link Character characters} of a name component
   * without crossing directory boundaries.
   * </p>
   * </li>
   *
   * <li>
   * <p>
   * The {@code **} characters matches zero or more {@link Character characters} crossing directory
   * boundaries.
   * </p>
   * </li>
   *
   * <li>
   * <p>
   * The {@code ?} character matches exactly one character of a name component.
   * </p>
   * </li>
   *
   * <li>
   * <p>
   * The backslash character ({@code \}) is used to escape characters that would otherwise be
   * interpreted as special characters. The expression {@code \\} matches a single backslash and
   * "\{" matches a left brace for example.
   * </p>
   * </li>
   *
   * <li>
   * <p>
   * The {@code [ ]} characters are a <i>bracket expression</i> that match a single character of a
   * name component out of a set of characters. For example, {@code [abc]} matches {@code "a"},
   * {@code "b"}, or {@code "c"}. The hyphen ({@code -}) may be used to specify a range so
   * {@code [a-z]} specifies a range that matches from {@code "a"} to {@code "z"} (inclusive). These
   * forms can be mixed so [abce-g] matches {@code "a"}, {@code "b"}, {@code "c"}, {@code "e"},
   * {@code "f"} or {@code "g"}. If the character after the {@code [} is a {@code !} then it is used
   * for negation so {@code
   *   [!a-c]} matches any character except {@code "a"}, {@code "b"}, or {@code
   *   "c"}.
   * <p>
   * Within a bracket expression the {@code *}, {@code ?} and {@code \} characters match themselves.
   * The ({@code -}) character matches itself if it is the first character within the brackets, or
   * the first character after the {@code !} if negating.
   * </p>
   * </li>
   *
   * <li>
   * <p>
   * The {@code { }} characters are a group of subpatterns, where the group matches if any
   * subpattern in the group matches. The {@code ","} character is used to separate the subpatterns.
   * Groups cannot be nested.
   * </p>
   * </li>
   *
   * <li>
   * <p>
   * Leading period<tt>&#47;</tt>dot characters in file name are treated as regular characters in
   * match operations. For example, the {@code "*"} glob pattern matches file name {@code ".login"}.
   * </p>
   * </li>
   * </ul>
   * <p>
   * For example:
   *
   * <blockquote>
   * <table border="0" summary="Pattern Language">
   * <tr>
   * <td>{@code *.java}</td>
   * <td>Matches a path that represents a file name ending in {@code .java}</td>
   * </tr>
   * <tr>
   * <td>{@code *.*}</td>
   * <td>Matches file names containing a dot</td>
   * </tr>
   * <tr>
   * <td>{@code *.{java,class}}</td>
   * <td>Matches file names ending with {@code .java} or {@code .class}</td>
   * </tr>
   * <tr>
   * <td>{@code foo.?}</td>
   * <td>Matches file names starting with {@code foo.} and a single character extension</td>
   * </tr>
   * <tr>
   * <td><tt>&#47;home&#47;*&#47;*</tt>
   * <td>Matches <tt>&#47;home&#47;gus&#47;data</tt> on UNIX platforms</td>
   * </tr>
   * <tr>
   * <td><tt>&#47;home&#47;**</tt>
   * <td>Matches <tt>&#47;home&#47;gus</tt> and <tt>&#47;home&#47;gus&#47;data</tt> on UNIX
   * platforms</td>
   * </tr>
   * <tr>
   * <td><tt>C:&#92;&#92;*</tt>
   * <td>Matches <tt>C:&#92;foo</tt> and <tt>C:&#92;bar</tt> on the Windows platform (note that the
   * backslash is escaped; as a string literal in the Java Language the pattern would be
   * <tt>"C:&#92;&#92;&#92;&#92;*"</tt>)</td>
   * </tr>
   *
   * </table>
   * </blockquote>
   *
   * @author bingo 下午4:30:24
   */
  public static class GlobPatterns {

    public static final String DIST_REG_CHARS = ".^$+{[]|()";
    public static final String DIST_GLO_CHARS = "\\*?[{";
    private static final char EOL = 0;

    public static Pattern build(String globExpress, boolean isDos, boolean ignoreCase) {
      if (isDos) {
        if (ignoreCase) {
          return Pattern.compile(toWindowsRegexPattern(globExpress),
              Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } else {
          return Pattern.compile(toWindowsRegexPattern(globExpress), Pattern.UNICODE_CASE);
        }
      } else {
        if (ignoreCase) {
          return Pattern.compile(toUnixRegexPattern(globExpress),
              Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } else {
          return Pattern.compile(toUnixRegexPattern(globExpress), Pattern.UNICODE_CASE);
        }
      }
    }

    public static boolean isGlobChar(char c) {
      return DIST_GLO_CHARS.indexOf(c) != -1;
    }

    public static boolean isGlobChar(int c) {
      return DIST_GLO_CHARS.indexOf(c) != -1;
    }

    public static boolean isRegexChar(char c) {
      return DIST_REG_CHARS.indexOf(c) != -1;
    }

    public static boolean isRegexChar(int c) {
      return DIST_REG_CHARS.indexOf(c) != -1;
    }

    static String toUnixRegexPattern(String globExpress) {
      return toRegexPattern(globExpress, false);
    }

    static String toWindowsRegexPattern(String globExpress) {
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

  /**
   * corant-shared
   *
   * @author bingo 上午11:18:13
   *
   */
  public interface PathMatcher extends Predicate<String> {

    String getExpress();

    String getPlainParent(String pathSeparator);

  }

  /**
   * corant-shared
   *
   * @author bingo 下午2:28:39
   *
   */
  public static class RegexMatcher implements PathMatcher {

    public static final String REG_CHARS = ".+*?^$()[]{}|";

    private final boolean ignoreCase;
    private final String express;
    private final Pattern pattern;

    /**
     * @param ignoreCase
     * @param express
     */
    protected RegexMatcher(boolean ignoreCase, String express) {
      super();
      this.ignoreCase = ignoreCase;
      this.express = shouldNotNull(express);
      pattern = Pattern.compile(express, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
    }

    public static boolean hasRegexChar(String str) {
      return str != null && str.chars().anyMatch(RegexMatcher::isRegexChar);
    }

    public static boolean isRegexChar(char c) {
      return REG_CHARS.indexOf(c) != -1;
    }

    public static boolean isRegexChar(int c) {
      return REG_CHARS.indexOf(c) != -1;
    }

    @Override
    public String getExpress() {
      return express;
    }

    public Pattern getPattern() {
      return pattern;
    }

    @Override
    public String getPlainParent(String pathSeparator) {
      return PathUtils.resolvePlainParent(REG_CHARS, pathSeparator, express);
    }

    public boolean isIgnoreCase() {
      return ignoreCase;
    }

    @Override
    public boolean test(String t) {
      return pattern.matcher(t).matches();
    }

  }
}

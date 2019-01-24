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
package org.corant.suites.security.jaxrs;

import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.corant.shared.util.PathUtils.GlobPatterns;

/**
 * corant-suites-security-jaxrs
 *
 * @author bingo 下午6:25:46
 *
 */
@FunctionalInterface
public interface PathMatcher {

  boolean match(String path);

  static class CompletePathMatcher implements PathMatcher {
    final Set<String> compareds = new HashSet<>();
    final boolean ignoreCase;

    public CompletePathMatcher(boolean ignoreCase) {
      super();
      this.ignoreCase = ignoreCase;
    }

    public static CompletePathMatcher of(boolean ignoreCase, Set<String> compareds) {
      CompletePathMatcher inst = new CompletePathMatcher(ignoreCase);
      inst.compareds.addAll(compareds);
      return inst;
    }

    public void addCompareds(String... strings) {
      for (String s : strings) {
        if (isNotBlank(s)) {
          if (ignoreCase) {
            compareds.add(s.toLowerCase(Locale.ROOT));
          } else {
            compareds.add(s);
          }
        }
      }
    }

    @Override
    public boolean match(String path) {
      if (compareds.isEmpty()) {
        return false;
      }
      return path != null && ignoreCase ? compareds.contains(path.toLowerCase(Locale.ROOT))
          : compareds.contains(path);
    }

    public void removeCompareds(String... strings) {
      for (String s : strings) {
        if (ignoreCase) {
          compareds.remove(s.toLowerCase(Locale.ROOT));
        } else {
          compareds.remove(s);
        }
      }
    }
  }

  static class GlobPathMatcher implements PathMatcher {
    final Map<String, Pattern> patterns = new HashMap<>();
    final boolean ignoreCase;

    public GlobPathMatcher(boolean ignoreCase) {
      this.ignoreCase = ignoreCase;
    }

    public static GlobPathMatcher of(boolean ignoreCase, Set<String> globExpresses) {
      GlobPathMatcher inst = new GlobPathMatcher(ignoreCase);
      for (String wildcardExpress : globExpresses) {
        inst.addExpresses(wildcardExpress);
      }
      return inst;
    }

    public void addExpresses(String... strings) {
      for (String s : strings) {
        if (isNotBlank(s)) {
          patterns.put(s, GlobPatterns.build(s, ignoreCase));
        }
      }
    }

    @Override
    public boolean match(String path) {
      if (path == null) {
        return false;
      }
      for (Pattern m : patterns.values()) {
        if (m.matcher(path).matches()) {
          return true;
        }
      }
      return false;
    }

    public void removeExpresses(String... strings) {
      for (String s : strings) {
        patterns.remove(s);
      }
    }

  }

  static class RegexPathMatcher implements PathMatcher {
    final Map<String, Pattern> patterns = new HashMap<>();
    final boolean ignoreCase;

    public RegexPathMatcher(boolean ignoreCase) {
      this.ignoreCase = ignoreCase;
    }

    public static RegexPathMatcher of(boolean ignoreCase, Set<String> regexExpresses) {
      RegexPathMatcher inst = new RegexPathMatcher(ignoreCase);
      for (String wildcardExpress : regexExpresses) {
        inst.addExpresses(wildcardExpress);
      }
      return inst;
    }

    public void addExpresses(String... strings) {
      for (String s : strings) {
        if (isNotBlank(s)) {
          if (ignoreCase) {
            patterns.put(s, Pattern.compile(s));
          } else {
            patterns.put(s, Pattern.compile(s, Pattern.CASE_INSENSITIVE));
          }
        }
      }
    }

    @Override
    public boolean match(String path) {
      if (path == null) {
        return false;
      }
      for (Pattern m : patterns.values()) {
        if (m.matcher(path).matches()) {
          return true;
        }
      }
      return false;
    }

    public void removeExpresses(String... strings) {
      for (String s : strings) {
        patterns.remove(s);
      }
    }
  }

}

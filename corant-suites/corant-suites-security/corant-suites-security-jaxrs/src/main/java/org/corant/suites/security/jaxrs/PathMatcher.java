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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.corant.shared.util.StringUtils.WildcardMatcher;

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

    /**
     * @param ignoreCase
     */
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

  static class WildcardPathMatcher implements PathMatcher {

    final Set<WildcardMatcher> matchers = new HashSet<>();
    final boolean ignoreCase;

    /**
     * @param ignoreCase
     */
    public WildcardPathMatcher(boolean ignoreCase) {
      this.ignoreCase = ignoreCase;
    }

    public static WildcardPathMatcher of(boolean ignoreCase, Set<String> wildcardExpresses) {
      WildcardPathMatcher inst = new WildcardPathMatcher(ignoreCase);
      for (String wildcardExpress : wildcardExpresses) {
        inst.addExpresses(wildcardExpress);
      }
      return inst;
    }

    public void addExpresses(String... strings) {
      for (String s : strings) {
        if (isNotBlank(s)) {
          matchers.add(WildcardMatcher.of(ignoreCase, s));
        }
      }
    }

    @Override
    public boolean match(String path) {
      if (path == null) {
        return false;
      }
      for (WildcardMatcher m : matchers) {
        if (m.test(path)) {
          return true;
        }
      }
      return false;
    }

    public void removeExpresses(String... strings) {
      for (String s : strings) {
        matchers.remove(WildcardMatcher.of(ignoreCase, s));
      }
    }

  }

}

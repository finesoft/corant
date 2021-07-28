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
package org.corant.modules.security.shared.util;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Strings.strip;
import java.util.function.Predicate;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings.WildcardMatcher;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:57:31
 *
 */
public class StringPredicates {

  public static Predicate<String> predicateOf(String t) {
    String use = strip(t);
    if (isEmpty(use)) {
      return s -> true;
    } else if (WildcardMatcher.hasWildcard(use)) {
      return WildcardMatcher.of(false, use);
    } else {
      return s -> Objects.areEqual(s, use);
    }
  }
}

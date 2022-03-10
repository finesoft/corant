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
package org.corant.modules.security.shared;

import static org.corant.shared.util.Objects.areEqual;
import java.util.function.Predicate;
import org.corant.shared.util.Strings.WildcardMatcher;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午11:56:47
 *
 */
public abstract class Implication {

  protected transient Predicate<Object> predicate;

  protected Implication() {
    predicate = t -> true;
  }

  protected Implication(Object predicatable) {
    predicate = predicateOf(predicatable);
  }

  public static Predicate<Object> predicateOf(Object t) {
    if (t == null) {
      return s -> true;
    } else if (t instanceof String) {
      String sut = ((String) t).strip();
      if (WildcardMatcher.hasWildcard(sut)) {
        return s -> {
          final WildcardMatcher wm = WildcardMatcher.of(true, sut);
          return wm.test(sut);
        };
      }
    }
    return s -> areEqual(s, t);
  }
}

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
import static org.corant.shared.util.Strings.asDefaultString;
import java.util.function.Predicate;
import org.corant.shared.util.Functions;
import org.corant.shared.util.Strings.WildcardMatcher;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午11:56:47
 *
 */
public abstract class Predication {

  protected transient Predicate<Object> predicate;

  protected Predication() {}

  protected Predication(Object predicatable) {
    predicate = predicateOf(predicatable);
  }

  public static Predicate<Object> predicateOf(Object t) {
    if (t == null) {
      return Functions.emptyPredicate(true);
    } else if (t instanceof String) {
      final String sut = ((String) t).strip();
      if (WildcardMatcher.hasWildcard(sut)) {
        final WildcardMatcher wm = WildcardMatcher.of(true, sut);
        return s -> wm.test(asDefaultString(s));
      } else {
        return s -> sut.equalsIgnoreCase(asDefaultString(s));
      }
    }
    return s -> areEqual(s, t);
  }

  protected boolean test(Object predicatable) {
    if (predicate == null) {
      return false;
    }
    return predicate.test(predicatable);
  }
}

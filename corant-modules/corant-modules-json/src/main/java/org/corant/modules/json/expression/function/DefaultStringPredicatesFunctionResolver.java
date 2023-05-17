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
package org.corant.modules.json.expression.function;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Sets.immutableSetOf;
import java.util.Set;
import java.util.function.Function;
import org.corant.modules.json.expression.FunctionResolver;
import org.corant.shared.util.Strings;

/**
 * corant-modules-json
 *
 * @author bingo 下午3:29:41
 *
 */
public class DefaultStringPredicatesFunctionResolver implements FunctionResolver {

  public static final Set<String> SIGNS = immutableSetOf(
      "isBlank,isEmpty,contains,matchWildcard,length,trim,substring,indexOf,lastIndexOf");

  @Override
  public Function<Object[], Object> resolve(String name) {
    return fs -> {
      shouldBeTrue(fs.length >= 1);
      String object = fs[0] == null ? null : fs[0].toString();
      if ("isBlank".equals(name)) {
        return Strings.isBlank(object);
      } else if ("length".equals(name)) {
        return object == null ? 0 : object.length();
      } else if ("trim".equals(name)) {
        return Strings.trim(object);
      } else if ("isEmpty".equals(name)) {
        return object == null || object.isEmpty();
      } else if ("substring".equals(name)) {
        shouldBeTrue(fs.length >= 2 && fs[1] != null);
        if (fs.length > 2) {
          return object.substring(Integer.parseInt(fs[1].toString()),
              Integer.parseInt(fs[2].toString()));
        } else {
          return object.substring(Integer.parseInt(fs[1].toString()));
        }
      } else if ("indexOf".equals(name)) {
        shouldBeTrue(fs.length >= 2 && fs[1] != null);
        if (fs.length > 2) {
          return object.indexOf(fs[1].toString(), Integer.parseInt(fs[2].toString()));
        }
        return object.indexOf(fs[1].toString());
      } else if ("lastIndexOf".equals(name)) {
        shouldBeTrue(fs.length >= 2 && fs[1] != null);
        if (fs.length > 2) {
          return object.lastIndexOf(fs[1].toString(), Integer.parseInt(fs[2].toString()));
        }
        return object.lastIndexOf(fs[1].toString());
      } else if ("contains".equals(name)) {
        shouldBeTrue(fs.length >= 2 && fs[1] != null);
        return Strings.contains(object, fs[1].toString());
      } else if ("matchWildcard".equals(name)) {
        shouldBeTrue(fs.length >= 2 && fs[1] != null);
        return Strings.matchWildcard(object, true, fs[1].toString());
      } else {
        throw new IllegalArgumentException("Unsupported string predicates function: " + name);
      }
    };

  }

  @Override
  public boolean supports(String name) {
    return SIGNS.contains(name);
  }

}

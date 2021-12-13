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
package org.corant.modules.json.expression.predicate.function;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Conversions.toInstant;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import org.corant.modules.json.expression.predicate.FunctionResolver;

/**
 * corant-modules-json
 *
 * @author bingo 下午3:29:37
 *
 */
public class DefaultDateFunctionResolver implements FunctionResolver {

  @Override
  public Function<Object[], Object> resolve(String name) {
    return fs -> {
      shouldBeTrue(fs.length == 3);
      ChronoUnit unit = ChronoUnit.valueOf(fs[0].toString());
      Instant left = toInstant(fs[1]);
      Instant right = toInstant(fs[2]);
      return left.until(right, unit);
    };
  }

  @Override
  public boolean supports(String name) {
    return name != null && name.equalsIgnoreCase("datediff");
  }

}

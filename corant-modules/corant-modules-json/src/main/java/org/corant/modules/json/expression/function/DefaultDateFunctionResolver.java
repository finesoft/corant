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
import static org.corant.shared.util.Conversions.toInstant;
import static org.corant.shared.util.Maps.mapOf;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import org.corant.modules.json.expression.FunctionResolver;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-modules-json
 *
 * @author bingo 下午3:29:37
 *
 */
public class DefaultDateFunctionResolver implements FunctionResolver {

  public static final String SIGN = "datediff";

  @Override
  public Function<Object[], Object> resolve(String name) {
    return fs -> {
      // FIXME Unfinished yet~
      shouldBeTrue(fs.length > 2);
      ChronoUnit unit = ChronoUnit.valueOf(fs[0].toString());
      ZoneId zoneId = fs.length > 3 ? ZoneId.of(fs[3].toString()) : ZoneId.systemDefault();
      Map<String, Object> hint = mapOf(ConverterHints.CVT_ZONE_ID_KEY, zoneId);
      Instant left = toInstant(fs[1], hint);
      Instant right = toInstant(fs[2], hint);
      return left.until(right, unit);
    };
  }

  @Override
  public boolean supports(String name) {
    return SIGN.equalsIgnoreCase(name);
  }

}

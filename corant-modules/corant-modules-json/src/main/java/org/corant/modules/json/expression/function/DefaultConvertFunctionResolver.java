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
import static org.corant.shared.util.Classes.asClass;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Maps.mapOf;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import org.corant.modules.json.expression.FunctionResolver;

/**
 * corant-modules-json
 *
 * @author bingo 下午3:29:41
 *
 */
public class DefaultConvertFunctionResolver implements FunctionResolver {

  public static final String SIGN = "convert";

  @Override
  public Function<Object[], Object> resolve(String name) {
    return fs -> {
      shouldBeTrue(fs.length > 1);
      Object object = fs[0];
      Class<?> targetClass = asClass(fs[1].toString());
      Map<String, ?> hints = null;
      if (fs.length > 2) {
        hints = mapOf(Arrays.copyOfRange(fs, 2, fs.length));
      }
      return toObject(object, targetClass, hints);
    };
  }

  @Override
  public boolean supports(String name) {
    return SIGN.equalsIgnoreCase(name);
  }

}

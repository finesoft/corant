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
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.corant.modules.json.expression.FunctionResolver;
import org.corant.shared.ubiquity.Tuple;
import org.corant.shared.util.Methods;

/**
 * corant-modules-json
 *
 * @author bingo 下午3:29:41
 */
public class DefaultSizeOfFunctionResolver implements FunctionResolver {

  public static final String SIGN = "sizeOf";

  @Override
  public Function<Object[], Object> resolve(String name) {
    return fs -> {
      shouldBeTrue(fs.length == 1);
      Object object = fs[0];
      if (object == null) {
        return 0;
      } else if (object instanceof Collection<?>) {
        return ((Collection<?>) object).size();
      } else if (object instanceof Map<?, ?>) {
        return ((Map<?, ?>) object).size();
      } else if (object instanceof Object[]) {
        return ((Object[]) object).length;
      } else if (object instanceof CharSequence) {
        return ((CharSequence) object).length();
      } else if (object instanceof Tuple) {
        return ((Tuple) object).toArray().length;
      } else {
        if (object.getClass().isArray()) {
          return Array.getLength(object);
        } else {
          Method m = Methods.getMatchingMethod(object.getClass(), "size");
          if (m != null && Modifier.isPublic(m.getModifiers())
              && (m.getReturnType().equals(Integer.TYPE)
                  || m.getReturnType().equals(Integer.class))) {
            try {
              return m.invoke(object);
            } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
              throw new IllegalArgumentException(e);
            }
          }
        }
        throw new IllegalArgumentException(
            "Unsupported object type: " + object.getClass().getName());
      }
    };

  }

  @Override
  public boolean supports(String name) {
    return SIGN.equals(name);
  }

}

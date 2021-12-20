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
import static org.corant.shared.util.Conversions.toDouble;
import static org.corant.shared.util.Conversions.toLong;
import java.util.Arrays;
import java.util.function.Function;
import org.corant.modules.json.expression.FunctionResolver;

/**
 * corant-modules-json
 *
 * @author bingo 下午3:29:47
 *
 */
public class DefaultMathFunctionResolver implements FunctionResolver {

  public static final String SIGN_ADD = "add";
  public static final String SIGN_SUB = "sub";
  public static final String SIGN_MUL = "mul";
  public static final String SIGN_DIV = "div";
  public static final String SIGN_MOD = "mod";

  public double[] convertDouble(Object[] factors) {
    double[] doubleFactors = new double[factors.length];
    for (int i = 0; i < factors.length; i++) {
      doubleFactors[i] = toDouble(factors[i]);
    }
    return doubleFactors;
  }

  public long[] convertLong(Object[] factors) {
    long[] longFactors = new long[factors.length];
    for (int i = 0; i < factors.length; i++) {
      longFactors[i] = toLong(factors[i]);
    }
    return longFactors;
  }

  @Override
  public Function<Object[], Object> resolve(String name) {
    final String operator = name.toLowerCase();
    return fs -> {
      shouldBeTrue(fs.length > 1 && Arrays.stream(fs).allMatch(f -> f instanceof Number));
      if (Arrays.stream(fs).anyMatch(p -> p instanceof Float || p instanceof Double)) {
        double[] dfs = convertDouble(fs);
        double result = dfs[0];
        switch (operator) {
          case SIGN_ADD:
            for (int i = 1; i < dfs.length; i++) {
              result += dfs[i];
            }
            break;
          case SIGN_SUB:
            for (int i = 1; i < dfs.length; i++) {
              result -= dfs[i];
            }
            break;
          case SIGN_MUL:
            for (int i = 1; i < dfs.length; i++) {
              result *= dfs[i];
            }
            break;
          case SIGN_MOD:
            for (int i = 1; i < dfs.length; i++) {
              result %= dfs[i];
            }
            break;
          default:
            for (int i = 1; i < dfs.length; i++) {
              result /= dfs[i];
            }
            break;
        }
        return result;
      } else {
        long[] lfs = convertLong(fs);
        long result = lfs[0];
        switch (operator) {
          case SIGN_ADD:
            for (int i = 1; i < lfs.length; i++) {
              result += lfs[i];
            }
            break;
          case SIGN_SUB:
            for (int i = 1; i < lfs.length; i++) {
              result -= lfs[i];
            }
            break;
          case SIGN_MUL:
            for (int i = 1; i < lfs.length; i++) {
              result *= lfs[i];
            }
            break;
          case SIGN_MOD:
            for (int i = 1; i < lfs.length; i++) {
              result %= lfs[i];
            }
            break;
          default:
            for (int i = 1; i < lfs.length; i++) {
              result /= lfs[i];
            }
            break;
        }
        return result;
      }
    };
  }

  @Override
  public boolean supports(String name) {
    return SIGN_ADD.equalsIgnoreCase(name) || SIGN_SUB.equalsIgnoreCase(name)
        || SIGN_MUL.equalsIgnoreCase(name) || SIGN_DIV.equalsIgnoreCase(name)
        || SIGN_MOD.equalsIgnoreCase(name);
  }
}

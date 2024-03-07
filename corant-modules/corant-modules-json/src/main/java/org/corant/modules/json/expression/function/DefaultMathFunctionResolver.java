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
import java.util.Arrays;
import java.util.function.Function;
import org.corant.modules.json.expression.FunctionResolver;

/**
 * corant-modules-json
 *
 * @author bingo 下午3:29:47
 */
public class DefaultMathFunctionResolver implements FunctionResolver {

  public static final String SIGN_ADD = "add";
  public static final String SIGN_SUB = "sub";
  public static final String SIGN_MUL = "mul";
  public static final String SIGN_DIV = "div";
  public static final String SIGN_MOD = "mod";

  @Override
  public Function<Object[], Object> resolve(String name) {
    return fs -> {
      shouldBeTrue(fs.length > 1);
      Number[] numbers = new Number[fs.length];
      Arrays.setAll(numbers, i -> (Number) fs[i]);
      return calculate(numbers, name);
    };
  }

  @Override
  public boolean supports(String name) {
    return SIGN_ADD.equals(name) || SIGN_SUB.equals(name) || SIGN_MUL.equals(name)
        || SIGN_DIV.equals(name) || SIGN_MOD.equals(name);
  }

  protected Object calculate(Number[] factors, String operator) {
    byte sign = 0; // 00000000
    for (Number factor : factors) {
      if (factor instanceof Float) {
        sign |= 2;
      } else if (factor instanceof Long) {
        sign |= 4;
      } else if (factor instanceof Integer || factor instanceof Short || factor instanceof Byte) {
        sign |= 8;
      } else {
        sign |= 1;
        break;
      }
    }
    if ((sign & 1) != 0) {
      double result = factors[0].doubleValue();
      switch (operator) {
        case SIGN_ADD:
          for (int i = 1; i < factors.length; i++) {
            result += factors[i].doubleValue();
          }
          break;
        case SIGN_SUB:
          for (int i = 1; i < factors.length; i++) {
            result -= factors[i].doubleValue();
          }
          break;
        case SIGN_MUL:
          for (int i = 1; i < factors.length; i++) {
            result *= factors[i].doubleValue();
          }
          break;
        case SIGN_MOD:
          for (int i = 1; i < factors.length; i++) {
            result %= factors[i].doubleValue();
          }
          break;
        default:
          for (int i = 1; i < factors.length; i++) {
            result /= factors[i].doubleValue();
          }
          break;
      }
      return result;
    } else if ((sign & 2) != 0) {
      float result = factors[0].floatValue();
      switch (operator) {
        case SIGN_ADD:
          for (int i = 1; i < factors.length; i++) {
            result += factors[i].floatValue();
          }
          break;
        case SIGN_SUB:
          for (int i = 1; i < factors.length; i++) {
            result -= factors[i].floatValue();
          }
          break;
        case SIGN_MUL:
          for (int i = 1; i < factors.length; i++) {
            result *= factors[i].floatValue();
          }
          break;
        case SIGN_MOD:
          for (int i = 1; i < factors.length; i++) {
            result %= factors[i].floatValue();
          }
          break;
        default:
          for (int i = 1; i < factors.length; i++) {
            result /= factors[i].floatValue();
          }
          break;
      }
      return result;
    } else if ((sign & 4) != 0) {
      long result = factors[0].longValue();
      switch (operator) {
        case SIGN_ADD:
          for (int i = 1; i < factors.length; i++) {
            result += factors[i].longValue();
          }
          break;
        case SIGN_SUB:
          for (int i = 1; i < factors.length; i++) {
            result -= factors[i].longValue();
          }
          break;
        case SIGN_MUL:
          for (int i = 1; i < factors.length; i++) {
            result *= factors[i].longValue();
          }
          break;
        case SIGN_MOD:
          for (int i = 1; i < factors.length; i++) {
            result %= factors[i].longValue();
          }
          break;
        default:
          for (int i = 1; i < factors.length; i++) {
            result /= factors[i].longValue();
          }
          break;
      }
      return result;
    } else {
      int result = factors[0].intValue();
      switch (operator) {
        case SIGN_ADD:
          for (int i = 1; i < factors.length; i++) {
            result += factors[i].intValue();
          }
          break;
        case SIGN_SUB:
          for (int i = 1; i < factors.length; i++) {
            result -= factors[i].intValue();
          }
          break;
        case SIGN_MUL:
          for (int i = 1; i < factors.length; i++) {
            result *= factors[i].intValue();
          }
          break;
        case SIGN_MOD:
          for (int i = 1; i < factors.length; i++) {
            result %= factors[i].intValue();
          }
          break;
        default:
          for (int i = 1; i < factors.length; i++) {
            result /= factors[i].intValue();
          }
          break;
      }
      return result;
    }
  }
}

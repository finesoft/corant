/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.util;

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.util.Random;

/**
 * corant-shared
 *
 * @author bingo 下午8:39:13
 *
 */
public class RandomUtils {
  private static final Random RANDOM = new Random();
  public static final String NUMBERS_AND_LETTERS =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String NUMBERS = "0123456789";
  public static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String UPPER_CASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  public static final String LOWER_CASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";

  public static boolean randomBoolean() {
    return RANDOM.nextBoolean();
  }

  public static byte[] randomBytes(final int count) {
    shouldBeTrue(count >= 0, "Count cannot be negative.");
    final byte[] result = new byte[count];
    RANDOM.nextBytes(result);
    return result;
  }

  public static double randomDouble(double max) {
    return randomDouble(0, max);
  }

  public static double randomDouble(double min, double max) {
    shouldBeFalse(min > max);
    if (min == max) {
      return min;
    }
    return min + (max - min) * RANDOM.nextDouble();
  }

  public static double randomFloat(float max) {
    return randomFloat(0, max);
  }

  public static float randomFloat(float min, float max) {
    shouldBeFalse(min > max);
    if (min == max) {
      return min;
    }
    return min + (max - min) * RANDOM.nextFloat();
  }

  public static int randomInt(int max) {
    return randomInt(0, max);
  }

  public static int randomInt(int min, int max) {
    shouldBeFalse(min > max);
    if (min == max) {
      return min;
    }
    return min + RANDOM.nextInt(max - min);
  }

  public static String randomLetters(int length) {
    return randomString(LETTERS, length);
  }

  public static long randomLong(long max) {
    return randomLong(0L, max);
  }

  public static long randomLong(long min, long max) {
    return (long) randomDouble(min, max);
  }

  public static String randomLowerCaseLetters(int length) {
    return randomString(LOWER_CASE_LETTERS, length);
  }

  public static String randomNumbers(int length) {
    return randomString(NUMBERS, length);
  }

  public static String randomNumbersAndLetters(int length) {
    return randomString(NUMBERS_AND_LETTERS, length);
  }

  public static String randomString(char[] sourceChar, int length) {
    if (sourceChar == null || sourceChar.length == 0 || length < 0) {
      return null;
    }
    StringBuilder str = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      str.append(sourceChar[RANDOM.nextInt(sourceChar.length)]);
    }
    return str.toString();
  }

  public static String randomString(String source, int length) {
    return source == null ? null : randomString(source.toCharArray(), length);
  }

  public static String randomUpperCaseLetters(int length) {
    return randomString(UPPER_CASE_LETTERS, length);
  }
}

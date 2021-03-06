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
import static org.corant.shared.util.Strings.LETTERS;
import static org.corant.shared.util.Strings.LOWER_CASE_LETTERS;
import static org.corant.shared.util.Strings.NUMBERS;
import static org.corant.shared.util.Strings.NUMBERS_AND_LETTERS;
import static org.corant.shared.util.Strings.NUMBERS_AND_LOWER_CASE_LETTERS;
import static org.corant.shared.util.Strings.NUMBERS_AND_UPPER_CASE_LETTERS;
import static org.corant.shared.util.Strings.UPPER_CASE_LETTERS;
import java.security.SecureRandom;
import java.util.Random;

/**
 * corant-shared
 *
 * @author bingo 下午8:39:13
 *
 */
public class Randoms {

  public static final SecureRandom SECURITY_RANDOM = new SecureRandom();

  public static final Random RANDOM = new Random();

  public static boolean randomBoolean() {
    return RANDOM.nextBoolean();
  }

  public static byte[] randomBytes(final int count) {
    shouldBeTrue(count >= 0, "Count cannot be negative.");
    final byte[] result = new byte[count];
    RANDOM.nextBytes(result);
    return result;
  }

  public static char[] randomChars(final char[] sourceChar, final int length) {
    shouldBeTrue(sourceChar != null && sourceChar.length > 0 && length > 0);
    int sLen = sourceChar.length;
    char[] chars = new char[length];
    for (int i = 0; i < length; i++) {
      chars[i] = sourceChar[RANDOM.nextInt(sLen)];
    }
    return chars;
  }

  public static double randomDouble(final double max) {
    return randomDouble(0, max);
  }

  public static double randomDouble(final double min, final double max) {
    shouldBeFalse(min > max);
    return min == max ? min : min + (max - min) * RANDOM.nextDouble();
  }

  public static float randomFloat(final float max) {
    return randomFloat(0, max);
  }

  public static float randomFloat(final float min, final float max) {
    shouldBeFalse(min > max);
    return min == max ? min : min + (max - min) * RANDOM.nextFloat();
  }

  public static int randomInt(final int max) {
    return randomInt(0, max);
  }

  public static int randomInt(final int min, final int max) {
    shouldBeFalse(min > max);
    return min == max ? min : min + RANDOM.nextInt(max - min);
  }

  public static String randomLetters(final int length) {
    return randomString(LETTERS, length);
  }

  public static long randomLong(final long max) {
    return randomLong(0L, max);
  }

  public static long randomLong(final long min, final long max) {
    return (long) randomDouble(min, max);
  }

  public static String randomLowerCaseLetters(final int length) {
    return randomString(LOWER_CASE_LETTERS, length);
  }

  public static String randomNumbers(final int length) {
    return randomString(NUMBERS, length);
  }

  public static String randomNumbersAndLcLetters(final int length) {
    return randomString(NUMBERS_AND_LOWER_CASE_LETTERS, length);
  }

  public static String randomNumbersAndLetters(final int length) {
    return randomString(NUMBERS_AND_LETTERS, length);
  }

  public static String randomNumbersAndUcLetters(final int length) {
    return randomString(NUMBERS_AND_UPPER_CASE_LETTERS, length);
  }

  public static String randomString(final String source, final int length) {
    shouldBeTrue(source != null && source.length() > 0 && length > 0);
    StringBuilder sb = new StringBuilder(length);
    char[] sourceChar = source.toCharArray();
    int sLen = sourceChar.length;
    for (int i = 0; i < length; i++) {
      sb.append(sourceChar[RANDOM.nextInt(sLen)]);
    }
    return sb.toString();
  }

  public static String randomUpperCaseLetters(final int length) {
    return randomString(UPPER_CASE_LETTERS, length);
  }
}

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
import static org.corant.shared.util.Objects.defaultObject;
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
    return randomBytes(count, RANDOM);
  }

  public static byte[] randomBytes(final int count, final Random random) {
    shouldBeTrue(count >= 0, "Count cannot be negative.");
    final Random theRandom = defaultObject(random, RANDOM);
    final byte[] result = new byte[count];
    theRandom.nextBytes(result);
    return result;
  }

  public static char[] randomChars(final char[] sourceChar, final int length) {
    return randomChars(sourceChar, length, RANDOM);
  }

  public static char[] randomChars(final char[] sourceChar, final int length, final Random random) {
    shouldBeTrue(sourceChar != null && sourceChar.length > 0 && length > 0);
    final Random theRandom = defaultObject(random, RANDOM);
    final int sLen = sourceChar.length;
    final char[] chars = new char[length];
    for (int i = 0; i < length; i++) {
      chars[i] = sourceChar[theRandom.nextInt(sLen)];
    }
    return chars;
  }

  public static double randomDouble(final double max) {
    return randomDouble(max, RANDOM);
  }

  public static double randomDouble(final double min, final double max) {
    return randomDouble(min, max, RANDOM);
  }

  public static double randomDouble(final double min, final double max, final Random random) {
    shouldBeFalse(min > max);
    return min == max ? min : min + (max - min) * defaultObject(random, RANDOM).nextDouble();
  }

  public static double randomDouble(final double max, final Random random) {
    return randomDouble(0, max, random);
  }

  public static float randomFloat(final float max) {
    return randomFloat(max, RANDOM);
  }

  public static double randomFloat(final float min, final float max) {
    return randomFloat(min, max, RANDOM);
  }

  public static float randomFloat(final float min, final float max, final Random random) {
    shouldBeFalse(min > max);
    return min == max ? min : min + (max - min) * defaultObject(random, RANDOM).nextFloat();
  }

  public static float randomFloat(final float max, final Random random) {
    return randomFloat(0, max, random);
  }

  public static int randomInt(final int max) {
    return randomInt(max, RANDOM);
  }

  public static int randomInt(final int min, final int max) {
    return randomInt(min, max, RANDOM);
  }

  public static int randomInt(final int min, final int max, final Random random) {
    shouldBeFalse(min > max);
    return min == max ? min : min + defaultObject(random, RANDOM).nextInt(max - min);
  }

  public static int randomInt(final int max, final Random random) {
    return randomInt(0, max, random);
  }

  public static String randomLetters(final int length) {
    return randomString(LETTERS, length, RANDOM);
  }

  public static String randomLetters(final int length, final Random random) {
    return randomString(LETTERS, length, random);
  }

  public static long randomLong(final long max) {
    return randomLong(max, RANDOM);
  }

  public static long randomLong(final long min, final long max) {
    return randomLong(min, max, RANDOM);
  }

  public static long randomLong(final long min, final long max, final Random random) {
    return (long) randomDouble(min, max, random);
  }

  public static long randomLong(final long max, final Random random) {
    return randomLong(0L, max, random);
  }

  public static String randomLowerCaseLetters(final int length) {
    return randomLowerCaseLetters(length, RANDOM);
  }

  public static String randomLowerCaseLetters(final int length, final Random random) {
    return randomString(LOWER_CASE_LETTERS, length, random);
  }

  public static String randomNumbers(final int length) {
    return randomNumbers(length, RANDOM);
  }

  public static String randomNumbers(final int length, final Random random) {
    return randomString(NUMBERS, length, random);
  }

  public static String randomNumbersAndLcLetters(final int length) {
    return randomNumbersAndLcLetters(length, RANDOM);
  }

  public static String randomNumbersAndLcLetters(final int length, final Random random) {
    return randomString(NUMBERS_AND_LOWER_CASE_LETTERS, length, random);
  }

  public static String randomNumbersAndLetters(final int length) {
    return randomNumbersAndLetters(length, RANDOM);
  }

  public static String randomNumbersAndLetters(final int length, final Random random) {
    return randomString(NUMBERS_AND_LETTERS, length, random);
  }

  public static String randomNumbersAndUcLetters(final int length) {
    return randomNumbersAndUcLetters(length, RANDOM);
  }

  public static String randomNumbersAndUcLetters(final int length, final Random random) {
    return randomString(NUMBERS_AND_UPPER_CASE_LETTERS, length, random);
  }

  public static String randomString(final String source, final int length, final Random random) {
    shouldBeTrue(source != null && source.length() > 0 && length > 0);
    StringBuilder sb = new StringBuilder(length);
    final char[] sourceChar = source.toCharArray();
    final int sLen = sourceChar.length;
    final Random theRandom = defaultObject(random, RANDOM);
    for (int i = 0; i < length; i++) {
      sb.append(sourceChar[theRandom.nextInt(sLen)]);
    }
    return sb.toString();
  }

  public static String randomUpperCaseLetters(final int length) {
    return randomUpperCaseLetters(length, RANDOM);
  }

  public static String randomUpperCaseLetters(final int length, final Random random) {
    return randomString(UPPER_CASE_LETTERS, length, random);
  }
}

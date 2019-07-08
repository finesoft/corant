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

  public static String random(char[] sourceChar, int length) {
    if (sourceChar == null || sourceChar.length == 0 || length < 0) {
      return null;
    }
    StringBuilder str = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      str.append(sourceChar[RANDOM.nextInt(sourceChar.length)]);
    }
    return str.toString();
  }

  public static int random(int max) {
    return random(0, max);
  }

  public static int random(int min, int max) {
    if (min > max) {
      return 0;
    }
    if (min == max) {
      return min;
    }
    return min + RANDOM.nextInt(max - min);
  }

  public static String random(String source, int length) {
    return source == null ? null : random(source.toCharArray(), length);
  }

  public static String randomUpperCaseLetters(int length) {
    return random(UPPER_CASE_LETTERS, length);
  }

  public static String randomLetters(int length) {
    return random(LETTERS, length);
  }

  public static String randomLowerCaseLetters(int length) {
    return random(LOWER_CASE_LETTERS, length);
  }

  public static String randomNumbers(int length) {
    return random(NUMBERS, length);
  }

  public static String randomNumbersAndLetters(int length) {
    return random(NUMBERS_AND_LETTERS, length);
  }
}

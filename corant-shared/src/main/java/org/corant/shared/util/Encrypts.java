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

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Bytes.BitArray;

/**
 * corant-shared
 *
 * @author bingo 上午12:29:41
 */
public class Encrypts {

  private static final char[] R62_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
      'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
      'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
      'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

  private static final int R62_MAX_LEN = R62_DIGITS.length;

  private Encrypts() {}

  /**
   * 从字母组合转为正整数，A->1，B->2...Z-26，AA-27，AB-28....
   *
   * @param alphabet
   * @return
   */
  public static int alphabetToIntScale(String alphabet) {
    if (alphabet == null) {
      throw new CorantRuntimeException("Param can't null!");
    }
    if (!alphabet.chars().allMatch(x -> x >= 0x41 && x <= 0x5a || x >= 0x61 && x <= 0x7a)) {
      throw new CorantRuntimeException("Param must be alphabet!");
    }
    int[] idx = {0};
    return new StringBuilder(alphabet.trim().toUpperCase(Locale.ENGLISH)).reverse().chars()
        .map(i -> (i - 64) * (int) Math.pow(26, idx[0]++)).reduce(-1, Integer::sum) + 1;
  }

  /**
   * 将1010101010字符转为byte数组
   *
   * @param s
   * @return
   */
  public static byte[] binaryStringToBytes(String s) {
    if (s == null) {
      return Bytes.EMPTY_ARRAY;
    }
    int length = s.length();
    if ((length & 7) != 0) {
      throw new CorantRuntimeException("Expecting 8 bit values to construct a byte[]");
    }
    BitArray ba = Bytes.asBitArray(length, false);
    for (int i = 0; i < length; i++) {
      ba.setBit(i, s.charAt(i) == '1');
    }
    return ba.getBytes();
  }

  /**
   * 将十六进制字符串转换成byte数组
   *
   * @param hs
   * @return
   */
  public static byte[] hexStringToBytes(String hs) {
    if (hs.length() < 1) {
      return Bytes.EMPTY_ARRAY;
    }
    byte[] result = new byte[hs.length() / 2];
    for (int i = 0; i < hs.length() / 2; i++) {
      int high = Integer.parseInt(hs.substring(i * 2, i * 2 + 1), 16);
      int low = Integer.parseInt(hs.substring(i * 2 + 1, i * 2 + 2), 16);
      result[i] = (byte) (high * 16 + low);
    }
    return result;
  }

  /**
   * 正整数转为26进制的大写字母组合，1->A，2->B...26->Z，27->AA...
   *
   * @param number
   * @return
   */
  public static String intToAlphabetScale(int number) {
    int num = number;
    if (num <= 0) {
      throw new CorantRuntimeException("Param must greater than 0!");
    }
    StringBuilder sb = new StringBuilder();
    while (num > 0) {
      int r = num % 26;
      if (r == 0) {
        r = 26;
      }
      sb.append((char) (r + 64));
      num = (num - r) / 26;
    }
    return sb.reverse().toString();
  }

  /**
   * 将62进制字符串转为long型
   *
   * @param r62String
   * @return
   */
  public static long r62StringToLong(String r62String) {
    String r62str = r62String;
    long result = 0;
    boolean negative = false;
    if (r62str.indexOf('-') == 0) {
      negative = true;
      r62str = r62str.substring(1);
    }
    for (int index = 0; index < r62str.length(); index++) {
      char c = r62str.charAt(index);
      int tableIndex = 0;
      for (int i = 0; i < R62_MAX_LEN; i++) {
        if (R62_DIGITS[i] == c) {
          tableIndex = i;
          break;
        }
      }
      result += tableIndex * (long) Math.pow(R62_MAX_LEN, r62str.length() - index - 1d);
    }

    return negative ? result * -1 : result;
  }

  /**
   * 将2~36进制字符串转为long
   *
   * @param s 进制字符串
   * @param radix 进制
   * @return
   */
  public static long radixStringToLong(String s, int radix) {
    return new BigInteger(s, radix).longValue();
  }

  /**
   * 把字节转为字节字符串例如 00101010
   *
   * @param b
   * @return
   */
  public static String toBinaryString(byte b) {
    String formatted = Integer.toBinaryString(b);
    if (formatted.length() > 8) {
      formatted = formatted.substring(formatted.length() - 8);
    }
    StringBuilder buf = new StringBuilder("0".repeat(8));
    buf.replace(8 - formatted.length(), 8, formatted);
    return buf.toString();
  }

  /**
   * 把byte转为比特流字符串
   *
   * @param b
   * @return
   */
  public static String toBinaryString(byte[] b) {
    BitArray ba = new BitArray(b);
    StringBuilder buf = new StringBuilder();
    int len = b.length * 8;
    for (int i = 0; i < len; i++) {
      buf.append(ba.getBit(i) ? "1" : "0");
    }
    return buf.toString();
  }

  /**
   * 把int转为字节字符串例如 00101010
   *
   * @param i
   * @return
   */
  public static String toBinaryString(int i) {
    String formatted = Integer.toBinaryString(i);
    StringBuilder buf = new StringBuilder("0".repeat(32));
    buf.replace(32 - formatted.length(), 32, formatted);
    return buf.toString();
  }

  /**
   * 把long转为字节字符串例如 00101010
   *
   * @param l
   * @return
   */
  public static String toBinaryString(long l) {
    String formatted = Long.toBinaryString(l);
    StringBuilder buf = new StringBuilder("0".repeat(64));
    buf.replace(64 - formatted.length(), 64, formatted);
    return buf.toString();
  }

  /**
   * 将short转为字节字符串
   *
   * @param i
   * @return
   */
  public static String toBinaryString(short i) {
    return toBinaryString((int) i).substring(16);
  }

  /**
   * 将byte数组转换成十六进制字符串
   *
   * @param b 待转换的字节数组
   * @return
   */
  public static String toHexString(byte[] b) {
    StringBuilder hs = new StringBuilder();
    String stmp = "";
    for (byte element : b) {
      stmp = Integer.toHexString(element & 0xFF);
      if (stmp.length() == 1) {
        hs.append("0");
      }
      hs.append(stmp);
    }
    return hs.toString().toUpperCase(Locale.ENGLISH);
  }

  /**
   * 将字符串以指定的编码转为字节再转成16进制字符串
   *
   * @param s 字符串
   * @param charset 获取字符串字节时指定的字符集
   * @return
   */
  public static String toHexString(String s, Charset charset) {
    if (s == null) {
      return null;
    }
    return charset == null ? toHexString(s.getBytes(StandardCharsets.UTF_8))
        : toHexString(s.getBytes(charset));
  }

  /**
   * 转为62进制字符串
   *
   * @param r62Number
   * @return
   */
  public static String toR62String(long r62Number) {
    long r62n = r62Number;
    StringBuilder tmp = new StringBuilder();
    StringBuilder result = new StringBuilder();
    boolean negative = false;
    if (r62n == 0) {
      return Character.toString(R62_DIGITS[0]);
    } else if (r62n < 0) {
      negative = true;
      r62n = -r62n;
    }
    while (r62n > 0) {
      tmp.append(R62_DIGITS[(int) (r62n % R62_MAX_LEN)]);
      r62n = r62n / R62_MAX_LEN;
    }
    String str = tmp.toString();
    for (int i = str.length() - 1; i >= 0; i--) {
      result.append(str.charAt(i));
    }
    return negative ? '-' + result.toString() : result.toString();

  }

  /**
   * 将整形转为2~36进制的字符串
   *
   * @param n 数字
   * @param radix 进制
   * @return
   */
  public static String toRadixString(long n, int radix) {
    return BigInteger.valueOf(n).toString(radix);
  }

}

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
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Bytes.BitArray;

/**
 * @author bingo 上午12:29:41
 */
public class Encrypts {

  public static final String DFLT_CHARSET_NAME = "utf-8";
  public static final Charset DFLT_CHARSET = Charset.forName(DFLT_CHARSET_NAME);

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
   * AES解密，使用默认字符集对key进行字节获取，返回默认字符集的原文字符串
   *
   * @param aesB64Str b64密文字符串
   * @param key 密钥字符串
   * @return 默认字符集的原文字符串
   * @throws GeneralSecurityException
   */
  public static String decodeAesB64StringToString(String aesB64Str, String key)
      throws GeneralSecurityException {
    return decodeAesBytesToString(decodeB64StringToBytes(aesB64Str), key.getBytes(DFLT_CHARSET),
        DFLT_CHARSET);
  }

  /**
   * AES解密，使用指定字符集对key进行字节获取，返回指定字符集的原文字符串
   *
   * @param aesB64Str b64密文字符串
   * @param key 密钥字符串
   * @param charset 指定字符集
   * @return 指定字符集的原文字符串
   * @throws GeneralSecurityException
   */
  public static String decodeAesB64StringToString(String aesB64Str, String key, Charset charset)
      throws GeneralSecurityException {
    return decodeAesBytesToString(decodeB64StringToBytes(aesB64Str), key.getBytes(charset),
        charset);
  }

  /**
   * AES解密
   *
   * @param aesBytes 密文字节数组
   * @param key 密钥字节数组
   * @return 原文字节数组
   * @throws GeneralSecurityException
   */
  public static byte[] decodeAesBytesToBytes(byte[] aesBytes, byte[] key)
      throws GeneralSecurityException {
    KeyGenerator kgen = KeyGenerator.getInstance("AES");
    kgen.init(128, new SecureRandom(key));
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
    return cipher.doFinal(aesBytes);
  }

  /**
   * AES解密字符串，使用指定的字符串返回原文字符串
   *
   * @param aesBytes 密文字节数组
   * @param key 密钥字节数组
   * @param charset 字符集
   * @return 指定字符集的原文字符串
   * @throws GeneralSecurityException
   */
  public static String decodeAesBytesToString(byte[] aesBytes, byte[] key, Charset charset)
      throws GeneralSecurityException {
    return new String(decodeAesBytesToBytes(aesBytes, key), charset);
  }

  /**
   * AES解密，使用默认字符集对key进行字节获取，返回默认字符集的原文字符串
   *
   * @param aesHexStr 16进制字符串密文
   * @param key 密钥字符串
   * @return 默认字符集的原文字符串
   * @throws NumberFormatException
   * @throws GeneralSecurityException
   */
  public static String decodeAesHexStringToString(String aesHexStr, String key)
      throws GeneralSecurityException {
    return decodeAesBytesToString(hexStringToBytes(aesHexStr), key.getBytes(DFLT_CHARSET),
        DFLT_CHARSET);
  }

  /**
   * AES解密，使用指定字符集对key进行字节获取，返回指定字符集的原文字符串
   *
   * @param aesHexStr 16进制字符串密文
   * @param key 密钥字符串
   * @param charset 指定字符集
   * @return 指定字符集的原文字符串
   * @throws NumberFormatException
   * @throws GeneralSecurityException
   */
  public static String decodeAesHexStringToString(String aesHexStr, String key, Charset charset)
      throws GeneralSecurityException {
    return decodeAesBytesToString(hexStringToBytes(aesHexStr), key.getBytes(charset), charset);
  }

  /**
   * 对base64字节数组进行解码
   *
   * @param bytes
   * @return
   */
  public static byte[] decodeB64BytesToBytes(byte[] bytes) {
    return Base64.getDecoder().decode(bytes);
  }

  /**
   * 从b64字节解码为字符串
   *
   * @param bytes
   * @return
   */
  public static String decodeB64BytesToString(byte[] bytes) {
    return decodeB64BytesToString(bytes, DFLT_CHARSET);
  }

  /**
   * 从B64字节解码为指定charset的字符串
   *
   * @param bytes
   * @param toCharset
   * @return
   */
  public static String decodeB64BytesToString(byte[] bytes, Charset toCharset) {
    return new String(decodeB64BytesToBytes(bytes), toCharset);
  }

  /**
   * 从B64字符串解码为字节数组
   *
   * @param b64Str
   * @return
   */
  public static byte[] decodeB64StringToBytes(String b64Str) {
    return decodeB64BytesToBytes(b64Str.getBytes(StandardCharsets.ISO_8859_1));
  }

  /**
   * 从B64字符串解码为指定字符集的字符串
   *
   * @param b64Str
   * @param toCharset
   * @return
   */
  public static String decodeB64StringToString(String b64Str, Charset toCharset) {
    return new String(decodeB64StringToBytes(b64Str), toCharset);
  }

  /**
   * AES加密
   *
   * @param content 待加密的内容字节数组
   * @param key 密钥字节数组
   * @return 加密过的字节数组
   * @throws GeneralSecurityException
   */
  public static byte[] encodeBytesToAesBytes(byte[] content, byte[] key)
      throws GeneralSecurityException {
    KeyGenerator kgen = KeyGenerator.getInstance("AES");
    kgen.init(128, new SecureRandom(key));
    Cipher cipher = Cipher.getInstance("AES");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kgen.generateKey().getEncoded(), "AES"));
    return cipher.doFinal(content);
  }

  /**
   * 二进制数据编码为BASE64字节
   *
   * @param bytes
   * @return
   * @throws Exception
   */
  public static byte[] encodeBytesToB64Bytes(byte[] bytes) {
    return Base64.getEncoder().encode(bytes);
  }

  /**
   * 字节编码成B64编码字符串
   *
   * @param bytes
   * @return
   */
  public static String encodeBytesToB64String(byte[] bytes) {
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * AES加密，使用默认的字符集对待加密内容和密钥获取字节数组，加密后返回密文为b64格式
   *
   * @param content 待加密的字符串
   * @param key 密钥字符串
   * @return b64格式的密文
   * @throws GeneralSecurityException
   */
  public static String encodeStringToAesB64String(String content, String key)
      throws GeneralSecurityException {
    return encodeBytesToB64String(encodeStringToAesBytes(content, key));
  }

  /**
   * AES加密 使用指定的字符集对待加密内容和密钥获取字节数组
   *
   * @param content 待加密的字符串
   * @param key 密钥字符串
   * @param charset 字符集
   * @return b64格式的密文
   * @throws GeneralSecurityException
   */
  public static String encodeStringToAesB64String(String content, String key, Charset charset)
      throws GeneralSecurityException {
    return encodeBytesToB64String(encodeStringToAesBytes(content, key, charset));
  }

  /**
   * AES加密，使用默认的字符集对待加密内容和密钥获取字节数组
   *
   * @param content 待加密的字符串
   * @param key 密钥字符串
   * @return 加密过的字节数组
   * @throws GeneralSecurityException
   */
  public static byte[] encodeStringToAesBytes(String content, String key)
      throws GeneralSecurityException {
    return encodeStringToAesBytes(content, key, DFLT_CHARSET);
  }

  /**
   * AES加密，使用指定字符集对待加密内容和密钥获取字节数组
   *
   * @param content 待加密的字符串
   * @param key 密钥字符串
   * @param charset 指定字符集
   * @return 密文字节数组
   * @throws GeneralSecurityException
   */
  public static byte[] encodeStringToAesBytes(String content, String key, Charset charset)
      throws GeneralSecurityException {
    return encodeBytesToAesBytes(content.getBytes(charset), key.getBytes(charset));
  }

  /**
   * AES加密 ，使用默认字符集对待加密内容和密钥获取字节数组，加密后返回密文为b64格式
   *
   * @param content 待加密的字符串
   * @param key 密钥字符串
   * @return hex格式密文
   * @throws GeneralSecurityException
   */
  public static String encodeStringToAesHexString(String content, String key)
      throws GeneralSecurityException {
    return toHexString(encodeStringToAesBytes(content, key));
  }

  /**
   * AES加密，使用指定的字符集对待加密内容和密钥获取字节数组，加密后返回密文为b64格式
   *
   * @param content 待加密的字符串
   * @param key 密钥字符串
   * @param charset 指定字符集
   * @return hex格式密文
   * @throws GeneralSecurityException
   */
  public static String encodeStringToAesHexString(String content, String key, Charset charset)
      throws GeneralSecurityException {
    return toHexString(encodeStringToAesBytes(content, key, charset));
  }

  /**
   * 把字符串编码成B64字符串，字符集为UTF-8
   *
   * @param str
   * @return
   */
  public static String encodeStringToB64String(String str) {
    return encodeStringToB64String(str, DFLT_CHARSET);
  }

  /**
   * 把字符串根据指定的字符集编码成B64码字符串
   *
   * @param str
   * @param fromCharset
   * @return
   */
  public static String encodeStringToB64String(String str, Charset fromCharset) {
    return new String(Base64.getEncoder().encode(str.getBytes(fromCharset)),
        StandardCharsets.ISO_8859_1);
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

  public static String repeat(char character, int times) {
    char[] buffer = new char[times];
    Arrays.fill(buffer, character);
    return new String(buffer);
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
    StringBuilder buf = new StringBuilder(repeat('0', 8));
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
    StringBuilder buf = new StringBuilder(repeat('0', 32));
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
    StringBuilder buf = new StringBuilder(repeat('0', 64));
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
    return charset == null ? toHexString(s.getBytes(DFLT_CHARSET))
        : toHexString(s.getBytes(charset));
  }

  /**
   * 获取MD5加密字串
   *
   * @param b 待转换的字节数组
   * @return
   * @throws NoSuchAlgorithmException
   */
  public static byte[] toMD5Bytes(byte[] b) throws NoSuchAlgorithmException {
    if (b == null) {
      return Bytes.EMPTY_ARRAY;
    }
    MessageDigest md = MessageDigest.getInstance("MD5");
    md.update(b);
    return md.digest();
  }

  /**
   * 获取MD5加密字串，16进制字符串
   *
   * @param s 待加密字符串
   * @return 加密后的16进制字符串
   * @throws NoSuchAlgorithmException
   */
  public static String toMD5HexString(String s) throws NoSuchAlgorithmException {
    if (s == null) {
      return null;
    }
    return toHexString(toMD5Bytes(s.getBytes(DFLT_CHARSET)));
  }

  /**
   * 获取MD5加密字串
   *
   * @param s 待加密字符串
   * @param cs 获取字符串字节时指定的字符集
   * @return 加密后的16进制字符串
   * @throws NoSuchAlgorithmException
   */
  public static String toMD5HexString(String s, Charset cs) throws NoSuchAlgorithmException {
    if (s == null) {
      return null;
    }
    return toHexString(toMD5Bytes(s.getBytes(cs == null ? DFLT_CHARSET : cs)));
  }

  /**
   * 把原始字符串和混淆字符串通过md5进行加密混淆
   *
   * @param s 待加密字符串
   * @param salt 盐
   * @return 加密后的16进制字符串
   * @throws NoSuchAlgorithmException
   */
  public static String toMD5HexStringWithSalt(String s, String salt)
      throws NoSuchAlgorithmException {
    return toMD5HexStringWithSalt(s, salt, DFLT_CHARSET);
  }

  /**
   * 把原始字符串和混淆字符串通过md5进行加密混淆
   *
   * @param s 待加密字符串
   * @param salt 盐
   * @param charset 指定字符集
   * @return
   * @throws NoSuchAlgorithmException
   */
  public static String toMD5HexStringWithSalt(String s, String salt, Charset charset)
      throws NoSuchAlgorithmException {
    if (s == null) {
      return null;
    }
    if (salt == null) {
      return toHexString(toMD5Bytes(s.getBytes(charset)));
    }
    String firstHash = toHexString(toMD5Bytes(s.getBytes(charset)));
    return toHexString(toMD5Bytes((firstHash + salt).getBytes(charset)));
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

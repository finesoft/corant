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

import static org.corant.shared.util.StringUtils.isNotBlank;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * @author bingo 2018年3月23日
 */
public class ValidateUtils {

  private ValidateUtils() {
    super();
  }

  /**
   * 判断字符串是否是HTTPURL
   *
   * @param httpUrl
   * @return
   */
  public static boolean isHttpUrl(String httpUrl) {
    return isNotBlank(httpUrl) && httpUrl.matches("[a-zA-z]+://[^\\s]*");
  }

  public static boolean isId(Long id) {
    return id != null && id > 0;
  }

  /**
   * 判断是否是图片
   *
   * @param is
   * @return
   */
  public static boolean isImage(InputStream is, String... extension) {
    if (is == null) {
      return false;
    }
    if (is.markSupported()) {
      is.mark(128);
    }
    try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {
      Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
      if (!iter.hasNext()) {
        return false;
      }
      if (extension != null && extension.length > 0) {
        ImageReader reader = iter.next();
        String formatName = reader.getFormatName();
        return Arrays.stream(extension).anyMatch(x -> x.equalsIgnoreCase(formatName));
      }
      return true;
    } catch (IOException ex) {
      throw new CorantRuntimeException(ex);
    } finally {
      if (is.markSupported()) {
        try {
          is.reset();
        } catch (IOException e) {
          throw new CorantRuntimeException(e);
        }
      }
    }
  }

  /**
   * 判断字符串是否为IP地址
   *
   * @param ipAddress
   * @return
   */
  public static boolean isIp4Address(String ipAddress) {
    return isNotBlank(ipAddress)
        && Pattern.compile("\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\"
            + ".((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\"
            + ".((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\"
            + ".((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b").matcher(ipAddress).matches();
  }

  /**
   * 判断字符串是否是mail地址
   *
   * @param mailAddress
   * @return
   */
  public static boolean isMailAddress(String mailAddress) {
    return isNotBlank(mailAddress)
        && Pattern.compile("^(\\w+)([\\-+.\\'][\\w]+)*@(\\w[\\-\\w]*\\.){1,5}([A-Za-z]){2,6}$")
            .matcher(mailAddress).matches();
  }

  /**
   * 判断字符串是否是中国的身份证
   *
   * @param idCardNumber
   * @return
   */
  public static boolean isZhIDCardNumber(String idCardNumber) {
    return isNotBlank(idCardNumber) && idCardNumber.matches("\\d{15}|\\d{18}");
  }

  /**
   * 判断字符串是否是中国的手机号码
   *
   * @param mobileNumber
   * @return
   */
  public static boolean isZhMobileNumber(String mobileNumber) {
    if (isNotBlank(mobileNumber)) {
      return Pattern.compile("^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$").matcher(mobileNumber)
          .matches();
    }
    return false;
  }

  /**
   * 判断字符串是否是中国的名字
   *
   * @param name
   * @param length
   * @return
   */
  public static boolean isZhName(String name, int length) {
    return isNotBlank(name) && name.matches("^[\u4e00-\u9fa5]+$") && name.length() <= length;
  }

  /**
   * 判断字符串是否为中国的电话号码
   *
   * @param phoneNumber
   * @return
   */
  public static boolean isZhPhoneNumber(String phoneNumber) {
    return isNotBlank(phoneNumber) && Pattern.compile("\\d{4}-\\d{8}|\\d{4}-\\d{7}|\\d(3)-\\d(8)")
        .matcher(phoneNumber).matches();
  }

  /**
   * 判断字符串是否是中国的邮政编码
   *
   * @param postcode
   * @return
   */
  public static boolean isZhPostcode(String postcode) {
    return isNotBlank(postcode) && postcode.matches("[1-9]\\d{5}(?!\\d)");
  }

  /**
   * 判断字符串达到最大长度要求
   *
   * @param text
   * @param maxLen
   * @param code
   * @param params
   * @return
   */
  public static boolean maxLength(String text, int maxLen) {
    return text == null || text.length() <= maxLen;
  }

  /**
   * 判断字符串是否非空并且达到最小长度要求
   *
   * @param text
   * @param minLen
   * @return
   */
  public static boolean minLength(String text, int minLen) {
    return text != null && text.length() >= minLen;
  }

  /**
   * 判断字符串是否达到最小和最大的长度要求
   *
   * @param text
   * @param minLen
   * @param maxLen
   * @param code
   * @param params
   * @return
   */
  public static boolean minMaxLength(String text, int minLen, int maxLen) {
    return text != null && text.length() <= maxLen && text.length() >= minLen;
  }
}

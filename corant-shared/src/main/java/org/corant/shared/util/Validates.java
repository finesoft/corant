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

import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.corant.shared.exception.CorantRuntimeException;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * corant-shared
 *
 * @author bingo 2018年3月23日
 */
public class Validates {

  public static final Pattern MOB_NUM_PTN =
      Pattern.compile("^((13[0-9])|(15[^4])|(18[0-9])|(17[0-9])|(147))\\d{8}$");
  public static final Pattern MAIL_ADDR_PTN = Pattern.compile(
      "^[\\w!#$%&’*+/=?`{|}~^-]+(?:\\.[\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$",
      Pattern.CASE_INSENSITIVE);
  public static final Pattern WEB_URL_PTN = Pattern.compile(
      "^(https?|ftp|file|rtsp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
      Pattern.CASE_INSENSITIVE);
  public static final Pattern IP_V4_PTN =
      Pattern.compile("\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\"
          + ".((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\"
          + ".((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\"
          + ".((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b");
  public static final Pattern ZH_ID_PTN = Pattern.compile("\\d{15}|\\d{18}");
  public static final Pattern ZH_PHONE_PTN =
      Pattern.compile("\\d{4}-\\d{8}|\\d{4}-\\d{7}|\\d(3)-\\d(8)");
  public static final Pattern ZH_PC_PTN = Pattern.compile("[1-9]\\d{5}(?!\\d)");

  private Validates() {}

  /**
   * Check if input stream is image format, the process prereads the input stream. If input stream
   * not support mark, then return false.
   *
   * @param is the image input stream to be checked
   * @param format the supports format
   */
  public static boolean isImage(InputStream is, String... format) {
    if (is == null || !is.markSupported()) {
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
      if (format != null && format.length > 0) {
        ImageReader reader = iter.next();
        String formatName = reader.getFormatName();
        return Arrays.stream(format).anyMatch(x -> x.equalsIgnoreCase(formatName));
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
   * Returns true if the given string is integer otherwise false.
   *
   * @param string the string to be checked
   */
  public static boolean isInteger(String string) {
    if (isBlank(string)) {
      return false;
    }
    try {
      Integer.parseInt(string);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Returns true if the given string is IP V4 address otherwise false.
   *
   * @param ipAddress the given IP V4 address to be checked
   */
  public static boolean isIp4Address(String ipAddress) {
    return isNotBlank(ipAddress) && IP_V4_PTN.matcher(ipAddress).matches();
  }

  /**
   * Returns true if the given string is mail address otherwise false.
   *
   * @param mailAddress the given mail address to be checked
   */
  public static boolean isMailAddress(String mailAddress) {
    return isNotBlank(mailAddress) && MAIL_ADDR_PTN.matcher(mailAddress).matches();
  }

  /**
   * Returns true if the given string is positive integer otherwise false.
   *
   * @param string the string to be checked
   */
  public static boolean isPositiveInteger(String string) {
    if (isBlank(string)) {
      return false;
    }
    try {
      return Integer.parseInt(string) > 0;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isValidMacAddress(byte[] address) {
    if (address == null || address.length != 6) {
      return false;
    }
    for (byte b : address) {
      if (b != 0x00) {
        return true; // If any of the bytes are non zero assume a good address
      }
    }
    return false;
  }

  /**
   * Return http(s)/ftp/file/rtsp url
   *
   * @param url the url to be checked
   * @return isHttpUrl
   */
  public static boolean isWebUrl(String url) {
    return isNotBlank(url) && WEB_URL_PTN.matcher(url).matches();
  }

  /**
   * Returns true if the given string is Chinese ID card number otherwise false.
   *
   * @param idCardNumber the ID card number to be checked
   */
  public static boolean isZhIDCardNumber(String idCardNumber) {
    return isNotBlank(idCardNumber) && ZH_ID_PTN.matcher(idCardNumber).matches();
  }

  /**
   * Returns true if the given string is Chinese mobile number otherwise false.
   *
   * @param mobileNumber the mobile number to be checked
   */
  public static boolean isZhMobileNumber(String mobileNumber) {
    return isNotBlank(mobileNumber) && MOB_NUM_PTN.matcher(mobileNumber).matches();
  }

  /**
   * Returns true if the given string is Chinese phone number otherwise false.
   *
   * @param phoneNumber the phone number to be checked
   */
  public static boolean isZhPhoneNumber(String phoneNumber) {
    return isNotBlank(phoneNumber) && ZH_PHONE_PTN.matcher(phoneNumber).matches();
  }

  /**
   * Returns true if the given string is Chinese post number otherwise false.
   *
   * @param postcode the postcode to be checked
   */
  public static boolean isZhPostcode(String postcode) {
    return isNotBlank(postcode) && ZH_PC_PTN.matcher(postcode).matches();
  }

  /**
   * Returns true if the given text is not null and does not exceed the given length otherwise
   * false.
   *
   * @param text the text to be checked
   * @param maxLen the length to be checked
   */
  public static boolean maxLength(String text, int maxLen) {
    return text == null || text.length() <= maxLen;
  }

  /**
   * Returns true if the given string is not null and does not less than the given length otherwise
   * false.
   *
   * @param text the text to be checked
   * @param minLen the length to be checked
   */
  public static boolean minLength(String text, int minLen) {
    return text != null && text.length() >= minLen;
  }

  /**
   * Returns true if the given string is not null and not less than the given min length and not
   * greater than the given max length otherwise false.
   *
   * @param text the text to be checked
   * @param minLen the min length to be checked
   * @param maxLen the max length to be checked
   */
  public static boolean minMaxLength(String text, int minLen, int maxLen) {
    return text != null && text.length() <= maxLen && text.length() >= minLen;
  }

  /**
   * Validate XML document with schema
   *
   * @param doc the document to be checked
   * @param schema the xml schema to be checked
   * @return validateXmlDocument
   */
  public static List<Exception> validateXmlDocument(Document doc, Schema schema) {
    final List<Exception> errors = new ArrayList<>();
    Validator validator = schema.newValidator();
    validator.setErrorHandler(new ErrorHandler() {
      @Override
      public void error(SAXParseException exception) throws SAXException {
        errors.add(exception);
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
        errors.add(exception);
      }

      @Override
      public void warning(SAXParseException exception) throws SAXException {}
    });
    try {
      validator.validate(new DOMSource(doc));
    } catch (SAXException | IOException e) {
      errors.add(e);
    }
    return errors;
  }
}

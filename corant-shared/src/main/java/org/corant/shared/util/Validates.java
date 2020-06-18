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
 * @author bingo 2018年3月23日
 */
public class Validates {

  public static final Pattern MOB_NUM_PTN = Pattern.compile(
      "^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(166)|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\\d{8}$");

  private Validates() {
    super();
  }

  /**
   * @param httpUrl
   * @return isHttpUrl
   */
  public static boolean isHttpUrl(String httpUrl) {
    return isNotBlank(httpUrl) && httpUrl.matches("[a-zA-z]+://[^\\s]*");
  }

  public static boolean isId(Long id) {
    return id != null && id > 0;
  }

  /**
   * Check if input stream is image format, the process prereads the input stream. If input stream
   * not support mark, then return false.
   * 
   * @param is
   * @return
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
   * @param mailAddress
   * @return
   */
  public static boolean isMailAddress(String mailAddress) {
    return isNotBlank(mailAddress)
        && Pattern.compile("^(\\w+)([\\-+.\\'][\\w]+)*@(\\w[\\-\\w]*\\.){1,5}([A-Za-z]){2,6}$")
            .matcher(mailAddress).matches();
  }

  /**
   * @param idCardNumber
   * @return
   */
  public static boolean isZhIDCardNumber(String idCardNumber) {
    return isNotBlank(idCardNumber) && idCardNumber.matches("\\d{15}|\\d{18}");
  }

  /**
   * @param mobileNumber
   * @return
   */
  public static boolean isZhMobileNumber(String mobileNumber) {
    if (isNotBlank(mobileNumber)) {
      return MOB_NUM_PTN.matcher(mobileNumber).matches();
    }
    return false;
  }

  /**
   * @param name
   * @param length
   * @return
   */
  public static boolean isZhName(String name, int length) {
    return isNotBlank(name) && name.matches("^[\u4e00-\u9fa5]+$") && name.length() <= length;
  }

  /**
   * @param phoneNumber
   * @return
   */
  public static boolean isZhPhoneNumber(String phoneNumber) {
    return isNotBlank(phoneNumber) && Pattern.compile("\\d{4}-\\d{8}|\\d{4}-\\d{7}|\\d(3)-\\d(8)")
        .matcher(phoneNumber).matches();
  }

  /**
   * @param postcode
   * @return
   */
  public static boolean isZhPostcode(String postcode) {
    return isNotBlank(postcode) && postcode.matches("[1-9]\\d{5}(?!\\d)");
  }

  /**
   * @param text
   * @param maxLen
   * @return
   */
  public static boolean maxLength(String text, int maxLen) {
    return text == null || text.length() <= maxLen;
  }

  /**
   * @param text
   * @param minLen
   * @return
   */
  public static boolean minLength(String text, int minLen) {
    return text != null && text.length() >= minLen;
  }

  /**
   * @param text
   * @param minLen
   * @param maxLen
   * @return
   */
  public static boolean minMaxLength(String text, int minLen, int maxLen) {
    return text != null && text.length() <= maxLen && text.length() >= minLen;
  }

  /**
   * Validate Xml document with schema
   * 
   * @param doc
   * @param schema
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

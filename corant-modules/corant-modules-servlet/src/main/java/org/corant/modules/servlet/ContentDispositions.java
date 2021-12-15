/*
 * Copyright 2002-2017 the original author or authors.
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
package org.corant.modules.servlet;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * corant-modules-servlet
 *
 * Code base from springframework, if there is infringement, please inform me(finesoft@gmail.com).
 *
 * @author bingo 下午2:09:49
 *
 */
public class ContentDispositions {

  public static String decodeHeaderFieldParam(String input) {
    shouldNotNull(input, "Input String should not be null");
    int firstQuoteIndex = input.indexOf(0x27);
    int secondQuoteIndex = input.indexOf(0x27, firstQuoteIndex + 1);
    // US_ASCII
    if (firstQuoteIndex == -1 || secondQuoteIndex == -1) {
      return input;
    }
    Charset charset = Charset.forName(input.substring(0, firstQuoteIndex));
    shouldBeTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
        "Charset should be UTF-8 or ISO-8859-1");
    byte[] value = input.substring(secondQuoteIndex + 1).getBytes(charset);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    int index = 0;
    while (index < value.length) {
      byte b = value[index];
      if (isRFC5987AttrChar(b)) {
        bos.write((char) b);
        index++;
      } else if (b == '%' && index + 3 < value.length) {
        char[] array = {(char) value[index + 1], (char) value[index + 2]};
        bos.write(Integer.parseInt(String.valueOf(array), 16));
        index += 3;
      } else {
        throw new IllegalArgumentException(
            "Invalid header field parameter format (as defined in RFC 5987)");
      }
    }
    return bos.toString(charset);
  }

  public static String encodeHeaderFieldParam(String input, Charset charset) {
    shouldNotNull(input, "The input string should not be null");
    shouldNotNull(charset, "The charset should not be null");
    if (StandardCharsets.US_ASCII.equals(charset)) {
      return input;
    }
    shouldBeTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
        "the charset should be UTF-8 or ISO-8859-1");
    byte[] source = input.getBytes(charset);
    int len = source.length;
    StringBuilder sb = new StringBuilder(len << 1);
    sb.append(charset.name());
    sb.append("''");
    for (byte b : source) {
      if (isRFC5987AttrChar(b)) {
        sb.append((char) b);
      } else {
        sb.append('%');
        char hex1 = Character.toUpperCase(Character.forDigit(b >> 4 & 0xF, 16));
        char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
        sb.append(hex1);
        sb.append(hex2);
      }
    }
    return sb.toString();
  }

  public static boolean isRFC5987AttrChar(byte c) {
    return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '!'
        || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' || c == '.' || c == '^'
        || c == '_' || c == '`' || c == '|' || c == '~';
  }

  public static ContentDisposition parse(String contentDisposition) {
    List<String> parts = tokenize(contentDisposition);
    String type = parts.get(0);
    String name = null;
    String filename = null;
    Charset charset = null;
    Long size = null;
    ZonedDateTime creationDate = null;
    ZonedDateTime modificationDate = null;
    ZonedDateTime readDate = null;
    for (int i = 1; i < parts.size(); i++) {
      String part = parts.get(i);
      int eqIndex = part.indexOf('=');
      if (eqIndex != -1) {
        String attribute = part.substring(0, eqIndex);
        String value = part.startsWith("\"", eqIndex + 1) && part.endsWith("\"")
            ? part.substring(eqIndex + 2, part.length() - 1)
            : part.substring(eqIndex + 1);
        if ("name".equals(attribute)) {
          name = value;
        } else if ("filename*".equals(attribute)) {
          filename = decodeHeaderFieldParam(value);
          charset = Charset.forName(value.substring(0, value.indexOf(0x27)));
          shouldBeTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
              "Charset should be UTF-8 or ISO-8859-1");
        } else if ("filename".equals(attribute) && filename == null) {
          filename = value;
        } else if ("size".equals(attribute)) {
          size = Long.parseLong(value);
        } else if ("creation-date".equals(attribute)) {
          try {
            creationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
          } catch (DateTimeParseException ex) {
            // ignore
          }
        } else if ("modification-date".equals(attribute)) {
          try {
            modificationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
          } catch (DateTimeParseException ex) {
            // ignore
          }
        } else if ("read-date".equals(attribute)) {
          try {
            readDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
          } catch (DateTimeParseException ex) {
            // ignore
          }
        }
      } else {
        throw new IllegalArgumentException("Invalid content disposition format");
      }
    }
    return new ContentDisposition(type, name, filename, charset, size, creationDate,
        modificationDate, readDate);
  }

  static List<String> tokenize(String headerValue) {
    int index = headerValue.indexOf(';');
    String type = (index >= 0 ? headerValue.substring(0, index) : headerValue).trim();
    if (type.isEmpty()) {
      throw new IllegalArgumentException("Content-Disposition header must not be empty");
    }
    List<String> parts = new ArrayList<>();
    parts.add(type);
    if (index >= 0) {
      do {
        int nextIndex = index + 1;
        boolean quoted = false;
        while (nextIndex < headerValue.length()) {
          char ch = headerValue.charAt(nextIndex);
          if (ch == ';') {
            if (!quoted) {
              break;
            }
          } else if (ch == '"') {
            quoted = !quoted;
          }
          nextIndex++;
        }
        String part = headerValue.substring(index + 1, nextIndex).trim();
        if (!part.isEmpty()) {
          parts.add(part);
        }
        index = nextIndex;
      } while (index < headerValue.length());
    }
    return parts;
  }

  public static class ContentDisposition {

    private final String type;

    private final String name;

    private final String filename;

    private final Charset charset;

    private final Long size;

    private final ZonedDateTime creationDate;

    private final ZonedDateTime modificationDate;

    private final ZonedDateTime readDate;

    private final boolean loose;

    /**
     * @param type the disposition type, like for example {@literal inline}, {@literal attachment},
     *        {@literal form-data}, or {@code null} if not defined.
     * @param name the value of the name parameter.
     * @param filename the value of the filename parameter.
     * @param charset the charset defined in filename* parameter.
     * @param size the value of the size parameter.
     * @param loose if true, the name and filename no quotes.
     */
    public ContentDisposition(String type, String name, String filename, Charset charset, Long size,
        boolean loose) {
      this(type, name, filename, charset, size, null, null, null, loose);
    }

    /**
     * @param type the disposition type, like for example {@literal inline}, {@literal attachment},
     *        {@literal form-data}, or {@code null} if not defined.
     * @param name the value of the name parameter.
     * @param filename the value of the filename parameter.
     * @param charset the charset defined in filename* parameter.
     * @param size the value of the size parameter.
     * @param creationDate the value of the creation-date parameter.
     * @param modificationDate the value of the modification-date parameter.
     * @param readDate the value of the read-date parameter.
     */
    public ContentDisposition(String type, String name, String filename, Charset charset, Long size,
        ZonedDateTime creationDate, ZonedDateTime modificationDate, ZonedDateTime readDate) {
      this(type, name, filename, charset, size, creationDate, modificationDate, readDate, false);
    }

    /**
     * @param type the disposition type, like for example {@literal inline}, {@literal attachment},
     *        {@literal form-data}, or {@code null} if not defined.
     * @param name the value of the name parameter.
     * @param filename the value of the filename parameter.
     * @param charset the charset defined in filename* parameter.
     * @param size the value of the size parameter.
     * @param creationDate the value of the creation-date parameter.
     * @param modificationDate the value of the modification-date parameter.
     * @param readDate the value of the read-date parameter.
     * @param loose if true, the name and filename no quotes.
     */
    public ContentDisposition(String type, String name, String filename, Charset charset, Long size,
        ZonedDateTime creationDate, ZonedDateTime modificationDate, ZonedDateTime readDate,
        boolean loose) {
      this.type = type;
      this.name = name;
      this.filename = filename;
      this.charset = charset;
      this.size = size;
      this.creationDate = creationDate;
      this.modificationDate = modificationDate;
      this.readDate = readDate;
      this.loose = loose;
    }

    public Charset getCharset() {
      return charset;
    }

    public ZonedDateTime getCreationDate() {
      return creationDate;
    }

    public String getFilename() {
      return filename;
    }

    public ZonedDateTime getModificationDate() {
      return modificationDate;
    }

    public String getName() {
      return name;
    }

    public ZonedDateTime getReadDate() {
      return readDate;
    }

    public Long getSize() {
      return size;
    }

    public String getType() {
      return type;
    }

    public boolean isLoose() {
      return loose;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (type != null) {
        sb.append(type);
      }
      if (name != null) {
        if (loose) {
          sb.append("; name=").append(name);
        } else {
          sb.append("; name=\"");
          sb.append(name).append('\"');
        }
      }
      if (filename != null) {
        if (loose) {
          sb.append("; filename=").append(filename);
        } else if (charset == null || US_ASCII.equals(charset)) {
          sb.append("; filename=\"");
          sb.append(filename).append('\"');
        } else {
          sb.append("; filename*=");
          sb.append(encodeHeaderFieldParam(filename, charset));
        }
      }
      if (size != null) {
        sb.append("; size=");
        sb.append(size);
      }
      if (creationDate != null) {
        sb.append("; creation-date=\"");
        sb.append(RFC_1123_DATE_TIME.format(creationDate));
        sb.append('\"');
      }
      if (modificationDate != null) {
        sb.append("; modification-date=\"");
        sb.append(RFC_1123_DATE_TIME.format(modificationDate));
        sb.append('\"');
      }
      if (readDate != null) {
        sb.append("; read-date=\"");
        sb.append(RFC_1123_DATE_TIME.format(readDate));
        sb.append('\"');
      }
      return sb.toString();
    }
  }
}

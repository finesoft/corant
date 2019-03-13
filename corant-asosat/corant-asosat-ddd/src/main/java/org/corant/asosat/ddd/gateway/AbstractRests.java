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
package org.corant.asosat.ddd.gateway;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.StreamUtils;
import org.corant.suites.ddd.annotation.stereotype.ApplicationServices;

/**
 * @author bingo 下午5:51:09
 *
 */
@ApplicationScoped
@ApplicationServices
public abstract class AbstractRests {

  protected static final Map<Class<?>, String> cachedPaths = new ConcurrentHashMap<>();

  /**
   * 202
   *
   * @param obj
   * @return accepted
   */
  protected Response accepted() {
    return Response.accepted().build();
  }

  /**
   * 202
   *
   * @param obj
   * @return accepted
   */
  protected Response accepted(Object obj) {
    return Response.accepted(obj).type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * 201
   *
   * @param id
   * @return created
   */
  protected Response created(Object id) {
    return created(URI.create(resolvePath() + "/get/" + id));
  }

  /**
   * 201
   *
   * @param location
   * @return created
   */
  protected Response created(URI location) {
    return Response.created(location).build();
  }

  /**
   * 204
   *
   * @return noContent
   */
  protected Response noContent() {
    return Response.noContent().build();
  }

  /**
   * 200
   *
   * @return ok
   */
  protected Response ok() {
    return Response.ok().type(MediaType.APPLICATION_JSON).build();
  }

  /**
   * 200
   *
   * @param obj
   * @return ok
   */
  protected Response ok(Object obj) {
    return Response.ok(obj).type(MediaType.APPLICATION_JSON).build();
  }

  protected String parseMpFileName(MultivaluedMap<String, String> headers) {
    for (String name : split(headers.getFirst("Content-Disposition"), ";", true, true)) {
      if (name.startsWith("filename")) {
        String[] tmp = split(name, "=", true, true);
        String fileName = tmp[1].replaceAll("\"", "");
        return fileName;
      }
    }
    return "unnamed-" + System.currentTimeMillis();
  }

  protected String resolvePath() {
    return cachedPaths.computeIfAbsent(getClass(), (cls) -> {
      Annotation[] annotations = cls.getAnnotations();
      for (Annotation annotation : annotations) {
        if (annotation instanceof Path) {
          Path pathAnnotation = (Path) annotation;
          return pathAnnotation.value();
        }
      }
      return "";
    });
  }

  protected StreamOutputBuilder stream(InputStream is) {
    return StreamOutputBuilder.of(is);
  }

  public static class StreamOutputBuilder {
    private final InputStream is;
    private boolean inline;
    private String name;
    private String fileName;
    private Charset charset;
    private Long size;
    private ZonedDateTime creationDate;
    private ZonedDateTime modificationDate;
    private ZonedDateTime readDate;
    private String contentType;
    private Map<String, Object> additionalHeaders = new HashMap<>();

    protected StreamOutputBuilder(InputStream is) {
      this.is = shouldNotNull(is, "The input stream can not null!");
    }

    public static StreamOutputBuilder of(InputStream is) {
      return new StreamOutputBuilder(is);
    }

    static String encodeHeaderFieldParam(String input, Charset charset) {
      shouldNotNull(input, "the input string should not be null");
      shouldNotNull(charset, "the charset should not be null");
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

    static boolean isRFC5987AttrChar(byte c) {
      return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '!'
          || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' || c == '.' || c == '^'
          || c == '_' || c == '`' || c == '|' || c == '~';
    }

    public StreamOutputBuilder additionalHeaders(Map<String, Object> additionalHeaders) {
      this.additionalHeaders.clear();
      this.additionalHeaders.putAll(additionalHeaders);
      return this;
    }

    public Response build() {
      ResponseBuilder rb =
          Response.ok((StreamingOutput) output -> StreamUtils.copy(is, output), contentType);
      if (size != null) {
        rb.header(HttpHeaders.CONTENT_LENGTH, size);
      }
      rb.header(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition());
      rb.header(HttpHeaders.LAST_MODIFIED, modificationDate);
      additionalHeaders.forEach((k, v) -> {
        if (k != null) {
          rb.header(k, v);
        }
      });
      return rb.build();
    }

    public StreamOutputBuilder charset(Charset charset) {
      this.charset = charset;
      return this;
    }

    public StreamOutputBuilder contentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public StreamOutputBuilder creationDate(ZonedDateTime creationDate) {
      this.creationDate = creationDate;
      return this;
    }

    public StreamOutputBuilder fileName(String fileName) {
      this.fileName = fileName;
      contentType = inline && isNotBlank(fileName)
          ? defaultObject(FileUtils.getContentType(fileName), MediaType.APPLICATION_OCTET_STREAM)
          : MediaType.APPLICATION_OCTET_STREAM;
      return this;
    }

    public String getContentDisposition() {
      StringBuilder sb = new StringBuilder();
      if (inline) {
        sb.append("inline");
      } else {
        sb.append("attachment");
      }
      if (name != null) {
        sb.append("; name=\"");
        sb.append(name).append('\"');
      }
      if (fileName != null) {
        if (charset == null || StandardCharsets.US_ASCII.equals(charset)) {
          sb.append("; filename=\"");
          sb.append(fileName).append('\"');
        } else {
          sb.append("; filename*=");
          sb.append(encodeHeaderFieldParam(fileName, charset));
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

    public StreamOutputBuilder inline(boolean inline) {
      this.inline = inline;
      return this;
    }

    public StreamOutputBuilder modificationDate(ZonedDateTime modificationDate) {
      this.modificationDate = modificationDate;
      return this;
    }

    public StreamOutputBuilder name(String name) {
      this.name = name;
      return this;
    }

    public StreamOutputBuilder readDate(ZonedDateTime readDate) {
      this.readDate = readDate;
      return this;
    }

    public StreamOutputBuilder size(Long size) {
      this.size = size;
      return this;
    }
  }
}

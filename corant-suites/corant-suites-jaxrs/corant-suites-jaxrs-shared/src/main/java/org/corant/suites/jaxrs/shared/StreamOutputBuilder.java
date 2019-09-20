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
package org.corant.suites.jaxrs.shared;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Resources.FileSystemResource;
import org.corant.shared.util.Resources.Resource;
import org.corant.shared.util.StreamUtils;
import org.corant.suites.servlet.abstraction.ContentDispositions;

/**
 * corant-suites-jaxrs-shared
 *
 * @author bingo 下午2:16:40
 *
 */
public class StreamOutputBuilder {
  private final InputStream is;
  private boolean inline;
  private String name;
  private String fileName;
  private Charset charset = UTF_8;
  private Long size;
  private ZonedDateTime creationDate;
  private ZonedDateTime modificationDate;
  private ZonedDateTime readDate;
  private String contentType;
  private Map<String, Object> additionalHeaders = new HashMap<>();

  protected StreamOutputBuilder(InputStream is) {
    this.is = shouldNotNull(is, "The input stream can not null!");
  }

  public static StreamOutputBuilder of(FileSystemResource resources) {
    try {
      return new StreamOutputBuilder(resources.openStream())
          .fileName(resources.getFile().getName());
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static StreamOutputBuilder of(InputStream is) {
    return new StreamOutputBuilder(is);
  }

  public static StreamOutputBuilder of(Resource resources) {
    try {
      return new StreamOutputBuilder(resources.openStream()).fileName(resources.getLocation());
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public StreamOutputBuilder additionalHeaders(Map<String, Object> additionalHeaders) {
    this.additionalHeaders.clear();
    this.additionalHeaders.putAll(additionalHeaders);
    return this;
  }

  public Response build() {
    ResponseBuilder rb =
        Response.ok((StreamingOutput) output -> StreamUtils.copy(is, output), contentType);
    if (inline) {
      rb.header(HttpHeaders.CONTENT_TYPE, contentType);
    } else {
      rb.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
    }
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
    contentType = isNotBlank(fileName)
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
        sb.append(ContentDispositions.encodeHeaderFieldParam(fileName, charset));
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

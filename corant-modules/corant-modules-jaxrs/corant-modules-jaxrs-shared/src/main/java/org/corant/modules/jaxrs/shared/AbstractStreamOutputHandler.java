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
package org.corant.modules.jaxrs.shared;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import org.corant.modules.servlet.ContentDispositions.ContentDisposition;
import org.corant.shared.util.FileUtils;

/**
 * corant-modules-jaxrs-shared
 *
 * @author bingo 下午2:16:40
 */
public abstract class AbstractStreamOutputHandler<T extends AbstractStreamOutputHandler<T>> {

  protected boolean inline;
  protected String name;
  protected String fileName;
  protected Charset charset = UTF_8;
  protected Long size;
  protected ZonedDateTime creationDate;
  protected ZonedDateTime modificationDate;
  protected ZonedDateTime readDate;
  protected String contentType;
  protected Map<String, Object> additionalHeaders = new HashMap<>();

  protected AbstractStreamOutputHandler() {}

  public T additionalHeaders(Map<String, Object> additionalHeaders) {
    this.additionalHeaders.clear();
    this.additionalHeaders.putAll(additionalHeaders);
    return me();
  }

  public T charset(Charset charset) {
    this.charset = charset;
    return me();
  }

  public T contentType(String contentType) {
    this.contentType = contentType;
    return me();
  }

  public T creationDate(ZonedDateTime creationDate) {
    this.creationDate = creationDate;
    return me();
  }

  public T fileName(String fileName) {
    this.fileName = fileName;
    return me();
  }

  public String getContentDisposition(boolean loose) {
    return new ContentDisposition(inline ? "inline" : "attachment", name, fileName, charset, size,
        creationDate, modificationDate, readDate, loose).toString();
  }

  public T inline(boolean inline) {
    this.inline = inline;
    return me();
  }

  public T modificationDate(ZonedDateTime modificationDate) {
    this.modificationDate = modificationDate;
    return me();
  }

  public T name(String name) {
    this.name = name;
    return me();
  }

  public T readDate(ZonedDateTime readDate) {
    this.readDate = readDate;
    return me();
  }

  public T size(Long size) {
    this.size = size;
    return me();
  }

  protected Response handle(StreamingOutput stm, boolean loose) {
    if (isBlank(contentType)) {
      if (isNotBlank(fileName)) {
        contentType = FileUtils.getContentType(fileName);
      }
      contentType = defaultObject(contentType, MediaType.APPLICATION_OCTET_STREAM);
    }
    ResponseBuilder rb = Response.ok(stm, contentType);
    if (inline) {
      rb.header(HttpHeaders.CONTENT_TYPE, contentType);
    } else {
      rb.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
    }
    if (size != null) {
      rb.header(HttpHeaders.CONTENT_LENGTH, size);
    }
    rb.header(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(loose));
    rb.header(HttpHeaders.LAST_MODIFIED, modificationDate);
    additionalHeaders.forEach(rb::header);
    return rb.build();
  }

  protected abstract T me();
}

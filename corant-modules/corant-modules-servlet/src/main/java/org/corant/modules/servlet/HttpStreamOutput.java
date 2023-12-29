/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.ubiquity.Throwing.uncheckedBiConsumer;
import static org.corant.shared.util.Assertions.shouldBeGreater;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Functions.emptyBiConsumer;
import static org.corant.shared.util.Functions.emptyFunction;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Streams.copy;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.join;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;
import org.corant.modules.servlet.ContentDispositions.ContentDisposition;
import org.corant.modules.servlet.HttpRanges.HttpRange;
import org.corant.shared.normal.Defaults;
import org.corant.shared.resource.LimitedStream.RangedInputStream;
import org.corant.shared.resource.Resource;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Randoms;
import org.corant.shared.util.Streams;

/**
 * corant-modules-servlet
 *
 * @author bingo 下午4:01:33
 */
public class HttpStreamOutput {

  public static final char HEADER_VALUE_BYTE_RANGE_SEPARATOR = '-';
  public static final String HEADER_NAME_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_NAME_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_NAME_CONTENT_DISPOSITION = "Content-Disposition";
  public static final String HEADER_VALUE_BYTE_RANGE_UNIT = "bytes";
  public static final String HEADER_VALUE_BYTE_RANGE_UNIT_PREFIX = "bytes=";
  public static final String HEADER_NAME_RANGE = "Range";
  public static final String HEADER_NAME_CONTENT_RANGE = "Content-Range";
  public static final String HEADER_NAME_IF_RANGE = "If-Range";
  public static final String HEADER_NAME_ETAG = "ETag";
  public static final String HEADER_NAME_LAST_MODIFIED = "Last-Modified";
  public static final String HEADER_NAME_ACCEPT_RANGES = "Accept-Ranges";
  public static final String HEADER_VALUE_ACCEPT_RANGES = "bytes";
  public static final String HEADER_VALUE_NO_ACCEPT_RANGES = "none";

  public static final int STATUS_OF_OK = 200;
  public static final int STATUS_OF_PARTIAL_CONTENT = 206;
  public static final int STATUS_OF_REQUESTED_RANGE_NOT_SATISFIABLE = 416;

  public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
  public static final String BOUNDARY_LINE_FMT = "--%s";
  public static final String CONTENT_TYPE_LINE_FMT = "Content-Type: %s";
  public static final String CONTENT_RANGE_FMT = HEADER_VALUE_BYTE_RANGE_UNIT + " %d-%d/%d";
  public static final String CONTENT_RANGE_LINE_FMT = "Content-Range: " + CONTENT_RANGE_FMT;
  public static final String MULTIPART_BYTE_RANGES_FMT = "multipart/byteranges; boundary=%s";
  public static final String NOT_SATISFIABLE_FMT = HEADER_VALUE_BYTE_RANGE_UNIT + " */%d";
  public static final String EMPTY_LINE = "\r\n";

  protected final static Logger logger = Logger.getLogger(HttpStreamOutput.class.getName());

  protected Function<String, String> requestHeaders;
  protected String contentType;
  protected String outputType;
  protected String name;
  protected String fileName;
  protected Charset charset;
  protected Long size;
  protected ZonedDateTime creationDate;
  protected ZonedDateTime modificationDate;
  protected ZonedDateTime readDate;
  protected boolean loose;
  protected Map<String, Object> additionalHeaders = new HashMap<>();

  public HttpStreamOutput(HttpStreamOutputBuilder builder) {
    shouldNotNull(builder);
    requestHeaders = builder.requestHeaders;
    contentType = builder.contentType;
    outputType = builder.outputType;
    name = builder.name;
    fileName = builder.fileName;
    charset = builder.charset;
    size = builder.size;
    creationDate = builder.creationDate;
    modificationDate = builder.modificationDate;
    readDate = builder.readDate;
    loose = builder.loose;
    additionalHeaders = builder.additionalHeaders;
  }

  protected HttpStreamOutput() {}

  public static HttpStreamOutputBuilder builder() {
    return new HttpStreamOutputBuilder();
  }

  public Map<String, Object> getAdditionalHeaders() {
    return unmodifiableMap(additionalHeaders);
  }

  public Charset getCharset() {
    return charset;
  }

  public String getContentType() {
    return contentType;
  }

  public ZonedDateTime getCreationDate() {
    return creationDate;
  }

  public String getFileName() {
    return fileName;
  }

  public ZonedDateTime getModificationDate() {
    return modificationDate;
  }

  public String getName() {
    return name;
  }

  public String getOutputType() {
    return outputType;
  }

  public ZonedDateTime getReadDate() {
    return readDate;
  }

  public Function<String, String> getRequestHeaders() {
    return requestHeaders;
  }

  public Long getSize() {
    return size;
  }

  public boolean isLoose() {
    return loose;
  }

  public Map<String, Object> resolveOutputHeaders() {
    Map<String, Object> headers = new HashMap<>();
    String useContentType = contentType;
    if (isBlank(useContentType)) {
      if (isNotBlank(fileName)) {
        useContentType = FileUtils.getContentType(fileName);
      }
      useContentType = defaultObject(useContentType, DEFAULT_CONTENT_TYPE);
    }
    headers.put(HEADER_NAME_CONTENT_TYPE, useContentType);
    if (size != null) {
      headers.put(HEADER_NAME_CONTENT_LENGTH, size.toString());
    }
    headers.put(HEADER_NAME_CONTENT_DISPOSITION, resolveContentDisposition());
    if (modificationDate != null) {
      headers.put(HEADER_NAME_LAST_MODIFIED,
          DateTimeFormatter.RFC_1123_DATE_TIME.format(modificationDate));
    }
    if (additionalHeaders != null) {
      headers.putAll(additionalHeaders);
    }
    return headers;
  }

  public HttpStreamOutputResult resolveRangeOutputResult() {
    shouldBeGreater(size, 0L);
    Date mdate = modificationDate == null ? null : Date.from(modificationDate.toInstant());
    if (HttpRanges.requireRange(requestHeaders, mdate)) {
      List<HttpRange> ranges;
      try {
        ranges = HttpRanges.parseRanges(requestHeaders, size);
        Collections.sort(ranges);
      } catch (Exception ex) {
        return resolveNotSatisfiableRangeOutputResult();
      }
      if (fullRangeOutput(ranges)) {
        return resolveFullRangeOutputResult();
      } else if (ranges.size() > 1) {
        return resolveMultiRangesOutputResult(ranges);
      } else {
        return resolveSingleRangeOutputResult(ranges);
      }
    } else {
      return resolveFullRangeOutputResult();
    }
  }

  protected boolean fullRangeOutput(List<HttpRange> ranges) {
    if (ranges.size() > 1) {
      return false;
    } else {
      HttpRange range = ranges.get(0);
      return range.getMax() >= size - 1 && range.getMin() == 0 || range.getMin() >= size - 1;
    }
  }

  protected String resolveContentDisposition() {
    return new ContentDisposition(defaultString(outputType, "attachment"), name, fileName, charset,
        size, creationDate, modificationDate, readDate, loose).toString();
  }

  protected HttpStreamOutputResult resolveFullRangeOutputResult() {
    logger.fine("Handle full output result!");
    Map<String, Object> headers = resolveOutputHeaders();
    headers.put(HEADER_NAME_ACCEPT_RANGES, HEADER_VALUE_ACCEPT_RANGES);
    return new HttpStreamOutputResult(STATUS_OF_OK, headers, uncheckedBiConsumer(Streams::copy));
  }

  protected HttpStreamOutputResult resolveMultiRangesOutputResult(List<HttpRange> ranges) {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HEADER_NAME_ACCEPT_RANGES, HEADER_VALUE_ACCEPT_RANGES);
    logger.fine(() -> format("Handle multi ranges %s output result!", join(",", ranges)));
    final String boundary = Randoms.randomNumbersAndUcLetters(32) + Defaults.CORANT_SIGN;
    headers.put(HEADER_NAME_CONTENT_TYPE, format(MULTIPART_BYTE_RANGES_FMT, boundary));
    if (additionalHeaders != null) {
      headers.putAll(additionalHeaders);
    }
    return new HttpStreamOutputResult(STATUS_OF_PARTIAL_CONTENT, headers,
        uncheckedBiConsumer((is, os) -> {
          long lastPos = 0;
          final String rangeLineFmt = CONTENT_RANGE_LINE_FMT.concat(EMPTY_LINE);
          for (HttpRange range : ranges) {
            os.write(format(BOUNDARY_LINE_FMT.concat(EMPTY_LINE), boundary).getBytes());
            os.write(format(CONTENT_TYPE_LINE_FMT.concat(EMPTY_LINE), contentType).getBytes());
            os.write(format(rangeLineFmt, range.start(), range.end(), size).getBytes());
            os.write(EMPTY_LINE.getBytes());
            copy(new RangedInputStream(is, range.start() - lastPos, range.size()), os);
            os.write(EMPTY_LINE.getBytes());
            lastPos = range.end() + 1;
          }
          os.write(format(BOUNDARY_LINE_FMT, boundary + "--").getBytes());
        }));
  }

  protected HttpStreamOutputResult resolveNotSatisfiableRangeOutputResult() {
    logger.fine("Handle not satisfiable range output result!");
    Map<String, Object> headers = new HashMap<>();
    headers.put(HEADER_NAME_ACCEPT_RANGES, HEADER_VALUE_ACCEPT_RANGES);
    headers.put(HEADER_NAME_CONTENT_RANGE, format(NOT_SATISFIABLE_FMT, size));
    if (additionalHeaders != null) {
      headers.putAll(additionalHeaders);
    }
    return new HttpStreamOutputResult(headers, STATUS_OF_REQUESTED_RANGE_NOT_SATISFIABLE);
  }

  protected HttpStreamOutputResult resolveSingleRangeOutputResult(List<HttpRange> ranges) {
    Map<String, Object> headers = new HashMap<>();
    headers.put(HEADER_NAME_ACCEPT_RANGES, HEADER_VALUE_ACCEPT_RANGES);
    HttpRange range = ranges.get(0);
    headers.put(HEADER_NAME_CONTENT_RANGE,
        format(CONTENT_RANGE_FMT, range.start(), range.end(), size));
    logger.fine(() -> format("Handle range %s output result; stream offset:%s limit: %s, total: %s",
        range, range.start(), range.size(), size));
    if (additionalHeaders != null) {
      headers.putAll(additionalHeaders);
    }
    return new HttpStreamOutputResult(STATUS_OF_PARTIAL_CONTENT, headers, uncheckedBiConsumer(
        (is, os) -> copy(new RangedInputStream(is, range.start(), range.size()), os)));
  }

  /**
   *
   * corant-modules-servlet
   *
   * @author bingo 上午11:20:38
   *
   */
  public static class HttpStreamOutputBuilder {
    protected Function<String, String> requestHeaders = emptyFunction();
    protected String contentType;
    protected String outputType = "attachment";
    protected String name;
    protected String fileName;
    protected Charset charset = UTF_8;
    protected Long size;
    protected ZonedDateTime creationDate;
    protected ZonedDateTime modificationDate;
    protected ZonedDateTime readDate;
    protected boolean loose;
    protected Map<String, Object> additionalHeaders = new HashMap<>();

    public HttpStreamOutputBuilder() {}

    public HttpStreamOutputBuilder additionalHeaders(Map<String, Object> additionalHeaders) {
      if (additionalHeaders != null) {
        this.additionalHeaders.putAll(additionalHeaders);
      }
      return this;
    }

    public HttpStreamOutput build() {
      return new HttpStreamOutput(this);
    }

    public HttpStreamOutputBuilder charset(Charset charset) {
      this.charset = charset;
      return this;
    }

    public HttpStreamOutputBuilder contentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public HttpStreamOutputBuilder creationDate(ZonedDateTime creationDate) {
      this.creationDate = creationDate;
      return this;
    }

    public HttpStreamOutputBuilder fileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public HttpStreamOutputBuilder fromResource(Resource resource) {
      shouldNotNull(resource);
      contentType = resource.getMetadataValue(Resource.META_CONTENT_TYPE, String.class);
      name = resource.getMetadataValue(Resource.META_NAME, String.class);
      fileName = resource.getMetadataValue(Resource.META_NAME, String.class);
      size = resource.getMetadataValue(Resource.META_CONTENT_LENGTH, Long.class);
      modificationDate =
          resource.getMetadataValue(Resource.META_LAST_MODIFIED, ZonedDateTime.class);
      return this;
    }

    public HttpStreamOutputBuilder loose(boolean loose) {
      this.loose = loose;
      return this;
    }

    public HttpStreamOutputBuilder modificationDate(ZonedDateTime modificationDate) {
      this.modificationDate = modificationDate;
      return this;
    }

    public HttpStreamOutputBuilder name(String name) {
      this.name = name;
      return this;
    }

    public HttpStreamOutputBuilder outputType(String outputType) {
      this.outputType = outputType;
      return this;
    }

    public HttpStreamOutputBuilder readDate(ZonedDateTime readDate) {
      this.readDate = readDate;
      return this;
    }

    public HttpStreamOutputBuilder requestHeaders(Function<String, String> requestHeaders) {
      this.requestHeaders = requestHeaders;
      return this;
    }

    public HttpStreamOutputBuilder size(Long size) {
      this.size = size;
      return this;
    }
  }

  /**
   * corant-modules-servlet
   *
   * @author bingo 下午9:54:49
   *
   */
  public static class HttpStreamOutputResult {
    protected final Map<String, Object> headers;
    protected final int status;
    protected final BiConsumer<InputStream, OutputStream> writer;

    public HttpStreamOutputResult(int status, Map<String, Object> headers,
        BiConsumer<InputStream, OutputStream> writer) {
      if (headers != null) {
        this.headers = unmodifiableMap(headers);
      } else {
        this.headers = emptyMap();
      }
      this.status = status;
      if (writer != null) {
        this.writer = writer;
      } else {
        this.writer = emptyBiConsumer();
      }
    }

    public HttpStreamOutputResult(Map<String, Object> headers, int status) {
      this(status, headers, null);
    }

    public Map<String, Object> getHeaders() {
      return headers;
    }

    public int getStatus() {
      return status;
    }

    public BiConsumer<InputStream, OutputStream> getWriter() {
      return writer;
    }
  }

}

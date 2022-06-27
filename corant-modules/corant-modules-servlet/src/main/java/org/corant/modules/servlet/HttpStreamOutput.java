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
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Functions.emptyBiConsumer;
import static org.corant.shared.util.Functions.uncheckedBiConsumer;
import static org.corant.shared.util.Maps.getMapLong;
import static org.corant.shared.util.Maps.getMapString;
import static org.corant.shared.util.Maps.getMapZonedDateTime;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.join;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
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
 *
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

  protected final static Logger logger =
      Logger.getLogger(HttpStreamOutput.class.getCanonicalName());

  protected final Function<String, String> requestHeaders;
  protected final String contentType;
  protected final ContentDisposition contentDisposition;
  protected final boolean supportRange;
  protected final boolean autoCloseInputStream;

  public HttpStreamOutput(Function<String, String> requestHeaders, Resource resource,
      boolean inline, boolean autoCloseInputStream, boolean supportRange) {
    this.requestHeaders = shouldNotNull(requestHeaders);
    contentType = getMapString(resource.getMetadata(), Resource.META_CONTENT_TYPE);
    contentDisposition = new ContentDisposition(inline ? "inline" : null,
        getMapString(resource.getMetadata(), Resource.META_NAME),
        getMapString(resource.getMetadata(), Resource.META_NAME), StandardCharsets.UTF_8,
        getMapLong(resource.getMetadata(), Resource.META_CONTENT_LENGTH),
        getMapZonedDateTime(resource.getMetadata(), Resource.META_LAST_MODIFIED), null, null);
    this.autoCloseInputStream = autoCloseInputStream;
    this.supportRange = supportRange;
  }

  public HttpStreamOutput(Function<String, String> requestHeaders, String contentType,
      ContentDisposition contentDisposition, boolean autoCloseInputStream, boolean supportRange) {
    this.requestHeaders = shouldNotNull(requestHeaders);
    this.contentType = contentType;
    this.contentDisposition = contentDisposition;
    this.autoCloseInputStream = autoCloseInputStream;
    this.supportRange = supportRange;
  }

  public HttpStreamOutput(Function<String, String> requestHeaders, String contentType, String type,
      String filename, Charset charset, Long size, ZonedDateTime modificationDate, boolean loose,
      boolean autoCloseInputStream, boolean supportRange) {
    this.requestHeaders = shouldNotNull(requestHeaders);
    this.contentType = contentType;
    contentDisposition = new ContentDisposition(type, null, filename, charset, size, null,
        modificationDate, null, loose);
    this.autoCloseInputStream = autoCloseInputStream;
    this.supportRange = supportRange;
  }

  public HttpStreamOutputResult handle() {
    if (contentDisposition.getSize() != null && contentDisposition.getSize() > 0 && supportRange) {
      if (HttpRanges.requireRange(requestHeaders,
          toObject(contentDisposition.getModificationDate(), Date.class))) {
        List<HttpRange> ranges;
        try {
          ranges = HttpRanges.parseRanges(requestHeaders, contentDisposition.getSize());
        } catch (Exception ex) {
          return handleNotSatisfiableRange();
        }
        if (fullOutput(ranges)) {
          return handleFull();
        } else {
          return handleRanges(ranges);
        }
      } else {
        return handleFull();
      }
    }
    return handleFull();
  }

  protected void copy(InputStream is, OutputStream os) throws IOException {
    if (autoCloseInputStream) {
      Streams.copy(is, new FilterOutputStream(os) {
        @Override
        public void close() throws IOException {
          try {
            super.close();
          } finally {
            try {
              is.close();
            } catch (IOException e) {
              logger.log(Level.WARNING,
                  "Auto close input stream  occurred error when output stream closed!", e);
            }
          }
        }
      });
    } else {
      Streams.copy(is, os);
    }
  }

  protected boolean fullOutput(List<HttpRange> ranges) {
    if (ranges.size() > 1) {
      return false;
    } else {
      HttpRange range = ranges.get(0);
      return range.getMax() >= contentDisposition.getSize() - 1 && range.getMin() == 0
          || range.getMin() >= contentDisposition.getSize() - 1;
    }
  }

  protected HttpStreamOutputResult handleFull() {
    logger.fine("Handle full output result!");
    Map<String, Object> headers = new HashMap<>();
    if (supportRange) {
      headers.put(HEADER_NAME_ACCEPT_RANGES, HEADER_VALUE_ACCEPT_RANGES);
    }
    String useContentType = contentType;
    if (isBlank(useContentType)) {
      if (isNotBlank(contentDisposition.getFilename())) {
        useContentType = FileUtils.getContentType(contentDisposition.getFilename());
      }
      useContentType = defaultObject(useContentType, DEFAULT_CONTENT_TYPE);
    }
    headers.put(HEADER_NAME_CONTENT_TYPE, useContentType);
    if (contentDisposition.getSize() != null) {
      headers.put(HEADER_NAME_CONTENT_LENGTH, contentDisposition.getSize().toString());
    }
    headers.put(HEADER_NAME_CONTENT_DISPOSITION, contentDisposition.toString());
    if (contentDisposition.getModificationDate() != null) {
      headers.put(HEADER_NAME_LAST_MODIFIED, contentDisposition.getModificationDate().toString());
    }
    return new HttpStreamOutputResult(STATUS_OF_OK, headers, uncheckedBiConsumer(Streams::copy));
  }

  protected HttpStreamOutputResult handleNotSatisfiableRange() {
    logger.fine("Handle not satisfiable range output result!");
    Map<String, Object> headers = new HashMap<>();
    if (supportRange) {
      headers.put(HEADER_NAME_ACCEPT_RANGES, HEADER_VALUE_ACCEPT_RANGES);
    }
    headers.put(HEADER_NAME_CONTENT_RANGE,
        format(NOT_SATISFIABLE_FMT, contentDisposition.getSize()));
    return new HttpStreamOutputResult(headers, STATUS_OF_REQUESTED_RANGE_NOT_SATISFIABLE);
  }

  protected HttpStreamOutputResult handleRanges(List<HttpRange> ranges) {
    List<HttpRange> useRanges = new ArrayList<>(ranges);
    Collections.sort(useRanges);
    if (useRanges.size() > 1) {
      return handleRangeMulti(useRanges);
    } else {
      return handleRangeSingle(useRanges);
    }
  }

  HttpStreamOutputResult handleRangeMulti(List<HttpRange> ranges) {
    Map<String, Object> headers = new HashMap<>();
    if (supportRange) {
      headers.put(HEADER_NAME_ACCEPT_RANGES, HEADER_VALUE_ACCEPT_RANGES);
    }
    logger.fine(() -> format("Handle multi ranges %s output result!", join(",", ranges)));
    final String boundary = Randoms.randomNumbersAndUcLetters(32) + Defaults.CORANT_SIGN;
    headers.put(HEADER_NAME_CONTENT_TYPE, format(MULTIPART_BYTE_RANGES_FMT, boundary));
    final long contentLength = contentDisposition.getSize();
    return new HttpStreamOutputResult(STATUS_OF_PARTIAL_CONTENT, headers,
        uncheckedBiConsumer((is, os) -> {
          long lastPos = 0;
          final String rangeLineFmt = CONTENT_RANGE_LINE_FMT.concat(EMPTY_LINE);
          for (HttpRange range : ranges) {
            os.write(format(BOUNDARY_LINE_FMT.concat(EMPTY_LINE), boundary).getBytes());
            os.write(format(CONTENT_TYPE_LINE_FMT.concat(EMPTY_LINE), contentType).getBytes());
            os.write(format(rangeLineFmt, range.start(), range.end(), contentLength).getBytes());
            os.write(EMPTY_LINE.getBytes());
            copy(new RangedInputStream(is, range.start() - lastPos, range.size()), os);
            os.write(EMPTY_LINE.getBytes());
            lastPos = range.end() + 1;
          }
          os.write(format(BOUNDARY_LINE_FMT, boundary + "--").getBytes());
        }));
  }

  HttpStreamOutputResult handleRangeSingle(List<HttpRange> ranges) {
    Map<String, Object> headers = new HashMap<>();
    if (supportRange) {
      headers.put(HEADER_NAME_ACCEPT_RANGES, HEADER_VALUE_ACCEPT_RANGES);
    }
    HttpRange range = ranges.get(0);
    headers.put(HEADER_NAME_CONTENT_RANGE,
        format(CONTENT_RANGE_FMT, range.start(), range.end(), contentDisposition.getSize()));
    logger.fine(() -> format("Handle range %s output result; stream offset:%s limit: %s, total: %s",
        range, range.start(), range.size(), contentDisposition.getSize()));
    return new HttpStreamOutputResult(STATUS_OF_PARTIAL_CONTENT, headers, uncheckedBiConsumer(
        (is, os) -> copy(new RangedInputStream(is, range.start(), range.size()), os)));
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

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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.corant.modules.servlet.HttpStreamOutput.HEADER_NAME_ETAG;
import static org.corant.modules.servlet.HttpStreamOutput.HEADER_NAME_IF_RANGE;
import static org.corant.modules.servlet.HttpStreamOutput.HEADER_NAME_RANGE;
import static org.corant.modules.servlet.HttpStreamOutput.HEADER_VALUE_BYTE_RANGE_SEPARATOR;
import static org.corant.modules.servlet.HttpStreamOutput.HEADER_VALUE_BYTE_RANGE_UNIT_PREFIX;
import static org.corant.shared.util.Assertions.shouldNotBeLess;
import static org.corant.shared.util.Conversions.toLong;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.min;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.split;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import org.corant.shared.ubiquity.Tuple.Range;

/**
 * corant-modules-servlet
 *
 * @author bingo 下午6:35:22
 *
 */
public class HttpRanges {

  public static List<HttpRange> parseRanges(Function<String, String> headerGetter,
      long contentLength) {
    return parseRanges(headerGetter.apply(HEADER_NAME_RANGE), contentLength);
  }

  public static List<HttpRange> parseRanges(String ranges, long contentLength) {
    String useRanges = ranges;
    if (isBlank(useRanges)) {
      return emptyList();
    }
    if (!useRanges.startsWith(HEADER_VALUE_BYTE_RANGE_UNIT_PREFIX)) {
      throw new IllegalArgumentException("Range '" + useRanges + "' does not start with 'bytes='");
    }
    useRanges = useRanges.substring(HEADER_VALUE_BYTE_RANGE_UNIT_PREFIX.length());
    String[] byteRangeExpress = split(useRanges, ",");
    List<HttpRange> byteRangeSet =
        streamOf(byteRangeExpress).map(e -> parseRange(e, contentLength)).collect(toList());
    if (!isSatisfiable(byteRangeSet, contentLength)) {
      throw new IllegalArgumentException("Range '" + useRanges + "' not supported!");
    }
    return byteRangeSet;
  }

  public static boolean requireRange(Function<String, String> headerGetter,
      Date resourceLastModified) {
    if (headerGetter.apply(HEADER_NAME_RANGE) == null) {
      return false;
    }
    String ifRange = headerGetter.apply(HEADER_NAME_IF_RANGE);
    if (ifRange != null) {
      if (ifRange.charAt(0) == '"') {
        String eTag = headerGetter.apply(HEADER_NAME_ETAG);
        if (eTag != null && !eTag.equals(ifRange)) {
          return false;
        }
      } else {
        Date ifDate = toObject(ifRange, Date.class);
        if (ifDate != null && resourceLastModified != null
            && ifDate.getTime() < resourceLastModified.getTime()) {
          return false;
        }
      }
    }
    return true;
  }

  static boolean isSatisfiable(List<HttpRange> ranges, long contentLength) {
    if (isEmpty(ranges)) {
      return false;
    }
    if (!ranges.stream().anyMatch(r -> r.min() < contentLength - 1 || r.max() > 0)) {
      return false;
    }
    int size = ranges.size();
    for (int i = 0; i < size; i++) {
      HttpRange range = ranges.get(i);
      if (ranges.stream().anyMatch(r -> !r.equals(range) && r.coincide(range))) {
        return false;
      }
    }
    return true;
  }

  static HttpRange parseRange(String range, long contentLength) {
    long endPos = contentLength - 1L;
    int separator = range.indexOf(HEADER_VALUE_BYTE_RANGE_SEPARATOR);
    if (separator > 0) {
      long firstPos = shouldNotBeLess(toLong(range.substring(0, separator)), 0L,
          "Invalid range spec %s", range);
      if (separator < range.length() - 1) {
        long lastPos = toLong(range.substring(separator + 1));
        return new HttpRange(firstPos, min(lastPos, endPos));
      } else {
        return new HttpRange(firstPos, endPos);
      }
    } else if (separator == 0) {
      long suffix = shouldNotBeLess(toLong(range.substring(1)), 0L, "Invalid range spec %s", range);
      if (endPos < suffix) {
        return new HttpRange(0L, endPos);
      } else {
        return new HttpRange(endPos - suffix, endPos);
      }
    } else {
      throw new IllegalArgumentException(
          "Invalid range spec '" + range + "' does not contain \"-\"");
    }
  }

  /**
   * corant-modules-servlet
   *
   * @author bingo 下午6:27:05
   *
   */
  public static class HttpRange extends Range<Long> implements Comparable<HttpRange> {

    public HttpRange(Long min, Long max) {
      super(min, max);
    }

    @Override
    public int compareTo(HttpRange o) {
      return min.compareTo(o.min);
    }

    public long end() {
      return max;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      HttpRange other = (HttpRange) obj;
      if (max == null) {
        if (other.max != null) {
          return false;
        }
      } else if (!max.equals(other.max)) {
        return false;
      }
      if (min == null) {
        return other.min == null;
      } else {
        return min.equals(other.min);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (max == null ? 0 : max.hashCode());
      return prime * result + (min == null ? 0 : min.hashCode());
    }

    public long size() {
      return max - min + 1;
    }

    public long start() {
      return min;
    }
  }
}

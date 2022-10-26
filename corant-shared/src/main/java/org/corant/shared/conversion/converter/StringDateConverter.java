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
package org.corant.shared.conversion.converter;

import static org.corant.shared.util.Lists.immutableListOf;
import static org.corant.shared.util.Strings.isBlank;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-shared
 *
 * @author bingo 下午5:40:35
 *
 */
public class StringDateConverter extends AbstractConverter<String, Date> {

  public static final List<Pair<Pattern, String>> PATTERNS = immutableListOf(
  //@formatter:off
      // Common date formatter
      Pair.of(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}(\\+\\d{2}:\\d{2})?$"), "yyyy-MM-dd"),
      Pair.of(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "yyyy-MM-dd HH:mm:ss"),
      Pair.of(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}\\:\\d{2}\\:\\d{2}\\.\\d{3,}$"), "yyyy-MM-dd HH:mm:ss.SSS"),
      Pair.of(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}[\\+,\\-]\\d{4}$"), "yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
      Pair.of(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1,3}[\\+,\\-]\\d{2}:\\d{2}$"), "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
      Pair.of(Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\s[\\+,\\-]\\d{4}$"), "yyyy-MM-dd'T'HH:mm:ss Z"),
      Pair.of(Pattern.compile("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$"), "yyyy-MM-dd HH:mm"),
      Pair.of(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "dd-MM-yyyy HH:mm:ss"),
      Pair.of(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$"), "dd-MM-yyyy HH:mm"),
      Pair.of(Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}$"), "dd-MM-yyyy"),
      Pair.of(Pattern.compile("^\\d{4}-\\d{2}$"), "yyyy-MM"),
      Pair.of(Pattern.compile("^\\d{4}-\\d{3}-\\d{3}$"), "yyyy-Www-D"), // ISO Week dates
      Pair.of(Pattern.compile("^\\d{4}-W\\d{2}-\\d{1}$"),"YYYY-'W'ww-u"),

      Pair.of(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}$"), "yyyy/MM/dd"),
      Pair.of(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$"), "yyyy/MM/dd HH:mm:ss"),
      Pair.of(Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$"), "yyyy/MM/dd HH:mm"),
      Pair.of(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}$"), "MM/dd/yyyy"),
      Pair.of(Pattern.compile("^\\d{4}/\\d{2}$"), "yyyy/MM"),
      Pair.of(Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$"), "MM/dd/yyyy HH:mm"),
      Pair.of(Pattern.compile("^\\[\\d{1,2}/[a-zA-Z]{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2}\\s[\\+,\\-]\\d{4}\\]$"), "[dd/MMM/yyyy:HH:mm:ss Z]"), // COMMON_LOG

      Pair.of(Pattern.compile("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}$"), "dd MMM yyyy"),// 14 Nov 1979 only for US
      Pair.of(Pattern.compile("^\\d{1,2}-[a-zA-Z]{3}-\\d{4}$"), "dd-MMM-yyyy"), // 14-Nov-1979 only for US
      Pair.of(Pattern.compile("^\\d{1,2}\\s[a-zA-Z]{4,9}\\s\\d{4}$"), "dd MMMM yyyy"),// 14 November 1979 only for US
      Pair.of(Pattern.compile("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$"), "dd MMM yyyy HH:mm"),
      Pair.of(Pattern.compile("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$"), "dd MMMM yyyy HH:mm"),

      Pair.of(Pattern.compile("^\\d{8}$"), "yyyyMMdd"),
      Pair.of(Pattern.compile("^\\d{14}$"), "yyyyMMddHHmmss"),
      Pair.of(Pattern.compile("^\\d{8}\\s\\d{6}$"), "yyyyMMdd HHmmss"),
      Pair.of(Pattern.compile("^\\d{12}$"), "yyyyMMddHHmm"),
      Pair.of(Pattern.compile("^\\d{8}\\s\\d{4}$"), "yyyyMMdd HHmm"),

      Pair.of(Pattern.compile("^\\d{4}\\.\\d{1,2}\\.\\d{1,2}$"),"yyyy.MM.dd"),
      Pair.of(Pattern.compile("^\\d{4}\\.\\d{1,2}\\.\\d{1,2}\\s\\d{2}:\\d{2}:\\d{2}$"),"yyyy.MM.dd HH:mm:ss"),
      Pair.of(Pattern.compile("^\\d{4}\\.\\d{1,2}\\.\\d{1,2}\\s\\d{2}:\\d{2}$"),"yyyy.MM.dd HH:mm"),
      Pair.of(Pattern.compile("^\\d{4}\\.\\d{1,2}$"),"yyyy.MM"),
      Pair.of(Pattern.compile("^\\d{4}\\.\\d{1,2}\\.\\d{1,2}\\s[a-zA-Z]{2,}\\sat\\s\\d{2}:\\d{1,2}:\\d{1,2}\\s[a-zA-Z]{2,}$"), "yyyy.MM.dd G 'at' HH:mm:ss z"),

      Pair.of(Pattern.compile("^([a-zA-Z]{6,9}\\,\\s)?\\d{1,2}-[a-zA-Z]{3}-\\d{2}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[a-zA-Z]{2,}$"), "EEEEEEEEE, dd-MMM-yy HH:mm:ss z"), // RFC1036
      Pair.of(Pattern.compile("^([a-zA-Z]{3}\\s)?[a-zA-Z]{3}\\s\\d{1,2}\\s\\d{2}:\\d{2}:\\d{2}\\s\\d{5}$"), "EEE MMM d HH:mm:ss yyyyy"), // ASCITIME
      Pair.of(Pattern.compile("^([a-zA-Z]{3}\\,\\s)?\\d{1,2}-[a-zA-Z]{3}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[a-zA-Z]{2,}$"), "EEE, dd-MMM-yyyy HH:mm:ss z"), // RFC1123
      Pair.of(Pattern.compile("^([a-zA-Z]{3}\\,\\s)?\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[a-zA-Z]{2,}$"), "EEE, dd MMM yyyy HH:mm:ss z") // OLD_COOKIE
  //@formatter:on
  );

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  public StringDateConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  public StringDateConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  public StringDateConverter(Date defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  public StringDateConverter(Date defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  @Override
  public boolean isPossibleDistortion() {
    return true;
  }

  @Override
  protected Date doConvert(String value, Map<String, ?> hints) throws Exception {
    if (value.isEmpty()) {
      return getDefaultValue();
    }
    String val = value.trim();
    int semicolonIndex = val.indexOf(';');
    final String useVal = semicolonIndex >= 0 ? val.substring(0, semicolonIndex) : val;
    String ptn = ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_FMT_PTN_KEY);
    if (isBlank(ptn)) {
      Pair<Pattern, String> p = PATTERNS.stream().filter(pp -> pp.left().matcher(useVal).matches())
          .findFirst().orElse(null);
      if (p != null) {
        ptn = p.right();
      }
    }
    if (ptn != null) {
      // FIXME ONLY SUPPORT US
      Locale lo = null;
      Object hintLocaleId = ConverterHints.getHint(hints, ConverterHints.CVT_LOCALE_KEY);
      if (hintLocaleId instanceof Locale) {
        lo = (Locale) hintLocaleId;
      } else if (hintLocaleId instanceof String) {
        lo = Locale.forLanguageTag(hintLocaleId.toString());
      }
      if (lo == null && (ptn.contains("MMM") || ptn.contains("EEE") || ptn.contains(" G "))) {
        lo = Locale.US;
      }
      TimeZone tz = null;
      Object hintZoneId = ConverterHints.getHint(hints, ConverterHints.CVT_TIME_ZONE_KEY);
      if (hintZoneId instanceof TimeZone) {
        tz = (TimeZone) hintZoneId;
      } else if (hintZoneId instanceof String) {
        tz = TimeZone.getTimeZone(hintZoneId.toString());
      }
      ParsePosition pp = new ParsePosition(0);
      SimpleDateFormat dateFormat =
          lo == null ? new SimpleDateFormat(ptn) : new SimpleDateFormat(ptn, lo);
      if (tz != null) {
        dateFormat.setTimeZone(tz);
      }
      Date date = dateFormat.parse(useVal, pp);
      if (date != null && pp.getIndex() == useVal.length()) {
        return date;
      }
    }
    return new SimpleDateFormat().parse(useVal);
  }
}

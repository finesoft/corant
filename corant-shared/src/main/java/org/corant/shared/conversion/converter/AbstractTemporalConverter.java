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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static org.corant.shared.util.Lists.immutableListOf;
import static org.corant.shared.util.Maps.immutableMapOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 下午9:21:10
 *
 */
public abstract class AbstractTemporalConverter<S, T extends Temporal>
    extends AbstractConverter<S, T> {

  public static final Map<Long, String> DEFAULT_DAY_OF_WEEK_LP = // NOSONAR
      immutableMapOf(1L, "Mon", 2L, "Tue", 3L, "Wed", 4L, "Thu", 5L, "Fri", 6L, "Sat", 7L, "Sun");

  public static final Map<Long, String> DEFAULT_ZH_DAY_OF_WEEK_LP = // NOSONAR
      immutableMapOf(1L, "星期一", 2L, "星期二", 3L, "星期三", 4L, "星期四", 5L, "星期五", 6L, "星期六", 7L, "星期日");

  public static final Map<Long, String> DEFAULT_MONTH_OF_YEAR_LP = // NOSONAR
      immutableMapOf(1L, "Jan", 2L, "Feb", 3L, "Mar", 4L, "Apr", 5L, "May", 6L, "Jun", 7L, "Jul",
          8L, "Aug", 9L, "Sep", 10L, "Oct", 11L, "Nov", 12L, "Dec");

  public static final Map<Long, String> DEFAULT_ZH_MONTH_OF_YEAR_LP = // NOSONAR
      immutableMapOf(1L, "一月", 2L, "二月", 3L, "三月", 4L, "四月", 5L, "五月", 6L, "六月", 7L, "七月", 8L, "八月",
          9L, "九月", 10L, "十月", 11L, "十一月", 12L, "十二月");

  public static final List<TemporalFormatter> DEFAULT_FORMATTERS = // NOSONAR

      immutableListOf(

          new TemporalFormatter("^\\d{8}$", DateTimeFormatter.BASIC_ISO_DATE, "yyyyMMdd", false),

          new TemporalFormatter("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy", false),

          new TemporalFormatter("^\\d{4}-\\d{1,2}-\\d{1,2}(\\+\\d{2}:\\d{2})?$",
              DateTimeFormatter.ISO_DATE, "yyyy-MM-dd", false),

          new TemporalFormatter("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy", false),

          new TemporalFormatter("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd", false),

          new TemporalFormatter("^\\d{4}\\.\\d{1,2}\\.\\d{1,2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('.').appendValue(MONTH_OF_YEAR, 2).appendLiteral('.')
                  .appendValue(DAY_OF_MONTH, 2).toFormatter(),
              "yyyy.MM.dd", false),

          // 1979年11月14日
          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日$", "yyyy年MM月dd日", false),

          // 14 Nov 1979 only for US
          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}$", "dd MMM yyyy", Locale.US,
              false),

          // 14-Nov-1979 only for US
          new TemporalFormatter("^\\d{1,2}-[a-zA-Z]{3}-\\d{4}$", "dd-MMM-yyyy", Locale.US, false),

          // 14 November 1979 only for US
          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}$", "dd MMMM yyyy", Locale.US,
              false),

          // ISO Week dates
          new TemporalFormatter("^\\d{4}-W\\d{2}-\\d{1}$", DateTimeFormatter.ISO_WEEK_DATE,
              "yyyy-Www-D", false),

          // ISO Week dates
          new TemporalFormatter("^\\d{4}W\\d{2}\\d{1}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .appendValue(IsoFields.WEEK_BASED_YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                  .appendLiteral("W").appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2)
                  .appendValue(DAY_OF_WEEK, 1).toFormatter(),
              "yyyyWwwD", false),

          new TemporalFormatter("^\\d{12}$", "yyyyMMddHHmm", true),

          new TemporalFormatter("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm", true),

          new TemporalFormatter("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm",
              true),

          new TemporalFormatter("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm",
              true),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\s\\d{1,2}时\\d{2}分$",
              "yyyy年MM月dd日 HH时mm分", true),

          new TemporalFormatter("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm",
              true),

          new TemporalFormatter("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm",
              true),

          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$",
              "dd MMM yyyy HH:mm", Locale.US, true),

          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$",
              "dd MMMM yyyy HH:mm", Locale.US, true),

          new TemporalFormatter("^\\d{14}$", "yyyyMMddHHmmss", true),

          new TemporalFormatter("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss", true),

          new TemporalFormatter("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
              "dd-MM-yyyy HH:mm:ss", true),

          new TemporalFormatter("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$",
              "yyyy-MM-dd HH:mm:ss", true),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\s\\d{1,2}时\\d{2}分\\d{2}秒$",
              "yyyy年MM月dd日 HH时mm分ss秒", true),

          // ISO_INSTANT
          new TemporalFormatter("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}Z$",
              DateTimeFormatter.ISO_INSTANT, "ISO_INSTANT yyyy-MM-ddTHH:mm:ssZ", true),

          // ISO_DATE_TIME
          new TemporalFormatter("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}(.*)?$",
              DateTimeFormatter.ISO_DATE_TIME, "ISO_DATE_TIME yyyy-MM-ddTHH:mm:ss+o[z]", true),

          new TemporalFormatter("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
              "MM/dd/yyyy HH:mm:ss", true),

          new TemporalFormatter("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$",
              "yyyy/MM/dd HH:mm:ss", true),

          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
              "dd MMM yyyy HH:mm:ss", Locale.US, true),

          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
              "dd MMMM yyyy HH:mm:ss", Locale.US, true),

          new TemporalFormatter("^\\d{4}-\\d{1,2}-\\d{1,2}-\\d{1,2}\\.\\d{2}\\.\\d{2}\\.\\d{1,6}$",
              "yyyy-MM-dd-HH.mm.ss.SSSSSS", true),

          // SQL timestamp NOTE:imprecision for MSSQL SERVER
          new TemporalFormatter("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}\\:\\d{2}\\:\\d{2}\\.\\d{3,}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                  .appendFraction(NANO_OF_SECOND, 3, 9, true).toFormatter(),
              "yyyy-MM-dd HH:mm:ss.[S...]", true),

          // 星期三, 14 十一月 1979 11:14:08
          new TemporalFormatter(
              "^([\u4E00-\u9FA5]{3}\\,\\s)?\\d{1,2}\\s[\u4E00-\u9FA5]{2,3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}(.*)?$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().optionalStart()
                  .appendText(DAY_OF_WEEK, DEFAULT_ZH_DAY_OF_WEEK_LP).appendLiteral(", ")
                  .optionalEnd().appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral(' ').appendText(MONTH_OF_YEAR, DEFAULT_ZH_MONTH_OF_YEAR_LP)
                  .appendLiteral(' ').appendValue(YEAR, 4).appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                  .optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).optionalEnd()
                  .optionalStart().appendLiteral(' ').appendOffset("+HHMM", "GMT").optionalEnd()
                  .toFormatter(),
              "RFC_1123_DATE_TIME(ZH)", true),

          // Wed, 14 Nov 1979 11:14:08
          new TemporalFormatter(
              "^([a-zA-Z]{3}\\,\\s)?\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().optionalStart()
                  .appendText(DAY_OF_WEEK, DEFAULT_DAY_OF_WEEK_LP).appendLiteral(", ").optionalEnd()
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, DEFAULT_MONTH_OF_YEAR_LP).appendLiteral(' ')
                  .appendValue(YEAR, 4).appendLiteral(' ').appendValue(HOUR_OF_DAY, 2)
                  .appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).optionalStart()
                  .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).optionalEnd().optionalStart()
                  .appendLiteral(' ').appendOffset("+HHMM", "GMT").optionalEnd().toFormatter(),
              "RFC_1123_DATE_TIMEX", true),

          // Wed, 14 Nov 1979 11:14:08 GMT
          new TemporalFormatter(
              "^([a-zA-Z]{3}\\,\\s)?\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[a-zA-Z]{1,}$",
              DateTimeFormatter.RFC_1123_DATE_TIME, "RFC_1123_DATE_TIME", true),

          // Wed Nov 14 11:26:28 CST 1979 java.util.Date string
          new TemporalFormatter(
              "^[a-zA-Z]{3}\\s[a-zA-Z]{3}\\s\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Z]{1,3}\\s\\d{4}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient()
                  .appendText(DAY_OF_WEEK, DEFAULT_DAY_OF_WEEK_LP).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, DEFAULT_MONTH_OF_YEAR_LP).appendLiteral(' ')
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                  .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).appendLiteral(' ')
                  .appendZoneText(TextStyle.SHORT).appendLiteral(' ').appendValue(YEAR, 4)
                  .toFormatter(),
              "java.util.Date().toString()", true),

          // 星期三 十一月 14 11:26:28 CST 1979 java.util.Date string
          new TemporalFormatter(
              "^[\u4E00-\u9FA5]{3}\\s[\u4E00-\u9FA5]{3}\\s\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Z]{1,3}\\s\\d{4}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient()
                  .appendText(DAY_OF_WEEK, DEFAULT_ZH_DAY_OF_WEEK_LP).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, DEFAULT_ZH_MONTH_OF_YEAR_LP).appendLiteral(' ')
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                  .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).appendLiteral(' ')
                  .appendZoneText(TextStyle.SHORT).appendLiteral(' ').appendValue(YEAR, 4)
                  .toFormatter(),
              "ZH java.util.Date().toString()", true)

      );

  /**
   *
   */
  protected AbstractTemporalConverter() {
    super();
  }

  /**
   * @param throwException
   */
  protected AbstractTemporalConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @param defaultValue
   */
  protected AbstractTemporalConverter(T defaultValue) {
    super(defaultValue);
  }

  /**
   * @param defaultValue
   * @param throwException
   */
  protected AbstractTemporalConverter(T defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  public static Optional<TemporalFormatter> decideFormatter(String value) {
    return decideFormatters(value).findFirst();
  }

  public static Stream<TemporalFormatter> decideFormatters(String value) {
    return DEFAULT_FORMATTERS.stream().filter(tm -> tm.match(value));
  }

  protected Optional<DateTimeFormatter> resolveHintFormatter(Map<String, ?> hints) {
    DateTimeFormatter dtf = ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_FMT_KEY);
    if (dtf == null) {
      String dtfPtn = ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_FMT_PTN_KEY);
      if (dtfPtn != null) {
        if (ConverterHints.getHint(hints, ConverterHints.CVT_LOCAL_KEY) != null) {
          dtf = DateTimeFormatter.ofPattern(dtfPtn,
              ConverterHints.getHint(hints, ConverterHints.CVT_LOCAL_KEY));
        } else {
          dtf = DateTimeFormatter.ofPattern(dtfPtn);
        }
      }
    }
    return Optional.ofNullable(dtf);
  }

  protected Optional<ZoneId> resolveHintZoneId(Map<String, ?> hints) {
    ZoneId zoneId = null;
    Object hintZoneId = ConverterHints.getHint(hints, ConverterHints.CVT_ZONE_ID_KEY);
    if (hintZoneId instanceof ZoneId) {
      zoneId = (ZoneId) hintZoneId;
    } else if (hintZoneId instanceof String) {
      zoneId = ZoneId.of(hintZoneId.toString());
    }
    return Optional.ofNullable(zoneId);
  }

  public static class TemporalFormatter {
    final String regex;
    final Pattern pattern;
    final DateTimeFormatter formatter;
    final boolean withTime;
    final String description;
    final Locale locale;

    TemporalFormatter(String regex, DateTimeFormatter formatter, String description,
        boolean withTime) {
      this(regex, formatter, description, Locale.getDefault(), withTime);
    }

    TemporalFormatter(String regex, DateTimeFormatter formatter, String description, Locale locale,
        boolean withTime) {
      this.regex = regex;
      this.formatter = formatter;
      pattern = Pattern.compile(regex);
      this.withTime = withTime;
      this.description = description;
      this.locale = locale;
    }

    TemporalFormatter(String regex, String pattern, boolean withTime) {
      this(regex, pattern, Locale.getDefault(), withTime);
    }

    TemporalFormatter(String regex, String pattern, Locale locale, boolean withTime) {
      this(regex, new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern)
          .toFormatter(locale), pattern, withTime);
    }

    public boolean find(String value) {
      return isNotBlank(value) && pattern.matcher(value.trim()).find();
    }

    public String getDescription() {
      return description;
    }

    public DateTimeFormatter getFormatter() {
      return formatter;
    }

    public Locale getLocale() {
      return locale;
    }

    public Pattern getPattern() {
      return pattern;
    }

    public String getRegex() {
      return regex;
    }

    public boolean isWithTime() {
      return withTime;
    }

    public boolean match(String value) {
      return isNotBlank(value) && pattern.matcher(value.trim()).matches();
    }

  }

}

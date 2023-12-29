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
 */
public abstract class AbstractTemporalConverter<S, T extends Temporal>
    extends AbstractConverter<S, T> {

  public static final Map<Long, String> DEFAULT_ZH_DAY_OF_WEEK_LP = // NOSONAR
      immutableMapOf(1L, "星期一", 2L, "星期二", 3L, "星期三", 4L, "星期四", 5L, "星期五", 6L, "星期六", 7L, "星期日");

  public static final Map<Long, String> DEFAULT_ZH_MONTH_OF_YEAR_LP = // NOSONAR
      immutableMapOf(1L, "一月", 2L, "二月", 3L, "三月", 4L, "四月", 5L, "五月", 6L, "六月", 7L, "七月", 8L, "八月",
          9L, "九月", 10L, "十月", 11L, "十一月", 12L, "十二月");

  public static final List<TemporalFormatter> DEFAULT_FORMATTERS = // NOSONAR

      immutableListOf(

          new TemporalFormatter("^\\d{2}-\\d{2}-\\d{4}$", "dd-MM-yyyy", false, 10, 10),

          new TemporalFormatter("^\\d{4}-\\d{2}-\\d{2}(\\+\\d{2}:\\d{2})?$",
              DateTimeFormatter.ISO_DATE, "yyyy-MM-dd", false, 10, 10),

          new TemporalFormatter("^\\d{2}/\\d{2}/\\d{4}$", "MM/dd/yyyy", false, 10, 10),

          new TemporalFormatter("^\\d{4}/\\d{2}/\\d{2}$", "yyyy/MM/dd", false, 10, 10),

          new TemporalFormatter("^\\d{4}\\.\\d{1,2}\\.\\d{1,2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('.').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('.').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .toFormatter(),
              "yyyy.MM.dd", false, 8, 10),

          // 1979年11月14日
          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').toFormatter(),
              "yyyy年MM月dd日", false, 9, 11),

          // 14 Nov 1979 only for US
          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(' ')
                  .appendValue(YEAR, 4).toFormatter(Locale.US),
              "dd MMM yyyy", false, 10, 11),

          // 14-Nov-1979 only for US
          new TemporalFormatter("^\\d{2}-[a-zA-Z]{3}-\\d{4}$", "dd-MMM-yyyy", Locale.US, false, 11,
              11),

          // 14 November 1979 only for US
          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{4,9}\\s\\d{4}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, TextStyle.FULL).appendLiteral(' ').appendValue(YEAR, 4)
                  .toFormatter(Locale.US),
              "dd MMMM yyyy", false, 10, 17),

          // ISO Week dates
          new TemporalFormatter("^\\d{4}-W\\d{2}-\\d{1}$", DateTimeFormatter.ISO_WEEK_DATE,
              "yyyy-Www-D", false, 10, 10),

          // ISO Week dates
          new TemporalFormatter("^\\d{4}W\\d{2}\\d{1}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .appendValue(IsoFields.WEEK_BASED_YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
                  .appendLiteral("W").appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2)
                  .appendValue(DAY_OF_WEEK, 1).toFormatter(),
              "yyyyWwwD", false, 8, 8),

          new TemporalFormatter("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm", true, 13, 13),

          new TemporalFormatter("^\\d{1}-\\d{1}-\\d{4}\\s\\d{1}:\\d{2}$", "dd-MM-yyyy HH:mm", true,
              16, 16),

          new TemporalFormatter("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm", true,
              16, 16),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\s\\d{1,2}时\\d{1,2}分$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral('时')
                  .appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral('分')
                  .toFormatter(),
              "yyyy年MM月dd日 HH时mm分", true, 14, 18),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\s\\d{1,2}:\\d{1,2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
                  .appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE).toFormatter(),
              "yyyy年MM月dd日 HH:mm", true, 13, 17),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\d{1,2}时\\d{1,2}分$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('时').appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('分').toFormatter(),
              "yyyy年MM月dd日HH时mm分", true, 13, 17),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\d{1,2}点\\d{1,2}分$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('点').appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('分').toFormatter(),
              "yyyy年MM月dd日HH点mm分", true, 13, 17),

          new TemporalFormatter("^\\d{2}/\\d{2}/\\d{4}\\s\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm", true,
              16, 16),

          new TemporalFormatter("^\\d{4}/\\d{2}/\\d{2}\\s\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm", true,
              16, 16),

          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{1,2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(' ')
                  .appendValue(YEAR, 4).appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
                  .appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE).toFormatter(Locale.US),
              "dd MMM yyyy HH:mm", true, 14, 17),

          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{4,9}\\s\\d{4}\\s\\d{1,2}:\\d{1,2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, TextStyle.FULL).appendLiteral(' ').appendValue(YEAR, 4)
                  .appendLiteral(' ').appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral(':').appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .toFormatter(Locale.US),
              "dd MMMM yyyy HH:mm", true, 14, 23),

          new TemporalFormatter("^\\d{8}$", DateTimeFormatter.BASIC_ISO_DATE, "yyyyMMdd", false, 8,
              8),

          new TemporalFormatter("^\\d{12}$", "yyyyMMddHHmm", true, 12, 12),

          new TemporalFormatter("^\\d{14}$", "yyyyMMddHHmmss", true, 14, 14),

          new TemporalFormatter("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss", true, 15, 15),

          new TemporalFormatter("^\\d{2}-\\d{2}-\\d{4}\\s\\d{2}:\\d{2}:\\d{2}$",
              "dd-MM-yyyy HH:mm:ss", true, 19, 19),

          new TemporalFormatter("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}$",
              "yyyy-MM-dd HH:mm:ss", true, 19, 19),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\s\\d{1,2}时\\d{1,2}分\\d{1,2}秒$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral('时')
                  .appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral('分')
                  .appendValue(SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral('秒')
                  .toFormatter(),
              "yyyy年MM月dd日 HH时mm分ss秒", true, 16, 21),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\s\\d{1,2}:\\d{1,2}:\\d{1,2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
                  .appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
                  .appendValue(SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE).toFormatter(),
              "yyyy年MM月dd日 HH:mm:ss", true, 15, 20),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\d{1,2}时\\d{1,2}分\\d{1,2}秒$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('时').appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('分').appendValue(SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('秒').toFormatter(),
              "yyyy年MM月dd日HH时mm分ss秒", true, 15, 20),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\s\\d{1,2}点\\d{1,2}分\\d{1,2}秒$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral('点')
                  .appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral('分')
                  .appendValue(SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral('秒')
                  .toFormatter(),
              "yyyy年MM月dd日 HH点mm分ss秒", true, 16, 21),

          new TemporalFormatter("^\\d{4}年\\d{1,2}月\\d{1,2}日\\d{1,2}点\\d{1,2}分\\d{1,2}秒$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
                  .appendLiteral('年').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('月').appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('日').appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('点').appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('分').appendValue(SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral('秒').toFormatter(),
              "yyyy年MM月dd日HH点mm分ss秒", true, 15, 20),

          // ISO_INSTANT
          new TemporalFormatter("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$",
              DateTimeFormatter.ISO_INSTANT, "ISO_INSTANT yyyy-MM-ddTHH:mm:ssZ", true, 20, 20),

          // ISO_DATE_TIME
          new TemporalFormatter("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(.*)?$",
              DateTimeFormatter.ISO_DATE_TIME, "ISO_DATE_TIME yyyy-MM-ddTHH:mm:ss+o[z]", true, 21,
              Integer.MAX_VALUE),

          new TemporalFormatter("^\\d{2}/\\d{2}/\\d{4}\\s\\d{2}:\\d{2}:\\d{2}$",
              "MM/dd/yyyy HH:mm:ss", true, 19, 19),

          new TemporalFormatter("^\\d{4}/\\d{2}/\\d{2}\\s\\d{2}:\\d{2}:\\d{2}$",
              "yyyy/MM/dd HH:mm:ss", true, 19, 19),

          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(' ')
                  .appendValue(YEAR, 4).appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
                  .appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
                  .appendValue(SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE)
                  .toFormatter(Locale.US),
              "dd MMM yyyy HH:mm:ss", true, 16, 20),

          new TemporalFormatter("^\\d{1,2}\\s[a-zA-Z]{4,9}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, TextStyle.FULL).appendLiteral(' ').appendValue(YEAR, 4)
                  .appendLiteral(' ').appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral(':').appendValue(MINUTE_OF_HOUR, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 1, 2, SignStyle.NOT_NEGATIVE)
                  .toFormatter(Locale.US),
              "dd MMMM yyyy HH:mm:ss", true, 16, 26),

          new TemporalFormatter("^\\d{4}-\\d{2}-\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{1,6}$",
              "yyyy-MM-dd-HH.mm.ss.SSSSSS", true, 21, 26),

          // SQL timestamp NOTE:imprecision for MSSQL SERVER
          new TemporalFormatter("^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}\\:\\d{2}\\:\\d{2}\\.\\d{1,9}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive()
                  .append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                  .appendFraction(NANO_OF_SECOND, 1, 9, true).toFormatter(),
              "yyyy-MM-dd HH:mm:ss.[S...]", true, 21, 29),

          // 星期三, 14 十一月 1979 11:14:08
          new TemporalFormatter(
              "^([\u4E00-\u9FA5]{3}\\,\\s)?\\d{1,2}\\s[\u4E00-\u9FA5]{2,3}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}(.*)?$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().optionalStart()
                  .appendText(DAY_OF_WEEK, DEFAULT_ZH_DAY_OF_WEEK_LP).appendLiteral(", ")
                  .optionalEnd().appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                  .appendLiteral(' ').appendText(MONTH_OF_YEAR, DEFAULT_ZH_MONTH_OF_YEAR_LP)
                  .appendLiteral(' ').appendValue(YEAR, 4).appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
                  .optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).optionalEnd()
                  .optionalStart().appendLiteral(' ').appendOffset("+HHMM", "GMT").optionalEnd()
                  .toFormatter(),
              "RFC_1123_DATE_TIME(ZH)", true, 23, 25),

          // Wed, 14 Nov 1979 11:14:08
          new TemporalFormatter(
              "^([a-zA-Z]{3}\\,\\s)?\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient().optionalStart()
                  .appendText(DAY_OF_WEEK, TextStyle.SHORT).appendLiteral(", ").optionalEnd()
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(' ')
                  .appendValue(YEAR, 4).appendLiteral(' ').appendValue(HOUR_OF_DAY, 2)
                  .appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).optionalStart()
                  .appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2).optionalEnd().optionalStart()
                  .appendLiteral(' ').appendOffset("+HHMM", "GMT").optionalEnd()
                  .toFormatter(Locale.US),
              "RFC_1123_DATE_TIMEX", true, 24, 25),

          // Wed, 14 Nov 1979 11:14:08 GMT
          new TemporalFormatter(
              "^([a-zA-Z]{3}\\,\\s)?\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\s[a-zA-Z]{1,}$",
              DateTimeFormatter.RFC_1123_DATE_TIME, "RFC_1123_DATE_TIME", true, 26,
              Integer.MAX_VALUE),

          // Wed Nov 14 11:26:28 CST 1979 java.util.Date string
          new TemporalFormatter(
              "^[a-zA-Z]{3}\\s[a-zA-Z]{3}\\s\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Z]{1,3}\\s\\d{4}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient()
                  .appendText(DAY_OF_WEEK, TextStyle.SHORT).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, TextStyle.SHORT).appendLiteral(' ')
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
                  .appendValue(MINUTE_OF_HOUR, 2).appendLiteral(':')
                  .appendValue(SECOND_OF_MINUTE, 2).appendLiteral(' ')
                  .appendZoneText(TextStyle.SHORT).appendLiteral(' ').appendValue(YEAR, 4)
                  .toFormatter(Locale.US),
              "java.util.Date().toString()", true, 24, 28),

          // 星期三 十一月 14 11:26:28 CST 1979 java.util.Date string
          new TemporalFormatter(
              "^[\u4E00-\u9FA5]{3}\\s[\u4E00-\u9FA5]{2,3}\\s\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}\\s[A-Z]{1,3}\\s\\d{4}$",
              new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient()
                  .appendText(DAY_OF_WEEK, DEFAULT_ZH_DAY_OF_WEEK_LP).appendLiteral(' ')
                  .appendText(MONTH_OF_YEAR, DEFAULT_ZH_MONTH_OF_YEAR_LP).appendLiteral(' ')
                  .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(' ')
                  .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).appendLiteral(':')
                  .appendValue(MINUTE_OF_HOUR, 2).appendLiteral(':')
                  .appendValue(SECOND_OF_MINUTE, 2).appendLiteral(' ')
                  .appendZoneText(TextStyle.SHORT).appendLiteral(' ').appendValue(YEAR, 4)
                  .toFormatter(),
              "ZH java.util.Date().toString()", true, 23, 28)

      );

  /**
   * @see AbstractConverter#AbstractConverter()
   */
  protected AbstractTemporalConverter() {}

  /**
   * @see AbstractConverter#AbstractConverter(boolean)
   */
  protected AbstractTemporalConverter(boolean throwException) {
    super(throwException);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object)
   */
  protected AbstractTemporalConverter(T defaultValue) {
    super(defaultValue);
  }

  /**
   * @see AbstractConverter#AbstractConverter(Object,boolean)
   */
  protected AbstractTemporalConverter(T defaultValue, boolean throwException) {
    super(defaultValue, throwException);
  }

  public static Optional<TemporalFormatter> decideFormatter(String value) {
    return decideFormatters(value).findFirst();
  }

  public static Stream<TemporalFormatter> decideFormatters(String value) {
    return DEFAULT_FORMATTERS.stream().filter(tm -> {
      int len = value.length();
      return len >= tm.getMinLength() && len <= tm.getMaxLength();
    }).filter(tm -> tm.match(value));
  }

  public static Optional<DateTimeFormatter> resolveHintFormatter(Map<String, ?> hints) {
    DateTimeFormatter dtf = ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_FMT_KEY);
    if (dtf == null) {
      String dtfPtn = ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_FMT_PTN_KEY);
      if (dtfPtn != null) {
        if (ConverterHints.getHint(hints, ConverterHints.CVT_LOCALE_KEY) != null) {
          dtf = DateTimeFormatter.ofPattern(dtfPtn,
              ConverterHints.getHint(hints, ConverterHints.CVT_LOCALE_KEY));
        } else {
          dtf = DateTimeFormatter.ofPattern(dtfPtn);
        }
      }
    }
    return Optional.ofNullable(dtf);
  }

  public static Optional<ZoneId> resolveHintZoneId(Map<String, ?> hints) {
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
    final int minLength;
    final int maxLength;

    TemporalFormatter(String regex, DateTimeFormatter formatter, String description,
        boolean withTime, int minLength, int maxLength) {
      this.regex = regex;
      this.formatter = formatter.withZone(ZoneId.systemDefault());
      pattern = Pattern.compile(regex);
      this.withTime = withTime;
      this.description = description;
      this.minLength = minLength;
      this.maxLength = maxLength;
    }

    TemporalFormatter(String regex, String pattern, boolean withTime, int minLength,
        int maxLength) {
      this(regex, pattern, Locale.getDefault(), withTime, minLength, maxLength);
    }

    TemporalFormatter(String regex, String pattern, Locale locale, boolean withTime, int minLength,
        int maxLength) {
      this(regex, new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern)
          .toFormatter(locale), pattern, withTime, minLength, maxLength);
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

    public int getMaxLength() {
      return maxLength;
    }

    public int getMinLength() {
      return minLength;
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

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
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static org.corant.shared.util.CollectionUtils.immutableListOf;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
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

  static final List<TemporalMatcher> mathers = immutableListOf(
      new TemporalMatcher("^\\d{8}$", DateTimeFormatter.BASIC_ISO_DATE, "yyyyMMdd", false),
      new TemporalMatcher("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy", false),
      new TemporalMatcher("^\\d{4}-\\d{1,2}-\\d{1,2}(\\+\\d{2}:\\d{2})?$",
          DateTimeFormatter.ISO_DATE, "yyyy-MM-dd", false),
      new TemporalMatcher("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy", false),
      new TemporalMatcher("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd", false),
      new TemporalMatcher("^\\d{4}\\.\\d{1,2}\\.\\d{1,2}$",
          new DateTimeFormatterBuilder().parseCaseInsensitive().appendValue(YEAR, 4)
              .appendLiteral('.').appendValue(MONTH_OF_YEAR, 2).appendLiteral('.')
              .appendValue(DAY_OF_MONTH, 2).toFormatter(),
          "yyyy.MM.dd", false),
      // 27 Sep 2015 only for US
      new TemporalMatcher("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}$", "dd MMM yyyy", Locale.US, false),
      // 27-Sep-2015 only for US
      new TemporalMatcher("^\\d{1,2}-[a-zA-Z]{3}-\\d{4}$", "dd-MMM-yyyy", Locale.US, false),
      // 27 June 2015 only for US
      new TemporalMatcher("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}$", "dd MMMM yyyy", Locale.US, false),
      // ISO Week dates
      new TemporalMatcher("^\\d{4}-W\\d{2}-\\d{1}$", DateTimeFormatter.ISO_WEEK_DATE, "yyyy-Www-D",
          false),
      // ISO Week dates
      new TemporalMatcher("^\\d{4}W\\d{2}\\d{1}$",
          new DateTimeFormatterBuilder().parseCaseInsensitive()
              .appendValue(IsoFields.WEEK_BASED_YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
              .appendLiteral("W").appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR, 2)
              .appendValue(DAY_OF_WEEK, 1).toFormatter(),
          "yyyyWwwD", false),
      new TemporalMatcher("^\\d{12}$", "yyyyMMddHHmm", true),
      new TemporalMatcher("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm", true),
      new TemporalMatcher("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm", true),
      new TemporalMatcher("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm", true),
      new TemporalMatcher("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm", true),
      new TemporalMatcher("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm", true),
      new TemporalMatcher("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$",
          "dd MMM yyyy HH:mm", Locale.US, true),
      new TemporalMatcher("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$",
          "dd MMMM yyyy HH:mm", Locale.US, true),
      new TemporalMatcher("^\\d{14}$", "yyyyMMddHHmmss", true),
      new TemporalMatcher("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss", true),
      new TemporalMatcher("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
          "dd-MM-yyyy HH:mm:ss", true),
      new TemporalMatcher("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$",
          "yyyy-MM-dd HH:mm:ss", true),

      // ISO_INSTANT
      new TemporalMatcher("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}Z$",
          DateTimeFormatter.ISO_INSTANT, "yyyy-MM-ddTHH:mm:ssZ", true),
      // ISO_DATE_TIME
      new TemporalMatcher("^\\d{4}-\\d{1,2}-\\d{1,2}T\\d{1,2}:\\d{2}:\\d{2}(.*)?",
          DateTimeFormatter.ISO_DATE_TIME, "yyyy-MM-ddTHH:mm:ss+o[z]", true),

      new TemporalMatcher("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
          "MM/dd/yyyy HH:mm:ss", true),
      new TemporalMatcher("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$",
          "yyyy/MM/dd HH:mm:ss", true),
      new TemporalMatcher("^\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
          "dd MMM yyyy HH:mm:ss", Locale.US, true),
      new TemporalMatcher("^\\d{1,2}\\s[a-zA-Z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$",
          "dd MMMM yyyy HH:mm:ss", Locale.US, true),
      new TemporalMatcher("^\\d{4}-\\d{1,2}-\\d{1,2}-\\d{1,2}\\.\\d{2}\\.\\d{2}\\.\\d{1,6}$",
          "yyyy-MM-dd-HH.mm.ss.SSSSSS", true),
      // Tue, 3 Jun 2008 11:05:30 GMT
      new TemporalMatcher(
          "^[a-zA-Z]{3}\\,\\s\\d{1,2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}\\s(.*)?",
          DateTimeFormatter.RFC_1123_DATE_TIME, "RFC_1123_DATE_TIME", true)

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

  // public static void main(String... strings) {
  // String s = "";
  // final String s = "2011-12-03T10:15:30+01:00[Europe/Paris]";//
  // ISO_DATE_TIME,ISO_ZONED_DATE_TIME
  // final String s = "2011-12-03T10:15:30Z";// ISO_INSTANT
  // final String s = "2011-12-03T10:15:30";// ISO_DATE_TIME
  // final String s = "2011-12-03T10:15:30+01:00";// ISO_DATE_TIME,ISO_OFFSET_DATE_TIME
  // final String s = "2011-12-03"; // ISO_DATE
  // final String s = "20111203"; // BASIC_ISO_DATE
  // final String s = "2011-12-03+01:00";// ISO_OFFSET_DATE,ISO_DATE
  // final String s = "31-12-2019";
  // final String s = "12/31/2019";
  // final String s = "2016/12/31";
  // final String s = "2016.12.31";
  // final String s = "27 June 2015";
  // final String s = "2004-W19-1";
  // final String s = "2004W191";
  // final String s = "200411142321";
  // final String s = "20041114 2321";
  // final String s = "14-11-2011 23:21";
  // final String s = "2011-11-14 23:21";
  // final String s = "11/14/1979 23:21";
  // final String s = "1979/11/14 23:21";
  // final String s = "27 june 2015 23:21";
  // final String s = "27 may 2015 23:21";
  // final String s = "20041114232123";
  // final String s = "20041114 232123";
  // final String s = "14-11-2011 23:21:10";
  // final String s = "2011-11-14 23:21:19";
  // final String s = "11/14/1979 23:21:13";
  // final String s = "1979/11/14 23:21:13";
  // final String s = "27 JUNE 2015 23:21:17";
  // final String s = "2016-01-19-09.55.00.000000";
  // mathers.stream().filter(t -> t.match(s)).forEach(t -> {
  // System.out.println(t.regex + "\t\t" + s + "\t\t" + t.patternString);
  // System.out
  // .println(t.formatter.parseBest(s, LocalDateTime::from, Instant::from, LocalDate::from));
  // });
  //
  // }

  protected Optional<TemporalMatcher> decideMatcher(String value) {
    return decideMatchers(value).findFirst();
  }

  protected Stream<TemporalMatcher> decideMatchers(String value) {
    return mathers.stream().filter(tm -> tm.match(value));
  }

  protected Optional<DateTimeFormatter> resolveHintFormatter(Map<String, ?> hints) {
    DateTimeFormatter dtf = ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_FMT_KEY);
    if (dtf == null) {
      String dtfPtn = ConverterHints.getHint(hints, ConverterHints.CVT_TEMPORAL_FMT_PTN_KEY);
      if (dtfPtn != null) {
        dtf = DateTimeFormatter.ofPattern(dtfPtn);
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

  static class TemporalMatcher {
    final String regex;
    final Pattern pattern;
    final DateTimeFormatter formatter;
    final boolean withTime;
    final String patternString;
    final Locale locale;

    TemporalMatcher(String regex, DateTimeFormatter formatter, String patternString,
        boolean withTime) {
      this(regex, formatter, patternString, Locale.getDefault(), withTime);
    }

    TemporalMatcher(String regex, DateTimeFormatter formatter, String patternString, Locale locale,
        boolean withTime) {
      this.regex = regex;
      this.formatter = formatter;
      pattern = Pattern.compile(regex);
      this.withTime = withTime;
      this.patternString = patternString;
      this.locale = locale;
    }

    TemporalMatcher(String regex, String pattern, boolean withTime) {
      this(regex, pattern, Locale.getDefault(), withTime);
    }

    TemporalMatcher(String regex, String pattern, Locale locale, boolean withTime) {
      this(regex, new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern)
          .toFormatter(locale), pattern, withTime);
    }

    public boolean find(String value) {
      return pattern.matcher(value).find();
    }

    public boolean match(String value) {
      return pattern.matcher(value).matches();
    }
  }

}

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
package org.corant.shared.util;

import static org.corant.shared.util.Conversions.toBigDecimal;
import static org.corant.shared.util.Conversions.toBigInteger;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Conversions.toInstant;
import static org.corant.shared.util.Conversions.toInteger;
import static org.corant.shared.util.Conversions.toLocalDate;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.mapOf;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.corant.shared.conversion.ConverterHints;
import org.corant.shared.conversion.converter.AbstractTemporalConverter;
import org.corant.shared.conversion.converter.AbstractTemporalConverter.TemporalFormatter;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Resources.SourceType;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 下午8:17:00
 *
 */
public class ConversionsTest extends TestCase {

  @Test
  public void testDateInstant() {
    Date date = new Date();
    Instant instant = date.toInstant();
    assertEquals(toInstant(date), instant);
  }

  @Test
  public void testEnum() {
    int idx = 0;
    assertEquals(Conversions.toEnum(idx, SourceType.class), SourceType.FILE_SYSTEM);
    String s = "SourceType.file_system";
    assertEquals(Conversions.toEnum(s, SourceType.class), SourceType.FILE_SYSTEM);
    s = "SourceType.FILE_SYSTEM";
    assertEquals(Conversions.toEnum(s, SourceType.class), SourceType.FILE_SYSTEM);
  }

  @Test
  public void testInstantTimestamp() {
    long num = System.currentTimeMillis();
    Instant instant = toInstant(num);
    Timestamp tt = new Timestamp(num);
    assertEquals(toObject(instant, Timestamp.class), tt);
  }

  @Test
  public void testLocalDate() {
    List<Integer> list = listOf(2020, 6, 28);
    Map<String, Object> map = mapOf("year", 2020, "month", 6, "dayOfMonth", 28);
    assertEquals(Conversions.toLocalDate(list), Conversions.toLocalDate(map));
    assertEquals(Conversions.toLocalDate(list), Conversions.toLocalDate(new int[] {2020, 6, 28}));
    assertEquals(Conversions.toLocalDate(list), Conversions.toLocalDate("2020-06-28"));
    List<List<Integer>> lists =
        listOf(listOf(2020, 6, 28), listOf(2020, 6, 28), listOf(2020, 6, 28));
    List<Map<String, Object>> maps = listOf(mapOf("year", 2020, "month", 6, "dayOfMonth", 28),
        mapOf("year", 2020, "month", 6, "dayOfMonth", 28),
        mapOf("year", 2020, "month", 6, "dayOfMonth", 28));
    Conversions.toList(lists, Conversions::toLocalDate)
        .forEach(r -> assertEquals(r, Conversions.toLocalDate("2020-06-28")));
    Conversions.toList(maps, Conversions::toLocalDate)
        .forEach(r -> assertEquals(r, Conversions.toLocalDate("2020-06-28")));
  }

  @Test
  public void testLocalDateSqlDate() {
    java.sql.Date date = java.sql.Date.valueOf("2020-06-29");
    assertEquals(toObject(toLocalDate(listOf(2020, 6, 29)), java.sql.Date.class), date);
  }

  @Test
  public void testLocalDateTimeTimestamp() {
    LocalDateTime ldt = LocalDateTime.of(2020, 6, 29, 10, 0, 0, 333330000);
    Timestamp tt = Timestamp.valueOf("2020-06-29 10:00:00.33333");
    assertEquals(toObject(ldt, Timestamp.class), tt);
  }

  @Test
  public void testNumberToNumber() {
    double d = 123.123d;
    float f = 234.12f;
    short s = 12;
    int i = 12345;
    byte b = 10;
    long l = 12345678L;

    assertTrue(toBigDecimal(d).compareTo(new BigDecimal("123.123")) == 0);
    assertTrue(toBigDecimal(f).compareTo(new BigDecimal("234.12")) == 0);
    assertTrue(toBigDecimal(s).compareTo(new BigDecimal("12")) == 0);
    assertTrue(toBigDecimal(i).compareTo(new BigDecimal("12345")) == 0);
    assertTrue(toBigDecimal(b).compareTo(new BigDecimal("10")) == 0);
    assertTrue(toBigDecimal(l).compareTo(new BigDecimal("12345678")) == 0);

    assertTrue(toBigInteger(s).compareTo(new BigInteger("12")) == 0);
    assertTrue(toBigInteger(i).compareTo(new BigInteger("12345")) == 0);
    assertTrue(toBigInteger(b).compareTo(new BigInteger("10")) == 0);
    assertTrue(toBigInteger(l).compareTo(new BigInteger("12345678")) == 0);

    assertTrue(toBoolean(1));
    assertFalse(toBoolean(0));

    assertTrue(toObject(s, byte.class).compareTo((byte) 12) == 0);
    assertTrue(toObject(d, Byte.class).compareTo(Double.valueOf(d).byteValue()) == 0);
    assertTrue(toObject(d, double.class).compareTo(d) == 0);
    assertTrue(toObject(f, double.class).compareTo((double) f) == 0);
    assertTrue(toObject(i, double.class).compareTo(Integer.valueOf(i).doubleValue()) == 0);
    assertTrue(toObject(d, float.class).compareTo((float) d) == 0);
    assertTrue(toObject(f, float.class).compareTo(f) == 0);

    assertTrue(toObject(s, Integer.class).compareTo((int) s) == 0);
    assertTrue(toObject(i, Integer.class).compareTo(i) == 0);
    assertTrue(toObject(l, Integer.class).compareTo((int) l) == 0);

  }

  @Test
  public void testPatternStringTemporal() {
    List<Pair<String, String>> values = new ArrayList<>();
    values.add(Pair.of("19791114", "yyyyMMdd"));
    values.add(Pair.of("14-11-1979", "dd-MM-yyyy"));
    values.add(Pair.of("1979-11-14", "yyyy-MM-dd"));
    values.add(Pair.of("11/14/1979", "MM/dd/yyyy"));
    values.add(Pair.of("1979/11/14", "yyyy/MM/dd"));
    values.add(Pair.of("1979.11.14", "yyyy.MM.dd"));
    values.add(Pair.of("1979年11月14日", "yyyy年MM月dd日"));
    values.add(Pair.of("14 Nov 1979", "dd MMM yyyy"));
    values.add(Pair.of("14-Nov-1979", "dd-MMM-yyyy"));
    values.add(Pair.of("14 November 1979", "dd MMMM yyyy"));
    values.add(Pair.of("1979-W46-3", "yyyy-Www-D"));
    values.add(Pair.of("1979W463", "yyyyWwwD"));
    values.add(Pair.of("197911141114", "yyyyMMddHHmm"));
    values.add(Pair.of("19791114 1114", "yyyyMMdd HHmm"));
    values.add(Pair.of("14-11-1979 11:14", "dd-MM-yyyy HH:mm"));
    values.add(Pair.of("1979-11-14 11:14", "yyyy-MM-dd HH:mm"));
    values.add(Pair.of("1979年11月14日 11时14分", "yyyy年MM月dd日 HH时mm分"));
    values.add(Pair.of("11/14/1979 11:14", "MM/dd/yyyy HH:mm"));
    values.add(Pair.of("1979/11/14 11:14", "yyyy/MM/dd HH:mm"));
    values.add(Pair.of("14 Nov 1979 11:14", "dd MMM yyyy HH:mm"));
    values.add(Pair.of("14 November 1979 11:14", "dd MMMM yyyy HH:mm"));
    values.add(Pair.of("19791114111408", "yyyyMMddHHmmss"));
    values.add(Pair.of("19791114 111408", "yyyyMMdd HHmmss"));
    values.add(Pair.of("14-11-1979 11:14:08", "dd-MM-yyyy HH:mm:ss"));
    values.add(Pair.of("1979-11-14 11:14:08", "yyyy-MM-dd HH:mm:ss"));
    values.add(Pair.of("1979-11-14T11:14:08", "ISO_DATE_TIME yyyy-MM-ddTHH:mm:ss+o[z]"));
    values.add(Pair.of("1979-11-14T11:14:08Z", "ISO_INSTANT yyyy-MM-ddTHH:mm:ssZ"));
    values.add(Pair.of("1979-11-14T11:14:08.080Z", "ISO_DATE_TIME yyyy-MM-ddTHH:mm:ss+o[z]"));
    values.add(Pair.of("1979-11-14T11:14:08+08:00", "ISO_DATE_TIME yyyy-MM-ddTHH:mm:ss+o[z]"));
    values.add(Pair.of("1979-11-14T11:14:08+08:00[Asia/Shanghai]",
        "ISO_DATE_TIME yyyy-MM-ddTHH:mm:ss+o[z]"));
    values.add(Pair.of("1979年11月14日 11时14分08秒", "yyyy年MM月dd日 HH时mm分ss秒"));
    values.add(Pair.of("11/14/1979 11:14:08", "MM/dd/yyyy HH:mm:ss"));
    values.add(Pair.of("1979/11/14 11:14:08", "yyyy/MM/dd HH:mm:ss"));
    values.add(Pair.of("14 Nov 1979 11:14:08", "dd MMM yyyy HH:mm:ss"));
    values.add(Pair.of("14 November 1979 11:14:08", "dd MMMM yyyy HH:mm:ss"));
    values.add(Pair.of("1979-11-14-11.14.08.888888", "yyyy-MM-dd-HH.mm.ss.SSSSSS"));
    values.add(Pair.of("1979-11-14 11:14:08.8888", "yyyy-MM-dd HH:mm:ss.[S...]"));
    values.add(Pair.of("Wed, 14 Nov 1979 11:14:08 GMT", "RFC_1123_DATE_TIME"));
    values.add(Pair.of("Wed, 14 Nov 1979 11:14:08", "RFC_1123_DATE_TIMEX"));
    values.add(Pair.of("星期三, 14 十一月 1979 11:14:08 GMT", "RFC_1123_DATE_TIME(ZH)"));
    values.add(Pair.of("Wed Nov 14 11:14:08 GMT 1979", "java.util.Date().toString()"));
    values.add(Pair.of("星期三 十一月 14 11:26:28 CST 1979", "ZH java.util.Date().toString()"));
    values.forEach(v -> {
      Optional<TemporalFormatter> tf = AbstractTemporalConverter.decideFormatter(v.getKey());
      if (tf.isPresent()) {
        try {
          if (tf.get().isWithTime()) {
            tf.get().getFormatter().parseBest(v.getKey(), ZonedDateTime::from, Instant::from,
                LocalDateTime::from);
            assertEquals(tf.get().getDescription(), v.getValue());
          } else {
            DateTimeFormatter.ISO_DATE
                .format(tf.get().getFormatter().parse(v.getKey(), LocalDate::from));
            assertEquals(tf.get().getDescription(), v.getValue());
          }
        } catch (Exception e) {
          System.out.println("Error PTN:" + tf.get().getDescription());
          e.printStackTrace(); // NOSONAR
        }
      }
    });
  }

  @Test
  public void testStringObject() {
    String s = "PT15M";
    assertEquals(Conversions.toDuration(s), Duration.ofMinutes(15));
  }

  @Test
  public void testStringToNumber() {
    assertEquals(toInteger("0xff"), toInteger("255"));
    assertEquals(toObject("0xff", Integer.class, mapOf(ConverterHints.CVT_NUMBER_RADIX_KEY, 10)),
        toInteger("255"));
    assertEquals(toInteger("0377"), toInteger("0xff"));
    assertEquals(toObject("377", Integer.class, mapOf(ConverterHints.CVT_NUMBER_RADIX_KEY, 8)),
        toInteger("0xff"));
    assertEquals(toObject("ff", Integer.class, mapOf(ConverterHints.CVT_NUMBER_RADIX_KEY, 16)),
        toInteger("0xff"));
    assertEquals(toObject("1111111", Integer.class, mapOf(ConverterHints.CVT_NUMBER_RADIX_KEY, 2)),
        toInteger("127"));

    assertEquals(toBigInteger("0xff"), toBigInteger("255"));
    assertEquals(toObject("0xff", BigInteger.class, mapOf(ConverterHints.CVT_NUMBER_RADIX_KEY, 10)),
        toBigInteger("255"));
    assertEquals(toBigInteger("0377"), toBigInteger("0xff"));
    assertEquals(toObject("377", BigInteger.class, mapOf(ConverterHints.CVT_NUMBER_RADIX_KEY, 8)),
        toBigInteger("0xff"));
    assertEquals(toObject("ff", BigInteger.class, mapOf(ConverterHints.CVT_NUMBER_RADIX_KEY, 16)),
        toBigInteger("0xff"));
    assertEquals(
        toObject("1111111", BigInteger.class, mapOf(ConverterHints.CVT_NUMBER_RADIX_KEY, 2)),
        toBigInteger("127"));
  }
}

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
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.corant.shared.conversion.converter.AbstractTemporalConverter;
import org.corant.shared.conversion.converter.AbstractTemporalConverter.TemporalFormatter;
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
    List<String> values = new ArrayList<>();
    values.add("19791114");
    values.add("14-11-1979");
    values.add("1979-11-14");
    values.add("11/14/1979");
    values.add("1979/11/14");
    values.add("1979.11.14");
    values.add("1979年11月14日");
    values.add("14 Nov 1979");
    values.add("14-Nov-1979");
    values.add("14 November 1979");
    values.add("1979-W46-3");
    values.add("1979W463");
    values.add("197911141114");
    values.add("19791114 1114");
    values.add("14-11-1979 11:14");
    values.add("1979-11-14 11:14");
    values.add("1979年11月14日 11时14分");
    values.add("11/14/1979 11:14");
    values.add("1979/11/14 11:14");
    values.add("14 Nov 1979 11:14");
    values.add("14 November 1979 11:14");
    values.add("19791114111408");
    values.add("19791114 111408");
    values.add("14-11-1979 11:14:08");
    values.add("1979-11-14 11:14:08");
    values.add("1979-11-14T11:14:08");
    values.add("1979-11-14T11:14:08Z");
    values.add("1979-11-14T11:14:08.080Z");
    values.add("1979-11-14T11:14:08+08:00");
    values.add("1979-11-14T11:14:08+08:00[Asia/Shanghai]");
    values.add("14-11-1979 11:14:08");
    values.add("1979-11-14 11:14:08");
    values.add("1979年11月14日 11时14分08秒");
    values.add("11/14/1979 11:14:08");
    values.add("1979/11/14 11:14:08");
    values.add("14 Nov 1979 11:14:08");
    values.add("14 November 1979 11:14:08");
    values.add("1979-11-14-11.14.08.888888");
    values.add("1979-11-14 11:14:08.8888");
    values.add("Wed, 14 Nov 1979 11:14:08 GMT");
    values.add("Wed, 14 Nov 1979 11:14:08");
    values.add("星期三, 14 十一月 1979 11:14:08 GMT");
    values.add("Wed Nov 14 11:14:08 GMT 1979");
    values.add("星期三 十一月 14 11:26:28 CST 1979");
    StopWatch sw = StopWatch.press("Time use");
    values.forEach(v -> {
      Optional<TemporalFormatter> tf = AbstractTemporalConverter.decideFormatter(v);
      if (tf.isPresent()) {
        try {
          if (tf.get().isWithTime()) {
            TemporalAccessor ta = tf.get().getFormatter().parseBest(v, ZonedDateTime::from,
                Instant::from, LocalDateTime::from);
            String s = ta.toString();
            System.out.println(v + "\t=>\t" + s + "\t[" + ta.getClass() + "]\t\tPTN: "
                + tf.get().getDescription());
          } else {
            String s = DateTimeFormatter.ISO_DATE
                .format(tf.get().getFormatter().parse(v, LocalDate::from));
            System.out.println(v + "\t=>\t" + s + "\t\tPTN: " + tf.get().getDescription());
          }
        } catch (Exception e) {
          System.out.println("Error PTN:" + tf.get().getDescription());
          e.printStackTrace(); // NOSONAR
        }
      } else {
        System.out.println(String.format("Formatter [%s] not found!", v));
      }
    });
    sw.stop(t -> System.out.println(t.getName() + " : " + t.getTimeMillis() + " ms!"));
  }

  @Test
  public void testStringObject() {
    String s = "PT15M";
    assertEquals(Conversions.toDuration(s), Duration.ofMinutes(15));
  }
}

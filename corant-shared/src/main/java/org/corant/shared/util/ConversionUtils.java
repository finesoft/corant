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

import static org.corant.shared.util.MapUtils.mapOf;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StreamUtils.streamOf;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.corant.shared.conversion.Conversions;
import org.corant.shared.conversion.ConverterHints;

/**
 * corant-shared
 *
 * @author bingo 上午10:05:26
 *
 */
@SuppressWarnings("unchecked")
public class ConversionUtils {

  private ConversionUtils() {
    super();
  }

  public static BigDecimal toBigDecimal(Object obj) {
    return toBigDecimal(obj, null);
  }

  public static BigDecimal toBigDecimal(Object obj, BigDecimal altVal) {
    return defaultObject(Conversions.convert(obj, BigDecimal.class), altVal);
  }

  public static BigDecimal toBigDecimal(Object obj, BigDecimal altVal, int scale,
      int roundingMode) {
    BigDecimal d = defaultObject(Conversions.convert(obj, BigDecimal.class), altVal);
    return d == null ? null : d.setScale(scale, roundingMode);
  }

  public static BigDecimal toBigDecimal(Object obj, int scale) {
    BigDecimal d = toBigDecimal(obj, null);
    return d == null ? null : d.setScale(scale, BigDecimal.ROUND_HALF_UP);
  }

  public static List<BigDecimal> toBigDecimalList(Object obj) {
    return toList(obj, BigDecimal.class);
  }

  public static BigInteger toBigInteger(Object obj) {
    return toBigInteger(obj, null);
  }

  public static BigInteger toBigInteger(Object obj, BigInteger altVal) {
    return defaultObject(Conversions.convert(obj, BigInteger.class), altVal);
  }

  public static List<BigInteger> toBigIntegerList(Object obj) {
    return toList(obj, BigInteger.class);
  }

  public static Boolean toBoolean(Object obj) {
    return defaultObject(Conversions.convert(obj, Boolean.class), Boolean.FALSE);
  }

  public static Byte toByte(Object obj) {
    return toByte(obj, null);
  }

  public static Byte toByte(Object obj, Byte altVal) {
    return defaultObject(Conversions.convert(obj, Byte.class), altVal);
  }

  public static Character toCharacter(Object obj) {
    return Conversions.convert(obj, Character.class, null);
  }

  public static Class<?> toClass(Object obj) {
    return Conversions.convert(obj, Class.class);
  }

  public static <T, C extends Collection<T>> C toCollection(Object obj, Class<T> itemClass,
      Supplier<C> collectionFactory) {
    return Conversions.convert(obj, itemClass, collectionFactory, null);
  }

  public static Currency toCurrency(Object obj) {
    return toCurrency(obj, null);
  }

  public static Currency toCurrency(Object obj, Currency altVal) {
    return defaultObject(Conversions.convert(obj, Currency.class), altVal);
  }

  public static Double toDouble(Object obj) {
    return toDouble(obj, null);
  }

  public static Double toDouble(Object obj, Double altVal) {
    return defaultObject(Conversions.convert(obj, Double.class), altVal);
  }

  public static List<Double> toDoubleList(Object obj) {
    return toList(obj, Double.class);
  }

  public static <T extends Enum<T>> T toEnum(Object obj, Class<T> enumClazz) {
    return Conversions.convert(obj, enumClazz);
  }

  public static <T extends Enum<T>> List<T> toEnumList(Object obj, Class<T> enumClazz) {
    return toList(obj, enumClazz);
  }

  public static Float toFloat(Object obj) {
    return toFloat(obj, null);
  }

  public static Float toFloat(Object obj, Float altVal) {
    return defaultObject(Conversions.convert(obj, Float.class), altVal);
  }

  public static List<Float> toFloatList(Object obj) {
    return toList(obj, Float.class);
  }

  public static Instant toInstant(Object obj) {
    return toInstant(obj, null);
  }

  public static Instant toInstant(Object obj, Instant altVal) {
    return defaultObject(Conversions.convert(obj, Instant.class), altVal);
  }

  public static List<Instant> toInstantList(Object obj) {
    return toList(obj, Instant.class);
  }

  public static Integer toInteger(Object obj) {
    return toInteger(obj, null);
  }

  public static Integer toInteger(Object obj, Integer altVal) {
    return defaultObject(Conversions.convert(obj, Integer.class), altVal);
  }

  public static List<Integer> toIntegerList(Object obj) {
    return toList(obj, Integer.class);
  }

  public static <T> List<T> toList(Object obj, Class<T> clazz) {
    return Conversions.convert(obj, clazz, ArrayList::new, null);
  }

  public static <T> List<T> toList(Object obj, Function<Object, T> convert) {
    List<T> values = new ArrayList<>();
    if (obj instanceof Iterable<?>) {
      values = streamOf((Iterable<?>) obj).map(convert).collect(Collectors.toList());
    } else if (obj instanceof Object[]) {
      values = streamOf((Object[]) obj).map(convert).collect(Collectors.toList());
    }
    return values;
  }

  public static LocalDate toLocalDate(Object obj) {
    return toLocalDate(obj, null, null);
  }

  public static LocalDate toLocalDate(Object obj, LocalDate altVal) {
    return toLocalDate(obj, null, altVal);
  }

  public static LocalDate toLocalDate(Object obj, String pattern) {
    return toLocalDate(obj, pattern, null);
  }

  public static LocalDate toLocalDate(Object obj, String pattern, LocalDate altVal) {
    if (pattern != null) {
      return defaultObject(Conversions.convert(obj, LocalDate.class,
          mapOf(ConverterHints.CVT_DATE_FMT_PTN_KEY, pattern)), altVal);
    } else {
      return defaultObject(Conversions.convert(obj, LocalDate.class), altVal);
    }
  }

  public static LocalDate toLocalDate(Object obj, ZoneId zoneId) {
    return Conversions.convert(obj, LocalDate.class, mapOf(ConverterHints.CVT_ZONE_ID_KEY, zoneId));
  }

  public static List<LocalDate> toLocalDateList(Object obj, String pattern) {
    if (pattern != null) {
      return Conversions.convert(obj, LocalDate.class, ArrayList::new,
          mapOf(ConverterHints.CVT_DATE_FMT_PTN_KEY, pattern));
    } else {
      return toList(obj, LocalDate.class);
    }
  }

  public static Locale toLocale(Object obj) {
    return toLocale(obj, null);
  }

  public static Locale toLocale(Object obj, Locale altVal) {
    return defaultObject(Conversions.convert(obj, Locale.class), altVal);
  }

  public static Long toLong(Object obj) {
    return toLong(obj, null);
  }

  public static Long toLong(Object obj, Long altVal) {
    return defaultObject(Conversions.convert(obj, Long.class), altVal);
  }

  public static List<Long> toLongList(Object obj) {
    return toList(obj, Long.class);
  }

  public static <T> T toObject(Object obj, Class<T> clazz) {
    return Conversions.convert(obj, clazz);
  }

  public static <T> Set<T> toSet(Object obj, Class<T> clazz) {
    return Conversions.convert(obj, clazz, HashSet::new, null);
  }

  public static Short toShort(Object obj) {
    return toShort(obj, null);
  }

  public static Short toShort(Object obj, Short altVal) {
    return defaultObject(Conversions.convert(obj, Short.class), altVal);
  }

  public static List<Short> toShortList(Object obj) {
    return toList(obj, Short.class);
  }

  public static String toString(Object obj) {
    return obj == null ? null : obj.toString();
  }

  public static TimeZone toTimeZone(Object obj) {
    return Conversions.convert(obj, TimeZone.class);
  }

  public static ZonedDateTime toZonedDateTime(Object obj) {
    return toZonedDateTime(obj, null, null);
  }

  public static ZonedDateTime toZonedDateTime(Object obj, String pattern) {
    return toZonedDateTime(obj, pattern, null);
  }

  public static ZonedDateTime toZonedDateTime(Object obj, String pattern, ZonedDateTime altVal) {
    return defaultObject(Conversions.convert(obj, ZonedDateTime.class,
        mapOf(ConverterHints.CVT_DATE_FMT_PTN_KEY, pattern)), altVal);
  }

  public static ZonedDateTime toZonedDateTime(Object obj, ZonedDateTime altVal) {
    return toZonedDateTime(obj, null, altVal);
  }

  public static ZonedDateTime toZonedDateTime(Object obj, ZoneId zoneId) {
    return defaultObject(Conversions.convert(obj, ZonedDateTime.class,
        mapOf(ConverterHints.CVT_ZONE_ID_KEY, zoneId)), null);
  }

  public static List<ZonedDateTime> toZonedDateTimeList(Object obj, String pattern) {
    if (pattern != null) {
      return Conversions.convert(obj, ZonedDateTime.class, ArrayList::new,
          mapOf(ConverterHints.CVT_DATE_FMT_PTN_KEY, pattern));
    } else {
      return toList(obj, ZonedDateTime.class);
    }
  }
}

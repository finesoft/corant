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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Conversions.toSet;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Primitives.wrapArray;
import static org.corant.shared.util.Strings.split;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.corant.shared.conversion.Conversion;
import org.corant.shared.conversion.converter.NumberLocalDateConverter;
import org.corant.shared.conversion.converter.SqlDateLocalDateConverter;
import org.corant.shared.conversion.converter.StringCurrencyConverter;
import org.corant.shared.conversion.converter.StringLocalDateConverter;
import org.corant.shared.conversion.converter.TemporalLocalDateConverter;
import org.corant.shared.conversion.converter.factory.IntArrayTemporalConverterFactory;
import org.corant.shared.conversion.converter.factory.ListTemporalConverterFactory;
import org.corant.shared.conversion.converter.factory.MapTemporalConverterFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.TypeLiteral;

/**
 * corant-shared
 *
 * @author bingo 下午4:21:10
 *
 */
public class Maps {

  private Maps() {}

  /**
   * Extract the value corresponding to the given key path in the given object, and remove the key
   * value in the given object. The given object can be a Map, or a collection of Maps or an array
   * of Maps. If the value extracted is a collection or array, it will be merged into a single list
   * and use the first element of the list as the result and then force type casting the result
   * according to the expected return type.
   *
   * @param <T> the expected return type
   * @param object the object to be extracted from
   * @param keyPath the key path
   */
  public static <T> T extractMapKeyPathValue(Object object, Object[] keyPath) {
    List<Object> holder = new ArrayList<>();
    iterateMapValue(object, keyPath, 0, true, true, holder);
    if (holder.isEmpty()) {
      return null;
    } else {
      T value = forceCast(holder.get(0));
      holder.clear();
      return value;
    }
  }

  /**
   * Extract the value corresponding to the given key path in the given object, and remove the key
   * value in the given object. The given object can be a Map, or a collection of Maps or an array
   * of Maps. If the value extracted is a collection or array, it will be merged into a single list
   * and use the first element of the list as the result and then use
   * {@link Conversions#toObject(Object, Class)} to cast the result according to the expected return
   * type.
   *
   * @param <T> the expected return type
   * @param object the object to be extracted from
   * @param keyPath the key path
   * @param expectedType the expected class
   */
  public static <T> T extractMapKeyPathValue(Object object, Object[] keyPath,
      Class<T> expectedType) {
    return toObject(extractMapKeyPathValue(object, keyPath), expectedType);
  }

  /**
   * Extract the value corresponding to the given key path in the given object, and remove the key
   * value in the given object. The given object can be a Map, or a collection of Maps or an array
   * of Maps. If the value extracted is not a collection and array, it will be added into a list and
   * return.
   *
   * @param object the object to be extracted from
   * @param keyPath the key path
   */
  public static List<Object> extractMapKeyPathValues(Object object, Object[] keyPath) {
    List<Object> holder = new ArrayList<>();
    iterateMapValue(object, keyPath, 0, true, true, holder);
    return holder;
  }

  /**
   * Extract the value corresponding to the given key path in the given object, and remove the key
   * value in the given object. The given object can be a Map, or a collection of Maps or an array
   * of Maps. If the value extracted is not a collection and array, it will be added into a list.
   *
   * @param <T> the element type
   * @param object the object to be extracted from
   * @param keyPath the key path
   * @param expectedElementType the expected class
   */
  public static <T> List<T> extractMapKeyPathValues(Object object, Object[] keyPath,
      Class<T> expectedElementType) {
    return toList(extractMapKeyPathValues(object, keyPath), t -> toObject(t, expectedElementType));
  }

  public static Map<FlatMapKey, Object> flatMap(Map<?, ?> map, int maxDepth) {
    Map<FlatMapKey, Object> flatMap = new HashMap<>();
    if (map != null) {
      for (Entry<?, ?> entry : map.entrySet()) {
        doFlatMap(flatMap, FlatMapKey.of(entry.getKey()), entry.getValue(), maxDepth);
      }
    }
    return flatMap;
  }

  public static Map<String, Object> flatMap(Map<String, ?> map, String splitor, int maxDepth) {
    Map<FlatMapKey, Object> flatMap = new HashMap<>();
    Map<String, Object> stringKeyMap = new TreeMap<>((k1, k2) -> {
      int s = Integer.compare(split(k1, splitor).length, split(k2, splitor).length);
      if (s == 0) {
        return k1.compareTo(k2);
      }
      return s;
    });
    if (map != null) {
      for (Entry<?, ?> entry : map.entrySet()) {
        doFlatMap(flatMap, FlatMapKey.of(entry.getKey()), entry.getValue(), maxDepth);
      }
    }
    flatMap.forEach((k, v) -> stringKeyMap.put(k.asStringKeys(splitor), v));
    return stringKeyMap;
  }

  public static Map<String, String> flatStringMap(Map<String, ?> map, String splitor,
      int maxDepth) {
    Map<String, Object> stringKeyMap = flatMap(map, splitor, maxDepth);
    Map<String, String> stringMap = new HashMap<>(stringKeyMap.size());
    stringKeyMap.forEach((k, v) -> stringMap.put(k, asString(v, null)));
    return stringMap;
  }

  /**
   * Returns the BigDecimal value to which the specified key is mapped, or {@code null} if this map
   * contains no mapping for the key, and converted value if the mapped value type is not
   * BigDecimal.
   *
   * @see Conversions#toBigDecimal(Object)
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped BigDecimal value
   */
  public static BigDecimal getMapBigDecimal(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toBigDecimal, null);
  }

  /**
   * Returns the BigDecimal value to which the specified key is mapped, or alternative value
   * {@code nvt} if this map contains no mapping for the key or the mapped value is {@code null},
   * and converted value if the mapped value type is not BigDecimal.
   *
   * @see Conversions#toBigDecimal(Object)
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt The return alternative value when the key or map does not exist or the value
   *        corresponding to the key is null
   * @return the mapped BigDecimal value
   */
  public static BigDecimal getMapBigDecimal(final Map<?, ?> map, final Object key, BigDecimal nvt) {
    return getMapObject(map, key, Conversions::toBigDecimal, nvt);
  }

  /**
   * Returns the BigInteger value to which the specified key is mapped, or {@code null} if this map
   * contains no mapping for the key, and converted value if the mapped value type is not
   * BigInteger.
   *
   * @see Conversions#toBigInteger(Object)
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped BigInteger value
   */
  public static BigInteger getMapBigInteger(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toBigInteger, null);
  }

  /**
   * Returns the BigInteger value to which the specified key is mapped, or alternative value
   * {@code nvt} if this map contains no mapping for the key or the mapped value is {@code null},
   * and converted value if the mapped value type is not BigInteger.
   *
   * @see Conversions#toBigInteger(Object)
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt The return alternative value when the key or map does not exist or the value
   *        corresponding to the key is null
   * @return the mapped BigInteger value
   */
  public static BigInteger getMapBigInteger(final Map<?, ?> map, final Object key, BigInteger nvt) {
    return getMapObject(map, key, Conversions::toBigInteger, nvt);
  }

  /**
   * Returns the Boolean value to which the specified key is mapped, or {@code Boolean.FALSE} if
   * this map contains no mapping for the key, and converted value if the mapped value type is not
   * Boolean.
   *
   * @see Conversions#toBoolean(Object)
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped boolean value
   */
  public static Boolean getMapBoolean(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toBoolean, Boolean.FALSE);
  }

  /**
   * Returns the Boolean value to which the specified key is mapped, or alternative value
   * {@code nvt} if this map contains no mapping for the key or the mapped value is {@code null},
   * and converted value if the mapped value type is not Boolean.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt The return alternative value when the key or map does not exist or the value
   *        corresponding to the key is null
   * @return the mapped boolean value
   */
  public static Boolean getMapBoolean(final Map<?, ?> map, final Object key, Boolean nvt) {
    return getMapObject(map, key, Conversions::toBoolean, nvt);
  }

  /**
   * Return and convert the collection value mapped to the given key in the given Map or
   * {@code null} if the given map is {@code null} or the map contains no mapping for the key or the
   * mapped value is {@code null}.
   *
   * <p>
   * Note: The returned collection is reconstructed, and the result of modifying the collection may
   * not be reflected in the given map.
   *
   * @param <T> the target class of item of the collection
   * @param <C> the target collection class
   * @param map the map to use
   * @param key the key to lookup
   * @param collectionFactory the constructor of collection
   * @param elementClazz the target class of item of the collection
   * @param hints the lastConverter hints use for intervening converters
   * @return the mapped expected collection value
   *
   * @see Conversion#convert(Object, Class, IntFunction, Map)
   * @see Conversion#convert(Collection, IntFunction, Class, Map)
   * @see Conversion#convert(Object[], IntFunction, Class, Map)
   */
  public static <T, C extends Collection<T>> C getMapCollection(final Map<?, ?> map,
      final Object key, final IntFunction<C> collectionFactory, final Class<T> elementClazz,
      final Map<String, ?> hints) {
    Object obj = map == null ? null : map.get(key);
    if (obj instanceof Collection) {
      return Conversion.convert((Collection<?>) obj, collectionFactory, elementClazz, hints);
    } else if (obj != null && obj.getClass().isArray()) {
      return Conversion.convert(wrapArray(obj), collectionFactory, elementClazz, hints);
    } else if (obj != null) {
      return Conversion.convert(obj, elementClazz, collectionFactory, hints);
    } else {
      return null;
    }
  }

  /**
   * Returns the Currency value that the specified key is mapped to, convert if necessary.
   *
   * @see StringCurrencyConverter
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped currency value
   */
  public static Currency getMapCurrency(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toCurrency, null);
  }

  /**
   * Returns the Currency value that the specified key is mapped to, convert if necessary, and
   * return the specified default value when the key value is not found or the mapped value is null.
   *
   * @see StringCurrencyConverter
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt default value when the key value is not found or the mapped value is null.
   * @return the mapped currency value
   */
  public static Currency getMapCurrency(final Map<?, ?> map, final Object key, Currency nvt) {
    return getMapObject(map, key, Conversions::toCurrency, nvt);
  }

  /**
   * Returns the Double value that the specified key is mapped to, convert if necessary.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped double value
   */
  public static Double getMapDouble(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toDouble, null);
  }

  /**
   * Returns the Double value that the specified key is mapped to, convert if necessary, and return
   * the specified default value when the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt default value when the key value is not found or the mapped value is null.
   * @return the mapped double value
   */
  public static Double getMapDouble(final Map<?, ?> map, final Object key, Double nvt) {
    return getMapObject(map, key, Conversions::toDouble, nvt);
  }

  /**
   * Returns the Duration value that the specified key is mapped to, convert if necessary.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped duration value
   */
  public static Duration getMapDuration(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toDuration, null);
  }

  /**
   * Returns the Duration value that the specified key is mapped to, convert if necessary, and
   * return the specified default value when the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt default value when the key value is not found or the mapped value is null.
   * @return the mapped duration value
   */
  public static Duration getMapDuration(final Map<?, ?> map, final Object key, Duration nvt) {
    return getMapObject(map, key, Conversions::toDuration, nvt);
  }

  /**
   * Returns the Enumeration value that the specified key is mapped to, convert if necessary, or
   * {@code null} if the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param enumClazz the expected enumeration class
   * @return the mapped enumeration value
   */
  public static <T extends Enum<T>> T getMapEnum(final Map<?, ?> map, final Object key,
      final Class<T> enumClazz) {
    return getMapObject(map, key, o -> Conversions.toEnum(o, enumClazz), null);
  }

  /**
   * Returns the Enumeration value that the specified key is mapped to, convert if necessary, and
   * return the specified default value when the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param enumClazz the expected enumeration class
   * @param nvt default value when the key value is not found or the mapped value is null.
   * @return the mapped enumeration value
   */
  public static <T extends Enum<T>> T getMapEnum(final Map<?, ?> map, final Object key,
      final Class<T> enumClazz, T nvt) {
    T enumObj = getMapEnum(map, key, enumClazz);
    return enumObj == null ? nvt : enumObj;
  }

  /**
   * Returns the Float value that the specified key is mapped to, convert if necessary, or
   * {@code null} if the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped float value
   */
  public static Float getMapFloat(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toFloat, null);
  }

  /**
   * Returns the Float value that the specified key is mapped to, convert if necessary, and return
   * the specified default value when the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt default value when the key value is not found or the mapped value is null.
   * @return the mapped float value
   */
  public static Float getMapFloat(final Map<?, ?> map, final Object key, Float nvt) {
    return getMapObject(map, key, Conversions::toFloat, nvt);
  }

  /**
   * Returns the Instant value that the specified key is mapped to, convert if necessary, or
   * {@code null} if the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped instant value
   */
  public static Instant getMapInstant(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toInstant, null);
  }

  /**
   * Returns the Instant value that the specified key is mapped to, convert if necessary, and return
   * the specified default value when the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt default value when the key value is not found or the mapped value is null.
   * @return the mapped instant value
   */
  public static Instant getMapInstant(final Map<?, ?> map, final Object key, Instant nvt) {
    return getMapObject(map, key, Conversions::toInstant, nvt);
  }

  /**
   * Returns the Integer value that the specified key is mapped to, convert if necessary, or
   * {@code null} if the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped integer value
   */
  public static Integer getMapInteger(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toInteger, null);
  }

  /**
   * Returns the Integer value that the specified key is mapped to, convert if necessary, and return
   * the specified default value when the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt default value when the key value is not found or the mapped value is null.
   * @return the mapped integer value
   */
  public static Integer getMapInteger(final Map<?, ?> map, final Object key, Integer nvt) {
    return getMapObject(map, key, Conversions::toInteger, nvt);
  }

  /**
   * Returns the value corresponding to the given key path in the given object. The given object can
   * be a Map, or a collection of Maps or an array of Maps. If the value retrieved is a collection
   * or array, it will be merged into a single list and use the first element of the list as the
   * result and then force type casting the result according to the expected return type.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it is not
   * guaranteed that it will affect the given object.
   *
   * @param <T> the expected return type
   * @param object the object to be lookup from
   * @param keyPath the key path
   */
  public static <T> T getMapKeyPathValue(Object object, Object[] keyPath) {
    List<Object> holder = new ArrayList<>();
    iterateMapValue(object, keyPath, 0, true, false, holder);
    if (holder.isEmpty()) {
      return null;
    } else {
      T value = forceCast(holder.get(0));
      holder.clear();
      return value;
    }
  }

  /**
   * Returns the value corresponding to the given key path in the given object. The given object can
   * be a Map, or a collection of Maps or an array of Maps. If the value retrieved is a collection
   * or array, it will be merged into a single list and use the first element of the list as the
   * result and then use {@link Conversions#toObject(Object, Class)} to cast the result according to
   * the expected return type.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it is not
   * guaranteed that it will affect the given object.
   *
   * @param <T> the expected return type
   * @param object the object to be lookup from
   * @param keyPath the key path
   * @param expectedType the expected class
   */
  public static <T> T getMapKeyPathValue(Object object, Object[] keyPath, Class<T> expectedType) {
    return toObject(getMapKeyPathValue(object, keyPath), expectedType);
  }

  /**
   * Return the value corresponding to the given key path in the given object. The given object can
   * be a Map, or a collection of Maps or an array of Maps. If the value retrieved is not a
   * collection and array, it will be added into a list and return.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it is not
   * guaranteed that it will affect the given object.
   *
   * @param object the object to be lookup from
   * @param keyPath the key path
   */
  public static List<Object> getMapKeyPathValues(Object object, Object[] keyPath) {
    List<Object> holder = new ArrayList<>();
    iterateMapValue(object, keyPath, 0, true, false, holder);
    return holder;
  }

  /**
   * Return the value corresponding to the given key path in the given object. The given object can
   * be a Map, or a collection of Maps or an array of Maps. If the value retrieved is not a
   * collection and array, it will be added into a list and return.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it is not
   * guaranteed that it will affect the given object.
   *
   * @param object the object to be lookup from
   * @param keyPath the key path
   * @param expectedElementType the expected element type
   */
  public static <T> List<T> getMapKeyPathValues(Object object, Object[] keyPath,
      Class<T> expectedElementType) {
    return toList(getMapKeyPathValues(object, keyPath), t -> toObject(t, expectedElementType));
  }

  /**
   * Retrieve and convert the list value with the key from a map, use force cast convert.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it may affect the
   * given original map.
   *
   * @param <T> the list element type
   * @param map the map to use
   * @param key the key to lookup
   * @return the expected list
   */
  public static <T> List<T> getMapList(final Map<?, ?> map, final Object key) {
    return getMapList(map, key, Objects::forceCast);
  }

  /**
   * Return a new list, the elements in the list come from the value corresponding to the given key
   * specified in the specified given map, and the intermediate process may involve type conversion.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it is not
   * guaranteed that it will affect the original map.
   *
   * @param <T> the expected returned element type
   * @param map the map to use
   * @param key the key to lookup
   * @param elementClazz the expected element class in list
   * @return the expected list
   */
  public static <T> List<T> getMapList(final Map<?, ?> map, final Object key,
      final Class<T> elementClazz) {
    return getMapCollection(map, key, ArrayList::new, elementClazz, null);
  }

  /**
   * Return a new list, the elements in the list come from the value corresponding to the given key
   * specified in the specified given map. the intermediate process may involve type conversion, if
   * involved use the given single element converter to convert.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it is not
   * guaranteed that it will affect the original map.
   *
   * @param <T> the expected returned element type
   * @param map the map to use
   * @param key the key to lookup
   * @param singleElementconverter the converter function that extract value to expected list
   *        element.
   * @return the expected list
   */
  public static <T> List<T> getMapList(final Map<?, ?> map, final Object key,
      final Function<Object, T> singleElementconverter) {
    return getMapObjectList(map, key, v -> toList(v, singleElementconverter));
  }

  /**
   * Returns the LocalDate value that the specified key is mapped to, convert if necessary, or
   * {@code null} if the key value is not found or the mapped value is null.
   *
   * <p>
   * Note: Supports the type of mapped value include {@link java.time.temporal.Temporal},
   * {@link java.lang.String}, {@link java.lang.Number},{@link java.sql.Date}, integer array or
   * list, a map contains day/year/month value. Be careful some conversions may violate JSR-310.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped LocalDate value
   *
   * @see NumberLocalDateConverter
   * @see StringLocalDateConverter
   * @see TemporalLocalDateConverter
   * @see SqlDateLocalDateConverter
   * @see ListTemporalConverterFactory
   * @see MapTemporalConverterFactory
   * @see IntArrayTemporalConverterFactory
   */
  public static LocalDate getMapLocalDate(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toLocalDate, null);
  }

  /**
   * Returns the LocalDate value that the specified key is mapped to, convert if necessary, and
   * return the specified default value when the key value is not found or the mapped value is null.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param nvt default value when the key value is not found or the mapped value is null.
   * @return the mapped LocalDate value
   *
   * @see #getMapLocalDate(Map, Object)
   */
  public static LocalDate getMapLocalDate(final Map<?, ?> map, final Object key, LocalDate nvt) {
    return getMapObject(map, key, Conversions::toLocalDate, nvt);
  }

  /**
   * Returns the LocalDate value that the specified key is mapped to, convert if necessary, or
   * {@code null} if the key value is not found or the mapped value is null.
   * <p>
   * Note: if the mapped value is {@link java.time.Instant}, the given zone id may be used, if the
   * zone id is {@code null} then use {@link ZoneId#systemDefault()}, be careful the converting may
   * violate JSR-310.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param zoneId the zoneId use for converting
   * @return the mapped LocalDate value
   */
  public static LocalDate getMapLocalDate(final Map<?, ?> map, final Object key, ZoneId zoneId) {
    return getMapObject(map, key, v -> Conversions.toLocalDate(v, zoneId), null);
  }

  public static LocalDateTime getMapLocalDateTime(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toLocalDateTime, null);
  }

  public static LocalDateTime getMapLocalDateTime(final Map<?, ?> map, final Object key,
      LocalDateTime nvt) {
    return getMapObject(map, key, Conversions::toLocalDateTime, nvt);
  }

  public static LocalDateTime getMapLocalDateTime(final Map<?, ?> map, final Object key,
      ZoneId zoneId) {
    return getMapObject(map, key, v -> Conversions.toLocalDateTime(v, zoneId), null);
  }

  public static Locale getMapLocale(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toLocale, null);
  }

  public static Locale getMapLocale(final Map<?, ?> map, final Object key, Locale nvt) {
    return getMapObject(map, key, Conversions::toLocale, nvt);
  }

  public static Long getMapLong(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toLong, null);
  }

  public static Long getMapLong(final Map<?, ?> map, final Object key, Long nvt) {
    return getMapObject(map, key, Conversions::toLong, nvt);
  }

  /**
   * Get the map value corresponding to the given key from the given map, use force cast convert.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it may affect the
   * given original map.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped map value
   */
  public static <K, V> Map<K, V> getMapMap(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Objects::forceCast, null);
  }

  /**
   * Get the list of maps value corresponding to the given key from the given map, use force cast
   * convert.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it may affect the
   * given original map.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped list maps value
   */
  public static <K, V> List<Map<K, V>> getMapMaps(final Map<?, ?> map, final Object key) {
    return getMapList(map, key, Objects::forceCast);
  }

  /**
   * Null safe {@link Map#get(Object)}
   *
   *
   * @param map the map to use
   * @param key the key to lookup
   * @return the mapped object
   */
  public static Object getMapObject(final Map<?, ?> map, final Object key) {
    return map == null ? null : map.get(key);
  }

  /**
   * Convert and return the value corresponding to the given key in the given map, and the
   * intermediate process may involve type conversion.
   *
   * @param <T> the expected value type
   * @param map the map to use
   * @param key the key to lookup
   * @param clazz the expected value class
   * @return the mapped object
   */
  public static <T> T getMapObject(final Map<?, ?> map, final Object key, final Class<T> clazz) {
    return toObject(map == null ? null : map.get(key), shouldNotNull(clazz));
  }

  /**
   * Convert and return the value corresponding to the given key in the given map, use the given
   * converter if involve type conversion.
   *
   * @param <T> the expected value type
   * @param map the map to use
   * @param key the key to lookup
   * @param converter the value converter
   * @return the mapped object
   */
  public static <T> T getMapObject(final Map<?, ?> map, final Object key,
      final Function<Object, T> converter) {
    return getMapObject(map, key, converter, null);
  }

  /**
   * Returns the converted value corresponding to the given key in the given map or return the given
   * default value when the key value is not found or the mapped value is null, use the given
   * converter if involve type conversion.
   *
   * @param <T> the expected value type
   * @param map the map to use
   * @param key the key to lookup
   * @param converter the value converter
   * @param nvt the default return value when the key value is not found or the mapped value is null
   * @return the mapped object
   */
  public static <T> T getMapObject(final Map<?, ?> map, final Object key,
      final Function<Object, T> converter, final T nvt) {
    Object val = map == null ? null : map.get(key);
    return val != null ? defaultObject(converter.apply(val), nvt) : nvt;
  }

  /**
   * Convert and return the value corresponding to the given key in the given map.
   *
   * @param map the map to use
   * @param key the key to lookup
   * @param typeLiteral the value type
   * @return the mapped object
   */
  public static <T> T getMapObject(final Map<?, ?> map, final Object key,
      final TypeLiteral<T> typeLiteral) {
    return map == null ? null : toObject(map.get(key), typeLiteral);
  }

  /**
   * Retrieve and convert the value of the specified type with key from map, use specified list
   * extractor.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it may not affect
   * the given original map, whether it will be affected depends on the given converter.
   *
   * @param <T> the expected value type
   * @param map the map to use
   * @param key the key to lookup
   * @param converter the value converter.
   * @return the list
   */
  public static <T> List<T> getMapObjectList(final Map<?, ?> map, final Object key,
      final Function<Object, List<T>> converter) {
    return map != null ? converter.apply(map.get(key)) : null;
  }

  /**
   * Retrieve and convert the value of the specified type with key from map, use specified set
   * extractor.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it may not affect
   * the given original map, whether it will be affected depends on the given converter.
   *
   * @param <T> the expected value type
   * @param map the map to use
   * @param key the key to lookup
   * @param converter the value converter.
   * @return the set
   */
  public static <T> Set<T> getMapObjectSet(final Map<?, ?> map, final Object key,
      final Function<Object, Set<T>> converter) {
    return map != null ? converter.apply(map.get(key)) : null;
  }

  /**
   * Retrieve and convert the set value with the key from a map, use force cast convert.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it may affect the
   * given original map.
   *
   * @param <T> the expected value type
   * @param map the map to use
   * @param key the key to lookup
   * @return the set
   */
  public static <T> Set<T> getMapSet(final Map<?, ?> map, final Object key) {
    return getMapSet(map, key, Objects::forceCast);
  }

  /**
   * Return a new set, the elements in the set come from the value corresponding to the given key
   * specified in the specified given map, and the intermediate process may involve type conversion.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it is not
   * guaranteed that it will affect the original map.
   *
   * @param <T> the expected value type
   * @param map the map to use
   * @param key the key to lookup
   * @param clazz the expected value class
   */
  public static <T> Set<T> getMapSet(final Map<?, ?> map, final Object key, final Class<T> clazz) {
    return getMapCollection(map, key, HashSet::new, clazz, null);
  }

  /**
   * Return a new set, the elements in the set come from the value corresponding to the given key
   * specified in the specified given map, and the intermediate process may involve type conversion,
   * if the conversion involve use the given single element converter.
   *
   * <p>
   * Note: If the value returned by this method is changed by another processing, it is not
   * guaranteed that it will affect the original map.
   *
   * @param <T> the expected value type
   * @param map the map to use
   * @param key the key to lookup
   * @param singleElementConverter the value converter.
   * @return the set
   */
  public static <T> Set<T> getMapSet(final Map<?, ?> map, final Object key,
      final Function<Object, T> singleElementConverter) {
    return getMapObjectSet(map, key, v -> toSet(v, singleElementConverter));
  }

  public static Short getMapShort(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toShort, null);
  }

  public static Short getMapShort(final Map<?, ?> map, final Object key, Short nvt) {
    return getMapObject(map, key, Conversions::toShort, nvt);
  }

  public static String getMapString(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toString, null);
  }

  public static String getMapString(final Map<?, ?> map, final Object key, String nvt) {
    return getMapObject(map, key, Conversions::toString, nvt);
  }

  public static ZonedDateTime getMapZonedDateTime(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toZonedDateTime, null);
  }

  public static ZonedDateTime getMapZonedDateTime(final Map<?, ?> map, final Object key,
      ZonedDateTime nvt) {
    return getMapObject(map, key, Conversions::toZonedDateTime, nvt);
  }

  public static ZonedDateTime getMapZonedDateTime(final Map<?, ?> map, final Object key,
      ZoneId zoneId) {
    return getMapObject(map, key, v -> Conversions.toZonedDateTime(v, zoneId), null);
  }

  public static <K, V> Optional<V> getOpt(Map<K, V> map, K key) {
    return Optional.ofNullable(map != null ? map.get(key) : null);
  }

  public static <T> Optional<T> getOptMapObject(final Map<?, ?> map, final Object key,
      final Function<Object, T> extractor) {
    return Optional.ofNullable(map != null ? extractor.apply(map.get(key)) : null);
  }

  public static <K, V> Map<K, V> immutableMapOf(Object... objects) {
    if (objects == null || objects.length == 0) {
      return Collections.emptyMap();
    }
    return Collections.unmodifiableMap(mapOf(objects));
  }

  public static <K, V> Map<V, K> invertMap(final Map<K, V> map) {
    Map<V, K> result = new HashMap<>(shouldNotNull(map, "The map can't null").size());
    map.forEach((k, v) -> result.put(v, k));
    return result;
  }

  @SafeVarargs
  public static <K, V> Map<K, V> linkedHashMapOf(Entry<? extends K, ? extends V>... entries) {
    Object[] array = new Object[entries.length << 1];
    int len = 0;
    for (Entry<? extends K, ? extends V> entry : entries) {
      array[len++] = entry.getKey();
      array[len++] = entry.getValue();
    }
    return mapOf(LinkedHashMap::new, array);
  }

  public static <K, V> Map<K, V> linkedHashMapOf(Object... objects) {
    return mapOf(LinkedHashMap::new, objects);
  }

  @SafeVarargs
  public static <K, V> Map<K, V> mapOf(Entry<? extends K, ? extends V>... entries) {
    Object[] array = new Object[entries.length << 1];
    int len = 0;
    for (Entry<? extends K, ? extends V> entry : entries) {
      array[len++] = entry.getKey();
      array[len++] = entry.getValue();
    }
    return mapOf(HashMap::new, array);
  }

  public static <K, V, M extends Map<K, V>> M mapOf(IntFunction<M> factory, Object... objects) {
    int oLen;
    if (objects == null || (oLen = objects.length) == 0) {
      return factory.apply(0);
    }
    int rLen = (oLen & 1) == 0 ? oLen : oLen - 1;
    int size = (rLen >> 1) + 1;
    M map = factory.apply(size);
    for (int i = 0; i < rLen; i += 2) {
      map.put(forceCast(objects[i]), forceCast(objects[i + 1]));
    }
    if (rLen < oLen) {
      map.put(forceCast(objects[rLen]), null);
    }
    return map;
  }

  public static <K, V> Map<K, V> mapOf(Object... objects) {
    return mapOf(HashMap::new, objects);
  }

  public static <K, V> Map<K, V> newHashMap(Map<? extends K, ? extends V> map) {
    return map != null ? new HashMap<>(map) : new HashMap<>();
  }

  public static Properties propertiesOf(String... strings) {
    Properties result = new Properties();
    result.putAll(mapOf((Object[]) strings));
    return result;
  }

  @SuppressWarnings("rawtypes")
  public static void putMapKeyPathValue(Map target, Object[] paths, Object value) {
    implantMapValue(target, paths, 0, value);
  }

  public static <K, V> Map<K, V> removeIfKey(Predicate<K> predicate, Map<K, V> map) {
    Map<K, V> removed = new HashMap<>();
    if (predicate != null && map != null) {
      Set<K> removeKeys = map.keySet().stream().filter(predicate).collect(Collectors.toSet());
      for (K key : removeKeys) {
        removed.put(key, map.remove(key));
      }
    }
    return removed;
  }

  public static Map<String, String> toMap(final Properties properties) {
    Map<String, String> map = new HashMap<>(shouldNotNull(properties).size());
    synchronized (properties) {
      for (Entry<Object, Object> entry : properties.entrySet()) {
        Object key = entry.getKey();
        Object val = entry.getValue();
        if (key != null && val != null) {
          map.put(key.toString(), val.toString());
        } else if (key != null) {
          map.put(key.toString(), null);
        } else if (val != null) {
          map.put(null, val.toString());
        }
      }
    }
    return map;
  }

  public static <K, V> Properties toProperties(final Map<K, V> map) {
    Properties pops = new Properties();
    if (map != null) {
      pops.putAll(map);
    }
    return pops;
  }

  /**
   * Returns a new map containing the given maps. The Map.putAll(Map) operation is used to append
   * the given maps into a new map.
   *
   * @param <K> the key type
   * @param <V> the value type
   * @param maps the maps to be union
   */
  @SafeVarargs
  public static <K, V> Map<K, V> union(Map<? extends K, ? extends V>... maps) {
    Map<K, V> union = new HashMap<>();
    if (maps.length > 0) {
      for (Map<? extends K, ? extends V> map : maps) {
        if (map != null) {
          union.putAll(map);
        }
      }
    }
    return union;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  static void doFlatMap(Map<FlatMapKey, Object> resultMap, FlatMapKey key, Object val,
      int maxDepth) {
    if (key == null || key.keys.size() > maxDepth) {
      return;
    }
    if (val instanceof Collection) {
      int idx = 0;
      Collection<?> vals = (Collection<?>) val;
      for (Object obj : vals) {
        doFlatMap(resultMap, FlatMapKey.of(key).append(idx++), obj, maxDepth);
      }
    } else if (val != null && val.getClass().isArray()) {
      int idx = 0;
      Object[] vals = wrapArray(val);
      for (Object obj : vals) {
        doFlatMap(resultMap, FlatMapKey.of(key).append(idx++), obj, maxDepth);
      }
    } else if (val instanceof Map) {
      ((Map) val).forEach(
          (k, nextVal) -> doFlatMap(resultMap, FlatMapKey.of(key).append(k), nextVal, maxDepth));
    } else if (resultMap.put(key, val) != null) {
      throw new CorantRuntimeException("FlatMap with key %s dup!", key);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static void implantMapValue(Map target, Object[] paths, int deep, Object value) {
    if (target != null && paths.length > deep) {
      Object key = paths[deep];
      if (paths.length - deep == 1) {
        target.put(key, value);
      } else {
        Object next = target.computeIfAbsent(key, k -> new HashMap<>());
        int nextDeep = deep + 1;
        if (next instanceof Map) {
          implantMapValue((Map) next, paths, nextDeep, value);
        } else if (next instanceof Iterable) {
          for (Object item : (Iterable) next) {
            if (item instanceof Map) {
              implantMapValue((Map) item, paths, nextDeep, value);
            } else if (item != null) {
              throw new NotSupportedException("We only support implants for a map object!");
            }
          }
        } else if (next.getClass().isArray()) {
          for (Object item : wrapArray(next)) {
            if (item instanceof Map) {
              implantMapValue((Map) item, paths, nextDeep, value);
            } else if (item != null) {
              throw new NotSupportedException("We only support implants for a map object!");
            }
          }
        } else {
          throw new NotSupportedException("We only support implants for a map object!");
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  static void iterateMapValue(Object value, Object[] keyPath, int deep, boolean flat,
      boolean remove, List<Object> holder) {
    if (value == null) {
      return;
    }
    final int index = keyPath.length - deep;
    final boolean removed = remove && index == 1;
    if (index > 0) {
      if (value instanceof Map) {
        final Object key = keyPath[deep];
        final Map mapValue = (Map) value;
        final Object next = removed ? mapValue.remove(key) : mapValue.get(key);
        if (next != null) {
          iterateMapValue(next, keyPath, deep + 1, flat, remove, holder);
        }
      } else if (value instanceof Iterable) {
        for (Object next : (Iterable<?>) value) {
          if (next != null) {
            iterateMapValue(next, keyPath, deep, flat, remove, holder);
          }
        }
        /*
         * final Iterator it = ((Iterable<?>) value).iterator(); while (it.hasNext()) { Object next
         * = it.next(); if (next != null) { iterateMapValue(next, keyPath, deep, flat, remove,
         * holder); } if (removed && isEmptyOrNull(next)) { it.remove(); } }
         */
      } else if (value.getClass().isArray()) {
        for (Object next : wrapArray(value)) {
          if (next != null) {
            iterateMapValue(next, keyPath, deep, flat, remove, holder);
          }
        }
        /*
         * final Object[] arrayValue = (Object[]) value; final int arrayLength = arrayValue.length;
         * for (int i = 0; i < arrayLength; i++) { if (arrayValue[i] != null) {
         * iterateMapValue(arrayValue[i], keyPath, deep, flat, remove, holder); } if (removed &&
         * isEmptyOrNull(arrayValue[i])) { arrayValue[i] = null; } }
         */
      } else {
        throw new NotSupportedException("We only extract value from map/iterable/array object");
      }
    } else if (value instanceof Iterable && flat) {
      for (Object next : (Iterable<?>) value) {
        holder.add(next);
      }
    } else if (value.getClass().isArray() && flat) {
      Collections.addAll(holder, (Object[]) value);
    } else {
      holder.add(value);
    }
  }

  public static class FlatMapKey {

    final List<Object> keys = new LinkedList<>();

    public static FlatMapKey of(FlatMapKey object) {
      if (object == null) {
        return new FlatMapKey();
      } else {
        FlatMapKey key = new FlatMapKey();
        key.keys.addAll(object.keys);
        return key;
      }
    }

    public static FlatMapKey of(Object object) {
      FlatMapKey key = new FlatMapKey();
      key.append(object);
      return key;
    }

    public String asStringKeys(String splitor) {
      String[] stringKeys = keys.stream().map(Objects::asString).toArray(String[]::new);
      return String.join(splitor, stringKeys);
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
      FlatMapKey other = (FlatMapKey) obj;
      return keys.equals(other.keys);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      return prime * result + keys.hashCode();
    }

    FlatMapKey append(Object key) {
      keys.add(key);
      return this;
    }

  }
}

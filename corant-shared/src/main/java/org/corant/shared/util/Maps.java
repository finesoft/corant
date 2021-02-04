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
import org.corant.shared.conversion.Conversion;
import org.corant.shared.conversion.converter.StringCurrencyConverter;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;

public class Maps {

  private Maps() {
    super();
  }

  /**
   * extract value from map object and remove the entry, use key path
   *
   * @param <T>
   * @param object
   * @param keyPath
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
   * extract and convert value from map object and remove the entry, use key path
   *
   * @param <T>
   * @param object
   * @param keyPath
   * @param expectedType
   */
  public static <T> T extractMapKeyPathValue(Object object, Object[] keyPath,
      Class<T> expectedType) {
    return toObject(extractMapKeyPathValue(object, keyPath), expectedType);
  }

  /**
   * extract list value from map object and remove the entry, use key path
   *
   * @param object
   * @param keyPath
   */
  public static List<Object> extractMapKeyPathValues(Object object, Object[] keyPath) {
    List<Object> holder = new ArrayList<>();
    iterateMapValue(object, keyPath, 0, true, true, holder);
    return holder;
  }

  /**
   * extract and convert list value from map object and remove the entry, use key path
   *
   * @param <T>
   * @param object
   * @param keyPath
   * @param expectedElementType
   * @return extractMapKeyPathValues
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
   * @param map
   * @param key
   * @return getMapBigDecimal
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
   * @param map
   * @param key
   * @param nvt The return alternative value when the key or map does not exist or the value
   *        corresponding to the key is null
   * @return getMapBigDecimal
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
   * @param map
   * @param key
   * @return getMapBigInteger
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
   * @param map
   * @param key
   * @param nvt The return alternative value when the key or map does not exist or the value
   *        corresponding to the key is null
   * @return getMapBigInteger
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
   * @param map
   * @param key
   * @return getMapBoolean
   */
  public static Boolean getMapBoolean(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toBoolean, Boolean.FALSE);
  }

  /**
   * Returns the Boolean value to which the specified key is mapped, or alternative value
   * {@code nvt} if this map contains no mapping for the key or the mapped value is {@code null},
   * and converted value if the mapped value type is not Boolean.
   *
   * @param map
   * @param key
   * @param nvt The return alternative value when the key or map does not exist or the value
   *        corresponding to the key is null
   * @return getMapBoolean
   */
  public static Boolean getMapBoolean(final Map<?, ?> map, final Object key, Boolean nvt) {
    return getMapObject(map, key, Conversions::toBoolean, nvt);
  }

  /**
   * Convert mapped value to collection, use built-in converter
   *
   * @param <T> the target class of item of the collection
   * @param <C> the target collection class
   * @param map the source map to convert
   * @param key the key corresponding to the value to be converted
   * @param elementClazz the target class of item of the collection
   * @param collectionFactory the constructor of collection
   * @param hints the lastConverter hints use for intervening converters
   * @return getMapCollection
   *
   * @see Conversion#convert(Object, Class, java.util.function.Supplier, Map)
   * @see Conversion#convert(Collection, IntFunction, Class, Map)
   * @see Conversion#convert(Object[], IntFunction, Class, Map)
   */
  public static <T, C extends Collection<T>> C getMapCollection(final Map<?, ?> map,
      final Object key, final IntFunction<C> collectionFactory, final Class<T> elementClazz,
      final Map<String, ?> hints) {
    Object obj = map == null ? null : map.get(key);
    if (obj instanceof Collection) {
      return Conversion.convert((Collection<?>) obj, collectionFactory, elementClazz, hints);
    } else if (obj instanceof Object[]) {
      return Conversion.convert((Object[]) obj, collectionFactory, elementClazz, hints);
    } else if (obj != null) {
      return Conversion.convert(obj, elementClazz, () -> collectionFactory.apply(10), hints);
    }
    return collectionFactory.apply(0);
  }

  /**
   * Returns the Currency value that the specified key is mapped to, convert if necessary.
   *
   * @see StringCurrencyConverter
   * @param map
   * @param key
   * @return getMapCurrency
   */
  public static Currency getMapCurrency(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toCurrency, null);
  }

  /**
   * Returns the Currency value that the specified key is mapped to, convert if necessary, and
   * return the specified default value when the key value is not found or the mapped value is null.
   *
   * @see StringCurrencyConverter
   * @param map
   * @param key
   * @param nvt
   * @return getMapCurrency
   */
  public static Currency getMapCurrency(final Map<?, ?> map, final Object key, Currency nvt) {
    return getMapObject(map, key, Conversions::toCurrency, nvt);
  }

  /**
   * Returns the Double value that the specified key is mapped to, convert if necessary.
   *
   * @param map
   * @param key
   * @return getMapDouble
   */
  public static Double getMapDouble(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toDouble, null);
  }

  /**
   * Returns the Double value that the specified key is mapped to, convert if necessary, and return
   * the specified default value when the key value is not found or the mapped value is null.
   *
   * @param map
   * @param key
   * @param nvt
   * @return getMapDouble
   */
  public static Double getMapDouble(final Map<?, ?> map, final Object key, Double nvt) {
    return getMapObject(map, key, Conversions::toDouble, nvt);
  }

  /**
   * Returns the Duration value that the specified key is mapped to, convert if necessary.
   *
   * @param map
   * @param key
   * @return getMapDuration
   */
  public static Duration getMapDuration(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toDuration, null);
  }

  public static Duration getMapDuration(final Map<?, ?> map, final Object key, Duration nvt) {
    return getMapObject(map, key, Conversions::toDuration, nvt);
  }

  public static <T extends Enum<T>> T getMapEnum(final Map<?, ?> map, final Object key,
      final Class<T> enumClazz) {
    return getMapObject(map, key, o -> Conversions.toEnum(o, enumClazz), null);
  }

  public static <T extends Enum<T>> T getMapEnum(final Map<?, ?> map, final Object key,
      final Class<T> enumClazz, T nvt) {
    T enumObj = getMapEnum(map, key, enumClazz);
    return enumObj == null ? nvt : enumObj;
  }

  public static Float getMapFloat(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toFloat, null);
  }

  public static Float getMapFloat(final Map<?, ?> map, final Object key, Float nvt) {
    return getMapObject(map, key, Conversions::toFloat, nvt);
  }

  public static Instant getMapInstant(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toInstant, null);
  }

  public static Instant getMapInstant(final Map<?, ?> map, final Object key, Instant nvt) {
    return getMapObject(map, key, Conversions::toInstant, nvt);
  }

  public static Integer getMapInteger(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toInteger, null);
  }

  public static Integer getMapInteger(final Map<?, ?> map, final Object key, Integer nvt) {
    return getMapObject(map, key, Conversions::toInteger, nvt);
  }

  /**
   * Get value from map object, use key path, only support extract value from map/iterable/array
   * object
   *
   * @param <T>
   * @param object map/iterable/array
   * @param keyPath
   * @return
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
   * Get and convert value from map object, use key path, only support extract value from
   * map/iterable/array
   *
   * @param <T>
   * @param object
   * @param keyPath
   * @param expectedType
   */
  public static <T> T getMapKeyPathValue(Object object, Object[] keyPath, Class<T> expectedType) {
    return toObject(getMapKeyPathValue(object, keyPath), expectedType);
  }

  /**
   * Get list value from map object, use key path, only support extract value from
   * map/iterable/array
   *
   * @param object
   * @param keyPath
   */
  public static List<Object> getMapKeyPathValues(Object object, Object[] keyPath) {
    List<Object> holder = new ArrayList<>();
    iterateMapValue(object, keyPath, 0, true, false, holder);
    return holder;
  }

  /**
   * Get and convert list value from map object, use key path, only support extract value from
   * map/iterable/array
   *
   * @param <T>
   * @param object
   * @param keyPath
   */
  public static <T> List<T> getMapKeyPathValues(Object object, Object[] keyPath,
      Class<T> expectedElementType) {
    return toList(getMapKeyPathValues(object, keyPath), t -> toObject(t, expectedElementType));
  }

  /**
   * Retrieve and convert the list value with the key from a map, use force cast convert.
   *
   * @param <T>
   * @param map
   * @param key
   * @return getMapList
   */
  public static <T> List<T> getMapList(final Map<?, ?> map, final Object key) {
    return getMapList(map, key, Objects::forceCast);
  }

  /**
   * Retrieve and convert the list value with the key and element class from a map, use built-in
   * converter.
   *
   * @param <T> the expected element type in list
   * @param map
   * @param key
   * @param elementClazz the expected element class in list
   * @return getMapList
   */
  public static <T> List<T> getMapList(final Map<?, ?> map, final Object key,
      final Class<T> elementClazz) {
    return getMapCollection(map, key, ArrayList::new, elementClazz, null);
  }

  /**
   * Retrieve and convert the list value with the key and convert function from a map, use specified
   * element extractor.
   *
   * @param <T> the expected element type in list
   * @param map
   * @param key
   * @param singleElementExtractor the extractor function that extract value to expected list
   *        element.
   * @return getMapList
   */
  public static <T> List<T> getMapList(final Map<?, ?> map, final Object key,
      final Function<Object, T> singleElementExtractor) {
    return getMapObjectList(map, key, v -> toList(v, singleElementExtractor));
  }

  public static LocalDate getMapLocalDate(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, Conversions::toLocalDate, null);
  }

  public static LocalDate getMapLocalDate(final Map<?, ?> map, final Object key, LocalDate nvt) {
    return getMapObject(map, key, Conversions::toLocalDate, nvt);
  }

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

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <K, V> Map<K, V> getMapMap(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, o -> o instanceof Map ? (Map) o : null, null);
  }

  public static List<Map<?, ?>> getMapMaps(final Map<?, ?> map, final Object key) {
    return getMapList(map, key, Objects::forceCast);
  }

  public static Object getMapObject(final Map<?, ?> map, final Object key) {
    return map == null ? null : map.get(key);
  }

  /**
   * Retrieves the value of the specified type with key from map, use converter.
   *
   * @param <T> the expected value type
   * @param map
   * @param key
   * @param clazz the expected value class
   * @return getMapObject
   */
  public static <T> T getMapObject(final Map<?, ?> map, final Object key, final Class<T> clazz) {
    return toObject(map == null ? null : map.get(key), shouldNotNull(clazz));
  }

  /**
   * Retrieves the value of the specified type with key from map, use specified extractor.
   *
   * @param <T> the expected value type
   * @param map
   * @param key
   * @param extractor the value extractor
   * @return getMapObject
   */
  public static <T> T getMapObject(final Map<?, ?> map, final Object key,
      final Function<Object, T> extractor) {
    return getMapObject(map, key, extractor, null);
  }

  public static <T> T getMapObject(final Map<?, ?> map, final Object key,
      final Function<Object, T> extractor, final T nvt) {
    Object val = map == null ? null : map.get(key);
    return val != null ? defaultObject(extractor.apply(val), nvt) : nvt;
  }

  /**
   * Retrieve and convert the value of the specified type with key from map, use specified list
   * extractor.
   *
   * @param <T> the expected value type
   * @param map
   * @param key
   * @param extractor the value extractor that extract map value.
   * @return getMapObjectList
   */
  public static <T> List<T> getMapObjectList(final Map<?, ?> map, final Object key,
      final Function<Object, List<T>> extractor) {
    return map != null ? extractor.apply(map.get(key)) : null;
  }

  /**
   * Retrieve and convert the value of the specified type with key from map, use specified set
   * extractor.
   *
   * @param <T> the expected value type
   * @param map
   * @param key
   * @param extractor the value extractor that extract map value.
   * @return getMapObjectSet
   */
  public static <T> Set<T> getMapObjectSet(final Map<?, ?> map, final Object key,
      final Function<Object, Set<T>> extractor) {
    return map != null ? extractor.apply(map.get(key)) : null;
  }

  /**
   * Retrieve and convert the set value with the key from a map, use force cast convert.
   *
   * @param <T>
   * @param map
   * @param key
   * @return getMapList
   */
  public static <T> Set<T> getMapSet(final Map<?, ?> map, final Object key) {
    return getMapSet(map, key, Objects::forceCast);
  }

  /**
   * Retrieve and convert the value of the specified type with key from map, use bulit-in converter.
   *
   * @param <T> the expected value type
   * @param map
   * @param key
   * @param clazz the expected value class
   */
  public static <T> Set<T> getMapSet(final Map<?, ?> map, final Object key, final Class<T> clazz) {
    return getMapCollection(map, key, HashSet::new, clazz, null);
  }

  /**
   * Retrieve and convert the value of the specified type with key from map, use specified element
   * extractor.
   *
   * @param <T>
   * @param map
   * @param key
   * @param singleElementExtractor
   * @return getMapSet
   */
  public static <T> Set<T> getMapSet(final Map<?, ?> map, final Object key,
      final Function<Object, T> singleElementExtractor) {
    return getMapObjectSet(map, key, v -> toSet(v, singleElementExtractor));
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

  public static Properties propertiesOf(String... strings) {
    Properties result = new Properties();
    mapOf((Object[]) strings).forEach(result::put);
    return result;
  }

  @SuppressWarnings("rawtypes")
  public static void putMapKeyPathValue(Map target, Object[] paths, Object value) {
    implantMapValue(target, paths, 0, value);
  }

  public static Map<String, String> toMap(final Properties properties) {
    Map<String, String> map = new HashMap<>(shouldNotNull(properties).size());
    if (properties != null) {
      properties.stringPropertyNames().forEach(name -> map.put(name, properties.getProperty(name)));
    }
    return map;
  }

  public static <K, V> Properties toProperties(final Map<K, V> map) {
    Properties pops = new Properties();
    if (map != null) {
      map.forEach((k, v) -> pops.getProperty(forceCast(k), forceCast(v)));
    }
    return pops;
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
    } else if (val instanceof Object[]) {
      int idx = 0;
      Object[] vals = (Object[]) val;
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
        } else if (next instanceof Object[]) {
          for (Object item : (Object[]) next) {
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
      } else if (value instanceof Object[]) {
        for (Object next : (Object[]) value) {
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
    } else {
      if (value instanceof Iterable && flat) {
        for (Object next : (Iterable<?>) value) {
          holder.add(next);
        }
      } else if (value.getClass().isArray() && flat) {
        for (Object next : (Object[]) value) {
          holder.add(next);
        }
      } else {
        holder.add(value);
      }
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
      result = prime * result + keys.hashCode();
      return result;
    }

    FlatMapKey append(Object key) {
      keys.add(key);
      return this;
    }

  }
}

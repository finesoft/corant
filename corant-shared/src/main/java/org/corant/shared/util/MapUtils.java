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
import static org.corant.shared.util.Empties.isEmptyOrNull;
import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.ObjectUtils.isEquals;
import static org.corant.shared.util.StringUtils.split;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.Iterator;
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
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;

public class MapUtils {

  private MapUtils() {
    super();
  }

  /**
   * extract value from map object, use key path
   *
   * @param <T>
   * @param value
   * @param keyPath
   * @param flat
   * @param remove
   * @param single
   * @return extractMapValue
   */
  public static <T> T extractMapValue(Object value, Object[] keyPath, boolean remove,
      boolean single) {
    List<Object> holder = new ArrayList<>();
    doExtractMapValue(value, keyPath, 0, true, remove, holder);
    return forceCast(single ? !holder.isEmpty() ? holder.get(0) : null : holder);
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

  public static BigDecimal getMapBigDecimal(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toBigDecimal, null);
  }

  public static BigDecimal getMapBigDecimal(final Map<?, ?> map, final Object key, BigDecimal nvt) {
    return getMapObject(map, key, ConversionUtils::toBigDecimal, nvt);
  }

  public static BigInteger getMapBigInteger(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toBigInteger, null);
  }

  public static BigInteger getMapBigInteger(final Map<?, ?> map, final Object key, BigInteger nvt) {
    return getMapObject(map, key, ConversionUtils::toBigInteger, nvt);
  }

  public static Boolean getMapBoolean(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toBoolean, Boolean.FALSE);
  }

  public static Currency getMapCurrency(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toCurrency, null);
  }

  public static Currency getMapCurrency(final Map<?, ?> map, final Object key, Currency nvt) {
    return getMapObject(map, key, ConversionUtils::toCurrency, nvt);
  }

  public static Double getMapDouble(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toDouble, null);
  }

  public static Double getMapDouble(final Map<?, ?> map, final Object key, Double nvt) {
    return getMapObject(map, key, ConversionUtils::toDouble, nvt);
  }

  public static <T extends Enum<T>> T getMapEnum(final Map<?, ?> map, final Object key,
      final Class<T> enumClazz) {
    return getMapObject(map, key, o -> ConversionUtils.toEnum(o, enumClazz), null);
  }

  public static <T extends Enum<T>> T getMapEnum(final Map<?, ?> map, final Object key,
      final Class<T> enumClazz, T nvt) {
    T enumObj = getMapEnum(map, key, enumClazz);
    return enumObj == null ? nvt : enumObj;
  }

  public static Float getMapFloat(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toFloat, null);
  }

  public static Float getMapFloat(final Map<?, ?> map, final Object key, Float nvt) {
    return getMapObject(map, key, ConversionUtils::toFloat, nvt);
  }

  public static Instant getMapInstant(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toInstant, null);
  }

  public static Instant getMapInstant(final Map<?, ?> map, final Object key, Instant nvt) {
    return getMapObject(map, key, ConversionUtils::toInstant, nvt);
  }

  public static Integer getMapInteger(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toInteger, null);
  }

  public static Integer getMapInteger(final Map<?, ?> map, final Object key, Integer nvt) {
    return getMapObject(map, key, ConversionUtils::toInteger, nvt);
  }

  /**
   * Retrieve the list value with the key from a map, use force cast convert.
   *
   * @param <T>
   * @param map
   * @param key
   * @return getMapList
   */
  public static <T> List<T> getMapList(final Map<?, ?> map, final Object key) {
    return getMapList(map, key, o -> forceCast(o));
  }

  /**
   * Retrieve the list value with the key and element class from a map, use converter.
   *
   * @param <T> the expected element type in list
   * @param map
   * @param key
   * @param elementClazz the expected element class in list
   * @return getMapList
   */
  public static <T> List<T> getMapList(final Map<?, ?> map, final Object key,
      final Class<T> elementClazz) {
    return getMapObjectList(map, key, o -> ConversionUtils.toList(o, elementClazz));
  }

  /**
   * Retrieve the list value with the key and convert function from a map.
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
    return getMapObjectList(map, key, v -> ConversionUtils.toList(v, singleElementExtractor));
  }

  public static LocalDate getMapLocalDate(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toLocalDate, null);
  }

  public static LocalDate getMapLocalDate(final Map<?, ?> map, final Object key, LocalDate nvt) {
    return getMapObject(map, key, ConversionUtils::toLocalDate, nvt);
  }

  public static LocalDate getMapLocalDate(final Map<?, ?> map, final Object key, ZoneId zoneId) {
    return getMapObject(map, key, v -> ConversionUtils.toLocalDate(v, zoneId), null);
  }

  public static LocalDateTime getMapLocalDateTime(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toLocalDateTime, null);
  }

  public static LocalDateTime getMapLocalDateTime(final Map<?, ?> map, final Object key,
      LocalDateTime nvt) {
    return getMapObject(map, key, ConversionUtils::toLocalDateTime, nvt);
  }

  public static LocalDateTime getMapLocalDateTime(final Map<?, ?> map, final Object key,
      ZoneId zoneId) {
    return getMapObject(map, key, v -> ConversionUtils.toLocalDateTime(v, zoneId), null);
  }

  public static Locale getMapLocale(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toLocale, null);
  }

  public static Locale getMapLocale(final Map<?, ?> map, final Object key, Locale nvt) {
    return getMapObject(map, key, ConversionUtils::toLocale, nvt);
  }

  public static Long getMapLong(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toLong, null);
  }

  public static Long getMapLong(final Map<?, ?> map, final Object key, Long nvt) {
    return getMapObject(map, key, ConversionUtils::toLong, nvt);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <K, V> Map<K, V> getMapMap(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, o -> o instanceof Map ? (Map) o : null, null);
  }

  public static List<Map<?, ?>> getMapMaps(final Map<?, ?> map, final Object key) {
    return getMapList(map, key, ObjectUtils::forceCast);
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
    return ConversionUtils.toObject(map == null ? null : map.get(key), shouldNotNull(clazz));
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
    return map != null ? defaultObject(extractor.apply(map.get(key)), nvt) : nvt;
  }

  /**
   * Retrieves the value of the specified type with key from map, use specified list extractor.
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

  public static <T> Set<T> getMapSet(final Map<?, ?> map, final Object key) {
    return new HashSet<>(getMapList(map, key, o -> forceCast(o)));
  }

  public static <T> Set<T> getMapSet(final Map<?, ?> map, final Object key, final Class<T> clazz) {
    return new HashSet<>(getMapObjectList(map, key, o -> ConversionUtils.toList(o, clazz)));
  }

  public static <T> Set<T> getMapSet(final Map<?, ?> map, final Object key,
      final Function<Object, T> objFunc) {
    return new HashSet<>(getMapObjectList(map, key, v -> ConversionUtils.toList(v, objFunc)));
  }

  public static Short getMapShort(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toShort, null);
  }

  public static Short getMapShort(final Map<?, ?> map, final Object key, Short nvt) {
    return getMapObject(map, key, ConversionUtils::toShort, nvt);
  }

  public static String getMapString(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toString, null);
  }

  public static String getMapString(final Map<?, ?> map, final Object key, String nvt) {
    return getMapObject(map, key, ConversionUtils::toString, nvt);
  }

  public static ZonedDateTime getMapZonedDateTime(final Map<?, ?> map, final Object key) {
    return getMapObject(map, key, ConversionUtils::toZonedDateTime, null);
  }

  public static ZonedDateTime getMapZonedDateTime(final Map<?, ?> map, final Object key,
      ZonedDateTime nvt) {
    return getMapObject(map, key, ConversionUtils::toZonedDateTime, nvt);
  }

  public static ZonedDateTime getMapZonedDateTime(final Map<?, ?> map, final Object key,
      ZoneId zoneId) {
    return getMapObject(map, key, v -> ConversionUtils.toZonedDateTime(v, zoneId), null);
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

  @SuppressWarnings("rawtypes")
  public static void implantMapValue(Map target, Object[] paths, Object value) {
    doImplantMapValue(target, paths, 0, value);
  }

  public static <K, V> Map<V, K> invertMap(final Map<K, V> map) {
    Map<V, K> result = new HashMap<>();
    if (map != null) {
      map.forEach((k, v) -> result.put(v, k));
    }
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

  public static Map<String, String> toMap(final Properties properties) {
    Map<String, String> map = new HashMap<>();
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

  @SuppressWarnings("rawtypes")
  static void doExtractMapValue(Object value, Object[] keyPath, int deep, boolean flat,
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
          doExtractMapValue(next, keyPath, deep + 1, flat, remove, holder);
        }
      } else if (value instanceof Iterable) {
        final Iterator it = ((Iterable<?>) value).iterator();
        while (it.hasNext()) {
          Object next = it.next();
          if (next != null) {
            doExtractMapValue(next, keyPath, deep, flat, remove, holder);
          }
          if (removed && isEmptyOrNull(next)) {
            it.remove();
          }
        }
      } else if (value instanceof Object[]) {
        final Object[] arrayValue = (Object[]) value;
        final int arrayLength = arrayValue.length;
        for (int i = 0; i < arrayLength; i++) {
          if (arrayValue[i] != null) {
            doExtractMapValue(arrayValue[i], keyPath, deep, flat, remove, holder);
          }
          if (removed && isEmptyOrNull(arrayValue[i])) {
            arrayValue[i] = null;
          }
        }
      } else {
        throw new NotSupportedException("We only extract value from map object");
      }
    } else {
      if (value instanceof Iterable && flat) {
        for (Object next : (Iterable<?>) value) {
          holder.add(next);
        }
      } else {
        holder.add(value);
      }
    }
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
  static void doImplantMapValue(Map target, Object[] paths, int deep, Object implantedValue) {
    Object key;
    if (target != null && paths.length > deep && target.containsKey(key = paths[deep])) {
      if (paths.length - deep == 1) {
        target.put(key, implantedValue);
      } else {
        Object value = target.get(key);
        int next = deep + 1;
        if (value instanceof Map) {
          doImplantMapValue((Map) value, paths, next, implantedValue);
        } else if (value instanceof Iterable) {
          for (Object item : (Iterable) value) {
            if (item instanceof Map) {
              doImplantMapValue((Map) item, paths, next, implantedValue);
            } else if (item != null) {
              throw new NotSupportedException("We only implant value to map object");
            }
          }
        } else if (value instanceof Object[]) {
          for (Object item : (Object[]) value) {
            if (item instanceof Map) {
              doImplantMapValue((Map) item, paths, next, implantedValue);
            } else if (item != null) {
              throw new NotSupportedException("We only implant value to map object");
            }
          }
        } else {
          throw new NotSupportedException("We only implant value to map object");
        }
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
      String[] stringKeys = keys.stream().map(ObjectUtils::asString).toArray(String[]::new);
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

  public interface WrappedMap<K, V> extends Map<K, V> {

    @Override
    default void clear() {
      unwrap().clear();
    }

    @Override
    default boolean containsKey(Object key) {
      return unwrap().containsKey(key);
    }

    @Override
    default boolean containsValue(Object value) {
      return unwrap().containsValue(value);
    }

    @Override
    default Set<Entry<K, V>> entrySet() {
      return unwrap().entrySet();
    }

    @Override
    default V get(Object key) {
      return unwrap().get(key);
    }

    default BigDecimal getBigDecimal(K key) {
      return getMapBigDecimal(unwrap(), key);
    }

    default BigDecimal getBigDecimal(K key, BigDecimal nvt) {
      return getMapBigDecimal(unwrap(), key, nvt);
    }

    default BigInteger getBigInteger(K key) {
      return getMapBigInteger(unwrap(), key);
    }

    default BigInteger getBigInteger(K key, BigInteger nvt) {
      return getMapBigInteger(unwrap(), key, nvt);
    }

    default Boolean getBoolean(K key) {
      return getMapBoolean(unwrap(), key);
    }

    default Currency getCurrency(K key) {
      return getMapCurrency(unwrap(), key);
    }

    default Currency getCurrency(K key, Currency nvt) {
      return getMapCurrency(unwrap(), key, nvt);
    }

    default Double getDouble(K key) {
      return getMapDouble(unwrap(), key);
    }

    default Double getDouble(K key, Double nvt) {
      return getMapDouble(unwrap(), key, nvt);
    }

    default <T extends Enum<T>> T getEnum(K key, final Class<T> enumClazz) {
      return getMapEnum(unwrap(), key, enumClazz);
    }

    default <T extends Enum<T>> T getEnum(K key, final Class<T> enumClazz, T nvt) {
      return getMapEnum(unwrap(), key, enumClazz, nvt);
    }

    default Float getFloat(K key) {
      return getMapFloat(unwrap(), key);
    }

    default Float getFloat(K key, Float nvt) {
      return getMapFloat(unwrap(), key, nvt);
    }

    default Instant getInstant(K key) {
      return getMapInstant(unwrap(), key);
    }

    default Instant getInstant(K key, Instant nvt) {
      return getMapInstant(unwrap(), key, nvt);
    }

    default Integer getInteger(K key) {
      return getMapInteger(unwrap(), key);
    }

    default Integer getInteger(K key, Integer nvt) {
      return getMapInteger(unwrap(), key, nvt);
    }

    default <T> List<T> getList(K key) {
      return getMapList(unwrap(), key);
    }

    default <T> List<T> getList(K key, final Function<Object, T> objFunc) {
      return getMapList(unwrap(), key, objFunc);
    }

    default LocalDate getLocalDate(K key) {
      return getMapLocalDate(unwrap(), key);
    }

    default LocalDate getLocalDate(K key, LocalDate nvt) {
      return getMapLocalDate(unwrap(), key, nvt);
    }

    default Locale getLocale(K key) {
      return getMapLocale(unwrap(), key);
    }

    default Locale getLocale(K key, Locale nvt) {
      return getMapLocale(unwrap(), key, nvt);
    }

    default Long getLong(K key) {
      return getMapLong(unwrap(), key);
    }

    default Long getLong(K key, Long nvt) {
      return getMapLong(unwrap(), key, nvt);
    }

    @SuppressWarnings("rawtypes")
    default Map getMap(K key) {
      V v = unwrap().get(key);
      if (v == null) {
        return null;
      } else if (v instanceof Map) {
        return (Map) v;
      } else {
        throw new CorantRuntimeException("Can't get map from key %s ", key);
      }
    }

    default Short getShort(K key) {
      return getMapObject(unwrap(), key, ConversionUtils::toShort, null);
    }

    default Short getShort(K key, Short nvt) {
      return getMapObject(unwrap(), key, ConversionUtils::toShort, nvt);
    }

    default String getString(K key) {
      return getMapObject(unwrap(), key, ConversionUtils::toString, null);
    }

    default String getString(K key, String nvt) {
      String att = getMapObject(unwrap(), key, ConversionUtils::toString, null);
      return att == null ? nvt : att;
    }

    WrappedMap<K, V> getSubset(K key);

    @Override
    default boolean isEmpty() {
      return unwrap().isEmpty();
    }

    @Override
    default Set<K> keySet() {
      return unwrap().keySet();
    }

    @Override
    default V put(K key, V value) {
      return unwrap().put(key, value);
    }

    @Override
    default void putAll(Map<? extends K, ? extends V> m) {
      unwrap().putAll(m);
    }

    default void putAll(WrappedMap<? extends K, ? extends V> m) {
      unwrap().putAll(m);
    }

    @Override
    default V remove(Object key) {
      return unwrap().remove(key);
    }

    default boolean same(K key, WrappedMap<K, V> other) {
      return same(key, other, key);
    }

    default boolean same(K key, WrappedMap<K, V> other, K keyInOther) {
      return other != null && isEquals(unwrap().get(key), other.unwrap().get(keyInOther));
    }

    @Override
    default int size() {
      return unwrap().size();
    }

    default Map<K, V> unwrap() {
      return Collections.emptyMap();
    }

    @Override
    default Collection<V> values() {
      return unwrap().values();
    }

  }
}

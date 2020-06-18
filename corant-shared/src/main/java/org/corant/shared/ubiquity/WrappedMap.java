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
package org.corant.shared.ubiquity;

import static org.corant.shared.util.ObjectUtils.isEquals;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Conversions;
import org.corant.shared.util.Maps;

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
    return Maps.getMapBigDecimal(unwrap(), key);
  }

  default BigDecimal getBigDecimal(K key, BigDecimal nvt) {
    return Maps.getMapBigDecimal(unwrap(), key, nvt);
  }

  default BigInteger getBigInteger(K key) {
    return Maps.getMapBigInteger(unwrap(), key);
  }

  default BigInteger getBigInteger(K key, BigInteger nvt) {
    return Maps.getMapBigInteger(unwrap(), key, nvt);
  }

  default Boolean getBoolean(K key) {
    return Maps.getMapBoolean(unwrap(), key);
  }

  default Currency getCurrency(K key) {
    return Maps.getMapCurrency(unwrap(), key);
  }

  default Currency getCurrency(K key, Currency nvt) {
    return Maps.getMapCurrency(unwrap(), key, nvt);
  }

  default Double getDouble(K key) {
    return Maps.getMapDouble(unwrap(), key);
  }

  default Double getDouble(K key, Double nvt) {
    return Maps.getMapDouble(unwrap(), key, nvt);
  }

  default <T extends Enum<T>> T getEnum(K key, final Class<T> enumClazz) {
    return Maps.getMapEnum(unwrap(), key, enumClazz);
  }

  default <T extends Enum<T>> T getEnum(K key, final Class<T> enumClazz, T nvt) {
    return Maps.getMapEnum(unwrap(), key, enumClazz, nvt);
  }

  default Float getFloat(K key) {
    return Maps.getMapFloat(unwrap(), key);
  }

  default Float getFloat(K key, Float nvt) {
    return Maps.getMapFloat(unwrap(), key, nvt);
  }

  default Instant getInstant(K key) {
    return Maps.getMapInstant(unwrap(), key);
  }

  default Instant getInstant(K key, Instant nvt) {
    return Maps.getMapInstant(unwrap(), key, nvt);
  }

  default Integer getInteger(K key) {
    return Maps.getMapInteger(unwrap(), key);
  }

  default Integer getInteger(K key, Integer nvt) {
    return Maps.getMapInteger(unwrap(), key, nvt);
  }

  default <T> List<T> getList(K key) {
    return Maps.getMapList(unwrap(), key);
  }

  default <T> List<T> getList(K key, final Function<Object, T> objFunc) {
    return Maps.getMapList(unwrap(), key, objFunc);
  }

  default LocalDate getLocalDate(K key) {
    return Maps.getMapLocalDate(unwrap(), key);
  }

  default LocalDate getLocalDate(K key, LocalDate nvt) {
    return Maps.getMapLocalDate(unwrap(), key, nvt);
  }

  default Locale getLocale(K key) {
    return Maps.getMapLocale(unwrap(), key);
  }

  default Locale getLocale(K key, Locale nvt) {
    return Maps.getMapLocale(unwrap(), key, nvt);
  }

  default Long getLong(K key) {
    return Maps.getMapLong(unwrap(), key);
  }

  default Long getLong(K key, Long nvt) {
    return Maps.getMapLong(unwrap(), key, nvt);
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
    return Maps.getMapObject(unwrap(), key, Conversions::toShort, null);
  }

  default Short getShort(K key, Short nvt) {
    return Maps.getMapObject(unwrap(), key, Conversions::toShort, nvt);
  }

  default String getString(K key) {
    return Maps.getMapObject(unwrap(), key, Conversions::toString, null);
  }

  default String getString(K key, String nvt) {
    String att = Maps.getMapObject(unwrap(), key, Conversions::toString, null);
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

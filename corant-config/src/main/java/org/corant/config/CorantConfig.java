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
package org.corant.config;

import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * corant-config
 *
 * @author bingo 下午5:47:24
 *
 */
public class CorantConfig implements Config {

  final Map<Class<?>, Converter<?>> converters;
  final AtomicReference<List<ConfigSource>> sources;
  final AtomicReference<Iterable<String>> propertyNames;
  final AtomicReference<Map<String, String>> properties;

  public CorantConfig(Map<Class<?>, Converter<?>> converters, List<ConfigSource> sources,
      UnaryOperator<Map<String, String>> uo) {
    this.converters = new HashMap<>(converters);
    this.sources = new AtomicReference<>(Collections.unmodifiableList(sources));
    Set<String> usePropertyNames = new HashSet<>();
    Map<String, String> useSourcesMap = new HashMap<>();
    for (ConfigSource source : sources) {
      source.getProperties().forEach((k, v) -> {
        usePropertyNames.add(k);
        useSourcesMap.computeIfAbsent(k, n -> v);
      });
    }
    if (uo != null) {
      properties = new AtomicReference<>(Collections.unmodifiableMap(uo.apply(useSourcesMap)));
    } else {
      properties = new AtomicReference<>(Collections.unmodifiableMap(useSourcesMap));
    }
    propertyNames = new AtomicReference<>(Collections.unmodifiableSet(usePropertyNames));
  }

  @Override
  public Iterable<ConfigSource> getConfigSources() {
    return defaultObject(sources.get(), Collections.emptyList());
  }

  @Override
  public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
    return null;
  }

  @Override
  public Iterable<String> getPropertyNames() {
    return defaultObject(propertyNames.get(), Collections.emptySet());
  }

  @Override
  public <T> T getValue(String propertyName, Class<T> propertyType) {
    String value = properties.get().get(propertyName);
    if (isNotBlank(value)) {
      return convert(value, propertyType);
    }
    if (propertyType.isAssignableFrom(OptionalInt.class)) {
      return propertyType.cast(OptionalInt.empty());
    } else if (propertyType.isAssignableFrom(OptionalLong.class)) {
      return propertyType.cast(OptionalLong.empty());
    } else if (propertyType.isAssignableFrom(OptionalDouble.class)) {
      return propertyType.cast(OptionalDouble.empty());
    }
    throw new CorantRuntimeException("Can not find any config value by %s", propertyName);
  }

  <T> T convert(String value, Class<T> propertyType) {
    return null;
  }

}

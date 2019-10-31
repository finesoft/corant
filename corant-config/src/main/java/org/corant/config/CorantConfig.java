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
import org.corant.config.spi.ConfigAdjuster;
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
  final AtomicReference<ConfigData> data;

  public CorantConfig(Map<Class<?>, Converter<?>> converters, List<ConfigSource> sources,
      ConfigAdjuster adjuster) {
    this.converters = new HashMap<>(converters);
    data = new AtomicReference<>(new ConfigData(sources, adjuster));
  }

  @Override
  public Iterable<ConfigSource> getConfigSources() {
    return defaultObject(data.get().sources, Collections.emptyList());
  }

  @Override
  public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
    return null;
  }

  @Override
  public Iterable<String> getPropertyNames() {
    return defaultObject(data.get().propertyNames, Collections.emptySet());
  }

  @Override
  public <T> T getValue(String propertyName, Class<T> propertyType) {
    String value = data.get().caches.get(propertyName);
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

  public void setConfigSources(List<ConfigSource> sources, ConfigAdjuster adjuster) {
    ConfigData newDat = new ConfigData(sources, adjuster);
    for (;;) {
      ConfigData oldDat = data.get();
      if (data.compareAndSet(oldDat, newDat)) {
        return;
      }
    }
  }

  <T> T convert(String value, Class<T> propertyType) {
    return null;
  }

  /**
   * corant-config
   *
   * @author bingo 上午11:03:42
   *
   */
  static class ConfigData {

    final List<ConfigSource> sources;
    final Set<String> propertyNames;
    final Map<String, String> caches;

    ConfigData(List<ConfigSource> configSources, ConfigAdjuster adjuster) {
      sources = Collections.unmodifiableList(configSources);
      Set<String> usePropertyNames = new HashSet<>();
      Map<String, String> useCaches = new HashMap<>();
      for (ConfigSource source : configSources) {
        source.getProperties().forEach((k, v) -> {
          usePropertyNames.add(k);
          useCaches.computeIfAbsent(k, n -> v);
        });
      }
      caches = Collections.unmodifiableMap(adjuster.apply(useCaches));
      propertyNames = Collections.unmodifiableSet(usePropertyNames);
    }
  }
}

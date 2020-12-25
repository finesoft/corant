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

import static org.corant.shared.util.Objects.forceCast;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.corant.config.spi.ConfigAdjuster;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * corant-config
 *
 * @author bingo 下午5:47:24
 *
 */
public class CorantConfig implements Config, Serializable {

  private static final long serialVersionUID = 8788710772538278522L;
  private static final Logger logger = Logger.getLogger(CorantConfig.class.getName());

  final CorantConfigConversion conversion;
  final AtomicReference<List<ConfigSource>> sources;

  public CorantConfig(CorantConfigConversion conversion, List<ConfigSource> sources) {
    this.conversion = conversion;
    this.sources = new AtomicReference<>(sources);
  }

  @Override
  public Iterable<ConfigSource> getConfigSources() {
    return sources.get();
  }

  @Override
  public CorantConfigValue getConfigValue(String propertyName) {
    // TODO MP 2.0
    return null;
  }

  public CorantConfigConversion getConversion() {
    return conversion;
  }

  public Object getConvertedValue(String propertyName, Type type, String defaultRawValue) {
    Object result = conversion.convert(getRawValue(propertyName), type);
    if (result == null && defaultRawValue != null
        && !defaultRawValue.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
      result = conversion.convert(defaultRawValue, type);
    }
    return conversion.convertIfNecessary(result, type);
  }

  @Override
  public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
    // TODO MP 2.0
    return conversion.getConverter(forType);
  }

  @Override
  public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
    logger.finer(() -> String.format("Retrieve optional config property key [%s] type [%s]",
        propertyName, propertyType.getName()));
    return Optional
        .ofNullable(forceCast(conversion.convert(getRawValue(propertyName), propertyType)));
  }

  @Override
  public <T> Optional<List<T>> getOptionalValues(String propertyName, Class<T> propertyType) {
    // TODO MP 2.0
    @SuppressWarnings("unchecked")
    Class<T[]> arrayType = (Class<T[]>) Array.newInstance(propertyType, 0).getClass();
    return getOptionalValue(propertyName, arrayType).map(Arrays::asList);
  }

  @Override
  public Iterable<String> getPropertyNames() {
    Set<String> names = new HashSet<>();
    for (ConfigSource configSource : sources.get()) {
      names.addAll(configSource.getPropertyNames());
    }
    return names;
  }

  public String getRawValue(String propertyName) {
    for (ConfigSource cs : sources.get()) {
      String value = cs.getValue(propertyName);
      if (value != null) {// FIXME
        return value;
      }
    }
    return null;
  }

  @Override
  public <T> T getValue(String propertyName, Class<T> propertyType) {
    logger.fine(() -> String.format("Retrieve config property key [%s] type [%s]", propertyName,
        propertyType.getName()));
    T value = forceCast(conversion.convert(getRawValue(propertyName), propertyType));
    if (value == null) {
      throw new NoSuchElementException(
          String.format("Config property name [%s] type [%s] not found! %n [%s]", propertyName,
              propertyType, String.join("\n", getPropertyNames())));
    }
    return value;
  }

  @Override
  public <T> List<T> getValues(String propertyName, Class<T> propertyType) {
    // TODO MP 2.0
    @SuppressWarnings("unchecked")
    Class<T[]> arrayType = (Class<T[]>) Array.newInstance(propertyType, 0).getClass();
    return Arrays.asList(getValue(propertyName, arrayType));
  }

  public void reset(List<ConfigSource> sources, ConfigAdjuster adjuster) {
    List<ConfigSource> newSources = CorantConfigSource.resolve(sources, adjuster);
    for (;;) {
      List<ConfigSource> oldSources = this.sources.get();
      if (this.sources.compareAndSet(oldSources, newSources)) {
        return;
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unwrap(Class<T> type) {
    // TODO MP 2.0
    if (CorantConfig.class.isAssignableFrom(type)) {
      return (T) this;
    }
    throw new IllegalArgumentException("Can't unwrap CorantConfig to " + type);
  }
}

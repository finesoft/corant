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
import static org.corant.shared.util.Strings.NEWLINE;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.corant.shared.ubiquity.TypeLiteral;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
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
  public static final String CORANT_CONFIG_SOURCE_BASE_NAME = "application";
  public static final String CORANT_CONFIG_SOURCE_BASE_NAME_PREFIX =
      CORANT_CONFIG_SOURCE_BASE_NAME + "-";
  public static final String MP_CONFIG_SOURCE_BASE_NAME = "microprofile-config";
  public static final String MP_CONFIG_SOURCE_BASE_NAME_PREFIX = MP_CONFIG_SOURCE_BASE_NAME + "-";

  final CorantConfigConversion configConversion;
  final AtomicReference<CorantConfigSources> configSources;

  public CorantConfig(CorantConfigConversion conversion, CorantConfigSources sources) {
    configConversion = conversion;
    configSources = new AtomicReference<>(sources);
  }

  @Override
  public Iterable<ConfigSource> getConfigSources() {
    return forceCast(configSources.get().getSources());
  }

  @Override
  public ConfigValue getConfigValue(String propertyName) {
    return getConfigValue(propertyName, null);
  }

  public ConfigValue getConfigValue(String propertyName, String defaultValue) {
    return configSources.get().getConfigValue(propertyName, defaultValue);
  }

  public CorantConfigConversion getConversion() {
    return configConversion;
  }

  public Object getConvertedValue(String propertyName, Type type, String defaultRawValue) {
    return getConvertedValue(propertyName, type, defaultRawValue,
        ConfigProperty.UNCONFIGURED_VALUE);
  }

  public Object getConvertedValue(String propertyName, Type type, String defaultRawValue,
      String unconfiguredValue) {
    Object result = configConversion.convert(configSources.get().getValue(propertyName), type);
    if (result == null && defaultRawValue != null && !defaultRawValue.equals(unconfiguredValue)) {
      result = configConversion.convert(defaultRawValue, type);
    }
    return configConversion.convertIfNecessary(result, type);
  }

  @Override
  public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
    // TODO MP 2.0
    return configConversion.getConverter(forType);
  }

  public CorantConfigSources getCorantConfigSources() {
    return configSources.get();
  }

  @Override
  public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
    logger.finer(() -> String.format("Retrieve optional config property key [%s] type [%s]",
        propertyName, propertyType.getName()));
    return Optional.ofNullable(forceCast(
        configConversion.convert(configSources.get().getValue(propertyName), propertyType)));
  }

  public <T> Optional<T> getOptionalValue(String propertyName,
      javax.enterprise.util.TypeLiteral<T> propertyType) {
    T value = forceCast(configConversion.convert(configSources.get().getValue(propertyName),
        propertyType.getType()));
    return Optional.ofNullable(value);
  }

  public <T> Optional<T> getOptionalValue(String propertyName, TypeLiteral<T> propertyType) {
    T value = forceCast(configConversion.convert(configSources.get().getValue(propertyName),
        propertyType.getType()));
    return Optional.ofNullable(value);
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
    return configSources.get().getPropertyNames();
  }

  @Override
  public <T> T getValue(String propertyName, Class<T> propertyType) {
    logger.fine(() -> String.format("Retrieve config property key [%s] type [%s]", propertyName,
        propertyType.getName()));
    T value = forceCast(
        configConversion.convert(configSources.get().getValue(propertyName), propertyType));
    if (value == null) {
      throw new NoSuchElementException(
          String.format("Config property name [%s] type [%s] not found! %n [%s]", propertyName,
              propertyType, String.join(NEWLINE, getPropertyNames())));
    }
    return value;
  }

  public <T> T getValue(String propertyName, TypeLiteral<T> propertyType) {
    T value = forceCast(configConversion.convert(configSources.get().getValue(propertyName),
        propertyType.getType()));
    if (value == null) {
      throw new NoSuchElementException(
          String.format("Config property name [%s] type [%s] not found! %n [%s]", propertyName,
              propertyType, String.join(NEWLINE, getPropertyNames())));
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

  public void reset(List<ConfigSource> sources, ClassLoader classLoader) {
    CorantConfigSources configSources = CorantConfigSources.of(sources, classLoader);
    for (;;) {
      CorantConfigSources oldConfigSources = this.configSources.get();
      if (this.configSources.compareAndSet(oldConfigSources, configSources)) {
        return;
      }
    }
  }

  @Override
  public <T> T unwrap(Class<T> type) {
    // TODO MP 2.0
    if (CorantConfig.class.isAssignableFrom(type)) {
      return type.cast(this);
    }
    if (Config.class.isAssignableFrom(type)) {
      return type.cast(this);
    }
    throw new IllegalArgumentException("Can't unwrap CorantConfig to " + type);
  }

  private Object writeReplace() throws ObjectStreamException {
    return SerializableConfig.instance;
  }

  static class SerializableConfig implements Serializable {
    private static final long serialVersionUID = -3558605352597004269L;
    private static final SerializableConfig instance = new SerializableConfig();

    private Object readResolve() throws ObjectStreamException {
      return ConfigProvider.getConfig();
    }
  }
}

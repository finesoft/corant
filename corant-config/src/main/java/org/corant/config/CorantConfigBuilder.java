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

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.corant.config.CorantConfigConversion.OrdinalConverter;
import org.corant.config.source.MicroprofileConfigSources;
import org.corant.config.source.SystemEnvironmentConfigSource;
import org.corant.config.source.SystemPropertiesConfigSource;
import org.corant.shared.util.ObjectUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * corant-config
 *
 * @author bingo 下午4:04:49
 *
 */
public class CorantConfigBuilder implements ConfigBuilder {

  static final Logger logger = Logger.getLogger(CorantConfigBuilder.class.getName());

  final List<ConfigSource> sources = new ArrayList<>();
  final List<OrdinalConverter> converters =
      new LinkedList<>(CorantConfigConversion.BUILT_IN_CONVERTERS);
  ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

  CorantConfigBuilder() {}

  @Override
  public ConfigBuilder addDefaultSources() {
    addSource(new SystemPropertiesConfigSource());
    addSource(new SystemEnvironmentConfigSource());
    MicroprofileConfigSources.get(getClassLoader()).forEach(this::addSource);
    return this;
  }

  @Override
  public ConfigBuilder addDiscoveredConverters() {
    ServiceLoader.load(Converter.class, getClassLoader()).forEach(this::addConverter);
    return this;
  }

  @Override
  public ConfigBuilder addDiscoveredSources() {
    ServiceLoader.load(ConfigSource.class, getClassLoader()).forEach(this::addSource);
    ServiceLoader.load(ConfigSourceProvider.class, getClassLoader())
        .forEach(csp -> csp.getConfigSources(getClassLoader()).forEach(this::addSource));
    return this;
  }

  @Override
  public Config build() {
    List<ConfigSource> resolvedSources =
        CorantConfigSource.resolveAdjust(sources, getClassLoader());
    logger.fine(() -> String.format("Resolve sources: %n %s",
        String.join("\n", ObjectUtils.asStrings(resolvedSources.stream().map(s -> {
          final StringBuilder sb = new StringBuilder(s.getClass().getName()).append(":\n{\n");
          s.getProperties().forEach((k, v) -> sb.append(k).append(" : ").append(v).append("\n"));
          return sb.append("}\n").toString();
        }).collect(Collectors.toList())))));
    return new CorantConfig(new CorantConfigConversion(converters), resolvedSources);
  }

  @Override
  public ConfigBuilder forClassLoader(ClassLoader loader) {
    classLoader = loader;
    return this;
  }

  @Override
  public <T> ConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
    converters.add(new OrdinalConverter(type, converter, priority));
    return this;
  }

  @Override
  public ConfigBuilder withConverters(Converter<?>... converters) {
    for (Converter<?> converter : converters) {
      addConverter(converter);
    }
    return this;
  }

  @Override
  public ConfigBuilder withSources(ConfigSource... sources) {
    for (ConfigSource source : sources) {
      addSource(source);
    }
    return this;
  }

  void addConverter(Converter<?> converter) {
    final Class<?> cls = converter.getClass();
    Class<?> type = (Class<?>) CorantConfigConversion.getTypeOfConverter(cls);
    shouldNotNull(type, "Converter %s must be a ParameterizedType.", cls);
    converters.add(new OrdinalConverter(type, converter, CorantConfigConversion.findPriority(cls)));
    logger.fine(() -> String.format("Add config converter %s, class loader is %s.", converter,
        getClassLoader()));
  }

  void addSource(ConfigSource source) {
    sources.add(shouldNotNull(source, "Config source can not null."));
    logger.info(() -> String.format(
        "Add config source[%s] include %s items, location is [%s], class loader is %s, source class is [%s].",
        source.getOrdinal(), source.getProperties().size(), source.getName(), getClassLoader(),
        source.getClass().getName()));
  }

  ClassLoader getClassLoader() {
    return classLoader;
  }

}

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
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.corant.config.spi.ConfigAdjuster;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 下午5:47:24
 *
 */
public class CorantConfig implements Config, Serializable {

  private static final long serialVersionUID = 8788710772538278522L;

  final ConfigConversion conversion;
  final AtomicReference<ConfigData> data;

  public CorantConfig(ConfigConversion conversion, ConfigData data) {
    this.conversion = conversion;
    this.data = new AtomicReference<>(data);
  }

  @Override
  public Iterable<ConfigSource> getConfigSources() {
    return defaultObject(data.get().sources, Collections.emptyList());
  }

  public ConfigConversion getConversion() {
    return conversion;
  }

  @Override
  public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
    return Optional.ofNullable(getValue(propertyName, propertyType));
  }

  @Override
  public Iterable<String> getPropertyNames() {
    return defaultObject(data.get().propertyNames, Collections.emptySet());
  }

  public String getRawValue(String propertyName) {
    return data.get().caches.get(propertyName);
  }

  @Override
  public <T> T getValue(String propertyName, Class<T> propertyType) {
    String value = getRawValue(propertyName);
    return forceCast(conversion.convert(value, propertyType));
  }

  public void reset(List<ConfigSource> sources, ConfigAdjuster adjuster) {
    ConfigData newDat = new ConfigData(sources, adjuster);
    for (;;) {
      ConfigData oldDat = data.get();
      if (data.compareAndSet(oldDat, newDat)) {
        return;
      }
    }
  }
}

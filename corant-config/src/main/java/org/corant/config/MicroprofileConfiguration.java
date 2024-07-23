/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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

import static java.util.Collections.emptyList;
import java.util.List;
import java.util.Optional;
import org.corant.shared.ubiquity.Configuration;
import org.corant.shared.ubiquity.TypeLiteral;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午11:47:06
 */
public class MicroprofileConfiguration implements Configuration {

  @Override
  public boolean containsKey(String key) {
    for (ConfigSource cs : getConfig().getConfigSources()) {
      if (cs.getPropertyNames().contains(key)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getAssembledValue(String value) {
    return Configs.assemblyStringConfigProperty(value);
  }

  @Override
  public List<String> getAssembledValues(String value) {
    return Configs.assemblyStringConfigProperties(value);
  }

  @Override
  public Iterable<String> getKeys() {
    return getConfig().getPropertyNames();
  }

  @Override
  public <T> Optional<T> getOptionalValue(String key, Class<T> valueType) {
    return getConfig().getOptionalValue(key, valueType);
  }

  @Override
  public String getValue(String key) {
    return getConfig().getOptionalValue(key, String.class).orElse(null);
  }

  @Override
  public <T> T getValue(String key, Class<T> valueType) {
    return getConfig().getOptionalValue(key, valueType).orElse(null);
  }

  @Override
  public <T> T getValue(String key, TypeLiteral<T> valueTypeLiteral) {
    return getConfig().getOptionalValue(key, valueTypeLiteral).orElse(null);
  }

  @Override
  public <T> List<T> getValues(String key, Class<T> valueType) {
    return getConfig().getOptionalValues(key, valueType).orElse(emptyList());
  }

  protected CorantConfig getConfig() {
    return (CorantConfig) org.eclipse.microprofile.config.ConfigProvider.getConfig();
  }

}

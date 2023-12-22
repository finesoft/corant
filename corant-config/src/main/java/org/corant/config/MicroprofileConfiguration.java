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

import static java.util.Collections.unmodifiableSet;
import static org.corant.shared.util.Sets.setOf;
import java.util.Set;
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
  public Set<String> getKeys() {
    return unmodifiableSet(setOf(getConfig().getPropertyNames()));
  }

  @Override
  public <T> T getValue(String name, Class<T> valueType) {
    return getConfig().getOptionalValue(name, valueType).orElse(null);
  }

  @Override
  public <T> T getValue(String name, TypeLiteral<T> valueTypeLiteral) {
    return getConfig().getOptionalValue(name, valueTypeLiteral).orElse(null);
  }

  protected CorantConfig getConfig() {
    return (CorantConfig) org.eclipse.microprofile.config.ConfigProvider.getConfig();
  }

}

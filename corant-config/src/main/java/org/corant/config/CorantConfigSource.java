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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.corant.config.spi.ConfigAdjuster;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 下午2:57:08
 *
 */
public class CorantConfigSource implements ConfigSource {

  final ConfigSource orginal;
  final Map<String, String> properties;
  final Set<String> propertyNames;

  private CorantConfigSource(ConfigSource orginal, ConfigAdjuster adjuster) {
    this.orginal = orginal;
    properties = Collections.unmodifiableMap(adjuster.apply(orginal.getProperties()));
    propertyNames = Collections.unmodifiableSet(properties.keySet());
  }

  public static CorantConfigSource of(ConfigSource orginal, ConfigAdjuster adjuster) {
    return new CorantConfigSource(orginal, adjuster);
  }

  @Override
  public String getName() {
    return orginal.getName();
  }

  @Override
  public int getOrdinal() {
    return orginal.getOrdinal();
  }

  @Override
  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public Set<String> getPropertyNames() {
    return propertyNames;
  }

  @Override
  public String getValue(String propertyName) {
    return properties.get(propertyName);
  }

}

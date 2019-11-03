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
 * @author bingo 下午5:18:28
 *
 */
public abstract class CorantConfigSource implements ConfigSource {

  protected String name;

  protected int ordinal;

  protected CorantConfigSource() {
    super();
  }

  /**
   * @param name
   * @param ordinal
   */
  protected CorantConfigSource(String name, int ordinal) {
    super();
    this.name = name;
    this.ordinal = ordinal;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getOrdinal() {
    return ordinal;
  }

  @Override
  public String getValue(String propertyName) {
    return getProperties().get(propertyName);
  }

  protected abstract CorantConfigSource withProperties(Map<String, String> properties);

  /**
   * corant-config
   *
   * @author bingo 下午5:18:52
   *
   */
  static class ConfigSourceInUse implements ConfigSource {

    final ConfigSource orginal;
    final Map<String, String> properties;
    final Set<String> propertyNames;
    final boolean corant;

    private ConfigSourceInUse(ConfigSource orginal, ConfigAdjuster adjuster) {
      this.orginal = orginal;
      if (corant = orginal instanceof CorantConfigSource) {
        properties = Collections.unmodifiableMap(adjuster.apply(orginal.getProperties()));
        propertyNames = Collections.unmodifiableSet(properties.keySet());
      } else {
        properties = Collections.emptyMap();
        propertyNames = Collections.emptySet();
      }
    }

    public static ConfigSourceInUse of(ConfigSource orginal, ConfigAdjuster adjuster) {
      return new ConfigSourceInUse(orginal, adjuster);
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
      return corant ? properties : orginal.getProperties();
    }

    @Override
    public Set<String> getPropertyNames() {
      return corant ? propertyNames : orginal.getPropertyNames();
    }

    @Override
    public String getValue(String propertyName) {
      return corant ? properties.get(propertyName) : orginal.getValue(propertyName);
    }

  }
}

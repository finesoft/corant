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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

  static final Comparator<ConfigSource> CONFIG_SOURCE_COMPARATOR = (o1, o2) -> {
    int res = Long.signum((long) o2.getOrdinal() - (long) o1.getOrdinal());
    return res != 0 ? res : o2.getName().compareTo(o1.getName());
  };

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

  /**
   * @see #resolve(List, ConfigAdjuster)
   * @param orginals
   * @param classLoader
   * @return sorted & adjusted config sources
   */
  public static List<ConfigSource> resolve(List<ConfigSource> orginals, ClassLoader classLoader) {
    return resolve(orginals, ConfigAdjuster.resolve(classLoader));
  }

  /**
   * Sort & adjust the config source, we only tweak the configuration sources that CORANT handles
   * and adds, and do not include system environment variables and system properties. Some dynamic
   * non-corant config sources may not be fully processed. All resolved config sources were cache in
   * Config.
   *
   * If the variable value of the configuration item comes from the system properties or environment
   * variable, the value of the configuration item was obtained from the system properties or
   * environment variable at the time of the configuration initialization, and it will not be
   * changed after initialization.
   *
   * @param orginals
   * @param adjuster
   * @return sorted & adjusted config sources
   */
  public static List<ConfigSource> resolve(List<ConfigSource> orginals, ConfigAdjuster adjuster) {
    shouldNotNull(orginals, "The config sources can not null!").sort(CONFIG_SOURCE_COMPARATOR);
    if (adjuster == null) {
      return orginals;
    }
    List<ConfigSource> resolved = new ArrayList<>(orginals.size());
    final List<ConfigSource> orginalSources = new ArrayList<>(orginals);
    for (ConfigSource orginal : orginals) {
      if (orginal instanceof CorantConfigSource) {
        resolved.add(new AdjustedConfigSource((CorantConfigSource) orginal,
            adjuster.apply(orginal.getProperties(), orginalSources)));
      } else {
        resolved.add(orginal);
      }
    }
    return resolved;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * <b>The current implementation:</b> Find {@link ConfigSource#CONFIG_ORDINAL} if is existen and
   * can be converted to Integer then return the value else use {@link #ordinal}
   * </p>
   */
  @Override
  public int getOrdinal() {
    String configOrdinal = getValue(CONFIG_ORDINAL);
    if (configOrdinal != null) {
      try {
        return Integer.parseInt(configOrdinal);
      } catch (NumberFormatException ignored) {
        // Noop
      }
    }
    return ordinal;
  }

  @Override
  public String getValue(String propertyName) {
    return getProperties().get(propertyName);
  }

  /**
   * corant-config
   *
   * @author bingo 下午5:18:52
   *
   */
  static class AdjustedConfigSource implements ConfigSource {

    final ConfigSource orginal;
    final Map<String, String> properties;
    final Set<String> propertyNames;

    AdjustedConfigSource(CorantConfigSource orginal, Map<String, String> newProperties) {
      this.orginal = orginal;
      properties = Collections.unmodifiableMap(newProperties);
      propertyNames = Collections.unmodifiableSet(properties.keySet());
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
}

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
package org.corant.config.source;

import static org.corant.shared.normal.Names.ConfigNames.CFG_ADJUST_PREFIX;
import static org.corant.shared.normal.Priorities.ConfigPriorities.APPLICATION_ADJUST_ORDINAL;
import static org.corant.shared.util.Maps.toMap;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.asDefaultString;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.corant.shared.util.Iterables;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午10:49:14
 *
 */
public class ApplicationAdjustConfigSourceProvider extends ApplicationConfigSourceProvider {

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
    final Properties props = new Properties();
    final String prefix = CFG_ADJUST_PREFIX;
    final int prefixLen = prefix.length();
    System.getProperties().forEach((k, v) -> {
      String key = asDefaultString(k);
      String value = asDefaultString(v);
      if (key.startsWith(prefix)) {
        key = key.substring(prefixLen);
        props.put(key, value);
      }
    });
    if (!props.isEmpty()) {
      return Iterables.iterableOf(new AdjustConfigSource(props));
    }
    return Collections.emptyList();
  }

  /**
   * corant-kernel
   *
   * @author bingo 下午8:56:38
   *
   */
  private static final class AdjustConfigSource implements ConfigSource {
    private final Properties props;

    /**
     * @param props the source config properties
     */
    private AdjustConfigSource(Properties props) {
      this.props = props;
    }

    @Override
    public String getName() {
      return CFG_ADJUST_PREFIX;
    }

    @Override
    public int getOrdinal() {
      return APPLICATION_ADJUST_ORDINAL;
    }

    @Override
    public Map<String, String> getProperties() {
      return toMap(props);
    }

    @Override
    public Set<String> getPropertyNames() {
      return defaultObject(props.stringPropertyNames(), HashSet::new);
    }

    @Override
    public String getValue(String propertyName) {
      return props.getProperty(propertyName);
    }
  }

}

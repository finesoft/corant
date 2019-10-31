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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.corant.config.spi.ConfigAdjuster;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午11:03:42
 *
 */
public class ConfigData {

  public static final Comparator<ConfigSource> CONFIG_SOURCE_COMPARATOR = (o1, o2) -> {
    int res = Long.signum((long) o2.getOrdinal() - (long) o1.getOrdinal());
    return res != 0 ? res : o2.getName().compareTo(o1.getName());
  };

  final List<ConfigSource> sources;
  final Set<String> propertyNames;
  final Map<String, String> caches;

  ConfigData(List<ConfigSource> configSources, ConfigAdjuster adjuster) {
    Collections.sort(configSources, CONFIG_SOURCE_COMPARATOR);
    sources = Collections.unmodifiableList(configSources);
    Set<String> usePropertyNames = new HashSet<>();
    Map<String, String> useCaches = new HashMap<>();
    for (ConfigSource source : configSources) {
      source.getProperties().forEach((k, v) -> {
        usePropertyNames.add(k);
        useCaches.computeIfAbsent(k, n -> v);
      });
    }
    caches = Collections.unmodifiableMap(adjuster.apply(useCaches));
    propertyNames = Collections.unmodifiableSet(usePropertyNames);
  }
}

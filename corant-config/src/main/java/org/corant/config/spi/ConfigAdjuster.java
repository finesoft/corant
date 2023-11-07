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
package org.corant.config.spi;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.corant.shared.ubiquity.Sortable;
import org.corant.shared.util.Services;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 * <p>
 * A config-source adjuster, use to adjust the config-source, for example: the adjuster can be used
 * to do some configuration value decryption works.
 *
 * @author bingo 下午8:50:45
 */
@FunctionalInterface
public interface ConfigAdjuster extends Sortable {

  static ConfigAdjuster resolve(ClassLoader classLoader) {
    ConfigAdjuster adjuster = a -> a;
    List<ConfigAdjuster> discoveredAdjusters =
        Services.selectRequired(ConfigAdjuster.class, classLoader).collect(Collectors.toList());
    discoveredAdjusters.sort(Sortable::reverseCompare);
    for (ConfigAdjuster discoveredAdjuster : discoveredAdjusters) {
      adjuster = adjuster.compose(discoveredAdjuster);
    }
    return adjuster;
  }

  default ConfigAdjuster andThen(ConfigAdjuster after) {
    Objects.requireNonNull(after);
    return a -> after.apply(apply(a));
  }

  ConfigSource apply(ConfigSource originalSource);

  default ConfigAdjuster compose(ConfigAdjuster before) {
    Objects.requireNonNull(before);
    return a -> apply(before.apply(a));
  }

}

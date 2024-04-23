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

import static java.lang.String.format;
import static org.corant.shared.normal.Names.ConfigNames.CFG_PROFILE_KEY;
import static org.corant.shared.normal.Priorities.ConfigPriorities.APPLICATION_PROFILE_ORDINAL;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.split;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.SourceType;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午10:49:14
 */
public class ApplicationProfileConfigSourceProvider extends ApplicationConfigSourceProvider {

  static String[] resolveProfileClassPaths(String[] profiles) {
    return Arrays.stream(profiles)
        .flatMap(p -> Arrays.stream(appExtName).map(e -> metaInf + appBaseNamePrefix + p + e))
        .toArray(String[]::new);
  }

  static String[] resolveProfileLocations(String[] profiles) {
    String locationDir = getLocation();
    return isBlank(locationDir) ? Strings.EMPTY_ARRAY
        : Arrays.stream(profiles)
            .flatMap(p -> Arrays.stream(appExtName).map(e -> locationDir
                + SourceType.decideSeparator(locationDir) + appBaseNamePrefix + p + e))
            .toArray(String[]::new);
  }

  static String[] resolveProfiles() {
    String pfs = Systems.getProperty(CFG_PROFILE_KEY);
    if (isBlank(pfs)) {
      pfs = Systems.getEnvironmentVariable(CFG_PROFILE_KEY);
    }
    return split(pfs, ",");
  }

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
    List<ConfigSource> list = new ArrayList<>();
    Predicate<URL> filter = resolveExPattern();
    String[] profiles = resolveProfiles();
    try {
      if (isNotEmpty(profiles)) {
        String[] locations = resolveProfileLocations(profiles);
        if (isNotEmpty(locations)) {
          // first find locations that designated in system properties or system environment
          logger.fine(() -> format("Load profile config source from designated locations %s.",
              String.join(",", locations)));
          list.addAll(ConfigSourceLoader.load(APPLICATION_PROFILE_ORDINAL, filter, locations));
        }
        // else {
        String[] classPaths = resolveProfileClassPaths(profiles);
        if (isNotEmpty(classPaths)) {
          logger.fine(() -> format("Load profile config source from class paths %s.",
              String.join(",", classPaths)));
          list.addAll(ConfigSourceLoader.load(classLoader, APPLICATION_PROFILE_ORDINAL, filter,
              classPaths));
        }
        // }
      }
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return list;
  }

}

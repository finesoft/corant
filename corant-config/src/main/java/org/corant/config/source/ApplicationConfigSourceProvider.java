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

import static org.corant.shared.normal.Names.ConfigNames.CFG_LOCATION_EXCLUDE_PATTERN;
import static org.corant.shared.normal.Names.ConfigNames.CFG_LOCATION_KEY;
import static org.corant.shared.normal.Priorities.ConfigPriorities.APPLICATION_ORDINAL;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.isBlank;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.corant.config.CorantConfig;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.SourceType;
import org.corant.shared.util.PathMatcher;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * corant-config
 *
 * @author bingo 下午6:34:41
 *
 */
public class ApplicationConfigSourceProvider implements ConfigSourceProvider {

  static Logger logger = Logger.getLogger(ApplicationConfigSourceProvider.class.getName());
  static String appBaseName = CorantConfig.CORANT_CONFIG_SOURCE_BASE_NAME;
  static String appBaseNamePrefix = CorantConfig.CORANT_CONFIG_SOURCE_BASE_NAME_PREFIX;
  static String[] appExtName = {".yaml", ".yml", ".properties", ".json", ".xml"};
  static String metaInf = "META-INF/";

  static String[] classPaths =
      Arrays.stream(appExtName).map(e -> metaInf + appBaseName + e).toArray(String[]::new);

  static String getLocation() {
    String location = Systems.getSystemProperty(CFG_LOCATION_KEY);
    if (isBlank(location)) {
      location = Systems.getSystemEnvValue(CFG_LOCATION_KEY);
    }
    return location;
  }

  static Predicate<URL> resolveExPattern() {
    String cfgUrlExPattern = Systems.getSystemProperty(CFG_LOCATION_EXCLUDE_PATTERN);
    return u -> isBlank(cfgUrlExPattern)
        || !PathMatcher.matchClassPath(u.toExternalForm(), cfgUrlExPattern);
  }

  static String[] resolveLocations() {
    final String locDir = getLocation();
    return isBlank(locDir) ? Strings.EMPTY_ARRAY
        : Arrays.stream(appExtName)
            .map(e -> locDir + SourceType.decideSeparator(locDir) + appBaseName + e)
            .toArray(String[]::new);
  }

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
    List<ConfigSource> list = new ArrayList<>();
    String[] locations = resolveLocations();
    Predicate<URL> filter = resolveExPattern();
    try {
      if (isNotEmpty(locations)) {
        // first find locations that designated in system properties or system environment
        logger.fine(() -> String.format("Load config source from designated locations %s.",
            String.join(",", locations)));
        list.addAll(ConfigSourceLoader.load(APPLICATION_ORDINAL, filter, locations));
      }
      // else {
      logger.fine(() -> String.format("Load config source from class paths %s.",
          String.join(",", classPaths)));
      list.addAll(ConfigSourceLoader.load(classLoader, APPLICATION_ORDINAL, filter, classPaths));
      // }
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return list;
  }

}

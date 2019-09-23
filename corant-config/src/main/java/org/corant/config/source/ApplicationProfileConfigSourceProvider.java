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

import static org.corant.kernel.normal.Names.ConfigNames.CFG_PROFILE_KEY;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.StringUtils.defaultBlank;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.corant.kernel.normal.Priorities.ConfigPriorities;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Resources.SourceType;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午10:49:14
 *
 */
public class ApplicationProfileConfigSourceProvider extends ApplicationConfigSourceProvider {

  static String sysPfPro = System.getProperty(CFG_PROFILE_KEY);
  static String sysPfEvn = System.getenv(CFG_PROFILE_KEY);
  static String[] profiles = split(defaultString(defaultBlank(sysPfPro, sysPfEvn)), ",");

  static String[] pfClassPaths = Arrays.stream(profiles)
      .flatMap(p -> Arrays.stream(appExtName).map(e -> metaInf + appBaseName + "-" + p + e))
      .toArray(String[]::new);

  static String[] pfLocations = isBlank(locationDir) ? new String[0]
      : Arrays.stream(profiles)
          .flatMap(p -> Arrays.stream(appExtName).map(e -> locationDir
              + SourceType.decideSeparator(locationDir) + appBaseName + "-" + p + e))
          .toArray(String[]::new);

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
    List<ConfigSource> list = new ArrayList<>();
    try {
      if (isNotEmpty(pfLocations)) {
        // first find locations that designated in system properties or system environment
        logger.info(String.format("Load profile config source from designated locations %s",
            String.join(",", pfLocations)));
        list.addAll(ConfigSourceLoader.load(ConfigPriorities.APPLICATION_PROFILE_ORDINAL, filter,
            pfLocations));
      } else if (isNotEmpty(pfClassPaths)) {
        logger.info(String.format("Load profile config source from class paths %s",
            String.join(",", pfClassPaths)));
        list.addAll(ConfigSourceLoader.load(classLoader,
            ConfigPriorities.APPLICATION_PROFILE_ORDINAL, filter, pfClassPaths));
      }
      list.forEach(cs -> logger.info(() -> String.format("Loaded profile config source[%s] %s.",
          cs.getOrdinal(), cs.getName())));
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return list;
  }

}

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
import static org.corant.shared.util.StringUtils.defaultBlank;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.PathUtils;
import org.corant.shared.util.Resources.SourceType;
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
  static String appBaseName = "application";
  static String[] appExtName = {".yaml", ".yml", ".properties", ".json", ".xml"};
  static String metaInf = "META-INF/";
  static String sysLcPro = System.getProperty(CFG_LOCATION_KEY);
  static String sysLcEnv = System.getenv(CFG_LOCATION_KEY);
  static String locationDir = defaultString(defaultBlank(sysLcPro, sysLcEnv));
  static String cfgUrlExPattern = System.getProperty(CFG_LOCATION_EXCLUDE_PATTERN);

  static String[] classPaths =
      Arrays.stream(appExtName).map(e -> metaInf + appBaseName + e).toArray(String[]::new);

  static String[] locations = isBlank(locationDir) ? new String[0]
      : Arrays.stream(appExtName)
          .map(e -> locationDir + SourceType.decideSeparator(locationDir) + appBaseName + e)
          .toArray(String[]::new);

  static Predicate<URL> filter = u -> isBlank(cfgUrlExPattern)
      || !PathUtils.matchClassPath(u.toExternalForm(), cfgUrlExPattern);

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
    List<ConfigSource> list = new ArrayList<>();
    try {
      if (isNotEmpty(locations)) {
        // first find locations that designated in system properties or system environment
        logger.info(String.format("Load config source from designated locations %s",
            String.join(",", locations)));
        list.addAll(ConfigSourceLoader.load(APPLICATION_ORDINAL, filter, locations));
      } else {
        logger.info(
            String.format("Load config source from class paths %s", String.join(",", classPaths)));
        list.addAll(ConfigSourceLoader.load(classLoader, APPLICATION_ORDINAL, filter, classPaths));
      }
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return list;
  }

}

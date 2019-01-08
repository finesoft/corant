/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.kernel.config;

import static org.corant.shared.normal.Names.ConfigNames.CFG_PF_KEY;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.ifBlank;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.split;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Priorities.ConfigPriorities;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-kernel
 *
 * @author bingo 上午10:49:14
 *
 */
public class ApplicationProfileConfigSourceProvider extends ApplicationConfigSourceProvider {

  static String sysPfPro = System.getProperty(CFG_PF_KEY);
  static String sysPfEvn = System.getenv(CFG_PF_KEY);
  static String[] profiles = split(defaultString(ifBlank(sysPfPro, sysPfEvn)), ",");

  static String[] pfClassPaths = Arrays.stream(profiles)
      .flatMap(p -> Arrays.stream(appExtName).map(e -> metaInf + appBaseName + "-" + p + e))
      .toArray(String[]::new);

  static String[] pfFilePaths = isBlank(fileDir) ? new String[0]
      : Arrays.stream(profiles)
          .flatMap(p -> Arrays.stream(appExtName)
              .map(e -> fileDir + File.separator + appBaseName + "-" + p + e))
          .toArray(String[]::new);

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
    List<ConfigSource> list = new ArrayList<>();
    try {
      list.addAll(
          ConfigSourceLoader.load(ConfigPriorities.APPLICATION_PROFILE_ORDINAL, pfFilePaths));
      list.addAll(ConfigSourceLoader.load(classLoader, ConfigPriorities.APPLICATION_PROFILE_ORDINAL,
          pfClassPaths));
      list.forEach(cs -> logger
          .info(() -> String.format("Loaded profile config source priority is %s, name is %s.",
              cs.getOrdinal(), cs.getName())));
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return list;
  }

}

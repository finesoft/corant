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

import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.ifBlank;
import static org.corant.shared.util.StringUtils.isBlank;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names.ConfigNames;
import org.corant.shared.normal.Priorities.ConfigPriorities;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * corant-kernel
 *
 * @author bingo 下午6:34:41
 *
 */
public class ApplicationConfigSourceProvider implements ConfigSourceProvider {

  static String appBaseName = "application";
  static String[] appExtName = {".yaml", ".yml", ".properties", ".json", ".xml"};
  static String metaInf = "META-INF/";
  static String sysPro = System.getProperty(ConfigNames.CFG_LOCATION_KEY);
  static String sysEnv = System.getenv(ConfigNames.CFG_LOCATION_KEY);
  static String fileDir = defaultString(ifBlank(sysPro, sysEnv));

  static String[] classPaths =
      Arrays.stream(appExtName).map(e -> metaInf + appBaseName + e).toArray(String[]::new);

  static String[] filePaths = isBlank(fileDir) ? new String[0]
      : Arrays.stream(appExtName).map(e -> fileDir + File.separator + appBaseName + e)
          .toArray(String[]::new);

  @Override
  public Iterable<ConfigSource> getConfigSources(ClassLoader classLoader) {
    List<ConfigSource> list = new ArrayList<>();
    try {
      list.addAll(ConfigSourceLoader.load(ConfigPriorities.APPLICATION_ORDINAL, filePaths));
      list.addAll(
          ConfigSourceLoader.load(classLoader, ConfigPriorities.APPLICATION_ORDINAL, classPaths));
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
    return list;
  }

}

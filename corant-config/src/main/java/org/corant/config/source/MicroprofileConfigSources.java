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

import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.strip;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.corant.config.CorantConfig;
import org.corant.shared.resource.ClassPathResourceLoader;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 下午5:05:04
 *
 */
public class MicroprofileConfigSources {

  public static final int DEFAULT_ORDINAL = 100;
  public static final int DEFAULT_PROFILE_ORDINAL = DEFAULT_ORDINAL + 50;

  public static final String META_INF_MICROPROFILE_CONFIG_PROPERTIES_BASE =
      "META-INF/" + CorantConfig.MP_CONFIG_SOURCE_BASE_NAME;
  public static final String WEB_INF_MICROPROFILE_CONFIG_PROPERTIES_BASE =
      "WEB-INF/classes/META-INF/" + CorantConfig.MP_CONFIG_SOURCE_BASE_NAME;
  public static final String MICROPROFILE_CONFIG_EXT = ".properties";

  public static List<ConfigSource> get(ClassLoader classLoader, String profile) {
    List<ConfigSource> sources = new CopyOnWriteArrayList<>();
    String suffix = MICROPROFILE_CONFIG_EXT;
    final int ordinal;
    if (isNotBlank(profile)) {
      suffix = "-" + strip(profile) + MICROPROFILE_CONFIG_EXT;
      ordinal = DEFAULT_PROFILE_ORDINAL;
    } else {
      ordinal = DEFAULT_ORDINAL;
    }
    try {
      new ClassPathResourceLoader(classLoader, false)
          .load(META_INF_MICROPROFILE_CONFIG_PROPERTIES_BASE + suffix).stream().parallel()
          .map(r -> new PropertiesConfigSource(r.getURL(), ordinal)).forEach(sources::add);
      new ClassPathResourceLoader(classLoader, false)
          .load(WEB_INF_MICROPROFILE_CONFIG_PROPERTIES_BASE + suffix).stream().parallel()
          .map(r -> new PropertiesConfigSource(r.getURL(), ordinal)).forEach(sources::add);
    } catch (IOException e) {
      // Noop
    }
    return sources;
  }

}

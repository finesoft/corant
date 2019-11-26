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

import java.util.LinkedList;
import java.util.List;
import org.corant.shared.util.Resources;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 下午5:05:04
 *
 */
public class MicroprofileConfigSources {

  public static final int DEFAULT_ORDINAL = 100;

  public static final String META_INF_MICROPROFILE_CONFIG_PROPERTIES =
      "META-INF/microprofile-config.properties";
  public static final String WEB_INF_MICROPROFILE_CONFIG_PROPERTIES =
      "WEB-INF/classes/META-INF/microprofile-config.properties";

  public static List<ConfigSource> get(ClassLoader classLoader) {
    List<ConfigSource> sources = new LinkedList<>();
    Resources.tryFromClassPath(classLoader, META_INF_MICROPROFILE_CONFIG_PROPERTIES)
        .map(r -> new PropertiesConfigSource(r.getURL(), DEFAULT_ORDINAL)).forEach(sources::add);
    Resources.tryFromClassPath(classLoader, WEB_INF_MICROPROFILE_CONFIG_PROPERTIES)
        .map(r -> new PropertiesConfigSource(r.getURL(), DEFAULT_ORDINAL)).forEach(sources::add);
    return sources;
  }

}

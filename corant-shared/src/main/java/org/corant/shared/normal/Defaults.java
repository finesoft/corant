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
package org.corant.shared.normal;

import static org.corant.shared.util.Strings.defaultString;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import org.corant.shared.util.Resources;
import org.corant.shared.util.Resources.ClassPathResource;

/**
 * corant-shared
 *
 * @author bingo 上午10:25:36
 *
 */
public interface Defaults {

  String DFLT_CHARSET_STR = "UTF-8";

  Charset DFLT_CHARSET = StandardCharsets.UTF_8;

  int MAX_BUFFERED_BYTES = Integer.MAX_VALUE - 8;

  int ONE_KB = 1024;

  int SIXTEEN_KBS = ONE_KB << 4;

  long ONE_MB = ONE_KB * (long) ONE_KB;

  int CORANT_SIGN = 3259;

  Optional<String> CORANT_VERSION = getCorantVersion();

  static Path corantUserDir(String suffix) {
    return Paths.get(System.getProperty("user.home"))
        .resolve("." + Names.applicationName() + defaultString(suffix));
  }

  static Optional<String> getCorantVersion() {
    String version = Defaults.class.getPackage().getImplementationVersion();
    if (version == null) {
      try {
        // FIXME get version from /META-INF/MANIFEST.MF
        Optional<ClassPathResource> res = Resources
            .fromClassPath("META-INF/maven/org.corant/corant-shared/pom.properties").findFirst();
        if (res.isPresent()) {
          try (InputStream is = res.get().openStream();
              InputStreamReader isr = new InputStreamReader(is, Defaults.DFLT_CHARSET)) {
            Properties properties = new Properties();
            properties.load(isr);
            version = properties.getProperty("version");
            properties.clear();
          }
        }
      } catch (IOException e) {
        // Ignore
      }
    }
    return Optional.ofNullable(version);
  }

}

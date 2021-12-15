/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.config;

import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNoneBlank;
import static org.corant.shared.util.Strings.split;
import java.util.Set;
import org.corant.shared.normal.Names.ConfigNames;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;

/**
 * corant-config
 *
 * @author bingo 下午8:23:22
 *
 */
public class Desensitizer {

  static final Set<String> sensitives = setOf("password", "username", "credential", ".pwd", ".user",
      "secret-key", "secretkey", "secret-access", "public-key", "private-key", "key-id",
      "publickey", "privatekey", "keyid");

  static final boolean enable =
      Systems.getProperty(ConfigNames.CFG_SENSITIVES_ENABLE, Boolean.class, true);

  static {
    streamOf(split(Systems.getProperty(ConfigNames.CFG_SENSITIVES), ",")).forEach(sensitives::add);
  }

  public static String desensitize(String propertyName, String propertyValue) {
    if (enable && isNoneBlank(propertyName, propertyValue)) {
      for (String s : sensitives) {
        if (propertyName.toLowerCase().contains(s)) {
          if (propertyValue.length() > 128) {
            return Strings.ASTERISK.repeat(32).concat("......");
          }
          return Strings.ASTERISK.repeat(propertyValue.length());
        }
      }
    }
    return propertyValue;
  }
}

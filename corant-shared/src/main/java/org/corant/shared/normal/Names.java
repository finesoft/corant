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

import static org.corant.shared.util.Strings.escapedPattern;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import static org.corant.shared.util.Strings.replace;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Systems;

/**
 * corant-shared
 *
 * @author bingo 下午11:41:22
 *
 */
public interface Names {

  String CORANT = "corant";

  char NAME_SPACE_SEPARATOR = '.';

  String NAME_SPACE_SEPARATORS = ".";

  Pattern NAME_SPACE_SPLITTER_PATTERN = escapedPattern("\\", NAME_SPACE_SEPARATORS);

  String NAME_SPACE_SPLITTER_ESCAPES = "\\" + NAME_SPACE_SEPARATORS;

  char DOMAIN_SPACE_SEPARATOR = ':';

  String DOMAIN_SPACE_SEPARATORS = ":";

  String CORANT_PREFIX = CORANT + NAME_SPACE_SEPARATORS;

  String CORANT_APP_NAME_KEY = CORANT_PREFIX + "application-name";

  String CORANT_SYS_IP = CORANT_PREFIX + "system.ip";

  String CORANT_DEV_MODE = CORANT_PREFIX + "development";

  String CORANT_CFG_PREFIX = CORANT_PREFIX + "config";

  static String applicationName() {
    return Systems.getProperty(CORANT_APP_NAME_KEY, CORANT);
  }

  static String[] splitNameSpace(final String nameSpaceString, final boolean removeBlank,
      final boolean strip) {
    String[] array;
    if (nameSpaceString == null
        || (array = NAME_SPACE_SPLITTER_PATTERN.split(nameSpaceString)).length == 0) {
      return Strings.EMPTY_ARRAY;
    }
    String[] result = new String[array.length];
    int i = 0;
    for (String e : array) {
      if (isNotBlank(e) || isBlank(e) && !removeBlank) {
        String te = replace(e, NAME_SPACE_SPLITTER_ESCAPES, NAME_SPACE_SEPARATORS);
        result[i++] = strip ? Strings.strip(te) : te;
      }
    }
    return Arrays.copyOf(result, i);
  }

  interface ConfigNames {
    String CFG_SENSITIVES_ENABLE = CORANT_CFG_PREFIX + NAME_SPACE_SEPARATORS + "sensitive.enable";
    String CFG_SENSITIVES = CORANT_CFG_PREFIX + NAME_SPACE_SEPARATORS + "sensitive.keys";
    String CFG_LOCATION_KEY = CORANT_CFG_PREFIX + NAME_SPACE_SEPARATORS + "location";
    String CFG_PROFILE_KEY = CORANT_CFG_PREFIX + NAME_SPACE_SEPARATORS + "profile";
    String CFG_ADJUST_KEY = CORANT_CFG_PREFIX + NAME_SPACE_SEPARATORS + "adjust";
    String CFG_ADJUST_PREFIX = CFG_ADJUST_KEY + NAME_SPACE_SEPARATORS;
    String CFG_LOCATION_EXCLUDE_PATTERN =
        CORANT_CFG_PREFIX + NAME_SPACE_SEPARATORS + "location.exclude.pattern";
  }

  interface JndiNames {
    String JNDI_ROOT_NME = "java:";
    String JNDI_COMP_NME = "java:comp";
    String JNDI_APPS_NME = "java:app";
  }

  interface PersistenceNames {
    String PU_DFLT_NME = CORANT;
    String PU_NME_KEY = CORANT + NAME_SPACE_SEPARATORS + "persistence-unit-name";
    String PU_ANN = CORANT + NAME_SPACE_SEPARATORS + "persistence-unit";
  }
}

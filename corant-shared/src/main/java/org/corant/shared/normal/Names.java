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

/**
 * corant-shared
 *
 * @author bingo 下午11:41:22
 *
 */
public interface Names {

  String CORANT = "corant";

  int CORANT_SIGN = 0xCA;

  char NAME_SPACE_SEPARATOR = '.';

  char DOMMAIN_SPACE_SEPARATOR = ':';

  String NAME_SPACE_SEPARATORS = ".";

  String DOMAIN_SPACE_SEPARATORS = ":";

  String CORANT_APP_NME_KEY = CORANT + NAME_SPACE_SEPARATORS + "application-name";

  String CORANT_SYS_IP = CORANT + NAME_SPACE_SEPARATORS + "system.ip";

  String CORANT_PREFIX = CORANT + NAME_SPACE_SEPARATORS;

  static String applicationName() {
    return defaultString(System.getProperty(CORANT_APP_NME_KEY), CORANT);
  }

  interface ConfigNames {
    String CFG_LOCATION_KEY = CORANT + NAME_SPACE_SEPARATORS + "config.location";
    String CFG_PROFILE_KEY = CORANT + NAME_SPACE_SEPARATORS + "config.profile";
    String CFG_ADJUST_KEY = CORANT + NAME_SPACE_SEPARATORS + "config.adjust";
    String CFG_ADJUST_PREFIX = CFG_ADJUST_KEY + NAME_SPACE_SEPARATORS;
    String CFG_LOCATION_EXCLUDE_PATTERN =
        CORANT + NAME_SPACE_SEPARATORS + "config.location.exclude.pattern";
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

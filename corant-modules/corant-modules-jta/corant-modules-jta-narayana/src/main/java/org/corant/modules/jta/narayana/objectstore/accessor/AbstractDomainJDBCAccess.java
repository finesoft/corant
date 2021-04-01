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
package org.corant.modules.jta.narayana.objectstore.accessor;

import static org.corant.shared.util.Strings.isNoneBlank;
import static org.corant.shared.util.Strings.split;
import java.util.HashMap;
import java.util.Map;
import org.corant.modules.jta.narayana.objectstore.driver.AbstractDomainJDBCDriver;
import com.arjuna.ats.arjuna.objectstore.jdbc.JDBCAccess;

/**
 * corant-modules-jta-narayana
 *
 * @author bingo 15:30:49
 *
 */
public abstract class AbstractDomainJDBCAccess implements JDBCAccess {

  public static Map<String, String> resolveConfig(String str) {
    Map<String, String> configuration = new HashMap<>();
    for (String s : split(str, ";", true, true)) {
      int pos = s.indexOf('=');
      if (pos > 0) {
        String key = s.substring(0, pos);
        String val = s.substring(pos + 1);
        if (isNoneBlank(key, val)) {
          configuration.put(key, val);
        }
      }
    }
    return configuration;
  }

  public abstract String getDomain();

  public abstract AbstractDomainJDBCDriver getDriver();

}

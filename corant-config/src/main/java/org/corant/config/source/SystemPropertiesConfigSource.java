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

import static org.corant.shared.normal.Priorities.ConfigPriorities.SYSTEM_PROPERTIES_ORDINAL;
import static org.corant.shared.util.Maps.toMap;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.corant.shared.util.Systems;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午11:04:36
 *
 */
public class SystemPropertiesConfigSource implements ConfigSource, Serializable {

  private static final long serialVersionUID = -8695390762272664908L;

  @Override
  public String getName() {
    return "System.properties";
  }

  @Override
  public int getOrdinal() {
    String configOrdinal = getValue(CONFIG_ORDINAL);
    if (configOrdinal != null) {
      try {
        return Integer.parseInt(configOrdinal);
      } catch (NumberFormatException ignored) {

      }
    }
    return SYSTEM_PROPERTIES_ORDINAL;
  }

  @Override
  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(
        toMap(AccessController.doPrivileged((PrivilegedAction<Properties>) System::getProperties)));
  }

  @Override
  public Set<String> getPropertyNames() {
    return getProperties().keySet();
  }

  @Override
  public String getValue(String s) {
    return Systems.getProperty(s);
  }

}

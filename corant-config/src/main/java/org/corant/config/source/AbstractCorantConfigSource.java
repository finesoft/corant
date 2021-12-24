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

import java.io.Serializable;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 下午5:18:28
 *
 */
public abstract class AbstractCorantConfigSource implements ConfigSource, Serializable {

  private static final long serialVersionUID = 8513468393148039580L;

  protected String name;

  protected int ordinal;

  protected AbstractCorantConfigSource() {}

  /**
   * @param name the name of the configuration source. The name might be used for logging or for
   *        analysis of configured values,and also may be used in ordering decisions.
   * @param ordinal the ordinal priority value of this configuration source
   */
  protected AbstractCorantConfigSource(String name, int ordinal) {
    this.name = name;
    this.ordinal = ordinal;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * <b>The current implementation:</b> Find {@link ConfigSource#CONFIG_ORDINAL} if is exist and can
   * be converted to Integer then return the value else use {@link #ordinal}
   * </p>
   */
  @Override
  public int getOrdinal() {
    String configOrdinal = getValue(CONFIG_ORDINAL);
    if (configOrdinal != null) {
      try {
        return Integer.parseInt(configOrdinal);
      } catch (NumberFormatException ignored) {
        // Noop
      }
    }
    return ordinal;
  }

  @Override
  public Set<String> getPropertyNames() {
    return getProperties().keySet();
  }

  @Override
  public String getValue(String propertyName) {
    return getProperties().get(propertyName);
  }
}

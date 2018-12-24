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
package org.corant.kernel.config;

import java.util.Map;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-kernel
 *
 * @author bingo 下午5:18:28
 *
 */
public abstract class AbstractConfigSource implements ConfigSource {

  protected String name;

  protected int ordinal;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getOrdinal() {
    return ordinal;
  }

  @Override
  public String getValue(String propertyName) {
    return getProperties().get(propertyName);
  }

  abstract AbstractConfigSource withProperties(Map<String, String> properties);
}

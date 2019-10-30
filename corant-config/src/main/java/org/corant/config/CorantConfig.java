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
package org.corant.config;

import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 * 
 * @author bingo 下午5:47:24
 *
 */
public class CorantConfig implements Config {

  @Override
  public <T> T getValue(String propertyName, Class<T> propertyType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Iterable<String> getPropertyNames() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Iterable<ConfigSource> getConfigSources() {
    // TODO Auto-generated method stub
    return null;
  }

}

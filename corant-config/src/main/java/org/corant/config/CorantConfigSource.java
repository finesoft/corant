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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 下午5:18:52
 *
 */
public class CorantConfigSource implements ConfigSource, AutoCloseable {

  final ConfigSource delegate;
  final String sourceProfile;

  public CorantConfigSource(ConfigSource delegate, String sourceProfile) {
    this.delegate = delegate;
    this.sourceProfile = sourceProfile;
  }

  @Override
  public void close() throws Exception {
    if (delegate instanceof AutoCloseable) {
      ((AutoCloseable) delegate).close();
    }
  }

  public ConfigSource getDelegate() {
    return delegate;
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public int getOrdinal() {
    return delegate.getOrdinal();
  }

  @Override
  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(delegate.getProperties());
  }

  @Override
  public Set<String> getPropertyNames() {
    return getProperties().keySet();
  }

  public String getSourceProfile() {
    return sourceProfile;
  }

  @Override
  public String getValue(String propertyName) {
    return delegate.getValue(propertyName);
  }

  public <T extends ConfigSource> T unwrap(Class<T> type) {
    if (CorantConfigSource.class.isAssignableFrom(type)) {
      return type.cast(this);
    }
    if (ConfigSource.class.isAssignableFrom(type)) {
      return type.cast(this);
    }
    throw new IllegalArgumentException("Can't unwrap ConfigSource to " + type);
  }
}

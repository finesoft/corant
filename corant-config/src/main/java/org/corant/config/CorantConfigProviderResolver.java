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

import static org.corant.shared.util.Classes.defaultClassLoader;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Classes;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午10:24:08
 */
public class CorantConfigProviderResolver extends ConfigProviderResolver {

  private static final Map<ClassLoader, Config> configs = new HashMap<>();
  private static final StampedLock lock = new StampedLock();

  public void clear() {
    long stamp = lock.readLock();
    try {
      configs.clear();
    } finally {
      lock.unlockRead(stamp);
    }
  }

  @Override
  public ConfigBuilder getBuilder() {
    return new CorantConfigBuilder();
  }

  @Override
  public Config getConfig() {
    return getConfig(Thread.currentThread().getContextClassLoader());
  }

  @Override
  public Config getConfig(ClassLoader classLoader) {
    final ClassLoader useClassLoader = defaultObject(classLoader, defaultClassLoader());
    long stamp = lock.readLock();
    try {
      Config config = configs.get(useClassLoader);
      while (config == null) {
        long writeStamp = lock.tryConvertToWriteLock(stamp);
        if (writeStamp != 0L) {
          stamp = writeStamp;
          config = buildConfig(useClassLoader);
          cacheConfig(useClassLoader, config);
          break;
        } else {
          lock.unlockRead(stamp);
          stamp = lock.writeLock();
        }
      }
      return config;
    } finally {
      lock.unlock(stamp);
    }
  }

  @Override
  public void registerConfig(Config config, ClassLoader classLoader) {
    long stamp = lock.writeLock();
    try {
      cacheConfig(defaultObject(classLoader, Classes::defaultClassLoader), config);
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  @Override
  public void releaseConfig(final Config config) {
    long stamp = lock.writeLock();
    try {
      Iterator<Map.Entry<ClassLoader, Config>> iterator = configs.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<ClassLoader, Config> entry = iterator.next();
        if (entry.getValue() == config) {
          // According to the specification close config sources or converters if necessary
          closeCloseable(config);
          iterator.remove();
        }
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  protected void closeCloseable(Config config) {
    for (ConfigSource cs : config.getConfigSources()) {
      if (cs instanceof AutoCloseable) {
        try {
          ((AutoCloseable) cs).close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    if (config instanceof CorantConfig) {
      ((CorantConfig) config).getConversion().closeCloseableConverters();
    }
  }

  private Config buildConfig(ClassLoader loader) {
    return getBuilder().forClassLoader(loader).addDefaultSources().addDiscoveredSources()
        .addDiscoveredConverters().build();
  }

  private void cacheConfig(ClassLoader classLoader, Config config) {
    configs.put(classLoader, config);
  }

}

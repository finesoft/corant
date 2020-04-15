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

import static org.corant.shared.util.ClassUtils.defaultClassLoader;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ClassUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * corant-config
 *
 * @author bingo 上午10:24:08
 *
 */
public class CorantConfigProviderResolver extends ConfigProviderResolver {

  private static final Map<ClassLoader, Config> configs = new HashMap<>();
  private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();

  public void clear() {
    Lock lock = rwLock.writeLock();
    try {
      lock.lock();
      configs.clear();
    } finally {
      lock.unlock();
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
    Lock lock = rwLock.readLock();
    try {
      lock.lock();
      Config config = configs.get(useClassLoader);
      if (null == config) {
        lock.unlock();
        lock = rwLock.writeLock();
        lock.lock();
        config = buildConfig(useClassLoader);
        cacheConfig(useClassLoader, config);
      }
      return config;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void registerConfig(Config config, ClassLoader classLoader) {
    Lock lock = rwLock.writeLock();
    try {
      lock.lock();
      cacheConfig(defaultObject(classLoader, ClassUtils::defaultClassLoader), config);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void releaseConfig(final Config config) {
    Lock lock = rwLock.readLock();
    try {
      lock.lock();
      Iterator<Map.Entry<ClassLoader, Config>> iterator = configs.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<ClassLoader, Config> entry = iterator.next();
        if (entry.getValue() == config) {
          // According to the specification close config sources or converters if necessary
          for (ConfigSource cs : config.getConfigSources()) {
            if (cs instanceof AutoCloseable) {
              ((AutoCloseable) cs).close();
            }
          }
          if (config instanceof CorantConfig) {
            ((CorantConfig) config).getConversion().closeCloseableConverters();
          }
          iterator.remove();
        }
      }
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    } finally {
      lock.unlock();
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

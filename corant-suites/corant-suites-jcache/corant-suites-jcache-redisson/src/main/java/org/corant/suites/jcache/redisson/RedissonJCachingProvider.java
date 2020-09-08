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
package org.corant.suites.jcache.redisson;

import org.corant.shared.exception.CorantRuntimeException;
import org.redisson.Redisson;
import org.redisson.jcache.JCacheManager;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.OptionalFeature;
import javax.cache.spi.CachingProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.corant.context.Instances.resolve;
import static org.corant.shared.util.Objects.forceCast;

/**
 * corant-suites-jcache-redisson
 *
 * @author bingo 18:53:45
 */
public class RedissonJCachingProvider implements CachingProvider {

  static final String DEFAULT_URI_PATH = "jsr107-default-config";

  static URI defaulturi;

  static {
    try {
      defaulturi = new URI(DEFAULT_URI_PATH);
    } catch (URISyntaxException e) {
      throw new javax.cache.CacheException(e);
    }
  }

  final ConcurrentMap<ClassLoader, ConcurrentMap<URI, CacheManager>> managers =
      new ConcurrentHashMap<>();

  @Override
  public void close() {
    synchronized (managers) {
      for (ClassLoader classLoader : managers.keySet()) {
        close(classLoader);
      }
    }
  }

  @Override
  public void close(ClassLoader classLoader) {
    Map<URI, CacheManager> uri2manager = managers.remove(classLoader);
    if (uri2manager != null) {
      for (CacheManager manager : uri2manager.values()) {
        manager.close();
      }
    }
  }

  @Override
  public void close(URI uri, ClassLoader classLoader) {
    Map<URI, CacheManager> uri2manager = managers.get(classLoader);
    if (uri2manager == null) {
      return;
    }
    CacheManager manager = uri2manager.remove(uri);
    if (manager == null) {
      return;
    }
    manager.close();
    if (uri2manager.isEmpty()) {
      managers.remove(classLoader, Collections.emptyMap());
    }
  }

  @Override
  public CacheManager getCacheManager() {
    return getCacheManager(getDefaultURI(), getDefaultClassLoader());
  }

  @Override
  public CacheManager getCacheManager(URI uri, ClassLoader classLoader) {
    return getCacheManager(uri, classLoader, getDefaultProperties());
  }

  @Override
  public CacheManager getCacheManager(URI uri, ClassLoader classLoader, Properties properties) {
    if (uri == null) {
      uri = getDefaultURI();
    }
    if (uri == null) {
      throw new CacheException("Uri is not defined. Can't load default configuration");
    }

    if (classLoader == null) {
      classLoader = getDefaultClassLoader();
    }

    ConcurrentMap<URI, CacheManager> value = new ConcurrentHashMap<>();
    ConcurrentMap<URI, CacheManager> oldValue = managers.putIfAbsent(classLoader, value);
    if (oldValue != null) {
      value = oldValue;
    }

    CacheManager manager = value.get(uri);
    if (manager != null) {
      return manager;
    }

    Redisson redisson = resolve(Redisson.class);

    // manager = new JCacheManager(redisson, classLoader, this, properties, uri);
    manager = createCacheManager(redisson, classLoader, properties, uri);
    CacheManager oldManager = value.putIfAbsent(uri, manager);
    if (oldManager != null) {
      if (redisson != null) {
        redisson.shutdown();
      }
      manager = oldManager;
    }
    return manager;
  }

  @Override
  public ClassLoader getDefaultClassLoader() {
    return getClass().getClassLoader();
  }

  @Override
  public Properties getDefaultProperties() {
    return new Properties();
  }

  @Override
  public URI getDefaultURI() {
    return defaulturi;
  }

  @Override
  public boolean isSupported(OptionalFeature optionalFeature) {
    // TODO implement support of store_by_reference
    return false;
  }

  protected JCacheManager createCacheManager(
      Redisson redisson, ClassLoader classLoader, Properties properties, URI uri) {
    Constructor<?> cst =
        AccessController.doPrivileged(
            (PrivilegedAction<Constructor<?>>)
                () -> {
                  Constructor<?> c = JCacheManager.class.getDeclaredConstructors()[0];
                  c.setAccessible(true);
                  return c;
                });
    try {
      return forceCast(cst.newInstance(redisson, classLoader, this, properties, uri));
    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException e) {
      throw new CorantRuntimeException(e);
    }
  }
}

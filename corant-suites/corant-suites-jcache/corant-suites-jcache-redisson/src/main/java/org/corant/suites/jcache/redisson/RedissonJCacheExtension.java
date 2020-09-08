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

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;

/**
 * corant <br>
 *
 * @auther sushuaihao 2020/8/25
 * @since
 */
public class RedissonJCacheExtension implements Extension {
  private CacheManager cacheManager;
  private CachingProvider cachingProvider;

  public void observeAfterBeanDiscovery(
      @Observes AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
    cachingProvider =
        Caching.getCachingProvider("org.corant.suites.jcache.redisson.RedissonJCachingProvider");
    cacheManager = this.cachingProvider.getCacheManager();
    afterBeanDiscovery.addBean(new CacheManagerBean(beanManager, cacheManager));
    afterBeanDiscovery.addBean(new CacheProviderBean(beanManager, this.cachingProvider));
  }

  public void onBeforeShutdown(final @Observes BeforeShutdown beforeShutdown) {
    if (cacheManager != null) {
      cacheManager.close();
    }
    if (cachingProvider != null) {
      cachingProvider.close();
    }
  }
}

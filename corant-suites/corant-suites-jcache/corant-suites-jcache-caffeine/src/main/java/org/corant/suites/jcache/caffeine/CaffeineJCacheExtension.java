package org.corant.suites.jcache.caffeine;

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
public class CaffeineJCacheExtension implements Extension {
  private CacheManager cacheManager;
  private CachingProvider cachingProvider;

  public void observeAfterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery,
      final BeanManager beanManager) {
    cachingProvider = Caching.getCachingProvider();
    cacheManager = cachingProvider.getCacheManager();
    afterBeanDiscovery.addBean(new CacheManagerBean(beanManager, cacheManager));
    afterBeanDiscovery.addBean(new CacheProviderBean(beanManager, cachingProvider));
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

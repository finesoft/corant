package org.corant.suites.jcache.caffeine;

import org.jsr107.ri.annotations.cdi.CachePutInterceptor;
import org.jsr107.ri.annotations.cdi.CacheRemoveAllInterceptor;
import org.jsr107.ri.annotations.cdi.CacheRemoveEntryInterceptor;
import org.jsr107.ri.annotations.cdi.CacheResultInterceptor;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;

/**
 * corant <br>
 *
 * @auther sushuaihao 2020/8/25
 * @since
 */
public class CaffeineJCacheExtension implements Extension {
  private CacheManager cacheManager;
  private CachingProvider cachingProvider;

  public void observeAfterBeanDiscovery(
      @Observes AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
    cachingProvider = Caching.getCachingProvider();
    cacheManager = cachingProvider.getCacheManager();
    afterBeanDiscovery.addBean(new CacheManagerBean(beanManager, cacheManager));
    afterBeanDiscovery.addBean(new CacheProviderBean(beanManager, cachingProvider));
  }

  public void observeAfterTypeDiscovery(@Observes AfterTypeDiscovery afterTypeDiscovery) {
    afterTypeDiscovery.getInterceptors().add(CacheResultInterceptor.class);
    afterTypeDiscovery.getInterceptors().add(CacheRemoveEntryInterceptor.class);
    afterTypeDiscovery.getInterceptors().add(CacheRemoveAllInterceptor.class);
    afterTypeDiscovery.getInterceptors().add(CachePutInterceptor.class);
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

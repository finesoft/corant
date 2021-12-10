package org.corant.modules.jcache.shared;

import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jsr107.ri.annotations.DefaultGeneratedCacheKey;
import org.jsr107.ri.annotations.cdi.CachePutInterceptor;
import org.jsr107.ri.annotations.cdi.CacheRemoveAllInterceptor;
import org.jsr107.ri.annotations.cdi.CacheRemoveEntryInterceptor;
import org.jsr107.ri.annotations.cdi.CacheResultInterceptor;

/**
 * corant-modules-jcache-shared
 *
 * @author sushuaihao 2020/8/25
 * @since
 */
public class JCacheExtension implements Extension {

  private boolean enableGlobalAnnotation = ConfigProvider.getConfig()
      .getOptionalValue("corant.jcache.enable-global-annotation", Boolean.class)
      .orElse(Boolean.FALSE);

  protected CachingProvider cachingProvider;

  public static DefaultGeneratedCacheKey resolveDefaultCacheKey(Object... parameters) {
    return new DefaultGeneratedCacheKey(parameters);
  }

  public void observeAfterBeanDiscovery(@Observes AfterBeanDiscovery abd, final BeanManager bm) {
    cachingProvider = Caching.getCachingProvider();
    abd.addBean(new CachingProviderBean(bm, cachingProvider));
  }

  public void observeAfterTypeDiscovery(@Observes AfterTypeDiscovery afterTypeDiscovery) {
    if (enableGlobalAnnotation) {
      // FIXME Will be deprecated in next iteration
      afterTypeDiscovery.getInterceptors().add(CacheResultInterceptor.class);
      afterTypeDiscovery.getInterceptors().add(CacheRemoveEntryInterceptor.class);
      afterTypeDiscovery.getInterceptors().add(CacheRemoveAllInterceptor.class);
      afterTypeDiscovery.getInterceptors().add(CachePutInterceptor.class);
    }
  }

  protected void beforeShutdown(@Observes BeforeShutdown beforeShutdown) {
    if (cachingProvider != null) {
      cachingProvider.close();
    }
  }
}

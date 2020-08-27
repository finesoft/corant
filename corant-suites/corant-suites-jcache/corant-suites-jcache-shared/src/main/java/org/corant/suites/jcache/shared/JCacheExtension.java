package org.corant.suites.jcache.shared;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.Extension;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jsr107.ri.annotations.cdi.CachePutInterceptor;
import org.jsr107.ri.annotations.cdi.CacheRemoveAllInterceptor;
import org.jsr107.ri.annotations.cdi.CacheRemoveEntryInterceptor;
import org.jsr107.ri.annotations.cdi.CacheResultInterceptor;

/**
 * corant <br>
 *
 * @auther sushuaihao 2020/8/25
 * @since
 */
public class JCacheExtension implements Extension {

  private boolean enableGlobalAnnotation = ConfigProvider.getConfig()
      .getOptionalValue("jcache.enable_global_annotation", Boolean.class).orElse(Boolean.TRUE);

  public void observeAfterTypeDiscovery(@Observes AfterTypeDiscovery afterTypeDiscovery) {
    if (enableGlobalAnnotation) {
      afterTypeDiscovery.getInterceptors().add(CacheResultInterceptor.class);
      afterTypeDiscovery.getInterceptors().add(CacheRemoveEntryInterceptor.class);
      afterTypeDiscovery.getInterceptors().add(CacheRemoveAllInterceptor.class);
      afterTypeDiscovery.getInterceptors().add(CachePutInterceptor.class);
    }
  }

}

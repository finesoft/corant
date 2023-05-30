package org.corant.modules.jcache.shared;

import java.util.function.Function;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeShutdown;
import jakarta.enterprise.inject.spi.Extension;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Fields;
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

  public static Object unwrapKey(Object key) {
    if (key instanceof CorantGeneratedCacheKey) {
      return ((CorantGeneratedCacheKey) key).get(0);
    } else if (key instanceof DefaultGeneratedCacheKey) {
      DefaultGeneratedCacheKey ck = (DefaultGeneratedCacheKey) key;
      Object[] data = (Object[]) Fields.getFieldValue("parameters", ck);
      if (data != null) {
        return data[0];
      }
    }
    return key;
  }

  public static <K> K unwrapKey(Object key, Class<K> clazz) {
    return clazz.cast(unwrapKey(key));
  }

  public static <K> K unwrapKey(Object key, Function<Object[], K> converter) {
    if (key instanceof CorantGeneratedCacheKey) {
      return converter.apply(((CorantGeneratedCacheKey) key).parameters());
    } else if (key instanceof DefaultGeneratedCacheKey) {
      DefaultGeneratedCacheKey ck = (DefaultGeneratedCacheKey) key;
      Object[] data = (Object[]) Fields.getFieldValue("parameters", ck);
      converter.apply(data);
    }
    throw new NotSupportedException();
  }

  public static CorantGeneratedCacheKey wrapKey(Object... parameters) {
    return new CorantGeneratedCacheKey(parameters);
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

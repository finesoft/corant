package org.corant.modules.jcache.shared;

import java.lang.annotation.Annotation;
import java.net.URI;
import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.corant.config.Configs;
import org.corant.context.qualifier.SURI;

/**
 * corant-modules-jcache-shared
 *
 * @author sushuaihao 2020/9/8
 * @since
 */
@ApplicationScoped
public class CacheManagerProducer {

  @Inject
  protected CachingProvider cachingProvider;

  @Produces
  @SURI
  @Dependent
  protected CacheManager produce(InjectionPoint injectionPoint) {
    CacheManager cacheManager;
    SURI suri = null;
    for (Annotation ann : injectionPoint.getQualifiers()) {
      if (ann.annotationType().equals(SURI.class)) {
        suri = (SURI) ann;
      }
    }
    if (suri != null) {
      cacheManager = cachingProvider.getCacheManager(
          URI.create(Configs.assemblyStringConfigProperty(suri.value())),
          CacheManagerProducer.class.getClassLoader());
    } else {
      cacheManager = cachingProvider.getCacheManager();
    }
    return cacheManager;
  }
}

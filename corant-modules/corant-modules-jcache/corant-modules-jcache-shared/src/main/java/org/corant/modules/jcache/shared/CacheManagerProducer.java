package org.corant.modules.jcache.shared;

import java.lang.annotation.Annotation;
import java.net.URI;
import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
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

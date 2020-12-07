package org.corant.suites.jcache.shared;

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
import org.corant.context.SURI;

/**
 * corant <br>
 *
 * @author sushuaihao 2020/9/8
 * @since
 */
@ApplicationScoped
public class CacheManagerProducer {

  @Inject
  CachingProvider cachingProvider;

  @Produces
  @SURI
  @Dependent
  CacheManager produce(InjectionPoint injectionPoint) {
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

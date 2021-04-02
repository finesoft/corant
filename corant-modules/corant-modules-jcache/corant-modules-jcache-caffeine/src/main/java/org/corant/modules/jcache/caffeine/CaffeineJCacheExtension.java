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
package org.corant.modules.jcache.caffeine;

import static org.corant.shared.util.Empties.isEmpty;
import javax.cache.Caching;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.ConfigProvider;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;

/**
 * corant-modules-jcache-caffeine
 *
 * @author sushuaihao 2020/8/25
 *
 */
public class CaffeineJCacheExtension implements Extension {

  public static final String CACHE_PROVIDER_NAME = CaffeineCachingProvider.class.getName();

  // config caffeine's caches from this resource
  private String caffeineConfigResource = ConfigProvider.getConfig()
      .getOptionalValue("corant.jcache.caffeine.config.resource", String.class)
      .orElse("META-INF/application.properties");

  public void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery e) {
    System.setProperty("config.resource", caffeineConfigResource);
    if (isEmpty(System.getProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER))) {
      System.setProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER, CACHE_PROVIDER_NAME);
    } else if (!System.getProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER)
        .equals(CACHE_PROVIDER_NAME)) {
      throw new CorantRuntimeException("");
    }
  }
}

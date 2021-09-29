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
package org.corant.modules.jcache.redisson;

import static org.corant.shared.util.Empties.isEmpty;
import javax.cache.Caching;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Systems;

/**
 * corant-modules-jcache-redisson
 *
 * @author sushuaihao 2020/8/25
 * @since
 */
public class RedissonJCacheExtension implements Extension {
  public static final String CACHE_PROVIDER_NAME = RedissonJCachingProvider.class.getName();

  public void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery e) {
    if (isEmpty(Systems.getProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER))) {
      Systems.setProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER, CACHE_PROVIDER_NAME);
    } else if (!Systems.getProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER)
        .equals(CACHE_PROVIDER_NAME)) {
      throw new CorantRuntimeException(
          "Found another caching provider %s, the caching provider in current implementation is exclusive!",
          Systems.getProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER));
    }
  }
}

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
import static org.corant.shared.util.Strings.isNotBlank;
import javax.cache.Caching;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import org.corant.config.Configs;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.util.Systems;
import org.eclipse.microprofile.config.ConfigProvider;
import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;

/**
 * corant-modules-jcache-caffeine
 *
 * @author sushuaihao 2020/8/25
 *
 */
public class CaffeineJCacheExtension implements Extension {

  public static final String CORANT_CAFFE_PREFIX = Names.CORANT_PREFIX + "jcache.caffeine.";
  public static final int CORANT_CAFFE_PREFIX_LEN = CORANT_CAFFE_PREFIX.length();
  public static final String CACHE_CONFIG_RESOURCE_KEY = CORANT_CAFFE_PREFIX + "config.resource";
  public static final String CACHE_CONFIG_KEY_PREFIX = "caffeine.jcache.";
  public static final String CACHE_PROVIDER_NAME = CaffeineCachingProvider.class.getName();

  /**
   * Caffeine JCache supports.
   * <p>
   * Only support one caching provider in a process. Supports three kinds configurations for
   * Caffeine, see below:
   *
   * <p>
   * 1. Use additional configuration resource for Caffeine, the additional configuration resource
   * URI is specified in the cornat configuration properties named
   * '<b>corant.jcache.caffeine.config.resource</b>'.
   * <p>
   * 2. Use the configuration name prefixed with '<b>caffeine.jcache.</b>' in the coarnt
   * configuration properties to configure the Caffeine configuration option information.
   * <p>
   * 3. Use the configuration name prefixed with '<b>corant.jcache.caffeine.</b>' in the coarnt
   * configuration properties to configure the Caffeine configuration option information.
   *
   * <p>
   * Note: Of the above three configurations, the first has the highest priority and is exclusive.
   * the second and third do not guarantee priority. If you do not want to use the first, then it is
   * best to use one of the second or the third.
   *
   *
   * @param e onBeforeBeanDiscovery
   */
  public void onBeforeBeanDiscovery(@Observes BeforeBeanDiscovery e) {
    if (isEmpty(Systems.getProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER))) {
      Systems.setProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER, CACHE_PROVIDER_NAME);
    } else if (!Systems.getProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER)
        .equals(CACHE_PROVIDER_NAME)) {
      throw new CorantRuntimeException(
          "Found another caching provider %s, the caching provider in current implementation is exclusive!",
          Systems.getProperty(Caching.JAVAX_CACHE_CACHING_PROVIDER));
    }
    String configSource;
    if (isNotBlank(configSource = Configs.getValue(CACHE_CONFIG_RESOURCE_KEY, String.class))) {
      Systems.setProperty("config.resource", configSource);
    } else {
      for (String name : ConfigProvider.getConfig().getPropertyNames()) {
        if (name.startsWith(CACHE_CONFIG_KEY_PREFIX)) {
          Systems.setProperty(name, Configs.getValue(name, String.class));
        } else if (name.startsWith(CORANT_CAFFE_PREFIX)) {
          Systems.setProperty(CACHE_CONFIG_KEY_PREFIX + name.substring(CORANT_CAFFE_PREFIX_LEN),
              Configs.getValue(name, String.class));
        }
      }
    }
  }
}

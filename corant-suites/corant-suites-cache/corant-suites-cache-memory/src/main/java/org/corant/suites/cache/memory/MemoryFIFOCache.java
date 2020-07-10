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
package org.corant.suites.cache.memory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * corant-suites-query-shared
 *
 * <p>
 * Unfinish yet!
 *
 * @author bingo 下午2:02:27
 */
public class MemoryFIFOCache<K, V> extends AbstractMemoryCache<K, V> {

  public MemoryFIFOCache(final int cacheSize) {
    maxCacheSize = cacheSize;
    cacheMap = new LinkedHashMap<K, MemoryCacheObject<K, V>>(cacheSize + 1, 0.75f, false) {
      private static final long serialVersionUID = -1536681027894088591L;

      @Override
      protected boolean removeEldestEntry(Map.Entry<K, MemoryCacheObject<K, V>> eldest) {
        return MemoryFIFOCache.this.removeEldestEntry(size());
      }
    };
  }

  protected boolean removeEldestEntry(int currentSize) {
    return currentSize > maxCacheSize;
  }
}

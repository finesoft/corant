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
 * NOTE: NOT FINISHED!!! NEED TO RE-IMPLEMENT ALL!!!
 * <p>
 * Unfinish yet!
 *
 * @author bingo 下午12:54:42
 */
public class MemoryLRUCache<K, V> extends AbstractMemoryCache<K, V> {

  public MemoryLRUCache(final int maxSize) {
    maxCacheSize = maxSize;
    cacheMap = new LinkedHashMap<K, MemoryCacheObject<K, V>>(maxSize + 1, 0.75f, true) {

      private static final long serialVersionUID = 152381119030226885L;

      @Override
      protected boolean removeEldestEntry(final Map.Entry<K, MemoryCacheObject<K, V>> eldest) {
        return MemoryLRUCache.this.removeEldestEntry(size());
      }
    };
  }

  public MemoryLRUCache(final int initialCapacity, final float loadFactor, final int maxSize) {
    maxCacheSize = maxSize;
    cacheMap = new LinkedHashMap<K, MemoryCacheObject<K, V>>(initialCapacity, loadFactor, true) {

      private static final long serialVersionUID = -8204490126870926733L;

      @Override
      protected boolean removeEldestEntry(final Map.Entry<K, MemoryCacheObject<K, V>> eldest) {
        return MemoryLRUCache.this.removeEldestEntry(size());
      }
    };
  }

  protected boolean removeEldestEntry(int currentSize) {
    return currentSize > maxCacheSize;
  }
}

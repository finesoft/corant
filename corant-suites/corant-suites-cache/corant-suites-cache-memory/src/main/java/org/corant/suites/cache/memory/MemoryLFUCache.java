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

import java.util.HashMap;
import java.util.Iterator;

/**
 * corant-suites-query-shared
 *
 * <p>
 * Unfinish yet!
 *
 * @author bingo 下午2:01:05
 */
public class MemoryLFUCache<K, V> extends AbstractMemoryCache<K, V> {

  public MemoryLFUCache(final int maxSize) {
    maxCacheSize = maxSize;
    cacheMap = new HashMap<>(maxSize + 1);
  }

  @Override
  protected void pruneCache() {
    cacheMap.values().stream().min(MemoryCacheObject::compareAccessCount).ifPresent(comin -> {
      long minAccessCount = comin.getAccessCount().get();
      Iterator<MemoryCacheObject<K, V>> values = cacheMap.values().iterator();
      while (values.hasNext()) {
        MemoryCacheObject<K, V> co = values.next();
        long accessCount = co.getAccessCount().get();
        accessCount -= minAccessCount;
        if (accessCount <= 0) {
          values.remove();
          break;
        }
      }
    });
  }
}

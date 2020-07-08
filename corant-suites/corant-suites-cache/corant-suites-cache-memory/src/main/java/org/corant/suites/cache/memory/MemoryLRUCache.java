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

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * corant-suites-query-shared
 *
 * <p>
 * Unfinish yet!
 *
 * @author bingo 下午12:54:42
 */
public class MemoryLRUCache<K, V> implements MemoryCache<K, V> {

  protected final Map<K, V> map;
  protected final ReadWriteLock rwl = new ReentrantReadWriteLock();
  protected final int maxSize;

  public MemoryLRUCache(final int maxSize) {
    this.maxSize = checkSize(maxSize);
    this.map = new LinkedHashMap<K, V>(1 << 4, 0.75f, true) {

      private static final long serialVersionUID = 152381119030226885L;

      @Override
      protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
        return MemoryLRUCache.this.removeEldestEntry(size());
      }
    };
  }

  public MemoryLRUCache(final int initialCapacity, final float loadFactor, final int maxSize) {
    this.maxSize = checkSize(maxSize);
    this.map = new LinkedHashMap<K, V>(initialCapacity, loadFactor, true) {

      private static final long serialVersionUID = -8204490126870926733L;

      @Override
      protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
        return MemoryLRUCache.this.removeEldestEntry(size());
      }
    };
  }

  private static int checkSize(int maxSize) {
    if (maxSize < 1) {
      throw new IllegalArgumentException("maxSize must be >= 1");
    }
    return maxSize;
  }

  @Override
  public void clear() {
    Lock rl = rwl.writeLock();
    try {
      rl.lock();
      map.clear();
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    shouldNotNull(mappingFunction);
    V v = get(key);
    if (v == null) {
      rwl.writeLock().lock();
      try {
        if ((v = map.get(key)) == null) { // try again
          if ((v = mappingFunction.apply(key)) != null) {
            map.put(key, v);
          }
        }
      } finally {
        rwl.writeLock().unlock();
      }
    }
    return v;
  }

  @Override
  public V get(K key) {
    Lock rl = rwl.readLock();
    try {
      rl.lock();
      return map.get(key);
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V put(K key, V value) {
    Lock rl = rwl.writeLock();
    try {
      rl.lock();
      return map.put(key, value);
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V remove(K key) {
    Lock rl = rwl.writeLock();
    try {
      rl.lock();
      return map.remove(key);
    } finally {
      rl.unlock();
    }
  }

  protected boolean removeEldestEntry(int currentSize) {
    return currentSize > maxSize;
  }

}

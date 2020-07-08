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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * corant-suites-query-shared
 *
 * <p>Unfinish yet!
 *
 * @author bingo 下午2:02:27
 */
public class MemoryFIFOCache<K, V> implements MemoryCache<K, V> {

  protected final FIFOMap<K, V> map;
  protected final ReadWriteLock lock = new ReentrantReadWriteLock();

  public MemoryFIFOCache(int maxSize) {
    this.map = new FIFOMap<>(maxSize);
  }

  public MemoryFIFOCache(int initialCapacity, float loadFactor, int maxSize) {
    this.map = new MemoryFIFOCache.FIFOMap<>(initialCapacity, loadFactor, maxSize);
  }

  public MemoryFIFOCache(int initialCapacity, int maxSize) {
    this.map = new MemoryFIFOCache.FIFOMap<>(initialCapacity, maxSize);
  }

  @Override
  public void clear() {
    Lock rl = lock.writeLock();
    try {
      rl.lock();
      map.clear();
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    Lock rl = lock.writeLock();
    try {
      rl.lock();
      return MemoryCache.super.computeIfAbsent(key, mappingFunction);
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V get(K key) {
    Lock rl = lock.readLock();
    try {
      rl.lock();
      return map.get(key);
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V put(K key, V value) {
    Lock rl = lock.writeLock();
    try {
      rl.lock();
      return map.put(key, value);
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V remove(K key) {
    Lock rl = lock.writeLock();
    try {
      rl.lock();
      return map.remove(key);
    } finally {
      rl.unlock();
    }
  }

  static class FIFOMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = -2853520196107667114L;
    private final int maxSize;

    public FIFOMap(int maxSize) {
      this.maxSize = maxSize;
      checkSize();
    }

    public FIFOMap(int initialCapacity, float loadFactor, int maxSize) {
      super(initialCapacity, loadFactor);
      this.maxSize = maxSize;
      checkSize();
    }

    public FIFOMap(int initialCapacity, int maxSize) {
      super(initialCapacity);
      this.maxSize = maxSize;
      checkSize();
    }

    public FIFOMap(Map<? extends K, ? extends V> m, int maxSize) {
      super(m);
      this.maxSize = maxSize;
      checkSize();
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
      return size() > maxSize;
    }

    private void checkSize() {
      if (maxSize < 1) {
        throw new IllegalArgumentException("maxSize must be >= 1");
      }
    }
  }
}

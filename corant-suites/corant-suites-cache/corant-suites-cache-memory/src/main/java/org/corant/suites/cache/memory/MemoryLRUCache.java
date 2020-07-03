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
 * Unfinish yet!
 *
 * @author bingo 下午12:54:42
 *
 */
public class MemoryLRUCache<K, V> implements MemoryCache<K, V> {

  protected final LRUMap<K, V> map;
  protected final ReadWriteLock lock = new ReentrantReadWriteLock();

  public MemoryLRUCache(int maxSize) {
    this.map = new LRUMap<>(maxSize);
  }

  public MemoryLRUCache(int initialCapacity, float loadFactor, boolean accessOrder, int maxSize) {
    this.map = new LRUMap<>(initialCapacity, loadFactor, accessOrder, maxSize);
  }

  public MemoryLRUCache(int initialCapacity, float loadFactor, int maxSize) {
    this.map = new LRUMap<>(initialCapacity, loadFactor, maxSize);
  }

  public MemoryLRUCache(int initialCapacity, int maxSize) {
    this.map = new LRUMap<>(initialCapacity, maxSize);
  }

  public MemoryLRUCache(Map<? extends K, ? extends V> m, int maxSize) {
    this.map = new LRUMap<>(m, maxSize);
  }

  @Override
  public void clear() {
    Lock rl = lock.writeLock();
    try {
      map.clear();
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    Lock rl = lock.writeLock();
    try {
      return MemoryCache.super.computeIfAbsent(key, mappingFunction);
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V get(K key) {
    Lock rl = lock.readLock();
    try {
      return map.get(key);
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V put(K key, V value) {
    Lock rl = lock.writeLock();
    try {
      return map.put(key, value);
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V remove(K key) {
    Lock rl = lock.writeLock();
    try {
      return map.remove(key);
    } finally {
      rl.unlock();
    }
  }

  static class LRUMap<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 7293780572467721980L;
    private final int maxSize;

    public LRUMap(int maxSize) {
      this.maxSize = maxSize;
      checkSize();
    }

    public LRUMap(int initialCapacity, float loadFactor, boolean accessOrder, int maxSize) {
      super(initialCapacity, loadFactor, accessOrder);
      this.maxSize = maxSize;
      checkSize();
    }

    public LRUMap(int initialCapacity, float loadFactor, int maxSize) {
      super(initialCapacity, loadFactor);
      this.maxSize = maxSize;
      checkSize();
    }

    public LRUMap(int initialCapacity, int maxSize) {
      super(initialCapacity);
      this.maxSize = maxSize;
      checkSize();
    }

    public LRUMap(Map<? extends K, ? extends V> m, int maxSize) {
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

package org.corant.suites.cache.memory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import static org.corant.shared.util.Assertions.shouldNotNull;

/**
 * corant <br>
 * code base from jodd-core
 *
 * @auther sushuaihao 2020/7/8
 * @since
 */
public abstract class AbstractMemoryCacheMap<K, V> implements MemoryCache<K, V> {

  protected final ReadWriteLock rwl = new ReentrantReadWriteLock();
  protected Map<K, MemoryCacheObject<K, V>> cacheMap;
  protected int maxCacheSize; // max cache size, 0 = no limit

  @Override
  public void clear() {
    Lock rl = rwl.writeLock();
    try {
      rl.lock();
      cacheMap.clear();
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
        MemoryCacheObject<K, V> memoryCacheObject = cacheMap.get(key); // try again
        if (memoryCacheObject != null) {
          v = memoryCacheObject.cachedObject;
        } else {
          if ((v = mappingFunction.apply(key)) != null) {
            cacheMap.put(key, new MemoryCacheObject<K, V>(key, v));
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
    V getValue = null;
    Lock rl = rwl.readLock();
    try {
      rl.lock();
      MemoryCacheObject<K, V> cacheObject = cacheMap.get(key);
      if (cacheObject != null) {
        getValue = cacheObject.getObject();
      }
      return getValue;
    } finally {
      rl.unlock();
    }
  }

  @Override
  public boolean isFull() {
    if (maxCacheSize == 0) {
      return false;
    }
    return cacheMap.size() >= maxCacheSize;
  }

  @Override
  public void put(K key, V value) {
    shouldNotNull(value);
    Lock rl = rwl.writeLock();
    try {
      rl.lock();
      MemoryCacheObject<K, V> co = new MemoryCacheObject<>(key, value);
      if (isReallyFull(key)) {
        pruneCache();
      }
      cacheMap.put(key, co);
    } finally {
      rl.unlock();
    }
  }

  @Override
  public V remove(K key) {
    V removedValue = null;
    Lock rl = rwl.writeLock();
    try {
      rl.lock();
      MemoryCacheObject<K, V> co = cacheMap.remove(key);
      if (co != null) {
        removedValue = co.cachedObject;
      }
    } finally {
      rl.unlock();
    }
    return removedValue;
  }

  @Override
  public int size() {
    Lock rl = rwl.readLock();
    try {
      rl.lock();
      return cacheMap.size();
    } finally {
      rl.unlock();
    }
  }

  protected boolean isReallyFull(final K key) {
    if (maxCacheSize == 0) {
      return false;
    }
    if (cacheMap.size() >= maxCacheSize) {
      return !cacheMap.containsKey(key);
    } else {
      return false;
    }
  }

  protected abstract void pruneCache();

  class MemoryCacheObject<K1, V1> {
    final K1 key;
    final V1 cachedObject;
    AtomicLong accessCount;

    MemoryCacheObject(final K1 key, final V1 object) {
      this.key = key;
      this.cachedObject = object;
      this.accessCount = new AtomicLong();
    }

    V1 getObject() {
      accessCount.incrementAndGet();
      return cachedObject;
    }
  }
}

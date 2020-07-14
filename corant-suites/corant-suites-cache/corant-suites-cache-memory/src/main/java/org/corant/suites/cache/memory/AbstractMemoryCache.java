package org.corant.suites.cache.memory;

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * corant <br>
 * code base from jodd-core
 *
 * NOTE: NOT FINISHED!!! NEED TO RE-IMPLEMENT ALL!!!
 *
 * @auther sushuaihao 2020/7/8
 * @since
 */
public abstract class AbstractMemoryCache<K, V> implements MemoryCache<K, V> {

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
        MemoryCacheObject<K, V> cacheObject = cacheMap.get(key); // try again
        if (cacheObject != null) {
          v = cacheObject.getValue();
        } else {
          if ((v = mappingFunction.apply(key)) != null) {
            if (isReallyFull(key)) {
              pruneCache();
            }
            cacheMap.put(key, new MemoryCacheObject<>(key, v));
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
    V v = null;
    Lock rl = rwl.readLock();
    try {
      rl.lock();
      MemoryCacheObject<K, V> cacheObject = cacheMap.get(key);
      if (cacheObject != null) {
        v = cacheObject.getValue();
      }
      return v;
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
    V v = null;
    Lock rl = rwl.writeLock();
    try {
      rl.lock();
      MemoryCacheObject<K, V> co = cacheMap.remove(key);
      if (co != null) {
        v = co.value;
      }
    } finally {
      rl.unlock();
    }
    return v;
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

  protected void pruneCache() {
    return;
  }

  static class MemoryCacheObject<CK, CV> {
    final CK key;
    final CV value;
    final AtomicLong accessCount = new AtomicLong();
    final long timestamp = System.currentTimeMillis();

    MemoryCacheObject(final CK key, final CV object) {
      this.key = key;
      this.value = object;
    }

    static int compareAccessCount(MemoryCacheObject<?, ?> o1, MemoryCacheObject<?, ?> o2) {
      return Long.compare(o1.accessCount.get(), o2.accessCount.get());
    }

    static int compareTimestamp(MemoryCacheObject<?, ?> o1, MemoryCacheObject<?, ?> o2) {
      return Long.compare(o1.timestamp, o2.timestamp);
    }

    AtomicLong getAccessCount() {
      return accessCount;
    }

    CK getKey() {
      return key;
    }

    long getTimestamp() {
      return timestamp;
    }

    CV getValue() {
      accessCount.incrementAndGet();
      return value;
    }
  }
}

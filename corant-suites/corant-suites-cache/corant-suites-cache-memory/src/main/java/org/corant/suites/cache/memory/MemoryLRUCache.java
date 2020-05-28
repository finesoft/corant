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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * corant-suites-query-shared
 *
 * Unfinish yet!
 *
 * @author bingo 下午12:54:42
 *
 */
public class MemoryLRUCache<K, V> implements MemoryCache<K, V> {

  final ConcurrentHashMap<K, V> map;
  final ConcurrentLinkedDeque<K> queue;
  final int capacity;
  final ReadWriteLock rwl;

  public MemoryLRUCache(int capacity) {
    this.capacity = capacity;
    map = new ConcurrentHashMap<>(capacity);
    queue = new ConcurrentLinkedDeque<>();
    rwl = new ReentrantReadWriteLock();
  }

  @Override
  public V get(K key) {
    return null;// TODO
  }

  @Override
  public void put(K key, V value) {
    // TODO
  }

}

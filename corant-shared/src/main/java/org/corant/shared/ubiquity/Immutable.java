/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.ubiquity;

import static org.corant.shared.util.Maps.mapOf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * corant-shared
 *
 * @author bingo 下午12:13:46
 */
public interface Immutable {

  /**
   * corant-shared
   *
   * @author bingo 下午6:44:00
   */
  class ImmutableListBuilder<E> {

    final List<E> list = new ArrayList<>();

    public ImmutableListBuilder() {}

    public ImmutableListBuilder(Collection<? extends E> collection) {
      addAll(collection);
    }

    @SafeVarargs
    public ImmutableListBuilder(final E... eles) {
      addAll(eles);
    }

    public ImmutableListBuilder<E> add(final E ele) {
      list.add(ele);
      return this;
    }

    public ImmutableListBuilder<E> addAll(final Collection<? extends E> collections) {
      if (collections != null) {
        list.addAll(collections);
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    public ImmutableListBuilder<E> addAll(final E... eles) {
      if (eles.length > 0) {
        Collections.addAll(list, eles);
      }
      return this;
    }

    public ImmutableListBuilder<E> addAll(final ImmutableListBuilder<? extends E> other) {
      if (other != null) {
        list.addAll(other.list);
      }
      return this;
    }

    public ImmutableListBuilder<E> addAll(final ImmutableSetBuilder<? extends E> other) {
      if (other != null) {
        list.addAll(other.set);
      }
      return this;
    }

    public List<E> build() {
      if (list.isEmpty()) {
        return Collections.emptyList();
      } else if (list.size() == 1) {
        return Collections.singletonList(list.iterator().next());
      } else {
        return Collections.unmodifiableList(list);
      }
    }

    public ImmutableListBuilder<E> clear() {
      list.clear();
      return this;
    }

    public ImmutableListBuilder<E> remove(final E ele) {
      list.remove(ele);
      return this;
    }

    @SuppressWarnings("unchecked")
    public ImmutableListBuilder<E> removeAll(final E... eles) {
      for (E ele : eles) {
        list.remove(ele);
      }
      return this;
    }

    public ImmutableListBuilder<E> removeAll(final ImmutableListBuilder<? extends E> other) {
      if (other != null) {
        list.removeAll(other.list);
      }
      return this;
    }

    public ImmutableListBuilder<E> removeAll(final ImmutableSetBuilder<? extends E> other) {
      if (other != null) {
        list.removeAll(other.set);
      }
      return this;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 下午6:44:00
   */
  class ImmutableMapBuilder<K, V> {

    final Map<K, V> map = new HashMap<>();

    public ImmutableMapBuilder() {}

    public ImmutableMapBuilder(Map<? extends K, ? extends V> map) {
      putAll(map);
    }

    public ImmutableMapBuilder(final Object... eles) {
      putAll(eles);
    }

    public Map<K, V> build() {
      if (map.isEmpty()) {
        return Collections.emptyMap();
      } else {
        return Collections.unmodifiableMap(map);
      }
    }

    public ImmutableMapBuilder<K, V> clear() {
      map.clear();
      return this;
    }

    public ImmutableMapBuilder<K, V> put(K k, V v) {
      map.put(k, v);
      return this;
    }

    public ImmutableMapBuilder<K, V> putAll(
        final ImmutableMapBuilder<? extends K, ? extends V> other) {
      if (other != null) {
        map.putAll(other.map);
      }
      return this;
    }

    public ImmutableMapBuilder<K, V> putAll(final Map<? extends K, ? extends V> map) {
      if (map != null) {
        this.map.putAll(map);
      }
      return this;
    }

    public ImmutableMapBuilder<K, V> putAll(final Object... eles) {
      if (eles.length > 0) {
        Map<K, V> newMap = mapOf(eles);
        putAll(newMap);
      }
      return this;
    }

    public ImmutableMapBuilder<K, V> remove(K k) {
      map.remove(k);
      return this;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 下午6:44:00
   */
  class ImmutableSetBuilder<E> {

    final Set<E> set = new HashSet<>();

    public ImmutableSetBuilder() {}

    public ImmutableSetBuilder(Collection<? extends E> collection) {
      addAll(collection);
    }

    @SafeVarargs
    public ImmutableSetBuilder(final E... eles) {
      addAll(eles);
    }

    public ImmutableSetBuilder<E> add(final E ele) {
      set.add(ele);
      return this;
    }

    public ImmutableSetBuilder<E> addAll(final Collection<? extends E> collections) {
      if (collections != null) {
        set.addAll(collections);
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    public ImmutableSetBuilder<E> addAll(final E... eles) {
      if (eles.length > 0) {
        Collections.addAll(set, eles);
      }
      return this;
    }

    public ImmutableSetBuilder<E> addAll(final ImmutableListBuilder<? extends E> other) {
      if (other != null) {
        set.addAll(other.list);
      }
      return this;
    }

    public ImmutableSetBuilder<E> addAll(final ImmutableSetBuilder<? extends E> other) {
      if (other != null) {
        set.addAll(other.set);
      }
      return this;
    }

    public Set<E> build() {
      if (set.isEmpty()) {
        return Collections.emptySet();
      } else if (set.size() == 1) {
        return Collections.singleton(set.iterator().next());
      } else {
        return Collections.unmodifiableSet(set);
      }
    }

    public ImmutableSetBuilder<E> clear() {
      set.clear();
      return this;
    }

    public ImmutableSetBuilder<E> remove(final E ele) {
      set.remove(ele);
      return this;
    }

    @SuppressWarnings("unchecked")
    public ImmutableSetBuilder<E> removeAll(final E... eles) {
      for (E ele : eles) {
        set.remove(ele);
      }
      return this;
    }

    public ImmutableSetBuilder<E> removeAll(final ImmutableListBuilder<? extends E> other) {
      if (other != null) {
        other.list.forEach(set::remove);
      }
      return this;
    }

    public ImmutableSetBuilder<E> removeAll(final ImmutableSetBuilder<? extends E> other) {
      if (other != null) {
        set.removeAll(other.set);
      }
      return this;
    }
  }
}

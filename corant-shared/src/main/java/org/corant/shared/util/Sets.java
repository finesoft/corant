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
package org.corant.shared.util;

import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.collectionOf;
import static org.corant.shared.util.Objects.forceCast;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * corant-shared
 *
 * @author bingo 下午2:18:32
 *
 */
public class Sets {

  /**
   * The relative complement, or difference, of the specified left and right set. Namely, the
   * resulting set contains all the elements that are in the left set but not in the right set.
   * Neither input is mutated by this operation, an entirely new set is returned.
   *
   * @param <T>
   * @param left
   * @param right
   * @return difference
   */
  public static <T> Set<T> difference(Set<T> left, Set<T> right) {
    Set<T> diffs = new HashSet<>();
    if (isNotEmpty(left)) {
      if (isEmpty(right)) {
        diffs.addAll(left);
      } else {
        left.stream().filter(k -> !right.contains(k)).forEach(diffs::add);
      }
    }
    return diffs;
  }

  /**
   * Convert an array to immutable set
   *
   * @param <T>
   * @param objects the array
   * @return an immutable set that combined by the passed in array
   */
  @SafeVarargs
  public static <T> Set<T> immutableSetOf(final T... objects) {
    if (objects == null || objects.length == 0) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(setOf(objects));
  }

  /**
   * Convert an array to linked hash set
   *
   * @param <T>
   * @param objects
   * @return a linked hash set that combined by the passed in array
   */
  @SafeVarargs
  public static <T> LinkedHashSet<T> linkedHashSetOf(final T... objects) {
    return collectionOf(LinkedHashSet::new, objects);
  }

  /**
   * Collections.newSetFromMap(new ConcurrentHashMap<>());
   *
   * @param <T>
   * @return newConcurrentHashSet
   */
  public static <T> Set<T> newConcurrentHashSet() {
    return Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  /**
   * Convert an iterable to linked hash set
   *
   * @param <T>
   * @param iterable
   * @return setOf
   */
  public static <T> Set<T> setOf(final Iterable<T> iterable) {
    if (iterable instanceof Set) {
      return forceCast(iterable);
    } else if (iterable != null) {
      return collectionOf(HashSet::new, iterable.iterator());
    } else {
      return new HashSet<>();
    }
  }

  /**
   * Convert an iterator to linked hash set
   *
   * @param <T>
   * @param iterator
   * @return setOf
   */
  public static <T> Set<T> setOf(final Iterator<T> iterator) {
    return collectionOf(HashSet::new, iterator);
  }

  /**
   * Convert an array to set
   *
   * @param <T>
   * @param objects
   * @return a set that combined by the passed in array
   */
  @SafeVarargs
  public static <T> Set<T> setOf(final T... objects) {
    return collectionOf(HashSet::new, objects);
  }
}

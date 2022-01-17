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
import static org.corant.shared.util.Iterables.collectionOf;
import static org.corant.shared.util.Objects.forceCast;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
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
   * @param <E> the element type
   * @param left the source of the different elements
   * @param right the set for comparison
   * @return a new set contains the elements that are in the left set but not in the right set.
   */
  public static <E> Set<E> difference(Set<? extends E> left, Set<? extends E> right) {
    Set<E> diffs = new HashSet<>();
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
   * Convert an array to a non-null immutable set
   *
   * @param <E> the element type
   * @param objects the source of the elements in the set
   * @return an immutable set that combined by the passed in array
   */
  @SafeVarargs
  public static <E> Set<E> immutableSetOf(final E... objects) {
    if (objects == null || objects.length == 0) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(setOf(objects));
  }

  /**
   * Convert an array to a non-null linked hash set
   *
   * @param <E> the element type
   * @param objects the source of the elements in the set
   * @return a linked hash set that combined by the passed in array
   */
  @SafeVarargs
  public static <E> LinkedHashSet<E> linkedHashSetOf(final E... objects) {
    return collectionOf(LinkedHashSet::new, objects);
  }

  /**
   * Collections.newSetFromMap(new ConcurrentHashMap<>());
   *
   * @param <T> the element type
   * @return newConcurrentHashSet
   */
  public static <T> Set<T> newConcurrentHashSet() {
    return Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  /**
   * Constructs a new hash set containing the elements in the specified collection, null safe.
   *
   * @see HashSet#HashSet(Collection)
   *
   * @param <E> the element type
   * @param initials the collection whose elements are to be placed into the hash set
   */
  public static <E> Set<E> newHashSet(final Collection<E> initials) {
    return initials == null ? new HashSet<>() : new HashSet<>(initials);
  }

  /**
   * Constructs a new linked hash set containing the elements in the specified collection, null
   * safe.
   *
   * @see LinkedHashSet#LinkedHashSet(Collection)
   *
   * @param <E> the element type
   * @param initials the collection whose elements are to be placed into the hash set
   */
  public static <E> Set<E> newLinkedHashSet(final Collection<E> initials) {
    return initials == null ? new LinkedHashSet<>() : new LinkedHashSet<>(initials);
  }

  /**
   * Convert an array to a non-null set
   *
   * @param <E> the element type
   * @param objects the source of the elements in the set
   * @return a set that combined by the passed in array
   */
  @SafeVarargs
  public static <E> Set<E> setOf(final E... objects) {
    return collectionOf(HashSet::new, objects);
  }

  /**
   * Convert an iterable to a non-null linked hash set.
   * <p>
   * Note:If the given iterable object itself is a set, return the object itself directly.
   *
   * @param <E> the element type
   * @param iterable the source of the elements in the set
   * @return a new hash set consisting of elements iterated by the passed iterable
   */
  public static <E> Set<E> setOf(final Iterable<? extends E> iterable) {
    if (iterable instanceof Set) {
      return forceCast(iterable);
    } else if (iterable != null) {
      return collectionOf(HashSet::new, iterable.iterator());
    } else {
      return new HashSet<>();
    }
  }

  /**
   * Convert an iterator to a non-null linked hash set
   *
   * @param <E> the element type
   * @param iterator the source of the elements in the set
   * @return a new hash set consisting of elements iterated by the passed iterator
   */
  public static <E> Set<E> setOf(final Iterator<? extends E> iterator) {
    return collectionOf(HashSet::new, iterator);
  }

  /**
   * Convert an array to a non-null tree set
   *
   * @param <E> the element type
   * @param comparator the sort comparator
   * @param objects the objects that will be collected to tree set
   */
  @SafeVarargs
  public static <E> TreeSet<E> treeSetOf(final Comparator<? super E> comparator,
      final E... objects) {
    TreeSet<E> set = comparator == null ? new TreeSet<>() : new TreeSet<>(comparator);
    if (objects != null) {
      Collections.addAll(set, objects);
    }
    return set;
  }

  /**
   * Returns a new set containing the given collections. The Set.addAll(Collection) operation is
   * used to append the given collections into a new set.
   *
   * @param <E> the element types
   * @param collections the collections to be union
   */
  @SafeVarargs
  public static <E> Set<E> union(Collection<? extends E>... collections) {
    Set<E> union = new LinkedHashSet<>();
    if (collections.length > 0) {
      for (Collection<? extends E> collection : collections) {
        if (collection != null) {
          union.addAll(collection);
        }
      }
    }
    return union;
  }
}

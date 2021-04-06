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

import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Iterables.collectionOf;
import static org.corant.shared.util.Objects.forceCast;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * corant-shared
 *
 * @author bingo 上午12:31:10
 */
public class Lists {

  private Lists() {}

  /**
   * <p>
   * Appends all the elements of the given arrays into a new array.
   * <p>
   * The new array contains all of the element of {@code src} followed by all of the elements
   * {@code ts}. When an array is returned, it is always a new array.
   *
   * @param <T>
   * @param src
   * @param ts
   * @return append
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] append(T[] src, T... ts) {
    if (src == null) {
      return ts.clone();
    }
    final Class<?> st = src.getClass().getComponentType();
    final T[] appendArray = (T[]) Array.newInstance(st, src.length + ts.length);
    System.arraycopy(src, 0, appendArray, 0, src.length);
    try {
      System.arraycopy(ts, 0, appendArray, src.length, ts.length);
    } catch (ArrayStoreException e) {
      Class<?> tt = ts.getClass().getComponentType();
      if (!st.isAssignableFrom(tt)) {
        throw new IllegalArgumentException(
            "Cannot append " + tt.getName() + " in an array of " + st.getName(), e);
      }
      throw e;
    }
    return appendArray;
  }

  /**
   * Returns the element at the specified position in the list. If the passing index is negative
   * means that search element from last to first position.
   *
   * <pre>
   * example:
   * get(list,-1) equals list.get(list.size()-1)
   * get(list,-2) equals list.get(list.size()-2)
   * </pre>
   *
   * @param <T>
   * @param list
   * @param index
   * @return get
   */
  public static <T> T get(List<? extends T> list, int index) {
    return index < 0 ? list.get(sizeOf(list) + index) : list.get(index);
  }

  /**
   * Convert an array to a non-null immutable list
   *
   * @param <T>
   * @param objects
   * @return an immutable list that combined by the passed in array
   */
  @SafeVarargs
  public static <T> List<T> immutableListOf(final T... objects) {
    if (objects == null || objects.length == 0) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(listOf(objects));
  }

  /**
   * Convert an array to a non-null linked list
   *
   * @param <T>
   * @param objects
   * @return a linked list that combined by the passed in array
   */
  @SafeVarargs
  public static <T> LinkedList<T> linkedListOf(final T... objects) {
    LinkedList<T> list = new LinkedList<>();
    if (objects != null) {
      for (T object : objects) {
        list.add(object);
      }
    }
    return list;
  }

  /**
   * Convert an enumeration to a non-null list
   *
   * @param <T>
   * @param enumeration
   * @return a list that combined by the passed in enumeration
   */
  public static <T> List<T> listOf(final Enumeration<? extends T> enumeration) {
    List<T> list = new ArrayList<>();
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
        list.add(enumeration.nextElement());
      }
    }
    return list;
  }

  /**
   * Convert an iterable to a non-null list
   *
   * @param <T>
   * @param iterable
   * @return a list that combined by the passed in iterable
   */
  public static <T> List<T> listOf(final Iterable<? extends T> iterable) {
    if (iterable instanceof List) {
      return forceCast(iterable);
    } else if (iterable != null) {
      return listOf(iterable.iterator());
    } else {
      return new ArrayList<>();
    }
  }

  /**
   * Convert an iterator to a non-null list
   *
   * @param <T>
   * @param iterator
   * @return a list that combined by the passed in iterator
   */
  public static <T> List<T> listOf(final Iterator<? extends T> iterator) {
    List<T> list = new ArrayList<>();
    if (iterator != null) {
      while (iterator.hasNext()) {
        list.add(iterator.next());
      }
    }
    return list;
  }

  /**
   * Convert an array to non-null list
   *
   * @param <T>
   * @param objects
   * @return a list that combined by the passed in array
   */
  @SafeVarargs
  public static <T> List<T> listOf(final T... objects) {
    return collectionOf(ArrayList::new, objects);
  }

  /**
   * Break a collection into smaller non-null pieces
   *
   * @param <T>
   * @param collection
   * @param size
   * @return partition
   */
  public static <T> List<List<T>> partition(final Collection<T> collection, int size) {
    List<List<T>> result = new ArrayList<>();
    if (collection != null) {
      final AtomicInteger counter = new AtomicInteger(0);
      result.addAll(collection.stream()
          .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size)).values());
    }
    return result;
  }

  /**
   * Null safe removeIf, execution begins only if the parameters passed in are not null.
   *
   * @param <C>
   * @param <T>
   * @param collection
   * @param p
   * @return removeIf
   */
  public static <C extends Collection<T>, T> C removeIf(final C collection,
      Predicate<? super T> p) {
    if (collection != null && p != null) {
      collection.removeIf(p);
    }
    return collection;
  }

  /**
   * Null safe removeIf, execution begins only if the parameters passed in are not null.
   *
   * <p>
   * This method returns a new array with the same elements of the input array except the element
   * that pass predicate tests. The component type of the returned array is always the same as that
   * of the input array.
   *
   * @param <T>
   * @param src
   * @param predicate
   * @return removeIf
   */
  @SuppressWarnings("unchecked")
  public static <T> T[] removeIf(T[] src, Predicate<? super T> predicate) {
    if (src == null || predicate == null) {
      return src;
    }
    final Class<?> st = src.getClass().getComponentType();
    final T[] removedArray = (T[]) Array.newInstance(st, src.length);
    int j = 0;
    for (T element : src) {
      if (!predicate.test(element)) {
        removedArray[j++] = element;
      }
    }
    return Arrays.copyOf(removedArray, j);
  }

  /**
   * Swaps the elements at the specified positions in the specified list.(If the specified positions
   * are equal, invoking this method leaves the list unchanged.)
   *
   * @param <T>
   * @param l
   * @param i
   * @param j swap
   */
  public static <T> void swap(List<? extends T> l, int i, int j) {
    Collections.swap(l, i, j);
  }

  /**
   * Swaps the two specified elements in the specified array.
   *
   * @param <T>
   * @param a
   * @param i
   * @param j swap
   */
  public static <T> void swap(T[] a, int i, int j) {
    final T t = a[i];
    a[i] = a[j];
    a[j] = t;
  }
}

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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Iterables.collectionOf;
import static org.corant.shared.util.Objects.areEqual;
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
import org.corant.shared.ubiquity.Mutable.MutableInteger;

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
   * @param <E> the element type
   * @param src the first array whose elements are added to the new array
   * @param ts the second array whose elements are added to the new array
   * @return append
   */
  @SuppressWarnings("unchecked")
  public static <E> E[] append(E[] src, E... ts) {
    if (src == null || src.length == 0) {
      return ts.clone();
    } else if (ts == null || ts.length == 0) {
      return src.clone();
    }
    final Class<?> st = src.getClass().getComponentType();
    final E[] appendArray = (E[]) Array.newInstance(st, src.length + ts.length);
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
   * <p>
   * Appends all the elements of the given arrays which the given source array not contains into a
   * new array.
   * <p>
   * The new array contains all of the element of {@code src} followed by all of the {@code ts}
   * elements that not contains in the {@code src}. When an array is returned, it is always a new
   * array.
   *
   * @param <E> the element type
   * @param src the first array whose elements are added to the new array
   * @param ts the second array whose elements are added to the new array
   * @return append
   */
  @SuppressWarnings("unchecked")
  public static <E> E[] appendIfAbsent(E[] src, E... ts) {
    if (src == null || src.length == 0) {
      return ts.clone();
    } else if (ts == null || ts.length == 0) {
      return src.clone();
    }
    final Class<?> st = src.getClass().getComponentType();
    final E[] appendArray = (E[]) Array.newInstance(st, src.length + ts.length);
    System.arraycopy(src, 0, appendArray, 0, src.length);
    int length = src.length;
    try {
      for (E e : ts) {
        boolean contains = false;
        for (E s : appendArray) {
          if (areEqual(e, s)) {
            contains = true;
            break;
          }
        }
        if (!contains) {
          appendArray[length] = e;
          length++;
        }
      }
    } catch (ArrayStoreException e) {
      Class<?> tt = ts.getClass().getComponentType();
      if (!st.isAssignableFrom(tt)) {
        throw new IllegalArgumentException(
            "Cannot append " + tt.getName() + " in an array of " + st.getName(), e);
      }
      throw e;
    }
    return Arrays.copyOf(appendArray, length);
  }

  /**
   * Expand the {@link Arrays#copyOfRange(Object[], int, int)}, add convenient reverse index
   * support. When the given index>=0, the processing process is the same as
   * {@link Arrays#copyOfRange(Object[], int, int)}, when the index<0, the reverse index is used.
   *
   * @param <E> the element type
   * @param original the array from which a range is to be copied
   * @param from the initial index of the range to be copied, inclusive
   * @param to the final index of the range to be copied, exclusive.(This index may lie outside the
   *        array.)
   * @see Arrays#copyOfRange(Object[], int, int)
   */
  public static <E> E[] copyOfRange(E[] original, int from, int to) {
    if (original == null) {
      return null;
    }
    int length = original.length;
    int beginIndex = from < 0 ? length + from : from;
    int endIndex = to < 0 ? length + to : to;
    return Arrays.copyOfRange(original, beginIndex, endIndex);
  }

  /**
   * Remove duplicate elements of the array and return a new array of unique elements sorted in the
   * original order. Return null if the given {@code src} array is null, return an empty new array
   * if the given {@code src} array is empty.
   *
   * @param <E> the element type
   * @param src the original array
   */
  @SuppressWarnings("unchecked")
  public static <E> E[] distinct(E[] src) {
    if (src == null) {
      return null;
    } else {
      final int len = src.length;
      final E[] distinct = (E[]) Array.newInstance(src.getClass().getComponentType(), len);
      if (len == 0) {
        return distinct;
      } else {
        int index = 0;
        for (int i = 0; i < len; i++) {
          boolean contains = false;
          for (int j = 0; j < i; j++) {
            if (areEqual(src[i], src[j])) {
              contains = true;
              break;
            }
          }
          if (!contains) {
            distinct[index] = src[i];
            index++;
          }
        }
        return Arrays.copyOf(distinct, index);
      }
    }
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
   * @param <E> the element type
   * @param list the list to get a value from
   * @param index the index to get
   */
  public static <E> E get(List<? extends E> list, int index) {
    return index < 0 ? list.get(sizeOf(list) + index) : list.get(index);
  }

  /**
   * Convert and returns the element at the specified position in the list. If the passing index is
   * negative means that search element from last to first position.
   *
   * @param <T> the element type
   * @param list the list to get a value from
   * @param index the index to get
   * @param clazz the return class
   *
   * @see #get(List, int)
   */
  public static <T> T get(List<?> list, int index, Class<T> clazz) {
    return toObject(get(list, index), clazz);
  }

  /**
   * Convert an array to a non-null immutable list
   *
   * @param <E> the element type
   * @param objects the objects to construct an immutable list
   * @return an immutable list that combined by the passed in array
   */
  @SafeVarargs
  public static <E> List<E> immutableListOf(final E... objects) {
    if (objects == null || objects.length == 0) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(listOf(objects));
  }

  /**
   * Convert an array to a non-null linked list
   *
   * @param <E> the element type
   * @param objects the objects to construct a linked list
   * @return a linked list that combined by the passed in array
   */
  @SafeVarargs
  public static <E> LinkedList<E> linkedListOf(final E... objects) {
    LinkedList<E> list = new LinkedList<>();
    if (objects != null) {
      Collections.addAll(list, objects);
    }
    return list;
  }

  /**
   * Convert an array to non-null list
   *
   * @param <E> the element type
   * @param objects the objects to construct a list
   * @return a list that combined by the passed in array
   */
  @SafeVarargs
  public static <E> List<E> listOf(final E... objects) {
    return collectionOf(ArrayList::new, objects);
  }

  /**
   * Convert an enumeration to a non-null list
   *
   * @param <E> the element type
   * @param enumeration the elements that the list should contain
   * @return a list that combined by the passed in enumeration
   */
  public static <E> List<E> listOf(final Enumeration<? extends E> enumeration) {
    List<E> list = new ArrayList<>();
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
        list.add(enumeration.nextElement());
      }
    }
    return list;
  }

  /**
   * Convert an iterable to a non-null list.
   * <p>
   * Note: If the given iterable object itself is a list, return the object itself directly.
   *
   * @param <E> the element type
   * @param iterable the elements that the list should contain
   * @return a list that combined by the passed in iterable
   */
  public static <E> List<E> listOf(final Iterable<? extends E> iterable) {
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
   * @param <E> the element type
   * @param iterator the elements that the list should contain
   * @return a list that combined by the passed in iterator
   */
  public static <E> List<E> listOf(final Iterator<? extends E> iterator) {
    List<E> list = new ArrayList<>();
    if (iterator != null) {
      while (iterator.hasNext()) {
        list.add(iterator.next());
      }
    }
    return list;
  }

  /**
   * Constructs a new array list containing the elements in the specified collection, null safe.
   *
   * @see ArrayList#ArrayList(Collection)
   *
   * @param <E> the element type
   * @param initials the collection whose elements are to be placed into the array list
   */
  public static <E> List<E> newArrayList(final Collection<E> initials) {
    return initials == null ? new ArrayList<>() : new ArrayList<>(initials);
  }

  /**
   * Break a collection into smaller non-null pieces
   *
   * @param <E> the element type
   * @param collection the elements that the smaller list should contain
   * @param size the pieces list size
   */
  public static <E> List<List<E>> partition(final Collection<E> collection, int size) {
    List<List<E>> result = new ArrayList<>();
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
   * @param <C> the collection type
   * @param <E> the element type
   * @param collection the collection that elements will be removed
   * @param p the predicate which returns true for elements to be removed
   */
  public static <C extends Collection<E>, E> C removeIf(final C collection,
      Predicate<? super E> p) {
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
   * @param <E> the collection type
   * @param src the array that elements will be removed
   * @param predicate the predicate which returns true for elements to be removed
   */
  @SuppressWarnings("unchecked")
  public static <E> E[] removeIf(E[] src, Predicate<? super E> predicate) {
    if (src == null || predicate == null) {
      return src;
    }
    final Class<?> st = src.getClass().getComponentType();
    final E[] removedArray = (E[]) Array.newInstance(st, src.length);
    int j = 0;
    for (E element : src) {
      if (!predicate.test(element)) {
        removedArray[j++] = element;
      }
    }
    return Arrays.copyOf(removedArray, j);
  }

  /**
   * Split a collection into sub-lists with size.
   *
   * @param <E> the element type
   * @param size the sub-list size
   * @param collection the collection to split
   */
  public static <E> List<List<E>> split(int size, Collection<E> collection) {
    shouldBeTrue(size > 0 && collection != null);
    final MutableInteger counter = new MutableInteger(0);
    return new ArrayList<>(collection.stream()
        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size)).values());
  }

  /**
   * Expand the {@link List#subList(int, int)}, add convenient reverse index support. When the given
   * index>=0, the processing process is the same as {@link List#subList(int, int)}, when the
   * index<0, the reverse index is used.
   *
   * @param <E> the element type
   * @param list the list to gain the sub list
   * @param fromIndex low-end point (inclusive) of the subList
   * @param toIndex high-end point (exclusive) of the subList
   *
   * @see List#subList(int, int)
   */
  public static <E> List<E> subList(List<E> list, int fromIndex, int toIndex) {
    if (list == null) {
      return null;
    }
    int size = list.size();
    int beginIndex = fromIndex < 0 ? size + fromIndex : fromIndex;
    int endIndex = toIndex < 0 ? size + toIndex : toIndex;
    return list.subList(beginIndex, endIndex);
  }

  /**
   * Swaps the two specified elements in the specified array.
   *
   * @param <E> the element type
   * @param a the array in which the elements at two positions will be swapped
   * @param i the position index
   * @param j the other position index
   */
  public static <E> void swap(E[] a, int i, int j) {
    final E t = a[i];
    a[i] = a[j];
    a[j] = t;
  }

  /**
   * Swaps the elements at the specified positions in the specified list.(If the specified positions
   * are equal, invoking this method leaves the list unchanged.)
   *
   * @param <E> the element type
   * @param l the list in which the elements at two positions will be swapped
   * @param i the position index
   * @param j the other position index
   */
  public static <E> void swap(List<? extends E> l, int i, int j) {
    Collections.swap(l, i, j);
  }

  /**
   * Returns a new list containing the given collections. The List.addAll(Collection) operation is
   * used to append the given collections into a new list.
   *
   * @param <E> the element type
   * @param collections the collections to be union
   */
  @SafeVarargs
  public static <E> List<E> union(Collection<? extends E>... collections) {
    List<E> union = new ArrayList<>();
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

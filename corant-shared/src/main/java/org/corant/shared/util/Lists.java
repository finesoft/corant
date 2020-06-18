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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.corant.shared.util.Lists.ListJoins.JoinType;

/**
 *
 * @author bingo 上午12:31:10
 *
 */
public class Lists {

  private Lists() {
    super();
  }

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
   * Convert an array to a collection
   *
   * @param <T>
   * @param <C>
   * @param supplier the collection instance builder
   * @param objects the array
   * @return an collection that combined by the passed in array
   */
  @SafeVarargs
  public static <T, C extends Collection<T>> C collectionOf(IntFunction<C> supplier,
      final T... objects) {
    if (objects == null || objects.length == 0) {
      return supplier.apply(0);
    } else {
      final C collection = supplier.apply(objects.length);
      for (T object : objects) {
        collection.add(object);
      }
      return collection;
    }
  }

  /**
   * Convert an iterator to a collection
   *
   * @param <T>
   * @param <C>
   * @param supplier the collection instance builder
   * @param objects the iterator
   * @return an collection that combined by the passed in iterator
   */
  public static <T, C extends Collection<T>> C collectionOf(Supplier<C> supplier, Iterator<T> it) {
    if (it == null) {
      return supplier.get();
    } else {
      final C collection = supplier.get();
      while (it.hasNext()) {
        collection.add(it.next());
      }
      return collection;
    }
  }

  /**
   * Convert an array to immutable list
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
   * Convert an array to linked list
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
   * Convert an enumeration to list
   *
   * @param <T>
   * @param objects
   * @return a list that combined by the passed in enumeration
   */
  public static <T> List<T> listOf(final Enumeration<T> enumeration) {
    List<T> list = new ArrayList<>();
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
        list.add(enumeration.nextElement());
      }
    }
    return list;
  }

  /**
   * Convert an iterable to list
   *
   * @param <T>
   * @param iterable
   * @return a list that combined by the passed in iterable
   */
  public static <T> List<T> listOf(final Iterable<T> iterable) {
    if (iterable instanceof List) {
      return forceCast(iterable);
    } else if (iterable != null) {
      return collectionOf(ArrayList::new, iterable.iterator());
    } else {
      return new ArrayList<>();
    }
  }

  /**
   * Convert an iterator to list
   *
   * @param <T>
   * @param iterator
   * @return a list that combined by the passed in iterator
   */
  public static <T> List<T> listOf(final Iterator<T> iterator) {
    List<T> list = new ArrayList<>();
    if (iterator != null) {
      while (iterator.hasNext()) {
        list.add(iterator.next());
      }
    }
    return list;
  }

  /**
   * Convert an array to list
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
   * Merge a list and another list elements to a new list. Like SQL select join clause.
   *
   * @param <F> a list element type
   * @param <J> another list element type
   * @param <T> the return element type
   * @param from a list
   * @param join another list
   * @param combination the combination function that merge two element objects to a new object
   * @param condition the join conditions like SQL ON clause
   * @param type the join type INNER LEFT CARTESIAN(CROSS-JOIN)
   * @return the merged new list
   */
  public static <F, J, T> List<T> mergeList(final List<F> from, final List<J> join,
      final BiFunction<F, J, T> combination, final BiPredicate<F, J> condition, JoinType type) {
    return new ListJoins<F, J, T>().select(combination).from(from).join(type, join).on(condition)
        .execute();
  }

  /**
   * Break a collection into smaller pieces
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
      collection.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size))
          .values().forEach(result::add);
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
  public static <C extends Collection<T>, T> C removeIf(C collection, Predicate<T> p) {
    if (collection == null) {
      return null;
    } else if (p == null) {
      return collection;
    } else {
      collection.removeIf(p);
      return collection;
    }
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
  public static <T> void swap(List<T> l, int i, int j) {
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

  /**
   * corant-shared
   *
   * @author bingo 下午2:36:15
   *
   */
  public static class ListJoins<F, J, T> {

    private List<F> from;
    private List<J> join;
    private BiPredicate<F, J> on;
    private BiFunction<F, J, T> select;
    private JoinType type = JoinType.LEFT;

    public static <F, J, T> ListJoins<F, J, T> start() {
      return new ListJoins<>();
    }

    public List<T> execute() {
      List<T> result = new ArrayList<>();
      if (type == JoinType.LEFT && isEmpty(join)) {
        from.stream().map(x -> select.apply(x, null)).forEachOrdered(result::add);
      } else if (type == JoinType.LEFT && !isEmpty(join)) {
        from.stream().forEachOrdered(f -> {
          List<J> ms = join.stream().filter(j -> on.test(f, j)).collect(Collectors.toList());
          if (isEmpty(ms)) {
            result.add(select.apply(f, null));
          } else {
            ms.stream().map(m -> select.apply(f, m)).forEach(result::add);
          }
        });
      } else if (type == JoinType.INNER && !isEmpty(join)) {
        from.stream().forEachOrdered(f -> this.join.stream().filter(j -> on.test(f, j))
            .forEachOrdered(j -> result.add(select.apply(f, j))));
      } else if (type == JoinType.CARTESIAN && !isEmpty(join)) {
        from.stream()
            .forEachOrdered(f -> join.stream().forEachOrdered(j -> result.add(select.apply(f, j))));
      }
      return result;
    }

    public ListJoins<F, J, T> from(List<F> from) {
      this.from = shouldNotNull(from);
      return this;
    }

    public ListJoins<F, J, T> on(BiPredicate<F, J> on) {
      this.on = on == null ? (f, j) -> false : on;
      return this;
    }

    public ListJoins<F, J, T> select(BiFunction<F, J, T> select) {
      this.select = select;
      return this;
    }

    ListJoins<F, J, T> join(JoinType joinType, List<J> joined) {
      this.type = joinType == null ? JoinType.LEFT : joinType;
      this.join = joined;
      return this;
    }

    enum JoinType {
      LEFT, INNER, CARTESIAN;
    }
  }
}

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
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.corant.shared.util.CollectionUtils.ListJoins.JoinType;

/**
 *
 * @author bingo 上午12:31:10
 *
 */
public class CollectionUtils {

  private CollectionUtils() {
    super();
  }

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

  @SafeVarargs
  public static <T> List<T> immutableListOf(final T... objects) {
    if (objects == null || objects.length == 0) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(listOf(objects));
  }

  @SafeVarargs
  public static <T> Set<T> immutableSetOf(final T... objects) {
    if (objects == null || objects.length == 0) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(setOf(objects));
  }

  @SafeVarargs
  public static <T> LinkedHashSet<T> linkedHashSetOf(final T... objects) {
    if (objects == null || objects.length == 0) {
      return new LinkedHashSet<>();
    }
    LinkedHashSet<T> set = new LinkedHashSet<>(objects.length);
    Collections.addAll(set, objects);
    return set;
  }

  @SafeVarargs
  public static <T> LinkedList<T> linkedListOf(final T... objects) {
    if (objects == null || objects.length == 0) {
      return new LinkedList<>();
    }
    LinkedList<T> list = new LinkedList<>();
    Collections.addAll(list, objects);
    return list;
  }

  public static <T> List<T> listOf(final Enumeration<T> enumeration) {
    List<T> list = new ArrayList<>();
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
        list.add(enumeration.nextElement());
      }
    }
    return list;
  }

  public static <T> List<T> listOf(final Iterable<T> iterable) {
    if (iterable instanceof List) {
      return forceCast(iterable);
    } else {
      List<T> list = new ArrayList<>();
      if (iterable != null) {
        iterable.forEach(list::add);
      }
      return list;
    }
  }

  public static <T> List<T> listOf(final Iterator<T> iterator) {
    List<T> list = new ArrayList<>();
    if (iterator != null) {
      while (iterator.hasNext()) {
        list.add(iterator.next());
      }
    }
    return list;
  }

  @SafeVarargs
  public static <T> List<T> listOf(final T... objects) {
    if (objects == null || objects.length == 0) {
      return new ArrayList<>();
    }
    ArrayList<T> list = new ArrayList<>(objects.length);
    Collections.addAll(list, objects);
    return list;
  }

  public static void main(String... f) {
    List<String> list = listOf("1", "2", "3");
    for (String s : subList(list, 1)) {
      System.out.println(s);
    }
  }

  public static <F, J, T> List<T> mergeList(final List<F> from, final List<J> join,
      final BiFunction<F, J, T> combination, final BiPredicate<F, J> condition, JoinType type) {
    return new ListJoins<F, J, T>().select(combination).from(from).join(type, join).on(condition)
        .execute();
  }

  public static <T> List<List<T>> partition(final Collection<T> collection, int size) {
    List<List<T>> result = new ArrayList<>();
    if (collection != null) {
      final AtomicInteger counter = new AtomicInteger(0);
      collection.stream().collect(Collectors.groupingBy(it -> counter.getAndIncrement() / size))
          .values().forEach(result::add);
    }
    return result;
  }

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

  @SafeVarargs
  public static <T> Set<T> setOf(final T... objects) {
    if (objects == null || objects.length == 0) {
      return new HashSet<>();
    }
    Set<T> set = new HashSet<>(objects.length);
    Collections.addAll(set, objects);
    return set;
  }

  public static <T> List<T> subList(final List<T> list, int beginIndex) {
    return subList(list, beginIndex, sizeOf(list));
  }

  public static <T> List<T> subList(final List<T> list, int beginIndex, int endIndex) {
    int size = sizeOf(list);
    if (beginIndex < 0) {
      throw new ArrayIndexOutOfBoundsException(beginIndex);
    }
    if (endIndex > size) {
      throw new ArrayIndexOutOfBoundsException(endIndex);
    }
    int subLen = endIndex - beginIndex;
    if (subLen < 0) {
      throw new ArrayIndexOutOfBoundsException(subLen);
    }
    if (beginIndex == 0 && endIndex == size) {
      return list;
    } else {
      List<T> sub = new ArrayList<>();
      for (int i = 0; i < subLen; i++) {
        sub.add(list.get(beginIndex + i));
      }
      return sub;
    }
  }

  public static <T> void swap(List<T> l, int i, int j) {
    Collections.swap(l, i, j);
  }

  public static <T> void swap(T[] a, int i, int j) {
    final T t = a[i];
    a[i] = a[j];
    a[j] = t;
  }

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

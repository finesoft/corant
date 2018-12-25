/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.shared.util;

import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
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

  public static <T> Enumeration<T> asEnumeration(Iterator<T> it) {
    final Iterator<T> useIt = it == null ? emptyIterator() : it;
    return new Enumeration<T>() {

      @Override
      public boolean hasMoreElements() {
        return useIt.hasNext();
      }

      @Override
      public T nextElement() {
        return useIt.next();
      }
    };
  }

  @SafeVarargs
  public static <T> List<T> asImmutableList(final T... objects) {
    return Collections.unmodifiableList(asList(objects));
  }

  @SafeVarargs
  public static <T> Set<T> asImmutableSet(final T... objects) {
    return Collections.unmodifiableSet(asSet(objects));
  }

  public static <T> Iterable<T> asIterable(final Enumeration<T> enums) {
    return () -> new Iterator<T>() {
      final Enumeration<T> fromEnums = enums;

      @Override
      public boolean hasNext() {
        return fromEnums.hasMoreElements();
      }

      @Override
      public T next() {
        return fromEnums.nextElement();
      }
    };
  }

  public static <T> Iterable<T> asIterable(Iterable<?> it, Function<Object, T> convert) {
    return () -> new Iterator<T>() {
      private final Iterator<?> fromIterator = it.iterator();

      @Override
      public boolean hasNext() {
        return fromIterator.hasNext();
      }

      @Override
      public T next() {
        return convert.apply(fromIterator.next());
      }

      @Override
      public void remove() {
        fromIterator.remove();
      }
    };
  }

  @SafeVarargs
  public static <T> Iterable<T> asIterable(final T... objects) {
    return new Iterable<T>() {
      final T[] array = objects;
      final int size = array.length;

      @Override
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          int i = 0;

          @Override
          public boolean hasNext() {
            return size - i > 0;
          }

          @Override
          public T next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return array[i++];
          }
        };
      }
    };
  }

  public static <T> List<T> asList(final Enumeration<T> enumeration) {
    List<T> list = new ArrayList<>();
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
        list.add(enumeration.nextElement());
      }
    }
    return list;
  }

  public static <T> List<T> asList(final Iterable<T> iterable) {
    List<T> list = new ArrayList<>();
    if (iterable != null) {
      iterable.forEach(list::add);
    }
    return list;
  }

  public static <T> List<T> asList(final Iterator<T> iterator) {
    List<T> list = new ArrayList<>();
    if (iterator != null) {
      while (iterator.hasNext()) {
        list.add(iterator.next());
      }
    }
    return list;
  }

  @SafeVarargs
  public static <T> List<T> asList(final T... objects) {
    ArrayList<T> list = new ArrayList<>(objects.length);
    for (T obj : objects) {
      list.add(obj);
    }
    return list;
  }

  @SafeVarargs
  public static <T> Set<T> asSet(final T... objects) {
    Set<T> set = new HashSet<>(objects.length);
    for (T obj : objects) {
      set.add(obj);
    }
    return set;
  }

  public static <T extends Iterable<T>> Iterator<T> breadthIterator(T obj) {
    return new BreadthIterable<>(obj).iterator();
  }

  public static <T extends Iterable<T>> Iterator<T> depthIterator(T obj) {
    return new DepthIterable<>(obj).iterator();
  }

  public static <T> Enumeration<T> emptyEnumeration() {
    return new Enumeration<T>() {

      @Override
      public boolean hasMoreElements() {
        return false;
      }

      @Override
      public T nextElement() {
        throw new NoSuchElementException();
      }
    };
  }

  public static <T> Iterable<T> emptyIterable() {
    return () -> emptyIterator();
  }

  public static <T> Iterator<T> emptyIterator() {
    return new Iterator<T>() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public T next() {
        throw new NoSuchElementException();
      }
    };
  }

  public static <T> T get(final Enumeration<T> e, final int index) {
    int i = index;
    if (i < 0) {
      throw new IndexOutOfBoundsException("Index cannot be negative: " + i);
    }
    while (e.hasMoreElements()) {
      i--;
      if (i == -1) {
        return e.nextElement();
      }
      e.nextElement();
    }
    throw new IndexOutOfBoundsException("Entry does not exist: " + i);
  }

  public static <E> E get(final Iterable<E> iterable, final int index) {
    int i = index;
    if (i < 0) {
      throw new IndexOutOfBoundsException("Index cannot be negative: " + i);
    }
    return get(iterable.iterator(), index);
  }

  public static <E> E get(final Iterator<E> iterator, final int index) {
    int i = index;
    if (i < 0) {
      throw new IndexOutOfBoundsException("Index cannot be negative: " + i);
    }
    while (iterator.hasNext()) {
      i--;
      if (i == -1) {
        return iterator.next();
      }
      iterator.next();
    }
    throw new IndexOutOfBoundsException("Entry does not exist: " + i);
  }

  public static Object get(final Object object, final int index) {
    final int i = index;
    if (i < 0) {
      throw new IndexOutOfBoundsException("Index cannot be negative: " + i);
    }
    if (object == null) {
      throw new IllegalArgumentException("Unsupported object type: null");
    }
    if (object instanceof Object[]) {
      return ((Object[]) object)[i];
    } else if (object instanceof Iterator<?>) {
      return get((Iterator<?>) object, index);
    } else if (object instanceof Iterable<?>) {
      return get((Iterable<?>) object, i);
    } else if (object instanceof Enumeration<?>) {
      return get((Enumeration<?>) object, index);
    } else {
      try {
        return Array.get(object, i);
      } catch (final IllegalArgumentException ex) {
        throw new IllegalArgumentException(
            "Unsupported object type: " + object.getClass().getName());
      }
    }
  }

  public static int getSize(final Enumeration<?> enums) {
    int size = 0;
    while (enums.hasMoreElements()) {
      size++;
      enums.nextElement();
    }
    return size;
  }

  public static int getSize(final Iterable<?> iterable) {
    if (iterable != null) {
      getSize(iterable.iterator());
    }
    return 0;
  }

  public static int getSize(final Iterator<?> iterator) {
    int size = 0;
    if (iterator != null) {
      while (iterator.hasNext()) {
        iterator.next();
        size++;
      }
    }
    return size;
  }

  public static int getSize(final Object object) {
    if (object == null) {
      return 0;
    } else if (object instanceof Object[]) {
      return ((Object[]) object).length;
    } else if (object instanceof Collection<?>) {
      return ((Collection<?>) object).size();
    } else if (object instanceof Iterator<?>) {
      return getSize((Iterator<?>) object);
    } else if (object instanceof Iterable<?>) {
      return getSize((Iterable<?>) object);
    } else if (object instanceof Enumeration<?>) {
      return getSize((Enumeration<?>) object);
    } else {
      try {
        return Array.getLength(object);
      } catch (final IllegalArgumentException ex) {
        throw new IllegalArgumentException(
            "Unsupported object type: " + object.getClass().getName());
      }
    }
  }

  public static boolean isEmpty(final Object object) {
    if (object == null) {
      return true;
    } else if (object instanceof Collection<?>) {
      return ((Collection<?>) object).isEmpty();
    } else if (object instanceof Object[]) {
      return ((Object[]) object).length == 0;
    } else if (object instanceof Iterator<?>) {
      return !((Iterator<?>) object).hasNext();
    } else if (object instanceof Iterable<?>) {
      return !((Iterable<?>) object).iterator().hasNext();
    } else if (object instanceof Enumeration<?>) {
      return !((Enumeration<?>) object).hasMoreElements();
    } else if (object instanceof CharSequence) {
      return StringUtils.isEmpty((CharSequence) object);
    } else {
      try {
        return Array.getLength(object) == 0;
      } catch (final IllegalArgumentException ex) {
        throw new IllegalArgumentException(
            "Unsupported object type: " + object.getClass().getName());
      }
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

  public static final class BreadthIterable<T extends Iterable<T>> implements Iterable<T> {

    private final Iterable<T> node;

    private volatile Iterator<T> current;

    public BreadthIterable(Iterable<T> node) {
      this.node = node;
    }

    @Override
    public Iterator<T> iterator() {
      if (node == null || node.iterator() == null) {
        return emptyIterator();
      }
      return new Iterator<T>() {

        private Queue<Iterator<T>> queue = new LinkedList<>();
        {
          queue.add(node.iterator());
        }

        @Override
        public boolean hasNext() {
          if (queue.isEmpty()) {
            return false;
          } else {
            while (current == null) {
              current = queue.poll();
              if (queue.size() == 0) {
                break;
              }
            }
            if (current == null) {
              return false;
            } else if (current.hasNext()) {
              return true;
            } else {
              current = null;
              return hasNext();
            }
          }
        }

        @Override
        public T next() {
          if (current != null) {
            T next = current.next();
            if (next != null) {
              queue.add(next.iterator());
            }
            return next;
          } else {
            throw new NoSuchElementException();
          }
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException("remove");
        }
      };
    }
  }

  public static final class DepthIterable<T extends Iterable<T>> implements Iterable<T> {
    private final Iterable<T> node;

    public DepthIterable(Iterable<T> node) {
      this.node = node;
    }

    @Override
    public Iterator<T> iterator() {
      if (node == null || node.iterator() == null) {
        return null;
      }
      return new Iterator<T>() {
        private Stack<Iterator<T>> stack = new Stack<>();
        {
          stack.push(node.iterator());
        }

        @Override
        public boolean hasNext() {
          if (stack.isEmpty()) {
            return false;
          } else {
            Iterator<T> it = stack.peek();
            if (it.hasNext()) {
              return true;
            } else {
              stack.pop();
              return hasNext();
            }
          }
        }

        @Override
        public T next() {
          if (hasNext()) {
            Iterator<T> it = stack.peek();
            T next = it.next();
            if (next != null) {
              stack.push(next.iterator());
            }
            return next;
          } else {
            return null;
          }
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException("remove");
        }
      };
    }
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

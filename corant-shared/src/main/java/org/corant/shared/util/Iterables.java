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
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Streams.streamOf;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * corant-shared
 *
 * @author bingo 上午12:19:13
 *
 */
public class Iterables {

  private Iterables() {
    super();
  }

  /**
   * Return a breadth-first iterator for an iterable object whose internal elements are also
   * iterable, Mainly used for object traversal in tree structure.
   *
   * @param <T>
   * @param obj
   * @return breadthIterator
   */
  public static <T extends Iterable<T>> Iterator<T> breadthIterator(T obj) {
    return new BreadthIterable<>(obj).iterator();
  }

  /**
   * Convert an array to a non-null collection
   *
   * @param <T>
   * @param <C>
   * @param supplier the collection instance builder
   * @param objects the array
   * @return an collection that combined by the passed in array
   */
  @SafeVarargs
  public static <T, C extends Collection<T>> C collectionOf(final IntFunction<C> supplier,
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
   * Convert an iterator to a non-null collection
   *
   * @param <T>
   * @param <C>
   * @param supplier the collection instance builder
   * @param it the iterator
   * @return an collection that combined by the passed in iterator
   */
  public static <T, C extends Collection<T>> C collectionOf(final Supplier<C> supplier,
      final Iterator<? extends T> it) {
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
   * Connect multiple iterables of matching types into one iterable.
   *
   * @param <T>
   * @param inputs
   * @return concat
   */
  public static <T> Iterable<T> concat(
      @SuppressWarnings("unchecked") final Iterable<? extends T>... inputs) {
    shouldNotNull(inputs);
    return new Iterable<>() {
      final Iterable<? extends T>[] iterables = Arrays.copyOf(inputs, inputs.length);

      @SuppressWarnings("unchecked")
      @Override
      public Iterator<T> iterator() {
        return Stream.of(iterables)
            .map(it -> StreamSupport.stream((Spliterator<T>) it.spliterator(), false))
            .reduce(Stream::concat).orElseGet(Stream::empty).iterator();
      }
    };
  }

  /**
   * Connect multiple iterators of matching types into one iterator
   *
   * @param <T>
   * @param inputs
   * @return concat
   */
  public static <T> Iterator<T> concat(
      @SuppressWarnings("unchecked") final Iterator<? extends T>... inputs) {
    shouldNotNull(inputs);
    return new Iterator<>() {
      @SuppressWarnings("unchecked")
      final Iterator<T>[] iterators =
          streamOf(inputs).filter(Objects::isNotNull).toArray(Iterator[]::new);
      int index = 0;

      @Override
      public boolean hasNext() {
        boolean hasNext = false;
        while (index < iterators.length && !(hasNext = iterators[index].hasNext())) {
          index++;
        }
        return hasNext;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return iterators[index].next();
      }
    };
  }

  /**
   * Return a depth-first iterator for an iterable object whose internal elements are also iterable,
   * Mainly used for object traversal in tree structure.
   *
   * @param <T>
   * @param obj
   * @return depthIterator
   */
  public static <T extends Iterable<T>> Iterator<T> depthIterator(T obj) {
    return new DepthIterable<>(obj).iterator();
  }

  /**
   * Returns an empty enumeration
   *
   * @param <T>
   * @return emptyEnumeration
   */
  public static <T> Enumeration<T> emptyEnumeration() {
    return new Enumeration<>() {

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

  /**
   * Returns an empty iterable
   *
   * @param <T>
   * @return emptyIterable
   */
  public static <T> Iterable<T> emptyIterable() {
    return Iterables::emptyIterator;
  }

  /**
   * Returns an enpty iterator
   *
   * @param <T>
   * @return emptyIterator
   */
  public static <T> Iterator<T> emptyIterator() {
    return new Iterator<>() {
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

  /**
   * Converts an iterator to enumeration
   *
   * @param <T>
   * @param it
   * @return enumerationOf
   */
  public static <T> Enumeration<T> enumerationOf(Iterator<? extends T> it) {
    final Iterator<? extends T> useIt = it == null ? emptyIterator() : it;
    return new Enumeration<>() {

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

  /**
   * Returns the index-th value in enumeration, throwing IndexOutOfBoundsException if there is no
   * such element.
   *
   * @param <T>
   * @param e
   * @param index
   * @return get
   */
  public static <T> T get(final Enumeration<? extends T> e, final int index) {
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

  /**
   * Returns the index-th value in iterable, throwing IndexOutOfBoundsException if there is nosuch
   * element.
   *
   * @param <E>
   * @param iterable
   * @param index
   * @return get
   */
  public static <E> E get(final Iterable<? extends E> iterable, final int index) {
    int i = index;
    if (i < 0) {
      throw new IndexOutOfBoundsException("Index cannot be negative: " + i);
    }
    if (iterable instanceof List) {
      List<E> list = forceCast(iterable);
      return list.get(i);
    }
    return get(iterable.iterator(), index);
  }

  /**
   * Returns the index-th value in iterator, throwing IndexOutOfBoundsException if there is nosuch
   * element.
   *
   * @param <E>
   * @param iterator
   * @param index
   * @return get
   */
  public static <E> E get(final Iterator<? extends E> iterator, final int index) {
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

  /**
   * Converts an enumeration to an non-null iterable
   *
   * @param <T>
   * @param enums
   * @return iterableOf
   */
  public static <T> Iterable<T> iterableOf(final Enumeration<? extends T> enums) {
    return enums == null ? emptyIterable() : () -> new Iterator<>() {
      final Enumeration<? extends T> fromEnums = enums;

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

  /**
   * Converts an array to an non-null iterable
   *
   * @param <T>
   * @param objects
   * @return iterableOf
   */
  @SafeVarargs
  public static <T> Iterable<T> iterableOf(final T... objects) {
    return new Iterable<>() {
      final T[] array = objects;
      final int size = array.length;

      @Override
      public Iterator<T> iterator() {
        return new Iterator<>() {
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

  /**
   * Use the specified conversion function to convert the iterable element type
   *
   * @param <T>
   * @param it
   * @param convert
   * @return transform
   */
  public static <T> Iterable<T> transform(final Iterable<?> it, final Function<Object, T> convert) {
    return () -> transform(it == null ? (Iterator<?>) null : it.iterator(), convert);
  }

  /**
   * Use the specified conversion function to convert the iterator element type
   *
   * @param <T>
   * @param it
   * @param convert
   * @return transform
   */
  public static <T> Iterator<T> transform(final Iterator<?> it, final Function<Object, T> convert) {
    return it == null ? emptyIterator() : new Iterator<>() {
      final Iterator<?> fromIterator = it;
      final Function<Object, T> useConvert = defaultObject(convert, Objects::forceCast);

      @Override
      public boolean hasNext() {
        return fromIterator.hasNext();
      }

      @Override
      public T next() {
        return useConvert.apply(fromIterator.next());
      }

      @Override
      public void remove() {
        fromIterator.remove();
      }
    };
  }

  /**
   * corant-shared
   *
   * @author bingo 上午11:19:28
   *
   */
  public static class BreadthIterable<T extends Iterable<T>> implements Iterable<T> {

    protected final Iterable<T> node;

    protected volatile Iterator<T> current;

    public BreadthIterable(Iterable<T> node) {
      this.node = node;
    }

    @Override
    public Iterator<T> iterator() {
      if (node == null || node.iterator() == null) {
        return emptyIterator();
      }
      return new Iterator<>() {

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
              if (queue.isEmpty()) {
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

  /**
   * corant-shared
   *
   * @author bingo 上午11:19:56
   *
   */
  public static class DepthIterable<T extends Iterable<T>> implements Iterable<T> {
    protected final Iterable<T> node;

    public DepthIterable(Iterable<T> node) {
      this.node = node;
    }

    @Override
    public Iterator<T> iterator() {
      if (node == null || node.iterator() == null) {
        return null;
      }
      return new Iterator<>() {
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
}

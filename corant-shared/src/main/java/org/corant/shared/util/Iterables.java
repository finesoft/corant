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
import java.util.Collections;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * corant-shared
 *
 * @author bingo 上午12:19:13
 *
 */
public class Iterables {

  private Iterables() {}

  /**
   * Return a breadth-first iterator for an iterable object whose internal elements are also
   * iterable, Mainly used for object traversal in tree structure.
   *
   * @param <E> the element type
   * @param obj the object to be expanded
   * @return breadthIterator
   */
  public static <E extends Iterable<E>> Iterator<E> breadthIterator(E obj) {
    return new BreadthIterable<>(obj).iterator();
  }

  /**
   * Convert an array to a non-null collection
   *
   * @param <E> the element type
   * @param <C> the collection type
   * @param supplier the collection instance builder
   * @param objects the array
   * @return a collection that combined by the passed in array
   */
  @SafeVarargs
  public static <E, C extends Collection<E>> C collectionOf(final IntFunction<C> supplier,
      final E... objects) {
    if (objects == null || objects.length == 0) {
      return supplier.apply(0);
    } else {
      final C collection = supplier.apply(objects.length);
      Collections.addAll(collection, objects);
      return collection;
    }
  }

  /**
   * Convert an iterator to a non-null collection
   *
   * @param <E> the element type
   * @param <C> the collection type
   * @param supplier the collection instance builder
   * @param it the iterator
   * @return a collection that combined by the passed in iterator
   */
  public static <E, C extends Collection<E>> C collectionOf(final Supplier<C> supplier,
      final Iterator<? extends E> it) {
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
   * @param <E> the element type
   * @param inputs the iterables for connecting
   */
  @SafeVarargs
  public static <E> Iterable<E> concat(final Iterable<? extends E>... inputs) {
    shouldNotNull(inputs);
    return new Iterable<>() {
      final Iterable<? extends E>[] iterables = Arrays.copyOf(inputs, inputs.length);

      @SuppressWarnings("unchecked")
      @Override
      public Iterator<E> iterator() {
        return Stream.of(iterables)
            .map(it -> StreamSupport.stream((Spliterator<E>) it.spliterator(), false))
            .reduce(Stream::concat).orElseGet(Stream::empty).iterator();
      }
    };
  }

  /**
   * Connect multiple iterators of matching types into one iterator
   *
   * @param <E> the element type
   * @param inputs the iterator for connecting
   */
  @SafeVarargs
  public static <E> Iterator<E> concat(final Iterator<? extends E>... inputs) {
    shouldNotNull(inputs);
    return new Iterator<>() {
      @SuppressWarnings("unchecked")
      final Iterator<E>[] iterators =
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
      public E next() {
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
   * @param <E> the element type
   * @param obj the object to be expanded
   */
  public static <E extends Iterable<E>> Iterator<E> depthIterator(E obj) {
    return new DepthIterable<>(obj).iterator();
  }

  /**
   * Returns an empty enumeration
   *
   * @param <E> the element type
   * @return emptyEnumeration
   */
  public static <E> Enumeration<E> emptyEnumeration() {
    return new Enumeration<>() {

      @Override
      public boolean hasMoreElements() {
        return false;
      }

      @Override
      public E nextElement() {
        throw new NoSuchElementException();
      }
    };
  }

  /**
   * Returns an empty iterable
   *
   * @param <E> the element type
   * @return emptyIterable
   */
  public static <E> Iterable<E> emptyIterable() {
    return Iterables::emptyIterator;
  }

  /**
   * Returns an enpty iterator
   *
   * @param <T> the element type
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
   * @param <E> the element type
   * @param it the iterator to convert
   */
  public static <E> Enumeration<E> enumerationOf(Iterator<? extends E> it) {
    final Iterator<? extends E> useIt = it == null ? emptyIterator() : it;
    return new Enumeration<>() {

      @Override
      public boolean hasMoreElements() {
        return useIt.hasNext();
      }

      @Override
      public E nextElement() {
        return useIt.next();
      }
    };
  }

  /**
   * Returns the index-th value in enumeration, throwing IndexOutOfBoundsException if there is no
   * such element.
   *
   * @param <E> the element type
   * @param e the enumeration that contain the index-th value
   * @param index the index
   */
  public static <E> E get(final Enumeration<? extends E> e, final int index) {
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
   * @param <E> the element type
   * @param iterable the iterable that contain the index-th value
   * @param index the index
   */
  @Deprecated
  public static <E> E get(final Iterable<? extends E> iterable, final int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("Index cannot be negative: " + index);
    }
    if (iterable instanceof List) {
      List<E> list = forceCast(iterable);
      return list.get(index);
    }
    return get(iterable.iterator(), index);
  }

  /**
   * Returns the index-th value in iterator, throwing IndexOutOfBoundsException if there is nosuch
   * element.
   *
   * @param <E> the element type
   * @param iterator the iterator that used to retrieve the index-th value
   * @param index the index
   */
  @Deprecated
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
   * Converts an array to a non-null iterable
   *
   * @param <E> the element type
   * @param objects the elements array
   */
  @SafeVarargs
  public static <E> Iterable<E> iterableOf(final E... objects) {
    return new Iterable<>() {
      final E[] array = objects;
      final int size = array.length;

      @Override
      public Iterator<E> iterator() {
        return new Iterator<>() {
          int i = 0;

          @Override
          public boolean hasNext() {
            return size - i > 0;
          }

          @Override
          public E next() {
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
   * Converts an enumeration to a non-null iterable
   *
   * @param <E> the element type
   * @param enums the enumeration that contain the index-th value
   */
  public static <E> Iterable<E> iterableOf(final Enumeration<? extends E> enums) {
    return enums == null ? emptyIterable() : () -> new Iterator<>() {
      final Enumeration<? extends E> fromEnums = enums;

      @Override
      public boolean hasNext() {
        return fromEnums.hasMoreElements();
      }

      @Override
      public E next() {
        return fromEnums.nextElement();
      }
    };
  }

  /**
   * @see #range(int, int)
   * @param length
   * @return range
   */
  public static int[] range(int length) {
    return range(0, length);
  }

  /**
   * Returns a {@code int[] } from {@code startInclusive} (inclusive) to {@code endExclusive}
   * (exclusive) by an incremental step of {@code 1}, close to the python keyword 'range'. If you
   * have higher requirements for memory usage, please use {@link IntStream#range(int, int)}.
   *
   * @param startInclusive the (inclusive) initial value
   * @param endExclusive the exclusive upper bound
   * @return a sequential {@code int[] } for the range of {@code int} elements
   */
  public static int[] range(int startInclusive, int endExclusive) {
    // return IntStream.range(startInclusive, endExclusive).toArray();
    if (endExclusive <= startInclusive) {
      return Primitives.EMPTY_INTEGER_ARRAY;
    }
    int[] array = new int[endExclusive - startInclusive];
    for (int x = 0, i = startInclusive; i < endExclusive; i++, x++) {
      array[x] = i;
    }
    return array;
  }

  /**
   * Use the specified conversion function to convert the iterable element type
   *
   * @param <E> the element type
   * @param it the source element iterable
   * @param convert the conversion function
   */
  public static <E> Iterable<E> transform(final Iterable<?> it, final Function<Object, E> convert) {
    return () -> transform(it == null ? (Iterator<?>) null : it.iterator(), convert);
  }

  /**
   * Use the specified conversion function to convert the iterator element type
   *
   * @param <E> the element type
   * @param it the source element iterable
   * @param convert the conversion function
   */
  public static <E> Iterator<E> transform(final Iterator<?> it, final Function<Object, E> convert) {
    return it == null ? emptyIterator() : new Iterator<>() {
      final Iterator<?> fromIterator = it;
      final Function<Object, E> useConvert = defaultObject(convert, Objects::forceCast);

      @Override
      public boolean hasNext() {
        return fromIterator.hasNext();
      }

      @Override
      public E next() {
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
      if (node == null) {
        return emptyIterator();
      }
      return new Iterator<>() {

        private final Queue<Iterator<T>> queue = new LinkedList<>();
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
      if (node == null) {
        return null;
      }
      return new Iterator<>() {
        private final Stack<Iterator<T>> stack = new Stack<>();
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

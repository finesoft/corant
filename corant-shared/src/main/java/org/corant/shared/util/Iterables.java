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

import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.ObjectUtils.forceCast;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Stack;
import java.util.function.Function;

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
    return Iterables::emptyIterator;
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

  public static <T> Enumeration<T> enumerationOf(Iterator<T> it) {
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
    if (iterable instanceof List) {
      List<E> list = forceCast(iterable);
      return list.get(i);
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

  public static <T> Iterable<T> iterableOf(final Enumeration<T> enums) {
    return enums == null ? emptyIterable() : () -> new Iterator<T>() {
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

  public static <T> Iterable<T> iterableOf(final Iterable<?> it,
      final Function<Object, T> convert) {
    return it == null ? emptyIterable() : () -> new Iterator<T>() {
      private final Iterator<?> fromIterator = it.iterator();
      private final Function<Object, T> useConvert = defaultObject(convert, ObjectUtils::forceCast);

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

  @SafeVarargs
  public static <T> Iterable<T> iterableOf(final T... objects) {
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
}

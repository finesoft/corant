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
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Functions.emptyConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author bingo 2017年4月7日
 */
public class Streams {

  public static final int DFLT_BATCH_SIZE = 64;

  private Streams() {}

  /**
   * Receive stream object converts it to a list stream, use stream grouping.
   *
   * @param <T>
   * @param batchSize
   * @param source
   * @return batchCollectStream
   */
  public static <T> Stream<List<T>> batchCollectStream(int batchSize, Stream<T> source) {
    final int useBatchSize = batchSize < 0 ? DFLT_BATCH_SIZE : batchSize;
    final AtomicInteger counter = new AtomicInteger();
    return shouldNotNull(source)
        .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / useBatchSize)).values()
        .stream();
  }

  /**
   * Receive iterable object converts it to a list stream, use for batch processing.
   *
   * NOTE : parallel not support
   *
   * @param <T>
   * @param batchSize
   * @param source
   * @return batchStream
   */
  public static <T> Stream<List<T>> batchStream(int batchSize, Iterable<? extends T> source) {
    return batchStream(batchSize, shouldNotNull(source).iterator());
  }

  /**
   * Receive iterator object converts it to a list stream, use for batch processing.
   *
   * NOTE : parallel not support
   *
   * @param <T>
   * @param batchSize
   * @param it
   * @return batchStream
   */
  public static <T> Stream<List<T>> batchStream(int batchSize, Iterator<? extends T> it) {

    return streamOf(new Iterator<List<T>>() {

      final int useBatchSize = batchSize < 0 ? DFLT_BATCH_SIZE : batchSize;
      final Iterator<? extends T> useIt = shouldNotNull(it);
      final List<T> buffer = new ArrayList<>(useBatchSize);
      boolean end = false;

      @Override
      public boolean hasNext() {
        if (end) {
          return false;
        }
        if (isNotEmpty(buffer)) {
          return true;
        }
        int i = 0;
        while (useIt.hasNext()) {
          buffer.add(useIt.next());
          if (++i == useBatchSize) {
            break;
          }
        }
        end = i != useBatchSize;
        return i > 0;
      }

      @Override
      public List<T> next() {
        if (isNotEmpty(buffer) || hasNext()) {
          List<T> list = new ArrayList<>(buffer);
          buffer.clear();
          return list;
        } else {
          throw new NoSuchElementException();
        }
      }
    });
  }

  /**
   * Receive stream object converts it to a list stream, use for batch processing.
   *
   * NOTE : parallel not support
   *
   * @param <T>
   * @param batchSize
   * @param source
   * @return batchStream
   */
  public static <T> Stream<List<T>> batchStream(int batchSize, Stream<? extends T> source) {
    return batchStream(batchSize, shouldNotNull(source).iterator());
  }

  /**
   * Copy the input stream to the output stream without closing the streams.
   *
   * @param input
   * @param output
   * @return
   * @throws IOException copy
   */
  public static long copy(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[4096];
    long count;
    int n;
    for (count = 0L; -1 != (n = input.read(buffer)); count += n) {
      output.write(buffer, 0, n);
    }
    return count;
  }

  /**
   * Convert an Iterable object to parallel Stream object
   */
  @SuppressWarnings("unchecked")
  public static <T> Stream<T> parallelStreamOf(final Iterable<? extends T> iterable) {
    if (iterable != null) {
      return StreamSupport.stream((Spliterator<T>) iterable.spliterator(), true);
    }
    return Stream.empty();
  }

  /**
   * Convert Iterator object to parallel Stream object
   */
  public static <T> Stream<T> parallelStreamOf(final Iterator<? extends T> iterator) {
    if (iterator != null) {
      return StreamSupport
          .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), true);
    }
    return Stream.empty();
  }

  /**
   * Read the input stream to byte array.
   *
   * @param is
   * @return
   * @throws IOException readAllBytes
   */
  public static byte[] readAllBytes(InputStream is) throws IOException {
    byte[] buf = new byte[4096];
    int maxBufferSize = Integer.MAX_VALUE - 8;
    int capacity = buf.length;
    int nread = 0;
    int n;
    for (;;) {
      while ((n = is.read(buf, nread, capacity - nread)) > 0) {
        nread += n;
      }
      // returned -1, done
      if (n < 0) {
        break;
      }
      if (capacity <= maxBufferSize - capacity) {
        capacity = capacity << 1;
      } else {
        if (capacity == maxBufferSize) {
          throw new OutOfMemoryError("Required array size too large");
        }
        capacity = maxBufferSize;
      }
      buf = Arrays.copyOf(buf, capacity);
    }
    return capacity == nread ? buf : Arrays.copyOf(buf, nread);
  }

  /**
   * Convert Enumeration object to Stream object
   *
   * @param <T>
   * @param enumeration
   * @return streamOf
   */
  public static <T> Stream<T> streamOf(final Enumeration<? extends T> enumeration) {
    if (enumeration != null) {
      return streamOf(new Iterator<T>() {
        @Override
        public boolean hasNext() {
          return enumeration.hasMoreElements();
        }

        @Override
        public T next() {
          if (enumeration.hasMoreElements()) {
            return enumeration.nextElement();
          }
          throw new NoSuchElementException();
        }
      });
    }
    return Stream.empty();
  }

  /**
   * Convert Iterable object to Stream object
   *
   * @param <T>
   * @param iterable
   * @return streamOf
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static <T> Stream<T> streamOf(final Iterable<? extends T> iterable) {
    if (iterable instanceof Collection) {
      return ((Collection) iterable).stream();
    } else if (iterable != null) {
      return StreamSupport.stream((Spliterator<T>) iterable.spliterator(), false);
    }
    return Stream.empty();
  }

  /**
   * Convert Iterator object to Stream object
   *
   * @param <T>
   * @param iterator
   * @return streamOf
   */
  public static <T> Stream<T> streamOf(final Iterator<? extends T> iterator) {
    if (iterator != null) {
      return StreamSupport
          .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
    return Stream.empty();
  }

  /**
   * Convert the entries of Map to Stream
   *
   * @param <K>
   * @param <V>
   * @param map
   * @return streamOf
   */
  public static <K, V> Stream<Map.Entry<K, V>> streamOf(Map<K, V> map) {
    if (map != null) {
      return map.entrySet().stream();
    }
    return Stream.empty();
  }

  /**
   * Convert object array to Stream
   *
   * @param <T>
   * @param objects
   * @return streamOf
   */
  @SuppressWarnings("unchecked")
  public static <T> Stream<T> streamOf(T... objects) {
    return Arrays.stream(objects);
  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:39:59
   *
   */
  public abstract static class AbstractBatchHandlerSpliterator<T> extends AbstractSpliterator<T> {

    private final int batchSize;
    private final Consumer<Long> handler;

    protected AbstractBatchHandlerSpliterator(long est, int additionalCharacteristics,
        int forEachBathSize, Consumer<Long> handler) {
      super(est, additionalCharacteristics);
      this.batchSize = forEachBathSize;
      this.handler = handler == null ? emptyConsumer() : handler;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
      long j = 0;
      do {
        if (j % this.batchSize == 0 && j > 0) {
          this.handler.accept(j);
        }
        j++;
      } while (tryAdvance(action));
      this.handler.accept(j);
    }

    @Override
    public Comparator<? super T> getComparator() {
      if (hasCharacteristics(SORTED)) {
        return null;
      }
      throw new IllegalStateException();
    }

  }

}

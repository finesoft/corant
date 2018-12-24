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

import static org.corant.shared.util.ObjectUtils.shouldNotNull;
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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author bingo 2017年4月7日
 */
public class StreamUtils {

  public static final int DFLE_BATCH_SIZE = 64;

  private StreamUtils() {}

  public static <T> Stream<T> asStream(final Enumeration<T> enumeration) {
    if (enumeration != null) {
      asStream(new Iterator<T>() {
        @Override
        public boolean hasNext() {
          return enumeration.hasMoreElements();
        }

        @Override
        public T next() {
          return enumeration.nextElement();
        }
      });
    }
    return Stream.empty();
  }

  @SuppressWarnings("unchecked")
  public static <T> Stream<T> asStream(final Iterable<T> iterable) {
    if (iterable instanceof Collection) {
      return Collection.class.cast(iterable).stream();
    } else if (iterable != null) {
      return StreamSupport.stream(iterable.spliterator(), false);
    }
    return Stream.empty();
  }

  public static <T> Stream<T> asStream(final Iterator<T> iterator) {
    if (iterator != null) {
      return StreamSupport
          .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
    return Stream.empty();
  }

  public static <K, V> Stream<Map.Entry<K, V>> asStream(Map<K, V> map) {
    if (map != null) {
      return map.entrySet().stream();
    }
    return Stream.empty();
  }

  @SuppressWarnings("unchecked")
  public static <T> Stream<T> asStream(T... objects) {
    return Arrays.stream(objects);
  }

  public static <T> Stream<List<T>> batchCollectStream(int forEachBatchSize, Stream<T> source) {
    return new BatchCollectStreams<>(source, forEachBatchSize).stream();
  }

  public static long copy(InputStream input, OutputStream output) throws IOException {
    byte[] buffer = new byte[4096];
    long count;
    int n;
    for (count = 0L; -1 != (n = input.read(buffer)); count += n) {
      output.write(buffer, 0, n);
    }
    return count;
  }

  public abstract static class AbstractBatchHandlerSpliterator<T> extends AbstractSpliterator<T> {

    private final int batchSize;
    private Consumer<Long> handler;

    protected AbstractBatchHandlerSpliterator(long est, int additionalCharacteristics,
        int forEachBathSize, Consumer<Long> handler) {
      super(est, additionalCharacteristics);
      this.batchSize = forEachBathSize;
      this.handler = handler == null ? t -> {
      } : handler;
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

  public static class BatchCollectStreams<T> {
    private final Stream<T> source;
    private final int batchSize;

    public BatchCollectStreams(Stream<T> source, int forEachBathSize) {
      this.source = shouldNotNull(source);
      this.batchSize = forEachBathSize < 0 ? DFLE_BATCH_SIZE : forEachBathSize;
    }

    public Stream<List<T>> stream() {
      final Iterator<T> sourceIt = this.source.iterator();
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<List<T>>() {
        @Override
        public boolean hasNext() {
          return sourceIt.hasNext();
        }

        @Override
        public List<T> next() {
          List<T> tmpList = new ArrayList<>(BatchCollectStreams.this.batchSize);
          int seq = 0;
          while (sourceIt.hasNext()) {
            tmpList.add(sourceIt.next());
            seq++;
            if (seq >= BatchCollectStreams.this.batchSize) {
              break;
            }
          }
          return tmpList;
        }
      }, Spliterator.IMMUTABLE), false);
    }
  }
}

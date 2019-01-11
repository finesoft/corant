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
package org.corant.shared.conversion;

import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import java.util.Iterator;
import java.util.Map;

/**
 * corant-shared
 *
 * @author bingo 上午9:49:44
 *
 */
@FunctionalInterface
public interface Converter<S, T> extends Comparable<Converter<S, T>> {

  default <V> Converter<S, V> andThen(Converter<? super T, ? extends V> after) {
    return (t, hints) -> shouldNotNull(after).apply(apply(t, hints), hints);
  }

  T apply(S t, Map<String, ?> hints);

  @Override
  default int compareTo(Converter<S, T> converter) {
    return Integer.compare(getPriority(), converter.getPriority());
  }

  default <V> Converter<V, T> compose(Converter<? super V, ? extends S> before) {
    return (v, hints) -> apply(shouldNotNull(before).apply(v, hints), hints);
  }

  default int getNestingDepth() {
    return 1;
  }

  default int getPriority() {
    return 0;
  }

  default boolean isPossibleDistortion() {
    return false;
  }

  default boolean isThrowException() {
    return true;
  }

  default Iterable<T> iterable(final Iterable<? extends S> fromIterable,
      final Map<String, ?> hints) {
    shouldNotNull(fromIterable);
    return () -> new Iterator<T>() {
      private Iterator<? extends S> fromIterator = fromIterable.iterator();

      @Override
      public boolean hasNext() {
        return fromIterator.hasNext();
      }

      @Override
      public T next() {
        return apply(fromIterator.next(), hints);
      }

      @Override
      public void remove() {
        fromIterator.remove();
      }
    };
  }
}

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

import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.Iterator;
import java.util.Map;
import org.corant.shared.normal.Priorities;
import org.corant.shared.ubiquity.Sortable;

/**
 * corant-shared
 * <p>
 * Object converter, used for conversion between objects, the generic parameter S is the source type
 * that converter accepts and the generic parameter T is the target type that converter will convert
 * source to.
 *
 * <p>
 * All converters are loaded by ServiceLoader, which means that they need to be declared in
 * META-INF/service.
 *
 * @author bingo 上午9:49:44
 */
@FunctionalInterface
public interface Converter<S, T> extends Sortable {

  /**
   * Returns a composed converter that first applies this converter to its input, and then applies
   * the {@code after} converter to the result. If evaluation of either converter throws an
   * exception, it is relayed to the caller of the composed converter.
   *
   * @param <V> the type of output of the {@code after} converter, and of the composed converter
   * @param after the converter to apply after this converter is applied
   * @return a composed converter that first applies this converter and then applies the
   *         {@code after} converter
   * @throws NullPointerException if after is null
   *
   * @see #compose(Converter)
   */
  default <V> Converter<S, V> andThen(Converter<? super T, ? extends V> after) {
    return (t, hints) -> shouldNotNull(after).convert(convert(t, hints), hints);
  }

  /**
   * Returns a composed converter that first applies the {@code before} converter to its input, and
   * then applies this converter to the result. If evaluation of either converter throws an
   * exception, it is relayed to the caller of the composed converter.
   *
   * @param <V> the type of input to the {@code before} converter, and to the composed converter
   * @param before the converter to apply before this converter is applied
   * @return a composed converter that first applies the {@code before} converter and then applies
   *         this converter
   * @throws NullPointerException if before is null
   */
  default <V> Converter<V, T> compose(Converter<? super V, ? extends S> before) {
    return (v, hints) -> convert(shouldNotNull(before).convert(v, hints), hints);
  }

  /**
   * Convert an object, allowing intervention through the given hints parameters during the
   * conversion process such as specifying time zone, etc.
   *
   * @param t the source object that will be converted
   * @param hints the conversion hints use to intervene in the conversion process
   * @return the converted target object
   */
  T convert(S t, Map<String, ?> hints);

  /**
   * Returns the nesting depth of converters, non-combined converters return 1, and combined
   * converters returns the number of converters in the combined chain.
   *
   * @return getNestingDepth
   */
  default int getNestingDepth() {
    return 1;
  }

  /**
   * Returns the converter priority, used to identify the priority of converters of the same type,
   * the smaller the value, the more preferred. Default is {@link Priorities#FRAMEWORK_LOWER}
   *
   * @return the priority
   */
  @Override
  default int getPriority() {
    return Priorities.FRAMEWORK_LOWER;
  }

  /**
   * Indicates whether there will be distortion in the conversion process
   *
   * @return isPossibleDistortion
   */
  default boolean isPossibleDistortion() {
    return false;
  }

  /**
   * Returns whether the converter is a synthetic converter
   *
   * @see #compose(Converter)
   * @see Converter#andThen(Converter)
   */
  default boolean isSynthetic() {
    return true;
  }

  /**
   * Whether to throw an exception when it cannot be converted, the default is true, which means
   * that an exception is thrown.
   *
   * @return isThrowException
   */
  default boolean isThrowException() {
    return true;
  }

  default Iterable<T> iterable(final Iterable<? extends S> fromIterable,
      final Map<String, ?> hints) {
    shouldNotNull(fromIterable);
    return () -> new Iterator<>() {
      private final Iterator<? extends S> fromIterator = fromIterable.iterator();

      @Override
      public boolean hasNext() {
        return fromIterator.hasNext();
      }

      @Override
      public T next() {
        return convert(fromIterator.next(), hints);
      }

      @Override
      public void remove() {
        fromIterator.remove();
      }
    };
  }
}

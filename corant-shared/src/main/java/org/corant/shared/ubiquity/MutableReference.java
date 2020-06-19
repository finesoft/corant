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
package org.corant.shared.ubiquity;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.corant.shared.util.Objects;

/**
 * corant-shared
 *
 * @author bingo 上午11:06:08
 *
 */
public class MutableReference<T> {

  private final Object[] reference = new Object[] {null};

  public MutableReference(T reference) {
    this.reference[0] = reference;
  }

  public static <X> MutableReference<X> of(X reference) {
    return new MutableReference<>(reference);
  }

  public void accept(Consumer<T> func) {
    T obj = Objects.forceCast(reference[0]);
    func.accept(obj);
  }

  public void apply(UnaryOperator<T> func) {
    T obj = Objects.forceCast(reference[0]);
    reference[0] = func.apply(obj);
  }

  public T applyAndGet(UnaryOperator<T> func) {
    T obj = Objects.forceCast(reference[0]);
    reference[0] = func.apply(obj);
    return Objects.forceCast(reference[0]);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MutableReference other = (MutableReference) obj;
    return Arrays.deepEquals(reference, other.reference);
  }

  public T get() {
    return Objects.forceCast(reference[0]);
  }

  public T getAndApply(UnaryOperator<T> func) {
    final T obj = Objects.forceCast(reference[0]);
    reference[0] = func.apply(obj);
    return obj;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.deepHashCode(reference);
    return result;
  }

  public void set(T reference) {
    this.reference[0] = reference;
  }

}

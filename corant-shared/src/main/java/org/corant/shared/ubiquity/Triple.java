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

import java.io.Serializable;
import org.corant.shared.util.Objects;

/**
 * corant-shared
 *
 * @author bingo 下午11:26:23
 *
 */
public class Triple<L, M, R> implements Serializable {

  private static final long serialVersionUID = 6441751980847755625L;

  @SuppressWarnings("rawtypes")
  static final Triple emptyInstance = new Triple();

  private final L left;
  private final M middle;
  private final R right;

  protected Triple() {
    this(null, null, null);
  }

  protected Triple(L left, M middle, R right) {
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  @SuppressWarnings("unchecked")
  public static <L, M, R> Triple<L, M, R> empty() {
    return emptyInstance;
  }

  public static <L, M, R> Triple<L, M, R> of(L left, M middle, R right) {
    return new Triple<>(left, middle, right);
  }

  public String asString(final String format) {
    return String.format(format, getLeft(), getMiddle(), getRight());
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Triple<?, ?, ?>) {
      final Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
      return Objects.areEqual(getLeft(), other.getLeft())
          && Objects.areEqual(getMiddle(), other.getMiddle())
          && Objects.areEqual(getRight(), other.getRight());
    }
    return false;
  }

  public L getLeft() {
    return left;
  }

  public M getMiddle() {
    return middle;
  }

  public R getRight() {
    return right;
  }

  @Override
  public int hashCode() {
    return (getLeft() == null ? 0 : getLeft().hashCode())
        ^ (getMiddle() == null ? 0 : getMiddle().hashCode())
        ^ (getRight() == null ? 0 : getRight().hashCode());
  }

  public boolean isEmpty() {
    return left == null && middle == null && right == null;
  }

  @Override
  public String toString() {
    return asString("[%s,%s,%s]");
  }

  public Triple<L, M, R> withLeft(L left) {
    return new Triple<>(left, getMiddle(), getRight());
  }

  public Triple<L, M, R> withMiddle(M middle) {
    return new Triple<>(getLeft(), middle, getRight());
  }

  public Triple<L, M, R> withRight(R right) {
    return new Triple<>(getLeft(), getMiddle(), right);
  }
}

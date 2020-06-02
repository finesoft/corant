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

import java.beans.Transient;
import java.io.Serializable;
import java.util.Map;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.ObjectUtils;

/**
 * corant-shared
 *
 * @author bingo 下午11:26:10
 *
 */
public class Pair<L, R> implements Map.Entry<L, R>, Serializable {

  private static final long serialVersionUID = -474294448204498274L;

  @SuppressWarnings("rawtypes")
  static final Pair emptyInstance = new Pair();

  private final L left;
  private final R right;

  protected Pair() {
    this(null, null);
  }

  protected Pair(L left, R right) {
    this.left = left;
    this.right = right;
  }

  @SuppressWarnings("unchecked")
  public static <L, R> Pair<L, R> empty() {
    return emptyInstance;
  }

  public static <L, R> Pair<L, R> of(L left, R right) {
    return new Pair<>(left, right);
  }

  public static <L, R> Pair<L, R> of(Map.Entry<L, R> entry) {
    return new Pair<>(entry.getKey(), entry.getValue());
  }

  public String asString(final String format) {
    return String.format(format, getLeft(), getRight());
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Map.Entry<?, ?>) {
      final Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
      return ObjectUtils.isEquals(getKey(), other.getKey())
          && ObjectUtils.isEquals(getValue(), other.getValue());
    }
    return false;
  }

  @Override
  @Transient
  public L getKey() {
    return getLeft();
  }

  public L getLeft() {
    return left;
  }

  public R getRight() {
    return right;
  }

  @Override
  @Transient
  public R getValue() {
    return getRight();
  }

  @Override
  public int hashCode() {
    return (getKey() == null ? 0 : getKey().hashCode())
        ^ (getValue() == null ? 0 : getValue().hashCode());
  }

  public boolean isEmpty() {
    return left == null && right == null;
  }

  @Override
  public R setValue(R value) {
    throw new NotSupportedException();
  }

  @Override
  public String toString() {
    return asString("[%s,%s]");
  }

  public Pair<L, R> withKey(L key) {
    return new Pair<>(key, getValue());
  }

  public Pair<L, R> withLeft(L left) {
    return new Pair<>(left, getRight());
  }

  public Pair<L, R> withRight(R right) {
    return new Pair<>(getLeft(), right);
  }

  public Pair<L, R> withValue(R value) {
    return new Pair<>(getKey(), value);
  }
}

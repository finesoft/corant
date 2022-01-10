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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.compare;
import java.beans.Transient;
import java.io.Serializable;
import java.util.Map;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.util.Objects;

/**
 * corant-shared
 *
 * @author bingo 上午10:29:13
 *
 */
public interface Tuple {

  static <L, R> Pair<L, R> pairOf(final L left, final R right) {
    return Pair.of(left, right);
  }

  static <L, R> Pair<L, R> pairOf(final Map.Entry<L, R> entry) {
    return Pair.of(entry.getKey(), entry.getValue());
  }

  static <T extends Comparable<T>> Range<T> rangeOf(T min, T max) {
    return Range.of(min, max);
  }

  static <L, M, R> Triple<L, M, R> tripleOf(final L left, final M middle, final R right) {
    return Triple.of(left, middle, right);
  }

  /**
   * Returns true if this tuple contains the specified element. More formally, returns true if and
   * only if this tuple contains at least one element e such that Objects.equals(o, e).
   *
   * @param o element whose presence in this tuple is to be tested
   * @return {@code true} if this tuple contains the specified element
   */
  boolean contains(Object o);

  /**
   * Returns {@code true} if and only if all the elements of tuple are null.
   *
   * @return isEmpty
   */
  boolean isEmpty();

  /**
   * Returns an array containing all the elements in this tuple in proper sequence (from left to
   * right element or from min to max).
   *
   * @return toArray
   */
  Object[] toArray();

  /**
   * corant-shared
   *
   * @author bingo 上午10:37:41
   *
   */
  class Pair<L, R> implements Tuple, Map.Entry<L, R>, Serializable {

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

    public static <L, R> Pair<L, R> of(final L left, final R right) {
      return new Pair<>(left, right);
    }

    public static <L, R> Pair<L, R> of(final Map.Entry<L, R> entry) {
      return new Pair<>(entry.getKey(), entry.getValue());
    }

    public String asString(final String format) {
      return String.format(format, left, right);
    }

    @Override
    public boolean contains(Object o) {
      return areEqual(o, left) || areEqual(o, right);
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Map.Entry<?, ?>) {
        final Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
        return Objects.areEqual(left, other.getKey()) && Objects.areEqual(right, other.getValue());
      }
      return false;
    }

    @Override
    @Transient
    public L getKey() {
      return left;
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
      return right;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (left == null ? 0 : left.hashCode());
      return prime * result + (right == null ? 0 : right.hashCode());
    }

    @Override
    public boolean isEmpty() {
      return left == null && right == null;
    }

    public L key() {
      return left;
    }

    public L left() {
      return left;
    }

    public Pair<R, L> reverse() {
      return new Pair<>(getRight(), getLeft());
    }

    public R right() {
      return right;
    }

    @Override
    public R setValue(R value) {
      throw new NotSupportedException();
    }

    @Override
    public Object[] toArray() {
      return new Object[] {left, right};
    }

    @Override
    public String toString() {
      return asString("[%s,%s]");
    }

    public R value() {
      return right;
    }

    public Pair<L, R> withKey(final L key) {
      return new Pair<>(key, right);
    }

    public Pair<L, R> withLeft(final L left) {
      return new Pair<>(left, right);
    }

    public Pair<L, R> withRight(final R right) {
      return new Pair<>(left, right);
    }

    public Pair<L, R> withValue(final R value) {
      return new Pair<>(left, value);
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 20:26:59
   *
   */
  class Range<T extends Comparable<T>> implements Tuple {

    @SuppressWarnings({"unchecked", "rawtypes"})
    static final Range emptyInstance = new Range(null, null);

    protected final T min;
    protected final T max;

    protected Range(T min, T max) {
      shouldBeTrue(compare(min, max) <= 0, IllegalArgumentException::new);
      this.min = min;
      this.max = max;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> Range<T> empty() {
      return emptyInstance;
    }

    public static <T extends Comparable<T>> Range<T> of(T min, T max) {
      return new Range<>(min, max);
    }

    public String asString(final String format) {
      return String.format(format, min, max);
    }

    public boolean coincide(Range<T> other) {
      if (other == null) {
        return false;
      } else if (this.equals(other)) {
        return true;
      } else {
        return compare(min, other.min) == 0 && compare(max, other.max) == 0;
      }
    }

    @Override
    public boolean contains(Object o) {
      return areEqual(o, min) || areEqual(o, max);
    }

    public boolean cover(Range<T> other) {
      return lae(min, other.min) && gae(max, other.max);
    }

    public boolean cover(T value) {
      return lae(min, value) && gae(max, value);
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
      Range other = (Range) obj;
      if (max == null) {
        if (other.max != null) {
          return false;
        }
      } else if (!max.equals(other.max)) {
        return false;
      }
      if (min == null) {
        return other.min == null;
      } else {
        return min.equals(other.min);
      }
    }

    public T getMax() {
      return max;
    }

    public T getMin() {
      return min;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (max == null ? 0 : max.hashCode());
      return prime * result + (min == null ? 0 : min.hashCode());
    }

    public boolean intersect(Range<T> other) {
      return gae(min, other.min) && lae(min, other.max)
          || lae(min, other.min) && gae(max, other.max)
          || gae(max, other.min) && lae(max, other.max);
    }

    @Override
    public boolean isEmpty() {
      return min == null && max == null;
    }

    public T max() {
      return max;
    }

    public T min() {
      return min;
    }

    @Override
    public Object[] toArray() {
      return new Object[] {min, max};
    }

    public Pair<T, T> toPair() {
      return Pair.of(min, max);
    }

    @Override
    public String toString() {
      return asString("[%s,%s]");
    }

    public Range<T> withMax(T max) {
      return of(min, max);
    }

    public Range<T> withMin(T min) {
      return of(min, max);
    }

    private boolean gae(T d1, T d2) {
      return compare(d1, d2) >= 0;
    }

    private boolean lae(T d1, T d2) {
      return compare(d1, d2) <= 0;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:37:46
   *
   */
  class Triple<L, M, R> implements Tuple, Serializable {

    private static final long serialVersionUID = 6441751980847755625L;

    @SuppressWarnings("rawtypes")
    static final Triple emptyInstance = new Triple();

    private final L left;
    private final M middle;
    private final R right;

    protected Triple() {
      this(null, null, null);
    }

    protected Triple(final L left, final M middle, final R right) {
      this.left = left;
      this.middle = middle;
      this.right = right;
    }

    @SuppressWarnings("unchecked")
    public static <L, M, R> Triple<L, M, R> empty() {
      return emptyInstance;
    }

    public static <L, M, R> Triple<L, M, R> of(final L left, final M middle, final R right) {
      return new Triple<>(left, middle, right);
    }

    public String asString(final String format) {
      return String.format(format, left, middle, right);
    }

    @Override
    public boolean contains(Object o) {
      return areEqual(o, left) || areEqual(o, middle) || areEqual(o, right);
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Triple<?, ?, ?>) {
        final Triple<?, ?, ?> other = (Triple<?, ?, ?>) obj;
        return Objects.areEqual(left, other.left) && Objects.areEqual(middle, other.middle)
            && Objects.areEqual(right, other.right);
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
      final int prime = 31;
      int result = 1;
      result = prime * result + (left == null ? 0 : left.hashCode());
      result = prime * result + (middle == null ? 0 : middle.hashCode());
      return prime * result + (right == null ? 0 : right.hashCode());
    }

    @Override
    public boolean isEmpty() {
      return left == null && middle == null && right == null;
    }

    public L left() {
      return left;
    }

    public M middle() {
      return middle;
    }

    public Triple<R, M, L> reverse() {
      return new Triple<>(getRight(), getMiddle(), getLeft());
    }

    public R right() {
      return right;
    }

    @Override
    public Object[] toArray() {
      return new Object[] {left, middle, right};
    }

    @Override
    public String toString() {
      return asString("[%s,%s,%s]");
    }

    public Triple<L, M, R> withLeft(final L left) {
      return new Triple<>(left, middle, right);
    }

    public Triple<L, M, R> withMiddle(final M middle) {
      return new Triple<>(left, middle, right);
    }

    public Triple<L, M, R> withRight(final R right) {
      return new Triple<>(left, middle, right);
    }
  }
}

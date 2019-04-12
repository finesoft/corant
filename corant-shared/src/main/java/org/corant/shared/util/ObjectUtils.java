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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;

/**
 * @author bingo 下午2:51:32
 *
 */
public class ObjectUtils {

  protected ObjectUtils() {}

  public static String asString(Object o) {
    return Objects.toString(o);
  }

  public static String asString(Object o, String nullDefault) {
    return Objects.toString(o, nullDefault);
  }

  public static String[] asStrings(Object... objs) {
    return asStrings(null, objs);
  }

  public static String[] asStrings(UnaryOperator<String> uo, Object... objs) {
    if (uo == null) {
      return Arrays.stream(objs).map(o -> asString(o, "null")).toArray(String[]::new);
    } else {
      return Arrays.stream(objs).map(o -> uo.apply(asString(o, "null"))).toArray(String[]::new);
    }
  }

  public static <T> int compare(T a, T b, Comparator<? super T> c) {
    return Objects.compare(a, b, c);
  }

  public static <T> T defaultObject(T obj, Supplier<T> supplier) {
    return obj != null ? obj : supplier.get();
  }

  public static <T> T defaultObject(T obj, T altObj) {
    return defaultObject(obj, () -> altObj);
  }

  @SuppressWarnings("unchecked")
  public static <T> T forceCast(Object o) {
    return o != null ? (T) o : null;
  }

  public static int hash(Object... values) {
    return Objects.hash(values);
  }

  public static int hashCode(Object o) {
    return Objects.hashCode(o);
  }

  public static boolean isDeepEquals(Object a, Object b) {
    return Objects.deepEquals(a, b);
  }

  public static boolean isEquals(Object a, Object b) {
    return Objects.equals(a, b);
  }

  public static <T extends Number & Comparable<T>> boolean isEquals(T a, T b) {
    return Objects.equals(a, b) || a != null && b != null && a.compareTo(b) == 0;
  }

  public static boolean isNotNull(Object obj) {
    return Objects.nonNull(obj);
  }

  public static boolean isNull(Object obj) {
    return Objects.isNull(obj);
  }

  public static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
  }

  public static <T extends Comparable<T>> T min(T a, T b) {
    return a.compareTo(b) < 0 ? a : b;
  }

  public static <T> T newInstance(Class<T> cls) {
    try {
      return cls != null ? cls.newInstance() : null;
    } catch (InstantiationException | IllegalAccessException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <T> Optional<T> optional(T obj) {
    return Optional.ofNullable(obj);
  }

  public static <T> Optional<T> optionalCast(Object o, Class<T> cls) {
    return Optional.ofNullable(tryCast(o, cls));
  }

  public static <T> T tryCast(Object o, Class<T> cls) {
    return o != null && cls.isInstance(o) ? cls.cast(o) : null;
  }

  public static <T> T trySupplied(Supplier<T> supplier) {
    T supplied = null;
    if (supplier != null) {
      try {
        supplied = supplier.get();
      } catch (Exception e) {
      }
    }
    return supplied;
  }

  public static class Pair<L, R> implements Map.Entry<L, R>, Serializable {
    private static final long serialVersionUID = -474294448204498274L;
    private final L left;
    private final R right;

    protected Pair(L left, R right) {
      this.left = left;
      this.right = right;
    }

    public static <L, R> Pair<L, R> empty() {
      return new Pair<>(null, null);
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
        return isEquals(getKey(), other.getKey()) && isEquals(getValue(), other.getValue());
      }
      return false;
    }

    @Override
    public final L getKey() {
      return getLeft();
    }

    public L getLeft() {
      return left;
    }

    public R getRight() {
      return right;
    }

    @Override
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
      return "{" + getLeft() + ':' + getRight() + '}';
    }

    public Pair<L, R> withLeft(L left) {
      return new Pair<>(left, getRight());
    }

    public Pair<L, R> withRight(R right) {
      return new Pair<>(getLeft(), right);
    }
  }

  public static class Triple<L, M, R> implements Serializable {

    private static final long serialVersionUID = 6441751980847755625L;

    private final L left;
    private final M middle;
    private final R right;

    protected Triple(L left, M middle, R right) {
      this.left = left;
      this.middle = middle;
      this.right = right;
    }

    public static <L, M, R> Triple<L, M, R> empty() {
      return new Triple<>(null, null, null);
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
        return isEquals(getLeft(), other.getLeft()) && isEquals(getMiddle(), other.getMiddle())
            && isEquals(getRight(), other.getRight());
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
      return "[" + getLeft() + "," + getMiddle() + "," + getRight() + "]";
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
}

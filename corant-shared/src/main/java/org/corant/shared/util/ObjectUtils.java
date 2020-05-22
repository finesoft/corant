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

import static org.corant.shared.util.StreamUtils.streamOf;
import java.beans.Transient;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-shared
 *
 * @author bingo 下午11:26:00
 *
 */
public class ObjectUtils {

  @SuppressWarnings("rawtypes")
  public static final Consumer EMPTY_CONSUMER = o -> {
  };

  @SuppressWarnings("rawtypes")
  public static final Supplier EMPTY_SUPPLIER = () -> null;

  @SuppressWarnings("rawtypes")
  public static final Function EMPTY_FUNCTION = p -> null;

  @SuppressWarnings("rawtypes")
  public static final Predicate EMPTY_PREDICATE_TRUE = p -> true;

  @SuppressWarnings("rawtypes")
  public static final Predicate EMPTY_PREDICATE_FALSE = p -> false;

  protected ObjectUtils() {}

  public static String asString(Object o) {
    return Objects.toString(o);
  }

  public static String asString(Object o, String nullDefault) {
    return Objects.toString(o, nullDefault);
  }

  public static String[] asStrings(Iterable<?> it) {
    return asStrings(null, streamOf(it).map(Objects::toString).toArray(Object[]::new));
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
    return obj != null ? obj : altObj;
  }

  @SuppressWarnings("unchecked")
  public static <T> Consumer<T> emptyConsumer() {
    return EMPTY_CONSUMER;
  }

  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> emptyPredicate(boolean bool) {
    return bool ? EMPTY_PREDICATE_TRUE : EMPTY_PREDICATE_FALSE;
  }

  @SuppressWarnings("unchecked")
  public static <T> Supplier<T> emptySupplier() {
    return EMPTY_SUPPLIER;
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

  public static boolean isNoneNull(Object... objs) {
    for (Object obj : objs) {
      if (isNull(obj)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isNotNull(Object obj) {
    return Objects.nonNull(obj);
  }

  public static boolean isNull(Object obj) {
    return Objects.isNull(obj);
  }

  /**
   * Return the max one, if the two parameters are the same, then return the first.
   *
   * @param <T>
   * @param a
   * @param b
   * @return max
   */
  public static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
  }

  /**
   * Return the min one, if the two parameters are the same, then return the first.
   *
   * @param <T>
   * @param a
   * @param b
   * @return min
   */
  public static <T extends Comparable<T>> T min(T a, T b) {
    return a.compareTo(b) <= 0 ? a : b;
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
        // Noop! just try...
      }
    }
    return supplied;
  }

  public static void tryThreadSleep(Long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // Noop! just try...
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 上午11:06:08
   *
   */
  public static class MutableReference<T> {

    private final Object[] reference = new Object[] {null};

    public MutableReference(T reference) {
      this.reference[0] = reference;
    }

    public static <X> MutableReference<X> of(X reference) {
      return new MutableReference<>(reference);
    }

    public void accept(Consumer<T> func) {
      T obj = forceCast(reference[0]);
      func.accept(obj);
    }

    public void apply(UnaryOperator<T> func) {
      T obj = forceCast(reference[0]);
      reference[0] = func.apply(obj);
    }

    public T applyAndGet(UnaryOperator<T> func) {
      T obj = forceCast(reference[0]);
      reference[0] = func.apply(obj);
      return forceCast(reference[0]);
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
      return forceCast(reference[0]);
    }

    public T getAndApply(UnaryOperator<T> func) {
      final T obj = forceCast(reference[0]);
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

  /**
   * corant-shared
   *
   * @author bingo 下午11:26:10
   *
   */
  public static class Pair<L, R> implements Map.Entry<L, R>, Serializable {
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
        return isEquals(getKey(), other.getKey()) && isEquals(getValue(), other.getValue());
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

  /**
   * corant-shared
   *
   * @author bingo 下午11:26:17
   *
   */
  public static class ThreadLocalStack<T> {

    private final ThreadLocal<Deque<T>> local = new ThreadLocal<>();

    public void clear() {
      local.remove();
    }

    public boolean isEmpty() {
      Deque<T> stack = stack(false);
      return stack == null || stack.isEmpty();
    }

    public T peek() {
      Deque<T> stack = local.get();
      if (stack == null || stack.isEmpty()) {
        return null;
      }
      return stack.peek();
    }

    public T pop() {
      Deque<T> stack = local.get();
      if (stack == null || stack.isEmpty()) {
        return null;
      }
      return stack.pop();
    }

    public void push(T obj) {
      stack(true).push(obj);
    }

    public int size() {
      Deque<T> stack = stack(false);
      return stack == null ? 0 : stack.size();
    }

    private Deque<T> stack(boolean create) {
      Deque<T> stack = local.get();
      if (stack == null && create) {
        stack = new ArrayDeque<>();
        local.set(stack);
      }
      return stack;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 下午11:26:23
   *
   */
  public static class Triple<L, M, R> implements Serializable {

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
}

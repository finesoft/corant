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
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.forceCast;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-shared
 *
 * @author bingo 下午4:10:40
 *
 */
public interface Mutable<T> extends Serializable {

  T get();

  Mutable<T> set(T object);

  /**
   * corant-shared
   *
   * @author bingo 下午4:26:18
   *
   */
  public static class MutableNumber<T extends Number> extends Number
      implements Mutable<T>, Comparable<MutableNumber<T>>, Serializable, Cloneable {

    private static final long serialVersionUID = -6244772495084570391L;

    private final Object[] value = new Object[] {null};

    protected MutableNumber(final T object) {
      this.value[0] = validate(object);
    }

    public static <X extends Number> MutableNumber<X> of(X object) {
      return new MutableNumber<>(object);
    }

    public static <X extends Number> MutableNumber<X> subtract(MutableNumber<X> m1,
        MutableNumber<X> m2) {
      return new MutableNumber<>(m1.subtractAndGet(m2));
    }

    public static <X extends Number> MutableNumber<X> sum(MutableNumber<X> m1,
        MutableNumber<X> m2) {
      return new MutableNumber<>(m1.addAndGet(m2));
    }

    private static Object validate(final Object object) {
      shouldBeTrue(object != null && (object instanceof Long || object.getClass().equals(Long.TYPE)
          || object instanceof Integer || object.getClass().equals(Integer.TYPE)
          || object instanceof Short || object.getClass().equals(Short.TYPE)
          || object instanceof Byte || object.getClass().equals(Byte.TYPE)
          || object instanceof Double || object.getClass().equals(Double.TYPE)
          || object instanceof Float || object.getClass().equals(Float.TYPE)
          || object instanceof BigDecimal || object instanceof BigInteger));
      return object;
    }

    public void add(final T operand) {
      addAndGet(operand);
    }

    public T addAndGet(final Number operand) {
      this.value[0] = innerAdd(operand);
      return get();
    }

    @Override
    public MutableNumber<T> clone() {
      return new MutableNumber<>(get());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public int compareTo(MutableNumber<T> o) {
      Comparable t1 = (Comparable) get();
      Comparable t2 = (Comparable) o.get();
      return t1.compareTo(t2);
    }

    public void decrement() {
      subtractAndGet(1);
    }

    public T decrementAndGet() {
      return subtractAndGet(1);
    }

    @Override
    public double doubleValue() {
      return get().doubleValue();
    }

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
      @SuppressWarnings("rawtypes")
      MutableNumber other = (MutableNumber) obj;
      if (!Arrays.deepEquals(value, other.value)) {
        return false;
      }
      return true;
    }

    @Override
    public float floatValue() {
      return get().floatValue();
    }

    @Override
    public T get() {
      return forceCast(value[0]);
    }

    public T getAndAdd(final Number operand) {
      final T pre = get();
      addAndGet(operand);
      return pre;
    }

    public T getAndDecrement() {
      return getAndSubtract(1);
    }

    public T getAndIncrement() {
      return getAndAdd(1);
    }

    public T getAndSubtract(final Number operand) {
      final T pre = get();
      subtractAndGet(operand);
      return pre;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.deepHashCode(value);
      return result;
    }

    public void increment() {
      addAndGet(1);
    }

    public T incrementAndGet() {
      return addAndGet(1);
    }

    @Override
    public int intValue() {
      return get().intValue();
    }

    @Override
    public long longValue() {
      return get().longValue();
    }

    @Override
    public MutableNumber<T> set(final T object) {
      this.value[0] = validate(object);
      return this;
    }

    public void subtract(final T operand) {
      subtractAndGet(operand);
    }

    public T subtractAndGet(final Number operand) {
      this.value[0] = innersubtract(operand);
      return get();
    }

    @Override
    public String toString() {
      return value[0] == null ? "null" : value[0].toString();
    }

    private T innerAdd(Number operand) {
      shouldNotNull(operand);
      final T current = get();
      if (current instanceof Long) {
        return forceCast(Long.valueOf(current.longValue() + operand.longValue()));
      } else if (current instanceof Integer) {
        return forceCast(Integer.valueOf(current.intValue() + operand.intValue()));
      } else if (current instanceof Short) {
        return forceCast(Short.valueOf((short) (current.shortValue() + operand.shortValue())));
      } else if (current instanceof Byte) {
        return forceCast(Byte.valueOf((byte) (current.byteValue() + operand.byteValue())));
      } else if (current instanceof Byte) {
        return forceCast(Byte.valueOf((byte) (current.byteValue() + operand.byteValue())));
      } else if (current instanceof Double) {
        return forceCast(Double.valueOf(current.doubleValue() + operand.doubleValue()));
      } else if (current instanceof Float) {
        return forceCast(Float.valueOf(current.floatValue() + operand.floatValue()));
      } else if (current instanceof BigInteger) {
        return forceCast(((BigInteger) current).add(BigInteger.valueOf(operand.longValue())));
      } else if (current instanceof BigDecimal) {
        return forceCast(((BigDecimal) current).add(BigDecimal.valueOf(operand.doubleValue())));
      } else {
        throw new NotSupportedException();
      }
    }

    private T innersubtract(Number operand) {
      shouldNotNull(operand);
      final T current = get();
      if (current instanceof Long) {
        return forceCast(Long.valueOf(current.longValue() - operand.longValue()));
      } else if (current instanceof Integer) {
        return forceCast(Integer.valueOf(current.intValue() - operand.intValue()));
      } else if (current instanceof Short) {
        return forceCast(Short.valueOf((short) (current.shortValue() - operand.shortValue())));
      } else if (current instanceof Byte) {
        return forceCast(Byte.valueOf((byte) (current.byteValue() - operand.byteValue())));
      } else if (current instanceof Byte) {
        return forceCast(Byte.valueOf((byte) (current.byteValue() - operand.byteValue())));
      } else if (current instanceof Double) {
        return forceCast(Double.valueOf(current.doubleValue() - operand.doubleValue()));
      } else if (current instanceof Float) {
        return forceCast(Float.valueOf(current.floatValue() - operand.floatValue()));
      } else if (current instanceof BigInteger) {
        return forceCast(((BigInteger) current).subtract(BigInteger.valueOf(operand.longValue())));
      } else if (current instanceof BigDecimal) {
        return forceCast(
            ((BigDecimal) current).subtract(BigDecimal.valueOf(operand.doubleValue())));
      } else {
        throw new NotSupportedException();
      }
    }

  }

  /**
   *
   * corant-shared
   *
   * @author bingo 下午5:23:01
   *
   */
  public static class MutableReference<T> implements Mutable<T> {

    private static final long serialVersionUID = 6276199153168086544L;

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

    @Override
    public T get() {
      return forceCast(reference[0]);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.deepHashCode(reference);
      return result;
    }

    @Override
    public MutableReference<T> set(T reference) {
      this.reference[0] = reference;
      return this;
    }
  }
}

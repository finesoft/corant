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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.forceCast;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.function.Supplier;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-shared
 *
 * @author bingo 下午4:10:40
 *
 */
public interface Mutable<T> extends Serializable, Supplier<T> {

  @Override
  T get();

  Mutable<T> set(T object);

  /**
   * corant-shared
   *
   * @author bingo 上午1:00:35
   *
   */
  public static class MutableBoolean extends MutableObject<Boolean>
      implements Comparable<MutableBoolean> {

    private static final long serialVersionUID = -8744451088815002675L;

    protected MutableBoolean(final boolean value) {
      super(value);
    }

    public static MutableBoolean of(final boolean value) {
      return new MutableBoolean(value);
    }

    @Override
    public int compareTo(final MutableBoolean o) {
      return value == o.value ? 0 : !value ? -1 : 1;
    }

    public Boolean getAndSet(boolean other) {
      final Boolean pre = get();
      set(other);
      return pre;
    }

    public Boolean setAndGet(boolean other) {
      return set(other).get();
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 下午4:26:18
   *
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static class MutableNumber<T extends Number> extends Number
      implements Mutable<T>, Comparable<MutableNumber<T>>, Serializable, Cloneable {

    private static final long serialVersionUID = -6244772495084570391L;

    private T value = null;

    protected MutableNumber(final T object) {
      this.value = validate(object);
    }

    public static <X extends Number> MutableNumber<X> of(X object) {
      return new MutableNumber<>(object);
    }

    public static <X extends Number> MutableNumber<X> subtract(MutableNumber<X> m1,
        MutableNumber<X> m2) {
      return new MutableNumber<>((X) doSubtract(m1.get(), m2.get()));
    }

    public static <X extends Number> MutableNumber<X> sum(MutableNumber<X> m1,
        MutableNumber<X> m2) {
      return new MutableNumber<>((X) doAdd(m1.get(), m2.get()));
    }

    protected static Number doAdd(Number current, Number operand) {
      shouldNotNull(operand);
      if (current instanceof Long) {
        return Long.valueOf(current.longValue() + operand.longValue());
      } else if (current instanceof Integer) {
        return Integer.valueOf(current.intValue() + operand.intValue());
      } else if (current instanceof Short) {
        return Short.valueOf((short) (current.shortValue() + operand.shortValue()));
      } else if (current instanceof Byte) {
        return Byte.valueOf((byte) (current.byteValue() + operand.byteValue()));
      } else if (current instanceof Double) {
        return Double.valueOf(current.doubleValue() + operand.doubleValue());
      } else if (current instanceof Float) {
        return Float.valueOf(current.floatValue() + operand.floatValue());
      } else if (current instanceof BigInteger) {
        return ((BigInteger) current).add(BigInteger.valueOf(operand.longValue()));
      } else if (current instanceof BigDecimal) {
        return ((BigDecimal) current).add(BigDecimal.valueOf(operand.doubleValue()));
      } else {
        throw new NotSupportedException();
      }
    }

    protected static Number doSubtract(Number current, Number operand) {
      shouldNotNull(operand);
      if (current instanceof Long) {
        return Long.valueOf(current.longValue() - operand.longValue());
      } else if (current instanceof Integer) {
        return Integer.valueOf(current.intValue() - operand.intValue());
      } else if (current instanceof Short) {
        return Short.valueOf((short) (current.shortValue() - operand.shortValue()));
      } else if (current instanceof Byte) {
        return Byte.valueOf((byte) (current.byteValue() - operand.byteValue()));
      } else if (current instanceof Double) {
        return Double.valueOf(current.doubleValue() - operand.doubleValue());
      } else if (current instanceof Float) {
        return Float.valueOf(current.floatValue() - operand.floatValue());
      } else if (current instanceof BigInteger) {
        return ((BigInteger) current).subtract(BigInteger.valueOf(operand.longValue()));
      } else if (current instanceof BigDecimal) {
        return ((BigDecimal) current).subtract(BigDecimal.valueOf(operand.doubleValue()));
      } else {
        throw new NotSupportedException();
      }
    }

    protected static <T> T validate(final T object) {
      if (object != null && (object instanceof Long || object.getClass().equals(Long.TYPE)
          || object instanceof Integer || object.getClass().equals(Integer.TYPE)
          || object instanceof Short || object.getClass().equals(Short.TYPE)
          || object instanceof Byte || object.getClass().equals(Byte.TYPE)
          || object instanceof Double || object.getClass().equals(Double.TYPE)
          || object instanceof Float || object.getClass().equals(Float.TYPE)
          || object instanceof BigDecimal || object instanceof BigInteger)) {
        return object;
      } else {
        throw new NotSupportedException(
            "MutableNumber only support Integer/Long/Byte/Short/Double/Float/BigInteger/BigDecimal.");
      }
    }

    public MutableNumber<T> add(final T operand) {
      addAndGet(operand);
      return this;
    }

    public T addAndGet(final Number operand) {
      this.value = (T) doAdd(get(), operand);
      return get();
    }

    @Override
    public MutableNumber<T> clone() {
      return new MutableNumber<>(get());
    }

    @Override
    public int compareTo(MutableNumber<T> o) {
      Comparable t1 = (Comparable) get();
      Comparable t2 = (Comparable) o.get();
      return t1.compareTo(t2);
    }

    public MutableNumber<T> decrement() {
      subtractAndGet(1);
      return this;
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
      MutableNumber other = (MutableNumber) obj;
      if (value == null) {
        if (other.value != null) {
          return false;
        }
      } else if (!value.equals(other.value)) {
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
      return forceCast(value);
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
      result = prime * result + (value == null ? 0 : value.hashCode());
      return result;
    }

    public MutableNumber<T> increment() {
      addAndGet(1);
      return this;
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
      this.value = validate(object);
      return this;
    }

    public MutableNumber<T> subtract(final T operand) {
      subtractAndGet(operand);
      return this;
    }

    public T subtractAndGet(final Number operand) {
      this.value = (T) doSubtract(get(), operand);
      return get();
    }

    @Override
    public String toString() {
      return value == null ? "null" : value.toString();
    }

  }

  /**
   *
   * corant-shared
   *
   * @author bingo 下午5:23:01
   *
   */
  public static class MutableObject<T> implements Mutable<T> {

    private static final long serialVersionUID = 6276199153168086544L;

    T value;

    public MutableObject() {
      super();
    }

    protected MutableObject(final T value) {
      super();
      this.value = value;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (this == obj) {
        return true;
      }
      if (this.getClass() == obj.getClass()) {
        final MutableObject<?> that = (MutableObject<?>) obj;
        return this.value.equals(that.value);
      }
      return false;
    }

    @Override
    public T get() {
      return this.value;
    }

    @Override
    public int hashCode() {
      return value == null ? 0 : value.hashCode();
    }

    @Override
    public MutableObject<T> set(final T value) {
      this.value = value;
      return this;
    }

    @Override
    public String toString() {
      return value == null ? "null" : value.toString();
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 上午1:00:35
   *
   */
  public static class MutableString extends MutableObject<String>
      implements Comparable<MutableString> {

    private static final long serialVersionUID = 4187776698909971470L;

    protected MutableString() {
      super();
    }

    protected MutableString(final String value) {
      super(value);
    }

    public static MutableString of(final String value) {
      return new MutableString(value);
    }

    @Override
    public int compareTo(MutableString o) {
      return value.compareTo(o.value);
    }

    public String getAndSet(String other) {
      final String pre = get();
      set(other);
      return pre;
    }

    public String setAndGet(String other) {
      return set(other).get();
    }
  }

  /**
   *
   * corant-shared
   *
   * @author bingo 上午1:02:18
   *
   */
  @SuppressWarnings("unchecked")
  public static class MutableTemporal<T extends Temporal> extends MutableObject<T>
      implements Temporal {

    private static final long serialVersionUID = -5181139992639806156L;

    protected MutableTemporal(final T value) {
      super(value);
    }

    public static <X extends Temporal> MutableTemporal<X> of(final X value) {
      return new MutableTemporal<>(value);
    }

    public T getAndMinus(long amountToSubtract, TemporalUnit unit) {
      final T pre = get();
      set((T) get().minus(amountToSubtract, unit));
      return pre;
    }

    public T getAndMinus(TemporalAmount amount) {
      final T pre = get();
      set((T) amount.subtractFrom(get()));
      return pre;
    }

    public T getAndPlus(long amountToAdd, TemporalUnit unit) {
      final T pre = get();
      set((T) get().plus(amountToAdd, unit));
      return pre;
    }

    public T getAndPlus(TemporalAmount amount) {
      final T pre = get();
      set((T) amount.addTo(get()));
      return pre;
    }

    public T getAndSet(T other) {
      final T pre = get();
      set(other);
      return pre;
    }

    @Override
    public long getLong(TemporalField field) {
      return get().getLong(field);
    }

    @Override
    public boolean isSupported(TemporalField field) {
      return get().isSupported(field);
    }

    @Override
    public boolean isSupported(TemporalUnit unit) {
      return get().isSupported(unit);
    }

    @Override
    public MutableTemporal<T> minus(long amountToSubtract, TemporalUnit unit) {
      return set((T) get().minus(amountToSubtract, unit));
    }

    @Override
    public MutableTemporal<T> minus(TemporalAmount amount) {
      return set((T) amount.subtractFrom(get()));
    }

    public T minusAndGet(long amountToSubtract, TemporalUnit unit) {
      return minus(amountToSubtract, unit).get();
    }

    public T minusAndGet(TemporalAmount amount) {
      return minus(amount).get();
    }

    @Override
    public MutableTemporal<T> plus(long amountToAdd, TemporalUnit unit) {
      return set((T) get().plus(amountToAdd, unit));
    }

    @Override
    public MutableTemporal<T> plus(TemporalAmount amount) {
      return set((T) amount.addTo(get()));
    }

    public T plusAndGet(long amountToAdd, TemporalUnit unit) {
      return plus(amountToAdd, unit).get();
    }

    public T plusAndGet(TemporalAmount amount) {
      return plus(amount).get();
    }

    @Override
    public MutableTemporal<T> set(T value) {
      return (MutableTemporal<T>) super.set(value);
    }

    public T setAndGet(T other) {
      return set(other).get();
    }

    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
      return get().until(endExclusive, unit);
    }

    @Override
    public MutableTemporal<T> with(TemporalAdjuster adjuster) {
      return set((T) adjuster.adjustInto(get()));
    }

    @Override
    public MutableTemporal<T> with(TemporalField field, long newValue) {
      return set((T) get().with(field, newValue));
    }

  }
}

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
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.NULL;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-shared
 *
 * @author bingo 下午4:10:40
 *
 */
public interface Mutable<T> extends Supplier<T> {

  default Mutable<T> apply(UnaryOperator<T> op) {
    set(op.apply(get()));
    return this;
  }

  default boolean isEmpty() {
    return get() == null;
  }

  default T orElse(T alt) {
    return defaultObject(get(), alt);
  }

  default T orElseGet(Supplier<? extends T> supplier) {
    return defaultObject(get(), supplier);
  }

  Mutable<T> set(T object);

  /**
   * corant-shared
   *
   * @author bingo 上午1:00:35
   *
   */
  class MutableBoolean extends MutableObject<Boolean> implements Comparable<MutableBoolean> {

    public MutableBoolean(final boolean value) {
      super(value);
    }

    public static MutableBoolean of(final boolean value) {
      return new MutableBoolean(value);
    }

    @Override
    public int compareTo(final MutableBoolean o) {
      return areEqual(value, o.value) ? 0 : value ? 1 : -1;
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
   * @author bingo 14:12:27
   *
   */
  class MutableByte extends MutableNumber<Byte> {

    private static final long serialVersionUID = 7448907394279327148L;

    public MutableByte() {
      this(0);
    }

    public MutableByte(final byte object) {
      super(object);
    }

    public MutableByte(final Number object) {
      super(object.byteValue());
    }

    public static MutableByte of(final byte object) {
      return new MutableByte(object);
    }

    public static MutableByte subtract(MutableByte m1, MutableByte m2) {
      return new MutableByte(m1.subtractAndGet(m2));
    }

    public static MutableByte sum(MutableByte m1, MutableByte m2) {
      return new MutableByte(m1.addAndGet(m2));
    }

    @Override
    public MutableByte add(Byte operand) {
      super.add(operand);
      return this;
    }

    @Override
    public Byte addAndGet(Number operand) {
      final Byte current = value;
      value = (byte) (current + shouldNotNull(operand).byteValue());
      return value;
    }

    @Override
    public MutableByte decrement() {
      super.decrement();
      return this;
    }

    @Override
    public MutableByte increment() {
      super.increment();
      return this;
    }

    @Override
    public MutableByte set(Byte object) {
      super.set(object);
      return this;
    }

    @Override
    public MutableByte subtract(Byte operand) {
      super.subtract(operand);
      return this;
    }

    @Override
    public Byte subtractAndGet(final Number operand) {
      final Byte current = value;
      value = (byte) (current - shouldNotNull(operand).byteValue());
      return value;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 14:12:27
   *
   */
  class MutableDouble extends MutableNumber<Double> {

    private static final long serialVersionUID = -8025777846150268416L;

    public MutableDouble() {
      this(0.0d);
    }

    public MutableDouble(double object) {
      super(object);
    }

    public MutableDouble(Number object) {
      super(object.doubleValue());
    }

    public static MutableDouble of(final double object) {
      return new MutableDouble(object);
    }

    public static MutableDouble subtract(MutableDouble m1, MutableDouble m2) {
      return new MutableDouble(m1.subtractAndGet(m2));
    }

    public static MutableDouble sum(MutableDouble m1, MutableDouble m2) {
      return new MutableDouble(m1.addAndGet(m2));
    }

    @Override
    public MutableDouble add(Double operand) {
      super.add(operand);
      return this;
    }

    @Override
    public Double addAndGet(Number operand) {
      final Double current = value;
      value = current + shouldNotNull(operand).doubleValue();
      return value;
    }

    @Override
    public MutableDouble decrement() {
      super.decrement();
      return this;
    }

    @Override
    public MutableDouble increment() {
      super.increment();
      return this;
    }

    @Override
    public MutableDouble set(Double object) {
      super.set(object);
      return this;
    }

    @Override
    public MutableDouble subtract(Double operand) {
      super.subtract(operand);
      return this;
    }

    @Override
    public Double subtractAndGet(Number operand) {
      final Double current = value;
      value = current - shouldNotNull(operand).doubleValue();
      return value;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 14:12:27
   *
   */
  class MutableFloat extends MutableNumber<Float> {

    private static final long serialVersionUID = 8230534097662673489L;

    public MutableFloat() {
      this(0.0f);
    }

    public MutableFloat(float object) {
      super(object);
    }

    public MutableFloat(Number object) {
      super(object.floatValue());
    }

    public static MutableFloat of(final float object) {
      return new MutableFloat(object);
    }

    public static MutableFloat subtract(MutableFloat m1, MutableFloat m2) {
      return new MutableFloat(m1.subtractAndGet(m2));
    }

    public static MutableFloat sum(MutableFloat m1, MutableFloat m2) {
      return new MutableFloat(m1.addAndGet(m2));
    }

    @Override
    public MutableFloat add(Float operand) {
      super.add(operand);
      return this;
    }

    @Override
    public Float addAndGet(Number operand) {
      final Float current = value;
      value = current + shouldNotNull(operand).floatValue();
      return value;
    }

    @Override
    public MutableFloat decrement() {
      super.decrement();
      return this;
    }

    @Override
    public MutableFloat increment() {
      super.increment();
      return this;
    }

    @Override
    public MutableFloat set(Float object) {
      super.set(object);
      return this;
    }

    @Override
    public MutableFloat subtract(Float operand) {
      super.subtract(operand);
      return this;
    }

    @Override
    public Float subtractAndGet(Number operand) {
      final Float current = value;
      value = current - shouldNotNull(operand).floatValue();
      return value;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 14:12:27
   *
   */
  class MutableInteger extends MutableNumber<Integer> {

    private static final long serialVersionUID = -5952332917787746814L;

    public MutableInteger() {
      this(0);
    }

    public MutableInteger(int object) {
      super(object);
    }

    public MutableInteger(Number object) {
      super(object.intValue());
    }

    public static MutableInteger of(final int object) {
      return new MutableInteger(object);
    }

    public static MutableInteger subtract(MutableInteger m1, MutableInteger m2) {
      return new MutableInteger(m1.subtractAndGet(m2));
    }

    public static MutableInteger sum(MutableInteger m1, MutableInteger m2) {
      return new MutableInteger(m1.addAndGet(m2));
    }

    @Override
    public MutableInteger add(Integer operand) {
      super.add(operand);
      return this;
    }

    @Override
    public Integer addAndGet(Number operand) {
      final Integer current = value;
      value = current + shouldNotNull(operand).intValue();
      return value;
    }

    @Override
    public MutableInteger decrement() {
      super.decrement();
      return this;
    }

    @Override
    public MutableInteger increment() {
      super.increment();
      return this;
    }

    @Override
    public MutableInteger set(Integer object) {
      super.set(object);
      return this;
    }

    @Override
    public MutableInteger subtract(Integer operand) {
      super.subtract(operand);
      return this;
    }

    @Override
    public Integer subtractAndGet(Number operand) {
      final Integer current = value;
      value = current - shouldNotNull(operand).intValue();
      return value;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 14:12:27
   *
   */
  class MutableLong extends MutableNumber<Long> {

    private static final long serialVersionUID = -8400347013924878207L;

    public MutableLong() {
      this(0L);
    }

    public MutableLong(long object) {
      super(object);
    }

    public MutableLong(Number object) {
      super(object.longValue());
    }

    public static MutableLong of(final long object) {
      return new MutableLong(object);
    }

    public static MutableLong subtract(MutableLong m1, MutableLong m2) {
      return new MutableLong(m1.subtractAndGet(m2));
    }

    public static MutableLong sum(MutableLong m1, MutableLong m2) {
      return new MutableLong(m1.addAndGet(m2));
    }

    @Override
    public MutableLong add(Long operand) {
      super.add(operand);
      return this;
    }

    @Override
    public Long addAndGet(Number operand) {
      final Long current = value;
      value = current + shouldNotNull(operand).longValue();
      return value;
    }

    @Override
    public MutableLong decrement() {
      super.decrement();
      return this;
    }

    @Override
    public MutableLong increment() {
      super.increment();
      return this;
    }

    @Override
    public MutableLong set(Long object) {
      super.set(object);
      return this;
    }

    @Override
    public MutableLong subtract(Long operand) {
      super.subtract(operand);
      return this;
    }

    @Override
    public Long subtractAndGet(Number operand) {
      final Long current = value;
      value = current - shouldNotNull(operand).longValue();
      return value;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 下午4:26:18
   *
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  class MutableNumber<T extends Number> extends Number
      implements Mutable<T>, Comparable<MutableNumber<T>> {

    private static final long serialVersionUID = -6244772495084570391L;

    T value;

    protected MutableNumber(final T object) {
      value = validate(object);
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
        return current.longValue() + operand.longValue();
      } else if (current instanceof Integer) {
        return current.intValue() + operand.intValue();
      } else if (current instanceof Short) {
        return (short) (current.shortValue() + operand.shortValue());
      } else if (current instanceof Byte) {
        return (byte) (current.byteValue() + operand.byteValue());
      } else if (current instanceof Double) {
        return current.doubleValue() + operand.doubleValue();
      } else if (current instanceof Float) {
        return current.floatValue() + operand.floatValue();
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
        return current.longValue() - operand.longValue();
      } else if (current instanceof Integer) {
        return current.intValue() - operand.intValue();
      } else if (current instanceof Short) {
        return (short) (current.shortValue() - operand.shortValue());
      } else if (current instanceof Byte) {
        return (byte) (current.byteValue() - operand.byteValue());
      } else if (current instanceof Double) {
        return current.doubleValue() - operand.doubleValue();
      } else if (current instanceof Float) {
        return current.floatValue() - operand.floatValue();
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
      value = (T) doAdd(get(), operand);
      return get();
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
        return other.value == null;
      } else {
        return value.equals(other.value);
      }
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
      return prime * result + (value == null ? 0 : value.hashCode());
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
      value = validate(object);
      return this;
    }

    public MutableNumber<T> subtract(final T operand) {
      subtractAndGet(operand);
      return this;
    }

    public T subtractAndGet(final Number operand) {
      value = (T) doSubtract(get(), operand);
      return get();
    }

    @Override
    public String toString() {
      return value == null ? NULL : value.toString();
    }

  }

  /**
   *
   * corant-shared
   *
   * @author bingo 下午5:23:01
   *
   */
  class MutableObject<T> implements Mutable<T> {

    T value;

    public MutableObject() {}

    public MutableObject(final T value) {
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
      return value == null ? NULL : value.toString();
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 14:12:27
   *
   */
  class MutableShort extends MutableNumber<Short> {
    private static final long serialVersionUID = -6018676211622288641L;

    public MutableShort() {
      this(0);
    }

    public MutableShort(Number object) {
      super(object.shortValue());
    }

    public MutableShort(short object) {
      super(object);
    }

    public static MutableShort of(final short object) {
      return new MutableShort(object);
    }

    public static MutableShort subtract(MutableShort m1, MutableShort m2) {
      return new MutableShort(m1.subtractAndGet(m2));
    }

    public static MutableShort sum(MutableShort m1, MutableShort m2) {
      return new MutableShort(m1.addAndGet(m2));
    }

    @Override
    public MutableShort add(Short operand) {
      super.add(operand);
      return this;
    }

    @Override
    public Short addAndGet(Number operand) {
      final Short current = value;
      value = (short) (current + shouldNotNull(operand).shortValue());
      return value;
    }

    @Override
    public MutableShort decrement() {
      super.decrement();
      return this;
    }

    @Override
    public MutableShort increment() {
      super.increment();
      return this;
    }

    @Override
    public MutableShort set(Short object) {
      super.set(object);
      return this;
    }

    @Override
    public MutableShort subtract(Short operand) {
      super.subtract(operand);
      return this;
    }

    @Override
    public Short subtractAndGet(Number operand) {
      final Short current = value;
      value = (short) (current - shouldNotNull(operand).shortValue());
      return value;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 上午1:00:35
   *
   */
  class MutableString extends MutableObject<String> implements Comparable<MutableString> {

    public MutableString(final String value) {
      super(value);
    }

    protected MutableString() {}

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
  class MutableTemporal<T extends Temporal> extends MutableObject<T> implements Temporal {

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
      super.set(value);
      return this;
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

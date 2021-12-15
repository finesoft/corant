/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * corant-shared
 *
 * @author bingo 下午7:52:34
 *
 */
public class Atomics {

  private Atomics() {}

  /**
   * Return the atomic instance supplier. Suitable for singleton object construction in
   * multi-threaded scenarios; in most cases, it can replace those code blocks that use the volatile
   * or synchronized keywords for one-off singleton object construction.
   *
   * Note: The incoming {@code supplier} must non-null and must provide a non-null instance when
   * called, otherwise an exception will be thrown, the {@code supplier} may be called more than
   * once.
   *
   * Partial code block come from org.apache.commons.
   *
   * @param <T> the instance type
   * @param supplier the actual instance construction
   * @return the atomic instance supplier
   */
  public static <T> Supplier<T> atomicInitializer(final Supplier<T> supplier) {
    shouldNotNull(supplier);
    return new Supplier<>() {
      final AtomicReference<T> supplied = new AtomicReference<>();

      @Override
      public T get() {
        T result = supplied.get();
        if (result == null) {
          result = shouldNotNull(supplier.get());
          if (!supplied.compareAndSet(null, result)) {
            result = supplied.get();
          }
        }
        return result;
      }
    };
  }

  /**
   * Return the atomic instance supplier, and ensure that the incoming {@code supplier} is only
   * called once. Suitable for singleton object construction inmulti-threaded scenarios; in most
   * cases, it can replace those code blocks that use the volatile or synchronized keywords for
   * one-off singleton object construction.
   *
   * Note: The incoming {@code supplier} must non-null and must provide a non-null instance when
   * called, otherwise an exception will be thrown.
   *
   * Partial code block come from org.apache.commons.
   *
   * @param <T> the instance type
   * @param supplier the actual instance construction
   * @return the atomic instance supplier
   */
  public static <T> Supplier<T> strictAtomicInitializer(final Supplier<T> supplier) {
    shouldNotNull(supplier);
    return new Supplier<>() {
      final AtomicReference<Supplier<T>> factory = new AtomicReference<>();
      final AtomicReference<T> supplied = new AtomicReference<>();

      @Override
      public T get() {
        T result;
        while ((result = supplied.get()) == null) {
          if (factory.compareAndSet(null, supplier)) {
            supplied.set(shouldNotNull(factory.get().get()));
          }
        }
        return result;
      }
    };
  }

  /**
   * corant-shared
   *
   * @author bingo 下午8:54:10
   *
   */
  public static class AtomicDouble extends Number {

    private static final long serialVersionUID = -3529474371884254358L;

    final AtomicLong bits;

    public AtomicDouble() {
      this(0.0d);
    }

    public AtomicDouble(double value) {
      bits = new AtomicLong(doubleToRawLongBits(value));
    }

    public final double addAndGet(double delta) {
      return add(delta, false);
    }

    public final boolean compareAndSet(double expect, double update) {
      return bits.compareAndSet(doubleToRawLongBits(expect), doubleToRawLongBits(update));
    }

    public final double decrementAndGet() {
      return addAndGet(-1.0d);
    }

    @Override
    public double doubleValue() {
      return get();
    }

    @Override
    public float floatValue() {
      return (float) get();
    }

    public final double get() {
      return longBitsToDouble(bits.get());
    }

    public final double getAndAdd(double delta) {
      return add(delta, true);
    }

    public final double getAndDecrement() {
      return getAndAdd(-1.0d);
    }

    public final double getAndIncrement() {
      return getAndAdd(1.0d);
    }

    public final double getAndSet(double value) {
      return longBitsToDouble(bits.getAndSet(doubleToRawLongBits(value)));
    }

    public final double incrementAndGet() {
      return addAndGet(1.0d);
    }

    @Override
    public int intValue() {
      return (int) get();
    }

    @Override
    public long longValue() {
      return (long) get();
    }

    public final void set(double value) {
      bits.set(doubleToRawLongBits(value));
    }

    @Override
    public String toString() {
      return Double.toString(get());
    }

    public final boolean weakCompareAndSet(double expect, double update) {
      return bits.weakCompareAndSetPlain(doubleToRawLongBits(expect), doubleToRawLongBits(update));
    }

    private double add(double delta, boolean exp) {
      for (;;) {
        final long expectBits = bits.get();
        final double expect = longBitsToDouble(expectBits);
        final double update = expect + delta;
        final long updateBits = doubleToRawLongBits(update);
        if (bits.compareAndSet(expectBits, updateBits)) {
          return exp ? expect : update;
        }
      }
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午9:23:40
   *
   */
  public static class AtomicDoubleArray implements Serializable {

    private static final long serialVersionUID = 2409815249343148615L;

    final AtomicLongArray bitsArray;

    public AtomicDoubleArray(double[] array) {
      final int len = array.length;
      final long[] longArray = new long[len];
      for (int i = 0; i < len; i++) {
        longArray[i] = doubleToRawLongBits(array[i]);
      }
      bitsArray = new AtomicLongArray(longArray);
    }

    public AtomicDoubleArray(int length) {
      bitsArray = new AtomicLongArray(length);
    }

    public double addAndGet(int i, double delta) {
      return add(i, delta, false);
    }

    public final boolean compareAndSet(int i, double expect, double update) {
      return bitsArray.compareAndSet(i, doubleToRawLongBits(expect), doubleToRawLongBits(update));
    }

    public final double get(int i) {
      return longBitsToDouble(bitsArray.get(i));
    }

    public final double getAndAdd(int i, double delta) {
      return add(i, delta, true);
    }

    public final double getAndSet(int i, double newValue) {
      long next = doubleToRawLongBits(newValue);
      return longBitsToDouble(bitsArray.getAndSet(i, next));
    }

    public final void lazySet(int i, double newValue) {
      set(i, newValue);
    }

    public final int length() {
      return bitsArray.length();
    }

    public final void set(int i, double newValue) {
      long next = doubleToRawLongBits(newValue);
      bitsArray.set(i, next);
    }

    @Override
    public String toString() {
      int length = length();
      if (length == 0) {
        return "[]";
      } else {
        return "["
            .concat(IntStream.of(length).mapToObj(bitsArray::get).map(Double::longBitsToDouble)
                .map(d -> Double.toString(d)).collect(Collectors.joining(",")))
            .concat("]");
      }
    }

    public final boolean weakCompareAndSet(int i, double expect, double update) {
      return bitsArray.weakCompareAndSetPlain(i, doubleToRawLongBits(expect),
          doubleToRawLongBits(update));
    }

    private double add(int i, double delta, boolean exp) {
      for (;;) {
        final long expectBits = bitsArray.get(i);
        final double expect = longBitsToDouble(expectBits);
        final double update = expect + delta;
        final long updateBits = doubleToRawLongBits(update);
        if (bitsArray.compareAndSet(i, expectBits, updateBits)) {
          return exp ? expect : update;
        }
      }
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 下午8:54:15
   *
   */
  public static class AtomicFloat extends Number {

    private static final long serialVersionUID = -558074004209426437L;

    final AtomicInteger bits;

    public AtomicFloat() {
      this(0.0f);
    }

    public AtomicFloat(float value) {
      bits = new AtomicInteger(floatToIntBits(value));
    }

    public final float addAndGet(float delta) {
      return add(delta, false);
    }

    public final boolean compareAndSet(float expect, float update) {
      return bits.compareAndSet(floatToIntBits(expect), floatToIntBits(update));
    }

    public final float decrementAndGet() {
      return addAndGet(-1.0f);
    }

    @Override
    public double doubleValue() {
      return get();
    }

    @Override
    public float floatValue() {
      return get();
    }

    public final float get() {
      return intBitsToFloat(bits.get());
    }

    public final float getAndAdd(float delta) {
      return add(delta, true);
    }

    public final float getAndDecrement() {
      return getAndAdd(-1.0f);
    }

    public final float getAndIncrement() {
      return getAndAdd(1.0f);
    }

    public final float getAndSet(float value) {
      return intBitsToFloat(bits.getAndSet(floatToIntBits(value)));
    }

    public final float incrementAndGet() {
      return addAndGet(1.0f);
    }

    @Override
    public int intValue() {
      return (int) get();
    }

    @Override
    public long longValue() {
      return (long) get();
    }

    public final void set(float value) {
      bits.set(floatToIntBits(value));
    }

    @Override
    public String toString() {
      return Float.toString(get());
    }

    public final boolean weakCompareAndSet(float expect, float update) {
      return bits.weakCompareAndSetPlain(floatToIntBits(expect), floatToIntBits(update));
    }

    private float add(float delta, boolean exp) {
      for (;;) {
        final int expectBits = bits.get();
        final float expect = intBitsToFloat(expectBits);
        final float update = expect + delta;
        final int updateBits = floatToIntBits(update);
        if (bits.compareAndSet(expectBits, updateBits)) {
          return exp ? expect : update;
        }
      }
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午9:23:48
   *
   */
  public static class AtomicFloatArray implements Serializable {
    private static final long serialVersionUID = 7823813961079334435L;
    final AtomicIntegerArray bitsArray;

    public AtomicFloatArray(float[] array) {
      final int len = array.length;
      final int[] intArray = new int[len];
      for (int i = 0; i < len; i++) {
        intArray[i] = floatToIntBits(array[i]);
      }
      bitsArray = new AtomicIntegerArray(intArray);
    }

    public AtomicFloatArray(int length) {
      bitsArray = new AtomicIntegerArray(length);
    }

    public float addAndGet(int i, float delta) {
      return add(i, delta, false);
    }

    public final boolean compareAndSet(int i, float expect, float update) {
      return bitsArray.compareAndSet(i, floatToIntBits(expect), floatToIntBits(update));
    }

    public final float get(int i) {
      return intBitsToFloat(bitsArray.get(i));
    }

    public final float getAndAdd(int i, float delta) {
      return add(i, delta, true);
    }

    public final float getAndSet(int i, float newValue) {
      int next = floatToIntBits(newValue);
      return intBitsToFloat(bitsArray.getAndSet(i, next));
    }

    public final void lazySet(int i, float newValue) {
      set(i, newValue);
    }

    public final int length() {
      return bitsArray.length();
    }

    public final void set(int i, float newValue) {
      int next = floatToIntBits(newValue);
      bitsArray.set(i, next);
    }

    @Override
    public String toString() {
      int length = length();
      if (length == 0) {
        return "[]";
      } else {
        return "[".concat(IntStream.of(length).mapToObj(bitsArray::get).map(Float::intBitsToFloat)
            .map(f -> Float.toString(f)).collect(Collectors.joining(","))).concat("]");
      }
    }

    public final boolean weakCompareAndSet(int i, float expect, float update) {
      return bitsArray.weakCompareAndSetPlain(i, floatToIntBits(expect), floatToIntBits(update));
    }

    private float add(int i, float delta, boolean exp) {
      for (;;) {
        final int expectBits = bitsArray.get(i);
        final float expect = intBitsToFloat(expectBits);
        final float update = expect + delta;
        final int updateBits = floatToIntBits(update);
        if (bitsArray.compareAndSet(i, expectBits, updateBits)) {
          return exp ? expect : update;
        }
      }
    }
  }
}

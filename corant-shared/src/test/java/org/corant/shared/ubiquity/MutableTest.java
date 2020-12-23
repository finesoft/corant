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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.corant.shared.ubiquity.Mutable.MutableInteger;
import org.corant.shared.ubiquity.Mutable.MutableNumber;
import org.corant.shared.ubiquity.Mutable.MutableTemporal;
import org.corant.shared.util.Threads;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 下午11:47:34
 *
 */
public class MutableTest extends TestCase {

  @Test
  public void testMubtableNumber(MutableNumber<?> m, Number right) {
    assertEquals(m.byteValue(), right.byteValue());
    assertEquals(m.shortValue(), right.shortValue());
    assertEquals(m.intValue(), right.intValue());
    assertEquals(m.longValue(), right.longValue());
    assertEquals(m.floatValue(), right.floatValue());
    assertEquals(m.doubleValue(), right.doubleValue());
  }

  @Test
  public void testMubtableNumberCmpr() {
    List<MutableNumber<Integer>> lista = new ArrayList<>();
    List<MutableNumber<Integer>> listb = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      lista.add(MutableNumber.of(i));
      listb.add(MutableNumber.of(9 - i));
    }
    assertFalse(lista.equals(listb));
    Collections.sort(listb);
    assertTrue(lista.equals(listb));
  }

  @Test
  public void testMubtableNumberEquals() {
    assertTrue(MutableNumber.of(1).equals(MutableNumber.of(1)));
  }

  @Test
  public void testMubtableNumberLegality() {
    MutableInteger mi = new MutableInteger(0);
    assertEquals(mi.add(10).intValue(), 10);
    assertEquals(mi, new MutableInteger(10));
  }

  @Test
  public void testMutableBigDecimal() {
    BigDecimal i = BigDecimal.ZERO;
    BigDecimal s = BigDecimal.ONE;
    BigDecimal r = BigDecimal.valueOf(-1L);
    MutableNumber<BigDecimal> mi = MutableNumber.of(i);
    mi.add(s);
    assertTrue(s.compareTo(mi.get()) == 0);
    mi.set(i);
    assertTrue(s.compareTo(mi.addAndGet(1)) == 0);
    mi.set(i);
    assertTrue(i.compareTo(mi.getAndAdd(1)) == 0);
    assertTrue(s.compareTo(mi.get()) == 0);
    mi.set(i);
    mi.increment();
    assertTrue(s.compareTo(mi.get()) == 0);
    mi.set(i);
    assertTrue(s.compareTo(mi.incrementAndGet()) == 0);
    mi.set(i);
    assertTrue(i.compareTo(mi.getAndIncrement()) == 0);
    assertTrue(s.compareTo(mi.get()) == 0);
    mi.set(i);
    mi.subtract(s);
    assertTrue(r.compareTo(mi.get()) == 0);
    mi.set(i);
    assertTrue(r.compareTo(mi.subtractAndGet(1)) == 0);
    mi.set(i);
    assertTrue(i.compareTo(mi.getAndSubtract(1)) == 0);
    assertTrue(r.compareTo(mi.get()) == 0);
    mi.set(i);
    mi.decrement();
    assertTrue(r.compareTo(mi.get()) == 0);
    mi.set(i);
    assertTrue(r.compareTo(mi.decrementAndGet()) == 0);
    mi.set(i);
    assertTrue(i.compareTo(mi.getAndDecrement()) == 0);
    assertTrue(r.compareTo(mi.get()) == 0);
    testMubtableNumber(mi, r);
  }

  @Test
  public void testMutableBigInteger() {
    BigInteger i = BigInteger.ZERO;
    BigInteger s = BigInteger.ONE;
    BigInteger r = BigInteger.valueOf(-1L);
    MutableNumber<BigInteger> mi = MutableNumber.of(i);
    mi.add(s);
    assertTrue(s.compareTo(mi.get()) == 0);
    mi.set(i);
    assertTrue(s.compareTo(mi.addAndGet(1)) == 0);
    mi.set(i);
    assertTrue(i.compareTo(mi.getAndAdd(1)) == 0);
    assertTrue(s.compareTo(mi.get()) == 0);
    mi.set(i);
    mi.increment();
    assertTrue(s.compareTo(mi.get()) == 0);
    mi.set(i);
    assertTrue(s.compareTo(mi.incrementAndGet()) == 0);
    mi.set(i);
    assertTrue(i.compareTo(mi.getAndIncrement()) == 0);
    assertTrue(s.compareTo(mi.get()) == 0);
    mi.set(i);
    mi.subtract(s);
    assertTrue(r.compareTo(mi.get()) == 0);
    mi.set(i);
    assertTrue(r.compareTo(mi.subtractAndGet(1)) == 0);
    mi.set(i);
    assertTrue(i.compareTo(mi.getAndSubtract(1)) == 0);
    assertTrue(r.compareTo(mi.get()) == 0);
    mi.set(i);
    mi.decrement();
    assertTrue(r.compareTo(mi.get()) == 0);
    mi.set(i);
    assertTrue(r.compareTo(mi.decrementAndGet()) == 0);
    mi.set(i);
    assertTrue(i.compareTo(mi.getAndDecrement()) == 0);
    assertTrue(r.compareTo(mi.get()) == 0);
    testMubtableNumber(mi, r);
  }

  @Test
  public void testMutableDouble() {
    MutableNumber<Double> mi = MutableNumber.of(0.0d);
    mi.add(1.1d);
    assertEquals(Double.valueOf(1.1), mi.get());
    mi.set(0d);
    assertEquals(Double.valueOf(1.1), mi.addAndGet(1.1));
    mi.set(0d);
    assertEquals(Double.valueOf(0), mi.getAndAdd(1.1));
    assertEquals(Double.valueOf(1.1), mi.get());
    mi.set(0d);
    mi.increment();
    assertEquals(Double.valueOf(1), mi.get());
    mi.set(0d);
    assertEquals(Double.valueOf(1), mi.incrementAndGet());
    mi.set(0d);
    assertEquals(Double.valueOf(0), mi.getAndIncrement());
    assertEquals(Double.valueOf(1), mi.get());
    mi.set(0d);
    mi.subtract(1.1d);
    assertEquals(Double.valueOf(-1.1), mi.get());
    mi.set(0d);
    assertEquals(Double.valueOf(-1.1), mi.subtractAndGet(1.1));
    mi.set(0d);
    assertEquals(Double.valueOf(0), mi.getAndSubtract(1.1));
    assertEquals(Double.valueOf(-1.1), mi.get());
    mi.set(0d);
    mi.decrement();
    assertEquals(Double.valueOf(-1), mi.get());
    mi.set(0d);
    assertEquals(Double.valueOf(-1), mi.decrementAndGet());
    mi.set(0d);
    assertEquals(Double.valueOf(0), mi.getAndDecrement());
    assertEquals(Double.valueOf(-1), mi.get());
    mi.set(123.4d);
    testMubtableNumber(mi, 123.4d);
  }

  @Test
  public void testMutableFloat() {
    MutableNumber<Float> mi = MutableNumber.of(0.0f);
    mi.add(1.1f);
    assertEquals(Float.valueOf(1.1f), mi.get());
    mi.set(0f);
    assertEquals(Float.valueOf(1.1f), mi.addAndGet(1.1));
    mi.set(0f);
    assertEquals(Float.valueOf(0), mi.getAndAdd(1.1));
    assertEquals(Float.valueOf(1.1f), mi.get());
    mi.set(0f);
    mi.increment();
    assertEquals(Float.valueOf(1), mi.get());
    mi.set(0f);
    assertEquals(Float.valueOf(1), mi.incrementAndGet());
    mi.set(0f);
    assertEquals(Float.valueOf(0), mi.getAndIncrement());
    assertEquals(Float.valueOf(1), mi.get());
    mi.set(0f);
    mi.subtract(1.1f);
    assertEquals(Float.valueOf(-1.1f), mi.get());
    mi.set(0f);
    assertEquals(Float.valueOf(-1.1f), mi.subtractAndGet(1.1));
    mi.set(0f);
    assertEquals(Float.valueOf(0), mi.getAndSubtract(1.1));
    assertEquals(Float.valueOf(-1.1f), mi.get());
    mi.set(0f);
    mi.decrement();
    assertEquals(Float.valueOf(-1), mi.get());
    mi.set(0f);
    assertEquals(Float.valueOf(-1), mi.decrementAndGet());
    mi.set(0f);
    assertEquals(Float.valueOf(0), mi.getAndDecrement());
    assertEquals(Float.valueOf(-1), mi.get());
    mi.set(123.4f);
    testMubtableNumber(mi, 123.4f);
  }

  @Test
  public void testMutableInstant() {
    final TemporalAmount d = Duration.ofSeconds(20L);
    Instant now = Instant.now();
    MutableTemporal<Instant> m = new MutableTemporal<>(now);
    assertEquals(now, m.get());
    m.set(Instant.now());
    assertFalse(now.equals(m.get()));
    m.set(now);
    assertTrue(now.equals(m.getAndPlus(d)));
    assertTrue(now.plus(d).equals(m.get()));
    assertTrue(m.minusAndGet(d).equals(now));
    now = Instant.now();
    m.set(now);
    assertTrue(now.equals(m.getAndMinus(d)));
    assertTrue(now.minus(d).equals(m.get()));
    assertTrue(m.plusAndGet(d).equals(now));
    if (m.isSupported(ChronoField.YEAR)) {
      now.get(ChronoField.YEAR);
      assertEquals(m.get(ChronoField.YEAR), now.get(ChronoField.YEAR));
    }
    if (m.isSupported(ChronoUnit.SECONDS)) {
      now = Instant.now();
      m.set(now);
      assertTrue(now.equals(m.getAndPlus(20, ChronoUnit.SECONDS)));
      assertTrue(now.plus(20, ChronoUnit.SECONDS).equals(m.get()));
      assertTrue(m.minusAndGet(20, ChronoUnit.SECONDS).equals(now));
      now = Instant.now();
      m.set(now);
      assertTrue(now.equals(m.getAndMinus(20, ChronoUnit.SECONDS)));
      assertTrue(now.minus(20, ChronoUnit.SECONDS).equals(m.get()));
      assertTrue(m.plusAndGet(20, ChronoUnit.SECONDS).equals(now));
    }
  }

  @Test
  public void testMutableInteger() {
    MutableNumber<Integer> mi = MutableNumber.of(0);
    mi.add(1);
    assertEquals(Integer.valueOf(1), mi.get());
    mi.set(0);
    assertEquals(Integer.valueOf(1), mi.addAndGet(1));
    mi.set(0);
    assertEquals(Integer.valueOf(0), mi.getAndAdd(1));
    assertEquals(Integer.valueOf(1), mi.get());
    mi.set(0);
    mi.increment();
    assertEquals(Integer.valueOf(1), mi.get());
    mi.set(0);
    assertEquals(Integer.valueOf(1), mi.incrementAndGet());
    mi.set(0);
    assertEquals(Integer.valueOf(0), mi.getAndIncrement());
    assertEquals(Integer.valueOf(1), mi.get());
    mi.set(0);
    mi.subtract(1);
    assertEquals(Integer.valueOf(-1), mi.get());
    mi.set(0);
    assertEquals(Integer.valueOf(-1), mi.subtractAndGet(1));
    mi.set(0);
    assertEquals(Integer.valueOf(0), mi.getAndSubtract(1));
    assertEquals(Integer.valueOf(-1), mi.get());
    mi.set(0);
    mi.decrement();
    assertEquals(Integer.valueOf(-1), mi.get());
    mi.set(0);
    assertEquals(Integer.valueOf(-1), mi.decrementAndGet());
    mi.set(0);
    assertEquals(Integer.valueOf(0), mi.getAndDecrement());
    assertEquals(Integer.valueOf(-1), mi.get());
    testMubtableNumber(mi, -1);
  }

  @Test
  public void testMutableLong() {
    MutableNumber<Long> mi = MutableNumber.of(0L);
    mi.add(1L);
    assertEquals(Long.valueOf(1), mi.get());
    mi.set(0L);
    assertEquals(Long.valueOf(1), mi.addAndGet(1));
    mi.set(0L);
    assertEquals(Long.valueOf(0), mi.getAndAdd(1));
    assertEquals(Long.valueOf(1), mi.get());
    mi.set(0L);
    mi.increment();
    assertEquals(Long.valueOf(1), mi.get());
    mi.set(0L);
    assertEquals(Long.valueOf(1), mi.incrementAndGet());
    mi.set(0L);
    assertEquals(Long.valueOf(0), mi.getAndIncrement());
    assertEquals(Long.valueOf(1), mi.get());
    mi.set(0L);
    mi.subtract(1L);
    assertEquals(Long.valueOf(-1), mi.get());
    mi.set(0L);
    assertEquals(Long.valueOf(-1), mi.subtractAndGet(1));
    mi.set(0L);
    assertEquals(Long.valueOf(0), mi.getAndSubtract(1));
    assertEquals(Long.valueOf(-1), mi.get());
    mi.set(0L);
    mi.decrement();
    assertEquals(Long.valueOf(-1), mi.get());
    mi.set(0L);
    assertEquals(Long.valueOf(-1), mi.decrementAndGet());
    mi.set(0L);
    assertEquals(Long.valueOf(0), mi.getAndDecrement());
    assertEquals(Long.valueOf(-1), mi.get());
  }

  @Test
  public void testMutableShort() {
    short i = 0;
    short s = 1;
    short r = -1;
    MutableNumber<Short> mi = MutableNumber.of(i);
    mi.add(s);
    assertEquals(Short.valueOf(s), mi.get());
    mi.set(i);
    assertEquals(Short.valueOf(s), mi.addAndGet(1));
    mi.set(i);
    assertEquals(Short.valueOf(i), mi.getAndAdd(1));
    assertEquals(Short.valueOf(s), mi.get());
    mi.set(i);
    mi.increment();
    assertEquals(Short.valueOf(s), mi.get());
    mi.set(i);
    assertEquals(Short.valueOf(s), mi.incrementAndGet());
    mi.set(i);
    assertEquals(Short.valueOf(i), mi.getAndIncrement());
    assertEquals(Short.valueOf(s), mi.get());
    mi.set(i);
    mi.subtract(s);
    assertEquals(Short.valueOf(r), mi.get());
    mi.set(i);
    assertEquals(Short.valueOf(r), mi.subtractAndGet(1));
    mi.set(i);
    assertEquals(Short.valueOf(i), mi.getAndSubtract(1));
    assertEquals(Short.valueOf(r), mi.get());
    mi.set(i);
    mi.decrement();
    assertEquals(Short.valueOf(r), mi.get());
    mi.set(i);
    assertEquals(Short.valueOf(r), mi.decrementAndGet());
    mi.set(i);
    assertEquals(Short.valueOf(i), mi.getAndDecrement());
    assertEquals(Short.valueOf(r), mi.get());
    testMubtableNumber(mi, -1);
  }

  @Test
  public void testMutableZonedDateTime() {
    final TemporalAmount d = Duration.ofSeconds(20L);
    ZonedDateTime now = ZonedDateTime.now();
    MutableTemporal<ZonedDateTime> m = new MutableTemporal<>(now);
    assertEquals(now, m.get());
    Threads.tryThreadSleep(100L);
    m.set(ZonedDateTime.now());
    assertFalse(now.equals(m.get()));
    m.set(now);
    assertTrue(now.equals(m.getAndPlus(d)));
    assertTrue(now.plus(d).equals(m.get()));
    assertTrue(m.minusAndGet(d).equals(now));
    now = ZonedDateTime.now();
    m.set(now);
    assertTrue(now.equals(m.getAndMinus(d)));
    assertTrue(now.minus(d).equals(m.get()));
    assertTrue(m.plusAndGet(d).equals(now));
    if (m.isSupported(ChronoField.YEAR)) {
      now.get(ChronoField.YEAR);
      assertEquals(m.get(ChronoField.YEAR), now.get(ChronoField.YEAR));
    }
    if (m.isSupported(ChronoUnit.SECONDS)) {
      now = ZonedDateTime.now();
      m.set(now);
      assertTrue(now.equals(m.getAndPlus(20, ChronoUnit.SECONDS)));
      assertTrue(now.plus(20, ChronoUnit.SECONDS).equals(m.get()));
      assertTrue(m.minusAndGet(20, ChronoUnit.SECONDS).equals(now));
      now = ZonedDateTime.now();
      m.set(now);
      assertTrue(now.equals(m.getAndMinus(20, ChronoUnit.SECONDS)));
      assertTrue(now.minus(20, ChronoUnit.SECONDS).equals(m.get()));
      assertTrue(m.plusAndGet(20, ChronoUnit.SECONDS).equals(now));
      Threads.tryThreadSleep(100L);
      Temporal un = ZonedDateTime.now();
      assertEquals(m.until(un, ChronoUnit.SECONDS), now.until(un, ChronoUnit.SECONDS));
    }
  }

}

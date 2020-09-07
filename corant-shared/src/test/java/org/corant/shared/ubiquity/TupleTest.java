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

import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Sets.setOf;
import java.math.BigDecimal;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.ubiquity.Tuple.Range;
import org.corant.shared.ubiquity.Tuple.Triple;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 下午2:32:08
 *
 */
public class TupleTest extends TestCase {

  @Test
  public void testPair() {
    Pair<String, String> p1 = Pair.of("1", "2");
    Pair<String, String> p2 = Pair.of("1", "2");
    assertEquals(p1.getKey(), p2.getKey());
    assertEquals(p1.getRight(), p2.getRight());
    assertEquals(p1.getValue(), p2.getValue());
    assertEquals(p1.getLeft(), p2.getLeft());
    assertEquals(p1, p2);
    assertTrue(p2.withKey("a").equals(Pair.of("a", "2")));
    assertTrue(p2.withValue("a").equals(Pair.of("1", "a")));
    assertTrue(p2.withLeft("a").equals(Pair.of("a", "2")));
    assertTrue(p2.withRight("a").equals(Pair.of("1", "a")));
    assertTrue(Pair.empty().isEmpty());
    assertFalse(p1.isEmpty());
    assertEquals(mapOf("1", "a", "2", "b").entrySet(), setOf(Pair.of("1", "a"), Pair.of("2", "b")));
  }

  public void testRange() {
    Range<Integer> r1 = Range.of(1, 10);
    Range<Integer> r2 = Range.of(1, 9);
    assertFalse(r1.equals(r2));
    assertTrue(r1.overlap(r2));
    r2 = Range.of(1, 10);
    assertTrue(r1.equals(r2));
    assertTrue(r1.same(r2));
    Range<BigDecimal> rd1 = Range.of(new BigDecimal("1.0"), new BigDecimal("10.0"));
    Range<BigDecimal> rd2 = Range.of(new BigDecimal("1.0"), new BigDecimal("10"));
    assertFalse(rd1.equals(rd2));
    assertTrue(rd1.same(rd2));
    rd2 = Range.of(new BigDecimal("10.1"), new BigDecimal("20.0"));
    assertFalse(rd1.overlap(rd2));
  }

  @Test
  public void testTriple() {
    Triple<String, String, String> p1 = Triple.of("1", "2", "3");
    Triple<String, String, String> p2 = Triple.of("1", "2", "3");
    assertEquals(p1.getRight(), p2.getRight());
    assertEquals(p1.getMiddle(), p2.getMiddle());
    assertEquals(p1.getLeft(), p2.getLeft());
    assertEquals(p1, p2);
    assertTrue(p2.withLeft("a").equals(Triple.of("a", "2", "3")));
    assertTrue(p2.withMiddle("a").equals(Triple.of("1", "a", "3")));
    assertTrue(p2.withRight("a").equals(Triple.of("1", "2", "a")));
    assertTrue(Triple.empty().isEmpty());
    assertFalse(p1.isEmpty());
  }

}

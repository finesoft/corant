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

import static org.corant.shared.util.Bytes.toBytes;
import static org.corant.shared.util.Bytes.toChar;
import static org.corant.shared.util.Bytes.toDouble;
import static org.corant.shared.util.Bytes.toFloat;
import static org.corant.shared.util.Bytes.toInt;
import static org.corant.shared.util.Bytes.toLong;
import static org.corant.shared.util.Bytes.toShort;
import static org.corant.shared.util.Conversions.toZonedDateTime;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.corant.shared.util.Bytes.BitArray;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 上午11:13:03
 *
 */
public class BytesTest extends TestCase {

  public static void main(String... s) {
    long epoSecs = Instant.ofEpochMilli(Identifiers.TIME_EPOCH_MILLS).getEpochSecond();
    Instant now = Instant.now();
    long nowSecs = now.getEpochSecond();
    long nowMils = now.toEpochMilli();
    System.out.println(nowMils);
    System.out.println(nowSecs);
    BitArray ba = Bytes.asBitArray(64, false);
    for (int i = 0; i < 36; i++) {
      ba.setBit(63 - i, true);
    }
    for (int i = 0; i < ba.length(); i++) {
      System.out.print(ba.getBit(i) ? "1" : 0);
    }
    System.out.println();

    long maxSecs = Bytes.toLong(ba.getBytes());
    long usedSecs = maxSecs - epoSecs;
    double years = usedSecs / (60 * 60 * 24 * 365);
    System.out.println();
    System.out.println(usedSecs);
    System.out.println(years);
    System.out.println("=============================");
    ZonedDateTime zdt = toZonedDateTime("2080-11-20T15:34:32.000");
    System.out.println(zdt);

    Instant f = now.plus(365 * 32, ChronoUnit.DAYS);
    System.out.println(f.getEpochSecond());
    Bytes.toBytes(f.getEpochSecond());
    ba = Bytes.asBitArray(Bytes.toBytes(f.getEpochSecond()));
    for (int i = 0; i < ba.length(); i++) {
      System.out.print(ba.getBit(i) ? "1" : 0);
    }
    System.out.println();
    long _90 = zdt.toInstant().getEpochSecond();
    System.out.println(_90);
    _90 = _90 - epoSecs;
    System.out.println(_90 + "\t" + Integer.MAX_VALUE);
    System.out.println((int) _90);
    System.out.println(Long.toBinaryString(_90));
    // ba = Bytes.asBitArray(Bytes.toBytes(_90));
    // for (int i = 0; i < ba.getInitSize(); i++) {
    // System.out.print(ba.getBit(i) ? "1" : 0);
    // }

  }

  @Test
  public void testBitArray() {
    byte[] bytes = new byte[] {127, 0};// 01111111,00000000
    BitArray array = Bytes.asBitArray(bytes);
    int size = array.length();
    for (int i = 0; i < size; i++) {
      if (i < 7) {
        assertTrue(array.getBit(i));
      } else {
        assertFalse(array.getBit(i));
      }
    }
    array.setBit(7, true);
    assertTrue(array.getBytes()[0] + 1 == 0);

  }

  @Test
  public void testToBytes() {
    byte[] array = new byte[] {10, 32, 23, 12, 34, 23, 35, 17};
    assertTrue(Objects.areDeepEqual(toBytes(toLong(array)), array));
    assertTrue(
        Objects.areDeepEqual(toBytes(toInt(Arrays.copyOf(array, 4))), Arrays.copyOf(array, 4)));
    assertTrue(
        Objects.areDeepEqual(toBytes(toShort(Arrays.copyOf(array, 2))), Arrays.copyOf(array, 2)));
    assertTrue(
        Objects.areDeepEqual(toBytes(toChar(Arrays.copyOf(array, 2))), Arrays.copyOf(array, 2)));
    double d = 123.123d;
    assertEquals(toDouble(toBytes(d)), d);
    float f = 123.123f;
    assertEquals(toFloat(toBytes(f)), f);
  }

}

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

  @Test
  public void testBitArray() {
    byte[] bytes = new byte[] {127, 0};// 01111111,00000000
    BitArray array = Bytes.asBitArray(bytes);
    int size = array.getInitSize();
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

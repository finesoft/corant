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

import java.util.Arrays;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * Convert byte array to bit array.
 *
 * @author bingo 2015年3月23日
 */
public class BitUtils {

  private BitUtils() {
    super();
  }

  /**
   * Create BitArray from bytes array.
   *
   * @param bytes
   * @return asBitArray
   */
  public static BitArray asBitArray(byte[] bytes) {
    return new BitArray(bytes);
  }

  /**
   * Create BitArray and initialize with parameter.
   *
   * @param size the BitArray length
   * @param in the boolean that the BitArray initialized
   * @return asBitArray
   */
  public static BitArray asBitArray(int size, boolean in) {
    return new BitArray(size, in);
  }

  public static class BitArray {

    private byte[] array = new byte[0];

    private int size = 0;

    public BitArray(byte[] bytes) {
      if (bytes != null) {
        int len = bytes.length;
        array = Arrays.copyOf(bytes, len);
        size = len << 3;
      }
    }

    public BitArray(int size, boolean in) {
      if (size < 1) {
        throw new CorantRuntimeException("The size must be greater then 0 zero!");
      }
      array = new byte[(size >> 3) + ((size & 7) == 0 ? 0 : 1)];
      for (int i = 0; i < size; i++) {
        setBit(i, in);
      }
      this.size = size;
    }

    /**
     * Obtain bit value
     *
     * @param pos
     * @return getBit
     */
    public boolean getBit(int pos) {
      return (array[pos >> 3] & 1 << (pos & 7)) != 0;
    }

    public byte[] getBytes() {
      return Arrays.copyOf(array, array.length);
    }

    public int getInitSize() {
      return size;
    }

    /**
     * Set bit value into array
     *
     * @param pos
     * @param b setBit
     */
    public void setBit(int pos, boolean b) {
      byte b8 = array[pos >> 3];
      byte posBit = (byte) (1 << (pos & 7));
      if (b) {
        b8 |= posBit;
      } else {
        b8 &= 255 - posBit;
      }
      array[pos >> 3] = b8;
    }
  }
}

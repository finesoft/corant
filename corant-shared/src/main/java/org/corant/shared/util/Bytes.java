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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 * <p>
 * Bit byte processing tool class, used for conversion between several primitive types and byte
 * arrays, some method equivalent to {@link ByteBuffer} OR {@link BitSet}.
 * </p>
 *
 * @author bingo 2015年3月23日
 *
 */
public class Bytes {

  public static final byte[] EMPTY_ARRAY = {};

  private Bytes() {}

  /**
   * Create BitArray from bytes array.
   *
   * @param bytes the bytes array that used to create BitArray
   * @return A bit array constructed from the given bytes array
   */
  public static BitArray asBitArray(byte[] bytes) {
    return new BitArray(bytes);
  }

  /**
   * Create BitArray and initialize with the given size and the given default boolean value.
   *
   * @param size the BitArray length
   * @param in the boolean value that the BitArray initialized
   * @return a BitArray
   */
  public static BitArray asBitArray(int size, boolean in) {
    return new BitArray(size, in);
  }

  /**
   * Returns a big-endian representation of {@code value} in a 2-element byte array.
   *
   * @param value the char value
   * @return a big-endian representation of the given char value in a 2-element byte array.
   */
  public static byte[] toBytes(char value) {
    return new byte[] {(byte) (value >> 8), (byte) value};
  }

  /**
   * Returns a big-endian representation of {@code value} in an 8-element byte array.
   *
   * @param value the double value
   * @return a big-endian representation of the given double value in an 8-element byte array.
   */
  public static byte[] toBytes(double value) {
    long data = Double.doubleToRawLongBits(value);
    return new byte[] {(byte) (data >> 56 & 0xFF), (byte) (data >> 48 & 0xFF),
        (byte) (data >> 40 & 0xFF), (byte) (data >> 32 & 0xFF), (byte) (data >> 24 & 0xFF),
        (byte) (data >> 16 & 0xFF), (byte) (data >> 8 & 0xFF), (byte) (data & 0xFF)};
  }

  /**
   * Returns a big-endian representation of {@code value} in a 4-element byte array.
   *
   * @param value the float value
   * @return a big-endian representation of the given float value in a 4-element byte array.
   */
  public static byte[] toBytes(float value) {
    int data = Float.floatToIntBits(value);
    return new byte[] {(byte) (data >> 24 & 0xFF), (byte) (data >> 16 & 0xFF),
        (byte) (data >> 8 & 0xFF), (byte) (data & 0xFF)};
  }

  /**
   * Returns a big-endian representation of {@code value} in a 4-element byte array.
   *
   * @param value the integer value
   * @return a big-endian representation of the given integer value in a 4-element byte array.
   */
  public static byte[] toBytes(int value) {
    return new byte[] {(byte) (value >> 24), (byte) (value >> 16), (byte) (value >> 8),
        (byte) value};
  }

  /**
   * Returns a big-endian representation of {@code value} in an 8-element byte array.
   *
   * @param value the long integer value
   * @return a big-endian representation of the given long integer in an 8-element byte array.
   */
  public static byte[] toBytes(long value) {
    byte[] result = new byte[8];
    for (int i = 7; i >= 0; i--) {
      result[i] = (byte) (value & 0xFFL);
      value >>= 8;
    }
    return result;
  }

  /**
   * Returns a big-endian representation of {@code value} in a 2-element byte array.
   *
   * @param value the short integer value
   * @return a big-endian representation of the given short integer in a 2-element byte array.
   */
  public static byte[] toBytes(short value) {
    return new byte[] {(byte) (value >> 8), (byte) value};
  }

  /**
   * Returns the char value whose big-endian representation is stored in the first 2 bytes of byte
   * array.
   *
   * @param bytes the given bytes that make up Character
   * @return toChar
   */
  public static Character toChar(byte[] bytes) {
    return toChar(bytes, false);
  }

  /**
   * Returns the char value whose big-endian representation is stored in 2 bytes of byte array.
   *
   * @param bytes the given bytes that make up Character
   * @param strict whether to strictly follow the char bytes length constraint
   * @return toChar
   */
  public static Character toChar(byte[] bytes, boolean strict) {
    if (strict) {
      shouldBeTrue(bytes.length == Character.BYTES,
          "The bytes array length illegality: %s != %s for Char.", bytes.length, Character.BYTES);
    } else {
      shouldBeTrue(bytes.length >= Character.BYTES, "The bytes array too small: %s < %s for Char.",
          bytes.length, Character.BYTES);
    }
    return (char) (bytes[0] << 8 | bytes[1] & 0xFF);
  }

  /**
   * Returns the double value whose big-endian representation is stored in the first 8 bytes of byte
   * array.
   *
   * @param bytes the given bytes that make up double
   * @return toDouble
   */
  public static double toDouble(byte[] bytes) {
    return toDouble(bytes, false);
  }

  /**
   * Returns the double value whose big-endian representation is stored in 8 bytes of byte array.
   *
   * @param bytes the given bytes that make up double
   * @param strict whether to strictly follow the double bytes length constraint
   * @return toDouble
   */
  public static double toDouble(byte[] bytes, boolean strict) {
    if (strict) {
      shouldBeTrue(bytes.length == Double.BYTES,
          "The bytes array length illegality: %s != %s for Double.", bytes.length, Double.BYTES);
    } else {
      shouldBeTrue(bytes.length >= Double.BYTES, "The bytes array too small: %s < %s for Double.",
          bytes.length, Double.BYTES);
    }
    return Double.longBitsToDouble(toLong(bytes));
  }

  /**
   * Returns the float value whose big-endian representation is stored in the first 4 bytes of byte
   * array.
   *
   * @param bytes the given bytes that make up float
   * @return toFloat
   */
  public static float toFloat(byte[] bytes) {
    return toFloat(bytes, false);
  }

  /**
   * Returns the float value whose big-endian representation is stored in 4 bytes of byte array.
   *
   * @param bytes the given bytes that make up float
   * @param strict whether to strictly follow the float bytes length constraint
   * @return toFloat
   */
  public static float toFloat(byte[] bytes, boolean strict) {
    if (strict) {
      shouldBeTrue(bytes.length == Float.BYTES,
          "The bytes array length illegality: %s != %s for Float.", bytes.length, Float.BYTES);
    } else {
      shouldBeTrue(bytes.length >= Float.BYTES, "The bytes array too small: %s < %s for Float.",
          bytes.length, Float.BYTES);
    }
    return Float.intBitsToFloat(
        bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | bytes[3] & 0xFF);
  }

  /**
   * Returns the int value whose big-endian representation is stored in the first 4 bytes of byte
   * array.
   *
   * @param bytes the given bytes that make up int
   * @return toInt
   */
  public static int toInt(byte[] bytes) {
    return toInt(bytes, false);
  }

  /**
   * Returns the int value whose big-endian representation is stored in 4 bytes of byte array.
   *
   * @param bytes the given bytes that make up int
   * @param strict whether to strictly follow the int bytes length constraint
   * @return toInt
   */
  public static int toInt(byte[] bytes, boolean strict) {
    if (strict) {
      shouldBeTrue(bytes.length == Integer.BYTES,
          "The bytes array length illegality: %s != %s for Int.", bytes.length, Integer.BYTES);
    } else {
      shouldBeTrue(bytes.length >= Integer.BYTES, "The bytes array too small: %s < %s for Int.",
          bytes.length, Integer.BYTES);
    }
    return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | bytes[3] & 0xFF;
  }

  /**
   * Returns the long value whose big-endian representation is stored in the first 8 bytes of byte
   * array.
   *
   * @param bytes the given bytes that make up long
   * @return toLong
   */
  public static long toLong(byte[] bytes) {
    return toLong(bytes, false);
  }

  /**
   * Returns the long value whose big-endian representation is stored in 8 bytes of byte array.
   *
   * @param bytes the given bytes that make up long
   * @param strict whether to strictly follow the long bytes length constraint
   * @return toLong
   */
  public static long toLong(byte[] bytes, boolean strict) {
    if (strict) {
      shouldBeTrue(bytes.length == Long.BYTES,
          "The bytes array length illegality: %s != %s for Long.", bytes.length, Long.BYTES);
    } else {
      shouldBeTrue(bytes.length >= Long.BYTES, "The bytes array too small: %s < %s for Long.",
          bytes.length, Long.BYTES);
    }
    return (bytes[0] & 0xFFL) << 56 | (bytes[1] & 0xFFL) << 48 | (bytes[2] & 0xFFL) << 40
        | (bytes[3] & 0xFFL) << 32 | (bytes[4] & 0xFFL) << 24 | (bytes[5] & 0xFFL) << 16
        | (bytes[6] & 0xFFL) << 8 | bytes[7] & 0xFFL;
  }

  /**
   * Returns the short value whose big-endian representation is stored in the first 2 bytes of byte
   * array.
   *
   * @param bytes the given bytes that make up short
   * @return toShort
   */
  public static short toShort(byte[] bytes) {
    return toShort(bytes, false);
  }

  /**
   * Returns the short value whose big-endian representation is stored in 2 bytes of byte array.
   *
   * @param bytes the given bytes that make up short
   * @param strict whether to strictly follow the short bytes length constraint
   * @return toShort
   */
  public static short toShort(byte[] bytes, boolean strict) {
    if (strict) {
      shouldBeTrue(bytes.length == Short.BYTES,
          "The bytes array length illegality: %s != %s for Short.", bytes.length, Short.BYTES);
    } else {
      shouldBeTrue(bytes.length >= Short.BYTES, "The bytes array too small: %s < %s for Short.",
          bytes.length, Short.BYTES);
    }
    return (short) (bytes[0] << 8 | bytes[1] & 0xFF);
  }

  /**
   * corant-shared
   * <p>
   * A simple bits array, no thread-safe.
   *
   * @author bingo 上午10:35:39
   *
   */
  public static class BitArray implements Cloneable {

    private byte[] array = EMPTY_ARRAY;

    private int length = 0;

    /**
     * Create a new bit array containing all the bits in the given byte array.
     *
     * @param bytes a byte array containing a little-endian representation of a sequence of bits to
     *        be used as the initial bits of the new bit array
     */
    public BitArray(byte[] bytes) {
      if (bytes != null) {
        int len = bytes.length;
        length = len << 3;
        array = Arrays.copyOf(bytes, len);
      }
    }

    /**
     * Creates a new bit array with the given bits length which containing all the bits in the given
     * byte array.
     *
     * @param array the bytes array to be initialized
     * @param length the bits length
     */
    public BitArray(byte[] array, int length) {
      int arrayBitLen = array.length << 3;
      if (length > arrayBitLen) {
        throw new IndexOutOfBoundsException("length can't > " + arrayBitLen);
      }
      this.array = Arrays.copyOf(array, array.length);
      this.length = length;
    }

    /**
     * Creates a bit array whose initial size is {@code length} represent bits with indices in the
     * range {@code 0} through {@code length-1}. All bits are initially {@code in}.
     *
     * @param length the initial length of the bits in array
     * @param in the initial value
     */
    public BitArray(int length, boolean in) {
      if (length < 1) {
        throw new CorantRuntimeException("The size must be greater then 0 zero!");
      }
      this.length = length;
      array = new byte[(length >> 3) + ((length & 7) == 0 ? 0 : 1)];
      for (int i = 0; i < length; i++) {
        setBit(i, in);
      }
    }

    /**
     * Returns the underlying bytes array length
     */
    public int byteLength() {
      return array.length;
    }

    @Override
    public BitArray clone() throws CloneNotSupportedException {
      return new BitArray(array, length);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof BitArray)) {
        return false;
      }
      BitArray other = (BitArray) obj;
      if (!Arrays.equals(array, other.array)) {
        return false;
      }
      if (length != other.length) {
        return false;
      }
      return true;
    }

    /**
     * Returns the value of the bit with the specified index. The value is {@code true} if the bit
     * with the index {@code pos} is currently set in this {@code BitArray}; otherwise, the result
     * is {@code false}.
     *
     * @param pos the position
     * @return the value of the bit with the specified position
     * @throws IndexOutOfBoundsException if the specified pos is negative
     */
    public boolean getBit(int pos) {
      if (pos < 0) {
        throw new IndexOutOfBoundsException("pos < 0: " + pos);
      }
      return (array[pos >> 3] & 1 << (pos & 7)) != 0;
    }

    /**
     * Returns a new {@code BitArray} composed of bits from this {@code BitArray} from
     * {@code fromIndex} (inclusive) to {@code toIndex} (exclusive).
     *
     * @param fromIndex index of the first bit to include
     * @param toIndex index after the last bit to include
     * @return a new {@code BitArray} from a range of this {@code BitArray}
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, or {@code toIndex} is
     *         negative, or {@code fromIndex} is larger than {@code toIndex}, or {@code toIndex} is
     *         larger than the length of this {@code BitArray}
     */
    public BitArray getBits(int fromIndex, int toIndex) {
      if (fromIndex < 0) {
        throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
      }
      if (toIndex < 0) {
        throw new IndexOutOfBoundsException("toIndex < 0: " + toIndex);
      }
      if (fromIndex > toIndex) {
        throw new IndexOutOfBoundsException("fromIndex: " + fromIndex + " > toIndex: " + toIndex);
      }
      if (toIndex > length) {
        throw new IndexOutOfBoundsException("toIndex can't > " + length);
      }
      BitArray ba = new BitArray(toIndex - fromIndex, false);
      for (int i = 0; i < ba.length; i++) {
        ba.setBit(i, getBit(i + fromIndex));
      }
      return ba;
    }

    /**
     * Returns the underlying bytes array
     */
    public byte[] getBytes() {
      return Arrays.copyOf(array, array.length);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(array);
      return prime * result + length;
    }

    /**
     * Returns the bits length
     */
    public int length() {
      return length;
    }

    /**
     * Set bit value into array
     *
     * @param pos the position
     * @param b setBit the bit to set
     */
    public void setBit(int pos, boolean b) {
      if (pos >= length) {
        throw new IndexOutOfBoundsException("pos can't >= " + length);
      }
      byte b8 = array[pos >> 3];
      byte posBit = (byte) (1 << (pos & 7));
      if (b) {
        b8 |= posBit;
      } else {
        b8 &= 255 - posBit;
      }
      array[pos >> 3] = b8;
    }

    /**
     * Re-initial the underlying bytes array
     *
     * @param data the bytes array to be initialized
     */
    public void setBytes(byte[] data) {
      setBytes(data, (data.length << 3));
    }

    /**
     * Re-initial the underlying bytes array and given bits length
     *
     * @param data the bytes array to be initialized
     * @param length the bits length
     */
    public void setBytes(byte[] data, int length) {
      int dataBitLen = data.length << 3;
      if (length > dataBitLen) {
        throw new IndexOutOfBoundsException("length can't > " + dataBitLen);
      }
      this.length = length;
      array = new byte[data.length];
      System.arraycopy(data, 0, array, 0, data.length);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder((length << 1) + 2);
      sb.append('[');
      String[] bits = new String[length];
      for (int i = 0; i < length; i++) {
        bits[i] = getBit(i) ? "1" : "0";
      }
      sb.append(String.join(",", bits));
      sb.append(']');
      return sb.toString();
    }

  }
}

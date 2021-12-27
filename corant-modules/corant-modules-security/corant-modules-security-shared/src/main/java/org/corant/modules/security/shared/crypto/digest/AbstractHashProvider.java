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
package org.corant.modules.security.shared.crypto.digest;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNoneNull;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Objects.areDeepEqual;
import static org.corant.shared.util.Objects.max;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Bytes;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:24:08
 *
 */
public abstract class AbstractHashProvider implements HashProvider {

  public static final int DEFAULT_ITERATIONS = 1024;
  public static final int DEFAULT_SALT_SIZE = 128;

  protected static final SecureRandom secureRandom = new SecureRandom();
  protected final String algorithm;
  protected final int iterations;
  protected final int saltBitSize;
  protected final int saltByteSize;

  /**
   * Specify the hash algorithm name and the number of hash iterations times to create an instance.
   *
   * @param algorithm the hash algorithm name, can't empty
   * @param iterations the iterations times, the minimum value is 1024
   */
  protected AbstractHashProvider(String algorithm, int iterations) {
    this(algorithm, iterations, 0);
  }

  /**
   * Specify the hash algorithm name and the number of hash iterations times and salt bits size to
   * create an instance.
   *
   * @param algorithm the hash algorithm name, can't empty
   * @param iterations the iterations times, the minimum value is 1024
   * @param saltBitSize the salt bits size, the minimum value is 128
   */
  protected AbstractHashProvider(String algorithm, int iterations, int saltBitSize) {
    this.algorithm = shouldNotBlank(algorithm);
    this.iterations = max(DEFAULT_ITERATIONS, iterations);
    this.saltBitSize = max(DEFAULT_SALT_SIZE, saltBitSize);
    shouldBeTrue(this.saltBitSize % Byte.SIZE == 0,
        "The salt bits size error must be divisible by 8.");
    saltByteSize = this.saltBitSize / Byte.SIZE;
  }

  /**
   * Parse Base64 string to HashInfo
   *
   * @param b64 the bytes
   * @return fromMergedB64
   * @see #toMergedB64(String, int, byte[], byte[])
   */
  protected static HashInfo fromMergedB64(String b64) {
    byte[] bytes = Base64.getDecoder().decode(b64);
    HashInfo info = new HashInfo();
    int next = 0;
    int algoSize = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    info.iterations = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    info.saltSize = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4)) << 3;
    int digestSize = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    info.algorithm = new String(Arrays.copyOfRange(bytes, next, next += algoSize));
    info.salt = Arrays.copyOfRange(bytes, next, next += info.saltSize >>> 3);
    info.digested = Arrays.copyOfRange(bytes, next, next + digestSize);
    return info;
  }

  protected static MessageDigest getDigest(String algorithm, Object provider) {
    try {
      if (provider instanceof Provider) {
        return MessageDigest.getInstance(algorithm, (Provider) provider);
      } else if (provider instanceof String) {
        return MessageDigest.getInstance(algorithm, (String) provider);
      } else {
        return MessageDigest.getInstance(algorithm);
      }
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected static SecretKeyFactory getSecretKeyFactory(String algorithm, Object provider) {
    try {
      if (provider instanceof Provider) {
        return SecretKeyFactory.getInstance(algorithm, (Provider) provider);
      } else if (provider instanceof String) {
        return SecretKeyFactory.getInstance(algorithm, (String) provider);
      } else {
        return SecretKeyFactory.getInstance(algorithm);
      }
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new CorantRuntimeException(e, "The %s algorithm not found", algorithm);
    }
  }

  /**
   * Compares two byte arrays in length-constant time. This comparison method is used so that
   * password hashes cannot be extracted from on-line systems using a timing attack and then
   * attacked off-line.
   *
   * @param a the first {@code byte[]} to compare
   * @param b the second {@code byte[]} to compare
   */
  protected static boolean slowEquals(byte[] a, byte[] b) {
    int diff = a.length ^ b.length;
    for (int i = 0; i < a.length && i < b.length; i++) {
      diff |= a[i] ^ b[i];
    }
    return diff == 0;
  }

  /**
   * Merge algorithm and iterations and salt and digested to Base64 String
   *
   * @param algorithm the algorithm name
   * @param iterations the hash iterations
   * @param salt the salt data
   * @param digested the hashed data
   * @return toMergedB64
   */
  protected static String toMergedB64(String algorithm, int iterations, byte[] salt,
      byte[] digested) {
    byte[] algoNameBytes = algorithm.getBytes();
    byte[] bytes = new byte[(4 << 2) + algoNameBytes.length + salt.length + digested.length];
    // header length info
    int next = 0;
    System.arraycopy(Bytes.toBytes(algoNameBytes.length), 0, bytes, next, 4);
    System.arraycopy(Bytes.toBytes(iterations), 0, bytes, next += 4, 4);
    System.arraycopy(Bytes.toBytes(salt.length), 0, bytes, next += 4, 4);
    System.arraycopy(Bytes.toBytes(digested.length), 0, bytes, next += 4, 4);
    // body content info
    System.arraycopy(algoNameBytes, 0, bytes, next += 4, algoNameBytes.length);
    System.arraycopy(salt, 0, bytes, next += algoNameBytes.length, salt.length);
    System.arraycopy(digested, 0, bytes, next + salt.length, digested.length);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @Override
  public Object encode(Object data) {
    byte[] salt = getSalt();
    byte[] digested = encode(data, algorithm, iterations, salt);
    return toMergedB64(algorithm, iterations, salt, digested);
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public int getIterations() {
    return iterations;
  }

  @Override
  public String getName() {
    return algorithm;
  }

  public int getSaltBitSize() {
    return saltBitSize;
  }

  @Override
  public boolean validate(Object input, Object criterion) {
    shouldNoneNull(input, criterion);
    HashInfo criterionHash = fromMergedB64(criterion.toString());
    if (algorithm.equalsIgnoreCase(criterionHash.algorithm)
        && criterionHash.iterations == iterations && criterionHash.saltSize == saltBitSize) {
      return compare(encode(input, algorithm, iterations, criterionHash.salt),
          criterionHash.digested);
    }
    return false;
  }

  /**
   * Check whether the byte arrays are the same. Subclasses can modify this method to use algorithms
   * (such {@link #slowEquals(byte[], byte[])}) that slow down the CPU operation speed and increase
   * the difficulty of brute force cracking, default use
   * {@link java.util.Objects#deepEquals(Object, Object)}
   *
   * @param encoded the encoded input
   * @param criterion the criterion
   *
   * @see #slowEquals(byte[], byte[])
   */
  protected boolean compare(byte[] encoded, byte[] criterion) {
    return areDeepEqual(encoded, criterion);
  }

  /**
   * Encode the given input string to hash digested bytes.
   *
   * @param input the input string that will be encoded
   * @param algorithm the hash algorithm name, can't empty
   * @param iterations the iterations times
   * @param salt the salt bytes
   * @return encode
   */
  protected byte[] encode(Object input, String algorithm, int iterations, byte[] salt) {
    MessageDigest digest = getDigest(algorithm, getProvider());
    if (salt.length != 0) {
      digest.reset();
      digest.update(salt);
    }
    byte[] hashed = digest.digest((byte[]) input);
    int itr = iterations - 1;
    for (int i = 0; i < itr; i++) {
      digest.reset();
      hashed = digest.digest(hashed);
    }
    return hashed;
  }

  protected Object getProvider() {
    return null;
  }

  protected byte[] getSalt() {
    if (saltByteSize > 0) {
      byte[] buffer = new byte[saltByteSize];
      secureRandom.nextBytes(buffer);
      return buffer;
    } else {
      return Bytes.EMPTY_ARRAY;
    }
  }

  protected static class HashInfo {
    protected String algorithm;
    protected int iterations;
    protected int saltSize;
    protected byte[] salt;
    protected byte[] digested;

    @Override
    public String toString() {
      return "HashInfo [algorithm=" + algorithm + ", iterations=" + iterations + ", saltSize="
          + saltSize + "]";
    }
  }
}

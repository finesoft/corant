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
import static org.corant.shared.util.Objects.max;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.corant.shared.util.Bytes;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:24:08
 */
public abstract class AbstractHashProvider implements DigestProvider {

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
    this.algorithm = shouldNotBlank(algorithm, "The algorithm can't empty!");
    this.iterations = max(DEFAULT_ITERATIONS, iterations);
    this.saltBitSize = max(DEFAULT_SALT_SIZE, saltBitSize);
    shouldBeTrue(this.saltBitSize % Byte.SIZE == 0,
        "The salt bits size error must be divisible by 8.");
    saltByteSize = this.saltBitSize / Byte.SIZE;
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

  @Override
  public Object encode(Object data) {
    byte[] salt = getSalt();
    byte[] digested = encode(data, algorithm, iterations, salt, getProvider());
    return toCiphertext(algorithm, iterations, salt, digested);
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
    HashInfo criterionHash = fromCiphertext(criterion.toString());
    if (algorithm.equalsIgnoreCase(criterionHash.getAlgorithm())
        && criterionHash.getIterations() == iterations
        && criterionHash.getSaltSize() == saltBitSize) {
      return compare(encode(input, algorithm, iterations, criterionHash.getSalt(), getProvider()),
          criterionHash.getDigested());
    }
    return false;
  }

  /**
   * Check whether the byte arrays are the same. Subclasses can modify this method to use algorithms
   * (such {@link #slowEquals(byte[], byte[])}) that slow down the CPU operation speed and increase
   * the difficulty of brute force cracking, default use
   * {@link MessageDigest#isEqual(byte[], byte[])}
   *
   * @param encoded the encoded input
   * @param criterion the criterion
   *
   * @see #slowEquals(byte[], byte[])
   */
  protected boolean compare(byte[] encoded, byte[] criterion) {
    return MessageDigest.isEqual(encoded, criterion);
  }

  /**
   * Encode the given input string to hash digested bytes.
   *
   * @param input the input string that will be encoded
   * @param algorithm the hash algorithm name, can't empty
   * @param iterations the iterations times
   * @param salt the salt bytes
   * @param provider the instance of {@link java.security.Provider} or the name of
   *        {@link java.security.Provider} or use system default provider if null.
   * @return encode
   */
  protected byte[] encode(Object input, String algorithm, int iterations, byte[] salt,
      Object provider) {
    MessageDigest digest = DigestProvider.getDigest(algorithm, provider);
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

  /**
   * Parse the given ciphertext to HashInfo
   *
   * @param text the ciphertext, default is base 64 string.
   * @return a Hash info object containing the data related to the hash operation.
   * @see #toCiphertext(String, int, byte[], byte[])
   */
  protected HashInfo fromCiphertext(String text) {
    byte[] bytes = Base64.getDecoder().decode(text);
    return HashInfo.fromDefaultLayoutBytes(bytes);
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

  /**
   * Merge algorithm and iterations and salt and digested to String, default use base 64.
   *
   * @param algorithm the algorithm name
   * @param iterations the hash iterations
   * @param salt the salt data
   * @param digested the hashed data
   * @return the ciphertext
   */
  protected String toCiphertext(String algorithm, int iterations, byte[] salt, byte[] digested) {
    byte[] bytes =
        HashInfo.toDefaultLayoutBytes(new HashInfo(algorithm, iterations, salt, digested));
    return Base64.getEncoder().encodeToString(bytes);
  }
}

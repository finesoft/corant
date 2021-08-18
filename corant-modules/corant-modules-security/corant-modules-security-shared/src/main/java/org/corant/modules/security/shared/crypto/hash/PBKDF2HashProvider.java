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
package org.corant.modules.security.shared.crypto.hash;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.areDeepEqual;
import static org.corant.shared.util.Objects.max;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.bouncycastle.util.Arrays;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Bytes;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:24:08
 *
 */
public abstract class PBKDF2HashProvider implements HashProvider {

  public static final int DEFAULT_DERIVED_KEY_SIZE = 512;

  protected static final SecureRandom secureRandom = new SecureRandom();
  protected final String algorithm;
  protected final int iterations;
  protected final int derivedKeySize;
  protected final int saltSize;

  protected PBKDF2HashProvider(String algorithm, int iterations) {
    this(algorithm, iterations, 0, 0);
  }

  protected PBKDF2HashProvider(String algorithm, int iterations, int derivedKeySize,
      int defaultSaltSize) {
    this.algorithm = shouldNotBlank(algorithm);
    this.iterations = max(1024, iterations);
    this.derivedKeySize = max(DEFAULT_DERIVED_KEY_SIZE, derivedKeySize);
    saltSize = max(16, defaultSaltSize);
    shouldBeTrue(derivedKeySize % 8 == 0 && defaultSaltSize % 8 == 0);
    shouldNotNull(getSecretKeyFactory(algorithm));// for checking
  }

  protected static byte[] encode(String algorithm, String input, int iterations, byte[] salt,
      int derivedKeySize) {
    KeySpec spec = new PBEKeySpec(input.toCharArray(), salt, iterations, derivedKeySize);
    try {
      byte[] key = getSecretKeyFactory(algorithm).generateSecret(spec).getEncoded();
      return key;
    } catch (InvalidKeySpecException e) {
      throw new CorantRuntimeException(e, "Input could not be encoded");
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Parse Base64 string to HashInfo
   *
   * @param b64
   * @return fromMergedB64
   */
  protected static HashInfo fromMergedB64(String b64) {
    byte[] bytes = Base64.getDecoder().decode(b64);
    HashInfo info = new HashInfo();
    int next = 0;
    int algoSize = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    info.iterations = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    info.saltSize = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    int digestSize = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    info.algorithm = new String(Arrays.copyOfRange(bytes, next, next += algoSize));
    info.salt = Arrays.copyOfRange(bytes, next, next += info.saltSize);
    info.digested = Arrays.copyOfRange(bytes, next, next += digestSize);
    info.derivedKeySize = digestSize * 8;
    return info;
  }

  protected static SecretKeyFactory getSecretKeyFactory(String algorithm) {
    try {
      return SecretKeyFactory.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new CorantRuntimeException(e, "The %s algorithm not found", algorithm);
    }
  }

  /**
   * Merge iterations & bytes size of salt & bytes size of digested & salt bytes & digested bytes to
   * Base64 String
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
    byte[] bytes = new byte[4 * 4 + algoNameBytes.length + salt.length + digested.length];
    // header length info
    System.arraycopy(Bytes.toBytes(algoNameBytes.length), 0, bytes, 0, 4);
    System.arraycopy(Bytes.toBytes(iterations), 0, bytes, 4, 4);
    System.arraycopy(Bytes.toBytes(salt.length), 0, bytes, 8, 4);
    System.arraycopy(Bytes.toBytes(digested.length), 0, bytes, 12, 4);
    System.arraycopy(algoNameBytes, 0, bytes, 16, algoNameBytes.length);
    System.arraycopy(salt, 0, bytes, 16 + algoNameBytes.length, salt.length);
    System.arraycopy(digested, 0, bytes, 16 + algoNameBytes.length + salt.length, digested.length);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @Override
  public Object encode(Object data) {
    byte[] salt = getSalt();
    byte[] digested = encode(algorithm, data.toString(), iterations, salt, derivedKeySize);
    return toMergedB64(algorithm, iterations, salt, digested);
  }

  @Override
  public String getName() {
    return algorithm;
  }

  @Override
  public boolean validate(Object input, Object criterion) {
    HashInfo criterionHash = fromMergedB64(criterion.toString());
    if (algorithm.equalsIgnoreCase(criterionHash.algorithm)
        && criterionHash.derivedKeySize == derivedKeySize && criterionHash.iterations == iterations
        && criterionHash.saltSize == saltSize) {
      return areDeepEqual(
          encode(algorithm, input.toString(), iterations, criterionHash.salt, derivedKeySize),
          criterionHash.digested);
    }
    return false;
  }

  protected byte[] getSalt() {
    byte[] buffer = new byte[saltSize];
    secureRandom.nextBytes(buffer);
    return buffer;
  }

  protected static class HashInfo {
    protected String algorithm;
    protected int iterations;
    protected int derivedKeySize;
    protected int saltSize;
    protected byte[] salt;
    protected byte[] digested;

    @Override
    public String toString() {
      return "HashInfo [algorithm=" + algorithm + ", iterations=" + iterations + ", derivedKeySize="
          + derivedKeySize + ", saltSize=" + saltSize + "]";
    }

  }

}

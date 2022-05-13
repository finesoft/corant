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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import javax.crypto.SecretKeyFactory;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:31:04
 *
 */
public interface HashProvider {

  /**
   * Encode the given input string to hash digested bytes.
   *
   * @param input the input string that will be encoded
   * @param algorithm the hash algorithm name, can't empty
   * @param iterations the iterations times
   * @param salt the salt bytes
   * @param the instance of {@link java.security.Provider} or the name of
   *        {@link java.security.Provider} or use system default provider if null.
   * @return encode
   */
  static byte[] encode(Object input, String algorithm, int iterations, byte[] salt,
      Object provider) {
    MessageDigest digest = getDigest(algorithm, provider);
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
   * Returns a message digest with algorithm and provider.
   *
   * @param algorithm the hash algorithm name, can't empty
   * @param provider the instance of {@link java.security.Provider} or the name of
   *        {@link java.security.Provider} or use system default provider if null.
   * @return the message digest
   */
  static MessageDigest getDigest(String algorithm, Object provider) {
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

  /**
   * Returns a secret-key factory with algorithm and provider.
   *
   * @param algorithm the standard name of the requested secret-key algorithm
   * @param provider the instance of {@link java.security.Provider} or the name of
   *        {@link java.security.Provider} or use system default provider if null.
   * @return getSecretKeyFactory
   */
  static SecretKeyFactory getSecretKeyFactory(String algorithm, Object provider) {
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
   * Encode the given input data to hash digested bytes or hash digested bytes serialization data
   *
   * @param data the data to compute hash digested.
   * @return hash digested bytes or hash digested bytes serialization data
   */
  default Object encode(Object data) {
    return data;
  }

  /**
   * The hash algorithm name
   *
   * @return getName
   */
  String getName();

  /**
   * Return whether the hash digest of the given input data conforms to the hash digest of the
   * criterion
   *
   * @param input the given input data to compute hash digested.
   * @param criterion the hash digest of the criterion
   * @return true if matched, false otherwise.
   */
  boolean validate(Object input, Object criterion);

}

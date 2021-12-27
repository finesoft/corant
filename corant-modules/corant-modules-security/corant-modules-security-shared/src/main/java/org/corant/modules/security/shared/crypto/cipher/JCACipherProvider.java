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
package org.corant.modules.security.shared.crypto.cipher;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:52:35
 *
 */
public abstract class JCACipherProvider implements CipherProvider {

  static final int DEFAULT_KEY_BIT_SIZE = 128;
  static final String RANDOM_NUM_GENERATOR_ALGORITHM_NAME = "SHA1PRNG";

  protected final String algorithm;
  protected final int streamingBufferSize;
  protected final SecureRandom secureRandom;

  protected JCACipherProvider(String algorithm) {
    this(algorithm, DEFAULT_STREAMING_BUFFER_SIZE);
  }

  protected JCACipherProvider(String algorithm, int streamingBufferSize) {
    this(algorithm, streamingBufferSize, null);
  }

  protected JCACipherProvider(String algorithm, int streamingBufferSize,
      SecureRandom secureRandom) {
    this.algorithm = shouldNotBlank(algorithm, "algorithm can't be null or empty");
    this.streamingBufferSize =
        streamingBufferSize <= 0 ? DEFAULT_STREAMING_BUFFER_SIZE : streamingBufferSize;
    this.secureRandom = secureRandom;
  }

  public static Cipher createCipher(Object provider, String transformation, int mode, Key key,
      AlgorithmParameterSpec algoParamSpec, SecureRandom secureRandom) {
    try {
      final Cipher cipher;
      if (provider instanceof Provider) {
        cipher = Cipher.getInstance(transformation, (Provider) provider);
      } else if (provider instanceof String) {
        cipher = Cipher.getInstance(transformation, (String) provider);
      } else {
        cipher = Cipher.getInstance(transformation);
      }
      if (secureRandom != null) {
        if (algoParamSpec != null) {
          cipher.init(mode, key, algoParamSpec, secureRandom);
        } else {
          cipher.init(mode, key, secureRandom);
        }
      } else if (algoParamSpec != null) {
        cipher.init(mode, key, algoParamSpec);
      } else {
        cipher.init(mode, key);
      }
      return cipher;
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
        | InvalidAlgorithmParameterException | NoSuchProviderException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected static SecureRandom getDefaultSecureRandom() {
    try {
      return SecureRandom.getInstance(RANDOM_NUM_GENERATOR_ALGORITHM_NAME);
    } catch (NoSuchAlgorithmException e) {
      return new SecureRandom();
    }
  }

  public Cipher createCipher(int mode, Key key) {
    return createCipher(mode, key, null, null);
  }

  public Cipher createCipher(int mode, Key key, AlgorithmParameterSpec algoParamSpec,
      SecureRandom secureRandom) {
    return createCipher(getProvider(), getTransformation(), mode, key, algoParamSpec, secureRandom);
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public int getStreamingBufferSize() {
    return streamingBufferSize;
  }

  protected Object getProvider() {
    return null;
  }

  protected SecureRandom getSecureRandom() {
    return secureRandom;
  }

  protected String getTransformation() {
    return getAlgorithm();
  }

}

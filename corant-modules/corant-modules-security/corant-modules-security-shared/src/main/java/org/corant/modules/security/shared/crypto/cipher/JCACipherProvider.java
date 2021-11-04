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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Bytes;

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
  protected final int keyBitSize;
  protected final int ivBitSize;
  protected int streamingBufferSize;
  protected SecureRandom secureRandom;
  protected SecureRandom ivSecureRandom;

  protected JCACipherProvider(String algorithm) {
    this(algorithm, DEFAULT_KEY_BIT_SIZE, DEFAULT_KEY_BIT_SIZE, DEFAULT_STREAMING_BUFFER_SIZE);
  }

  protected JCACipherProvider(String algorithm, int keyBitSize, int ivBitSize) {
    this(algorithm, keyBitSize, ivBitSize, DEFAULT_STREAMING_BUFFER_SIZE);
  }

  protected JCACipherProvider(String algorithm, int keyBitSize, int ivBitSize,
      int streamingBufferSize) {
    this(algorithm, keyBitSize, ivBitSize, streamingBufferSize, null, null);
  }

  protected JCACipherProvider(String algorithm, int keyBitSize, int ivBitSize,
      int streamingBufferSize, SecureRandom secureRandom, SecureRandom ivSecureRandom) {
    this.algorithm = shouldNotBlank(algorithm, "algorithm can't be null or empty");
    this.keyBitSize = keyBitSize <= 0 ? DEFAULT_KEY_BIT_SIZE : keyBitSize;
    this.streamingBufferSize =
        streamingBufferSize <= 0 ? DEFAULT_STREAMING_BUFFER_SIZE : streamingBufferSize;
    if (ivBitSize > 0) {
      shouldBeTrue(ivBitSize % Byte.SIZE == 0);
      this.ivSecureRandom =
          defaultObject(ivSecureRandom, JCACipherProvider::getDefaultSecureRandom);
      this.ivBitSize = ivBitSize;
    } else {
      this.ivBitSize = 0;
    }
    checkSize(this.keyBitSize, this.ivBitSize);
    this.secureRandom = secureRandom;
  }

  public static Key generateKey(JCACipherProvider provider) {
    try {
      KeyGenerator generator = KeyGenerator.getInstance(provider.getAlgorithm());
      generator.init(provider.keyBitSize);
      return generator.generateKey();
    } catch (NoSuchAlgorithmException e) {
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

  @Override
  public byte[] decrypt(byte[] encrypted, byte[] decryptionKey) {
    try {
      return decryptBytes(encrypted, decryptionKey);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void decrypt(InputStream is, OutputStream os, byte[] decryptionKey) {
    try {
      decryptStream(is, os, decryptionKey);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public byte[] encrypt(byte[] unencrypted, byte[] encryptionKey) {
    try {
      return encryptBytes(unencrypted, encryptionKey);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void encrypt(InputStream is, OutputStream os, byte[] encryptionKey) {
    try {
      encryptStream(is, os, encryptionKey);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public int getIvBitSize() {
    return ivBitSize;
  }

  public int getKeyBitSize() {
    return keyBitSize;
  }

  public SecureRandom getSecureRandom() {
    return secureRandom;
  }

  public int getStreamingBufferSize() {
    return streamingBufferSize;
  }

  protected void checkSize(int keySize, int ivSize) {}

  protected Cipher createCipher(int mode, byte[] key, byte[] iv, boolean streaming) {
    try {
      final Cipher cipher = Cipher.getInstance(getTransformation());
      final Key secretKeySpec = new SecretKeySpec(key, getAlgorithm());
      final AlgorithmParameterSpec algoParamSpec = createParameterSpec(iv, streaming);
      if (secureRandom != null) {
        if (algoParamSpec != null) {
          cipher.init(mode, secretKeySpec, algoParamSpec, secureRandom);
        } else {
          cipher.init(mode, secretKeySpec, secureRandom);
        }
      } else {
        if (algoParamSpec != null) {
          cipher.init(mode, secretKeySpec, algoParamSpec);
        } else {
          cipher.init(mode, secretKeySpec);
        }
      }
      return cipher;
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
        | InvalidAlgorithmParameterException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected AlgorithmParameterSpec createParameterSpec(byte[] iv, boolean streaming) {
    if (iv.length > 0) {
      return new IvParameterSpec(iv);
    }
    return null;
  }

  protected byte[] decryptBytes(byte[] encrypted, byte[] key)
      throws IllegalBlockSizeException, BadPaddingException {
    byte[] iv = resolveIv(encrypted);
    if (iv.length > 0) {
      int ivByteSize = iv.length;
      int encryptedSize = encrypted.length - ivByteSize;
      byte[] encryptedWithoutIv = new byte[encryptedSize];
      System.arraycopy(encrypted, ivByteSize, encryptedWithoutIv, 0, encryptedSize);
      return createCipher(Cipher.DECRYPT_MODE, key, iv, false).doFinal(encryptedWithoutIv);
    } else {
      return createCipher(Cipher.DECRYPT_MODE, key, iv, false).doFinal(encrypted);
    }
  }

  protected void decryptStream(InputStream is, OutputStream os, byte[] key) throws IOException {
    byte[] iv = Bytes.EMPTY_ARRAY;
    if (ivBitSize > 0) {
      int ivByteSize = ivBitSize >>> 3;
      iv = new byte[ivByteSize];
      if (is.read(iv) != ivByteSize) {
        throw new CorantRuntimeException();
      }
    }
    try (CipherInputStream cis =
        new CipherInputStream(is, createCipher(Cipher.DECRYPT_MODE, key, iv, true))) {
      byte[] buffer = new byte[streamingBufferSize];
      int bytesRead;
      while ((bytesRead = cis.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
      }
    }
  }

  protected byte[] encryptBytes(byte[] unencrypted, byte[] key)
      throws IllegalBlockSizeException, BadPaddingException {
    byte[] output;
    byte[] iv = resolveIv(Bytes.EMPTY_ARRAY);
    if (iv.length > 0) {
      byte[] encrypted = createCipher(Cipher.ENCRYPT_MODE, key, iv, false).doFinal(unencrypted);
      output = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, output, 0, iv.length);
      System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);
    } else {
      output =
          createCipher(Cipher.ENCRYPT_MODE, key, Bytes.EMPTY_ARRAY, false).doFinal(unencrypted);
    }
    return output;
  }

  protected void encryptStream(InputStream is, OutputStream os, byte[] key) throws IOException {
    byte[] iv = resolveIv(Bytes.EMPTY_ARRAY);
    try (CipherInputStream cis =
        new CipherInputStream(is, createCipher(Cipher.ENCRYPT_MODE, key, iv, true))) {
      if (iv.length > 0) {
        os.write(iv);
      }
      byte[] buffer = new byte[streamingBufferSize];
      int bytesRead;
      while ((bytesRead = cis.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
      }
    }
  }

  protected String getTransformation() {
    return getAlgorithm();
  }

  protected byte[] resolveIv(byte[] encrypted) {
    byte[] iv = Bytes.EMPTY_ARRAY;
    if (encrypted.length > 0) {
      if (ivBitSize > 0) {
        int ivByteSize = ivBitSize >>> 3;
        iv = new byte[ivByteSize];
        System.arraycopy(encrypted, 0, iv, 0, ivByteSize);
      }
    } else {
      if (ivBitSize > 0) {
        iv = new byte[ivBitSize >>> 3];
        ivSecureRandom.nextBytes(iv);
      }
    }
    return iv;
  }

}

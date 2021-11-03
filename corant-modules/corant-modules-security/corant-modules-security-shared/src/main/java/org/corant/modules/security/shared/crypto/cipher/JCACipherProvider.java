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

  static final int DEFAULT_KEY_SIZE = 128;
  static final String RANDOM_NUM_GENERATOR_ALGORITHM_NAME = "SHA1PRNG";

  protected String algorithm;
  protected int keySize = DEFAULT_KEY_SIZE;
  protected int ivSize = 0;
  protected int streamingBufferSize = DEFAULT_STREAMING_BUFFER_SIZE;
  protected SecureRandom secureRandom;
  protected SecureRandom ivSecureRandom;

  protected JCACipherProvider(String algorithm) {
    this(algorithm, DEFAULT_KEY_SIZE, DEFAULT_KEY_SIZE, null, null);
  }

  protected JCACipherProvider(String algorithm, int keySize, int ivSize) {
    this(algorithm, keySize, ivSize, null, null);
  }

  protected JCACipherProvider(String algorithm, int keySize, int ivSize, SecureRandom secureRandom,
      SecureRandom ivSecureRandom) {
    this.algorithm = shouldNotBlank(algorithm, "algorithm can't be null or empty");
    this.keySize = keySize <= 0 ? DEFAULT_KEY_SIZE : keySize;
    if (ivSize > 0) {
      shouldBeTrue(ivSize % Byte.SIZE == 0);
      this.ivSecureRandom =
          defaultObject(ivSecureRandom, JCACipherProvider::getDefaultSecureRandom);
      this.ivSize = ivSize;
    }
    checkSize(this.keySize, this.ivSize);
    this.secureRandom = secureRandom;
  }

  public static Key generateKey(JCACipherProvider provider) {
    try {
      KeyGenerator generator = KeyGenerator.getInstance(provider.getAlgorithm());
      generator.init(provider.keySize);
      return generator.generateKey();
    } catch (NoSuchAlgorithmException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected static SecureRandom getDefaultSecureRandom() {
    try {
      return java.security.SecureRandom.getInstance(RANDOM_NUM_GENERATOR_ALGORITHM_NAME);
    } catch (java.security.NoSuchAlgorithmException e) {
      return new java.security.SecureRandom();
    }
  }

  @Override
  public byte[] decrypt(byte[] encrypted, byte[] decryptionKey) {
    try {
      return doDecrypt(encrypted, decryptionKey, false);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void decrypt(InputStream is, OutputStream os, byte[] decryptionKey) {
    try {
      doDecrypt(is, os, decryptionKey);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public byte[] encrypt(byte[] unencrypted, byte[] encryptionKey) {
    try {
      return doEncrypt(unencrypted, encryptionKey, false);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void encrypt(InputStream is, OutputStream os, byte[] encryptionKey) {
    try {
      doEncrypt(is, os, encryptionKey);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public int getKeySize() {
    return keySize;
  }

  protected void checkSize(int keySize, int ivSize) {

  }

  protected Cipher createCipher(int mode, byte[] key, byte[] iv, boolean streaming) {
    try {
      final Cipher cipher = Cipher.getInstance(getTransformation());
      Key secretKeySpec = new SecretKeySpec(key, getAlgorithm());
      AlgorithmParameterSpec algoParamSpec = createParameterSpec(iv, streaming);
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

  protected byte[] doDecrypt(byte[] encrypted, byte[] key, boolean streaming)
      throws IllegalBlockSizeException, BadPaddingException {
    byte[] iv = resolveIv(encrypted);
    if (iv.length > 0) {
      int ivByteSize = iv.length;
      int encryptedSize = encrypted.length - ivByteSize;
      byte[] encryptedWithoutIv = new byte[encryptedSize];
      System.arraycopy(encrypted, ivByteSize, encryptedWithoutIv, 0, encryptedSize);
      return createCipher(Cipher.DECRYPT_MODE, key, iv, streaming).doFinal(encryptedWithoutIv);
    } else {
      return createCipher(Cipher.DECRYPT_MODE, key, iv, streaming).doFinal(encrypted);
    }
  }

  protected void doDecrypt(InputStream is, OutputStream os, byte[] key) throws IOException {
    byte[] iv = Bytes.EMPTY_ARRAY;
    if (ivSize > 0) {
      int ivByteSize = ivSize / 8;
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

  protected byte[] doEncrypt(byte[] unencrypted, byte[] key, boolean streaming)
      throws IllegalBlockSizeException, BadPaddingException {
    byte[] output;
    byte[] iv = resolveIv(Bytes.EMPTY_ARRAY);
    if (iv.length > 0) {
      byte[] encrypted = createCipher(Cipher.ENCRYPT_MODE, key, iv, streaming).doFinal(unencrypted);
      output = new byte[iv.length + encrypted.length];
      System.arraycopy(iv, 0, output, 0, iv.length);
      System.arraycopy(encrypted, 0, output, iv.length, encrypted.length);
    } else {
      output =
          createCipher(Cipher.ENCRYPT_MODE, key, Bytes.EMPTY_ARRAY, streaming).doFinal(unencrypted);
    }
    return output;
  }

  protected void doEncrypt(InputStream is, OutputStream os, byte[] key) throws IOException {
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
      if (ivSize > 0) {
        int ivByteSize = ivSize / 8;
        iv = new byte[ivByteSize];
        System.arraycopy(encrypted, 0, iv, 0, ivByteSize);
      }
    } else {
      if (ivSize > 0) {
        iv = new byte[ivSize / 8];
        ivSecureRandom.nextBytes(iv);
      }
    }
    return iv;
  }

}

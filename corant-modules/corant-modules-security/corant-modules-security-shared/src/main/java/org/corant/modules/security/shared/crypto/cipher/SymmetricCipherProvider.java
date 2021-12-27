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
import static org.corant.shared.util.Objects.defaultObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.Bytes;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午11:17:28
 *
 */
public abstract class SymmetricCipherProvider extends JCACipherProvider {

  protected final SecretKeySpec key;
  protected final int ivByteSize;
  protected SecureRandom ivSecureRandom;

  protected SymmetricCipherProvider(String algorithm, byte[] key) {
    this(algorithm, key, DEFAULT_KEY_BIT_SIZE);
  }

  protected SymmetricCipherProvider(String algorithm, byte[] key, int ivBitSize) {
    this(algorithm, key, ivBitSize, DEFAULT_STREAMING_BUFFER_SIZE);
  }

  protected SymmetricCipherProvider(String algorithm, byte[] key, int ivBitSize,
      int streamingBufferSize) {
    this(algorithm, key, ivBitSize, streamingBufferSize, null, null);
  }

  protected SymmetricCipherProvider(String algorithm, byte[] key, int ivBitSize,
      int streamingBufferSize, SecureRandom secureRandom, SecureRandom ivSecureRandom) {
    super(algorithm, streamingBufferSize, secureRandom);
    this.key = new SecretKeySpec(key, algorithm);
    if (ivBitSize > 0) {
      shouldBeTrue(ivBitSize % Byte.SIZE == 0);
      this.ivSecureRandom =
          defaultObject(ivSecureRandom, JCACipherProvider::getDefaultSecureRandom);
      ivByteSize = ivBitSize >>> 3;
    } else {
      ivByteSize = 0;
    }
  }

  @Override
  public byte[] decrypt(byte[] encrypted) {
    try {
      return decryptBytes(encrypted);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void decrypt(InputStream is, OutputStream os) {
    try {
      decryptStream(is, os);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public byte[] encrypt(byte[] unencrypted) {
    try {
      return encryptBytes(unencrypted);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void encrypt(InputStream is, OutputStream os) {
    try {
      encryptStream(is, os);
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public int getIvByteSize() {
    return ivByteSize;
  }

  protected Cipher createCipher(int mode, Key key, byte[] iv, boolean streaming) {
    return createCipher(mode, key, createParameterSpec(iv, streaming), secureRandom);
  }

  protected AlgorithmParameterSpec createParameterSpec(byte[] iv, boolean streaming) {
    if (iv.length > 0) {
      return new IvParameterSpec(iv);
    }
    return null;
  }

  protected byte[] decryptBytes(byte[] encrypted)
      throws IllegalBlockSizeException, BadPaddingException {
    byte[] iv = resolveIvBytes(encrypted);
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

  protected void decryptStream(InputStream is, OutputStream os) throws IOException {
    byte[] iv = Bytes.EMPTY_ARRAY;
    if (ivByteSize > 0) {
      iv = new byte[ivByteSize];
      if (is.read(iv) != ivByteSize) {
        throw new CorantRuntimeException();
      }
    }
    try (CipherOutputStream cos =
        new CipherOutputStream(os, createCipher(Cipher.DECRYPT_MODE, key, iv, true))) {
      byte[] buffer = new byte[streamingBufferSize];
      int bytesRead;
      while ((bytesRead = is.read(buffer)) != -1) {
        cos.write(buffer, 0, bytesRead);
      }
    }
  }

  protected byte[] encryptBytes(byte[] unencrypted)
      throws IllegalBlockSizeException, BadPaddingException {
    byte[] output;
    byte[] iv = resolveIvBytes(Bytes.EMPTY_ARRAY);
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

  protected void encryptStream(InputStream is, OutputStream os) throws IOException {
    byte[] iv = resolveIvBytes(Bytes.EMPTY_ARRAY);
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

  protected SecretKeySpec getKey() {
    return key;
  }

  protected byte[] resolveIvBytes(byte[] encrypted) {
    byte[] iv = Bytes.EMPTY_ARRAY;
    if (ivByteSize > 0) {
      if (encrypted.length > 0) {
        iv = new byte[ivByteSize];
        System.arraycopy(encrypted, 0, iv, 0, ivByteSize);
      } else {
        iv = new byte[ivByteSize];
        ivSecureRandom.nextBytes(iv);
      }
    }
    return iv;
  }

}

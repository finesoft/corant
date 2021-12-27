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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午11:19:42
 *
 */
public abstract class AsymmetricCipherProvider extends JCACipherProvider {

  protected final PublicKey publicKey;
  protected final PrivateKey privateKey;

  protected AsymmetricCipherProvider(String algorithm, PublicKey publicKey, PrivateKey privateKey) {
    this(algorithm, publicKey, privateKey, DEFAULT_STREAMING_BUFFER_SIZE);
  }

  protected AsymmetricCipherProvider(String algorithm, PublicKey publicKey, PrivateKey privateKey,
      int streamingBufferSize) {
    this(algorithm, publicKey, privateKey, streamingBufferSize, null);
  }

  protected AsymmetricCipherProvider(String algorithm, PublicKey publicKey, PrivateKey privateKey,
      int streamingBufferSize, SecureRandom secureRandom) {
    super(algorithm, streamingBufferSize, secureRandom);
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }

  @Override
  public byte[] decrypt(byte[] encrypted) {
    try {
      return createCipher(Cipher.DECRYPT_MODE, privateKey).doFinal(encrypted);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void decrypt(InputStream is, OutputStream os) {
    try (CipherOutputStream cos =
        new CipherOutputStream(os, createCipher(Cipher.DECRYPT_MODE, privateKey))) {
      byte[] buffer = new byte[streamingBufferSize];
      int bytesRead;
      while ((bytesRead = is.read(buffer)) != -1) {
        cos.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public byte[] encrypt(byte[] unencrypted) {
    try {
      return createCipher(Cipher.ENCRYPT_MODE, publicKey).doFinal(unencrypted);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public void encrypt(InputStream is, OutputStream os) {
    try (CipherInputStream cis =
        new CipherInputStream(is, createCipher(Cipher.ENCRYPT_MODE, publicKey))) {
      byte[] buffer = new byte[streamingBufferSize];
      int bytesRead;
      while ((bytesRead = cis.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
      }
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

}

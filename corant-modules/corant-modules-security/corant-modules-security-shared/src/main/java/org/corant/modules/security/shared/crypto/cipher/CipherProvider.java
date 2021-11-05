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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:33:40
 *
 */
public interface CipherProvider {

  int DEFAULT_STREAMING_BUFFER_SIZE = 512;

  byte[] decrypt(byte[] encrypted);

  void decrypt(InputStream is, OutputStream os);

  default byte[] decryptB64(String base64Encrypted) {
    return decrypt(Base64.getDecoder().decode(base64Encrypted));
  }

  byte[] encrypt(byte[] unencrypted);

  void encrypt(InputStream is, OutputStream os);

  default String encryptB64(byte[] unencrypted) {
    return Base64.getEncoder().encodeToString(encrypt(unencrypted));
  }

}

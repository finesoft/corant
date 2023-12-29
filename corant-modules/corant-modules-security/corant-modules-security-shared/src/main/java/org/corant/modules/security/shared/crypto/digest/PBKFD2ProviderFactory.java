/*
 * Copyright (c) 2013-2022, Bingo.Chen (finesoft@gmail.com).
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

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午9:38:26
 */
public enum PBKFD2ProviderFactory {

  //@formatter:off
  PBKDF2WithHmacSHA1("PBKDF2WithHmacSHA1"),
  PBKDF2WithHmacSHA256("PBKDF2WithHmacSHA256"),
  PBKDF2WithHmacSHA512("PBKDF2WithHmacSHA512");
  //@formatter:on

  private final String algorithm;

  PBKFD2ProviderFactory(String algorithm) {
    this.algorithm = algorithm;
  }

  public DigestProvider createProvider(int iterations, int saltBitSize) {
    return createProvider(iterations, saltBitSize, 512);
  }

  public DigestProvider createProvider(int iterations, int saltBitSize, int derivedKeyBitSize) {
    return new PBKDF2ProviderTemplate(algorithm, iterations, saltBitSize, derivedKeyBitSize);
  }

  /**
   * corant-modules-security-shared
   *
   * @author bingo 下午12:58:09
   *
   */
  static final class PBKDF2ProviderTemplate extends AbstractPBKDF2Provider {

    PBKDF2ProviderTemplate(String algorithm, int iterations, int saltBitSize,
        int derivedKeyBitSize) {
      super(algorithm, iterations, saltBitSize, derivedKeyBitSize);
    }
  }

}

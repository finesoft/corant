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

import org.corant.modules.security.shared.crypto.Providers;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午9:38:26
 *
 */
public enum HashProviderFactory {

  //@formatter:off
  MD2("MD2"),
  MD5("MD5"),
  SHA1("SHA1"),
  SHA224("SHA-224"),
  SHA256("SHA-256"),
  SHA384("SHA-384"),
  SHA512("SHA-512"),
  SHA_512_224("SHA-512/224"),
  SHA_512_256("SHA-512/256"),
  SHA3224("SHA3-224"),
  SHA3256("SHA3-256"),
  SHA3384("SHA3-384"),
  SHA3512("SHA3-512"),
  SM3("SM3");
  //@formatter:on

  private final String algorithm;

  HashProviderFactory(String algorithm) {
    this.algorithm = algorithm;
  }

  public DigestProvider createProvider(int iterations, int saltBitSize) {
    return new HashProviderTemplate(algorithm, iterations, saltBitSize);
  }

  /**
   * corant-modules-security-shared
   *
   * @author bingo 下午12:54:41
   *
   */
  static final class HashProviderTemplate extends AbstractHashProvider {

    final boolean useBc;

    public HashProviderTemplate(String algorithm, int iterations, int saltBitSize) {
      super(algorithm, iterations, saltBitSize);
      useBc = algorithm.startsWith("SM3");
    }

    @Override
    protected Object getProvider() {
      return useBc ? Providers.BOUNCYCASTLE_PROVIDER : null;
    }
  }

}

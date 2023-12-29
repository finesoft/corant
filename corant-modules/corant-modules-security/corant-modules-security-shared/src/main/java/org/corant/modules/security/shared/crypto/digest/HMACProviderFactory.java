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
public enum HMACProviderFactory {

  //@formatter:off
  HMAC_MD5("HmacMD5"),
  HMAC_SHA1("HmacSHA1"),
  HMAC_SHA224("HmacSHA224"),
  HMAC_SHA256("HmacSHA256"),
  HMAC_SHA384("HmacSHA384"),
  HMAC_SHA512("HmacSHA512"),
  HMAC_SHA512_224("HmacSHA512/224"),
  HMAC_SHA512_256("HmacSHA512/256");
  //@formatter:on

  private final String algorithm;

  HMACProviderFactory(String algorithm) {
    this.algorithm = algorithm;
  }

  public AbstractHMACProvider createProvider(byte[] secret) {
    return new HMACProviderTemplate(algorithm, secret);
  }

  /**
   * corant-modules-security-shared
   *
   * @author bingo 下午12:57:27
   *
   */
  static final class HMACProviderTemplate extends AbstractHMACProvider {

    HMACProviderTemplate(String algorithm, byte[] secret) {
      super(algorithm, secret);
    }

  }

}

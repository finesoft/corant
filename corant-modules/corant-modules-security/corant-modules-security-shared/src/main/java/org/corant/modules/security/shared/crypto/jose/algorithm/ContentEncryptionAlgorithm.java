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
package org.corant.modules.security.shared.crypto.jose.algorithm;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:12:13
 *
 */
public enum ContentEncryptionAlgorithm {

  //@formatter:off
  A128GCM(true, "A128GCM", "AES", 128, "AES_128_GCM"),
  A192GCM(true, "A192GCM", "AES", 192, "AES_192_GCM"),
  A256GCM(true, "A256GCM", "AES", 256, "AES_256_GCM"),
  A128CBC_HS256(true, "A128CBC-HS256", "AES", 256, "AES_128_CBC_HMAC_SHA_256"),
  A192CBC_HS384(true, "A192CBC-HS384", "AES", 384, "AES_192_CBC_HMAC_SHA_384"),
  A256CBC_HS512(true, "A256CBC-HS512", "AES", 512, "AES_256_CBC_HMAC_SHA_512");
  //@formatter:on

  final String algorithmName;
  final String algorithmDescription;
  final String keyFactoryAlgorithm;
  final Integer keyFactoryDefaultKeyBitSize;
  final boolean asymmetric;

  ContentEncryptionAlgorithm(boolean asymmetric, String algorithmName, String keyFactoryAlgorithm,
      Integer keyFactoryDefaultKeyBitSize, String algorithmDescription) {
    this.asymmetric = asymmetric;
    this.algorithmName = algorithmName;
    this.keyFactoryAlgorithm = keyFactoryAlgorithm;
    this.keyFactoryDefaultKeyBitSize = keyFactoryDefaultKeyBitSize;
    this.algorithmDescription = algorithmDescription;
  }

  public String getAlgorithmDescription() {
    return algorithmDescription;
  }

  public String getAlgorithmName() {
    return algorithmName;
  }

  public String getKeyFactoryAlgorithm() {
    return keyFactoryAlgorithm;
  }

  public Integer getKeyFactoryDefaultKeyBitSize() {
    return keyFactoryDefaultKeyBitSize;
  }

  public boolean isAsymmetric() {
    return asymmetric;
  }

}

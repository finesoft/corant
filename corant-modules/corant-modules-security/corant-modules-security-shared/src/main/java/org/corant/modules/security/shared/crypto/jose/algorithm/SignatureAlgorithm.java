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
 * <p>
 * Symmetrically signed JWTs such as HMAC-SHA256 (hs256) are not recommended, deemed insecure for a
 * distributed microservice architecture where JWTs are expected to be passed around freely. Use of
 * symmetric signatures would require all microservices to share a secret, eliminating the ability
 * to determine who created the JWT.
 *
 * @author bingo 下午3:09:01
 *
 */
public enum SignatureAlgorithm {

  //@formatter:off
  //asymmetric
  RS256(true, "RSA", 2048, "RSA_USING_SHA256"),
  RS384(true, "RSA", 2048, "RSA_USING_SHA384"),
  RS512(true, "RSA", 2048, "RSA_USING_SHA512"),
  PS256(true, "RSA", 2048, "RSA_PSS_USING_SHA256"),
  PS384(true, "RSA", 2048, "RSA_PSS_USING_SHA384"),
  PS512(true, "RSA", 2048, "RSA_PSS_USING_SHA512"),
  ES256(true, "EC", 256, "ECDSA_USING_P256_CURVE_AND_SHA256"),
  ES384(true, "EC", 384, "ECDSA_USING_P384_CURVE_AND_SHA384"),
  ES512(true, "EC", 521, "ECDSA_USING_P521_CURVE_AND_SHA512"),
  //symmetric
  HS256(false, "HMAC", 256, "HMAC_SHA256"),
  HS384(false, "HMAC", 384, "HMAC_SHA384"),
  HS512(false, "HMAC", 512, "HMAC_SHA512");
  //@formatter:on

  final String algorithmDescription;
  final String keyFactoryAlgorithm;
  final Integer keyFactoryDefaultKeyBitSize;
  final boolean asymmetric;

  SignatureAlgorithm(boolean asymmetric, String keyFactoryAlgorithm,
      Integer keyFactoryDefaultKeyBitSize, String algorithmDescription) {
    this.asymmetric = asymmetric;
    this.keyFactoryAlgorithm = keyFactoryAlgorithm;
    this.keyFactoryDefaultKeyBitSize = keyFactoryDefaultKeyBitSize;
    this.algorithmDescription = algorithmDescription;
  }

  public String getAlgorithmDescription() {
    return algorithmDescription;
  }

  public String getAlgorithmName() {
    return name();
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

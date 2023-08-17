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
 * @author bingo 下午3:09:42
 *
 */
public enum KeyManagementAlgorithm {

  //@formatter:off

  //asymmetric
  //RSA1_5(true, "RSA1_5", "RSA", 2048, "RSAES-PKCS1-v1_5"), // blocked by jose4j 0.9.3
  RSA_OAEP(true, "RSA-OAEP", "RSA", 2048, "RSA_OAEP"),
  RSA_OAEP_256(true, "RSA-OAEP-256", "RSA", 2048, "RSA_OAEP_256"),
  ECDH_ES(true,"ECDH-ES", "EC", null, "ECDH_ES"),
  ECDH_ES_A128KW(true, "ECDH-ES+A128KW", "EC", null, "ECDH_ES_A128KW"),
  ECDH_ES_A192KW(true, "ECDH-ES+A192KW", "EC", null, "ECDH_ES_A192KW"),
  ECDH_ES_A256KW(true, "ECDH-ES+A256KW", "EC", null, "ECDH_ES_A256KW"),
  //symmetric
  A128KW(false, "A128KW", "AES", 128, "A128KW"),
  A192KW(false, "A192KW", "AES", 192, "A192KW"),
  A256KW(false, "A256KW", "AES", 256, "A256KW"),
  A128GCMKW(false, "A128GCMKW", "AES",128, "A128GCMKW"),
  A192GCMKW(false, "A192GCMKW", "AES",192, "A192GCMKW"),
  A256GCMKW(false, "A256GCMKW", "AES",256, "A256GCMKW"),
  PBES2_HS256_A128KW(false, "PBES2-HS256+A128KW", "AES", 128, "PBES2_HS256_A128KW"),
  PBES2_HS384_A192KW(false, "PBES2-HS384+A192KW", "AES", 192, "PBES2_HS384_A192KW"),
  PBES2_HS512_A256KW(false, "PBES2-HS512+A256KW", "AES", 256, "PBES2_HS512_A256KW");

  //@formatter:on

  final String algorithmName;
  final String algorithmDescription;
  final String keyFactoryAlgorithm;
  final Integer keyFactoryDefaultKeyBitSize;
  final boolean asymmetric;

  KeyManagementAlgorithm(boolean asymmetric, String algorithmName, String keyFactoryAlgorithm,
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

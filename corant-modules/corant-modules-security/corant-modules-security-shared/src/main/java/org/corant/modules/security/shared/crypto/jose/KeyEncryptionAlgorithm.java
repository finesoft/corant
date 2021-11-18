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
package org.corant.modules.security.shared.crypto.jose;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:09:42
 *
 */
public enum KeyEncryptionAlgorithm {

  //@formatter:off
  RSA_OAEP("RSA-OAEP"),
  RSA_OAEP_256("RSA-OAEP-256"),
  ECDH_ES("ECDH-ES"),
  ECDH_ES_A128KW("ECDH-ES+A128KW"),
  ECDH_ES_A192KW("ECDH-ES+A192KW"),
  ECDH_ES_A256KW("ECDH-ES+A256KW"),
  A128KW("A128KW"),
  A192KW("A192KW"),
  A256KW("A256KW"),
  PBES2_HS256_A128KW("PBES2-HS256+A128KW"),
  PBES2_HS384_A192KW("PBES2-HS384+A192KW"),
  PBES2_HS512_A256KW("PBES2-HS512+A256KW");
  //@formatter:on

  final String algorithmName;

  KeyEncryptionAlgorithm(String algorithmName) {
    this.algorithmName = algorithmName;
  }

  public String getAlgorithmName() {
    return algorithmName;
  }

}

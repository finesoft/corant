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
package org.corant.modules.security.shared.crypto.digest;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.corant.modules.security.shared.crypto.Keys;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午11:42:14
 */
public enum SignatureProviderFactory {

  //@formatter:off
  NONE_RSA_1024("NONEwithRSA", 1024),
  MD2_RSA_1024("MD2withRSA", 1024),
  MD5_RSA_1024("MD5withRSA", 1024),
  SHA1_RSA_1024("SHA1withRSA", 1024),
  SHA224_RSA_1024("SHA224withRSA", 1024),
  SHA256_RSA_1024("SHA256withRSA", 1024),
  SHA384_RSA_1024("SHA384withRSA", 1024),
  SHA512_RSA_1024("SHA512withRSA", 1024),
  SHA512_224_RSA_1024("SHA512/224withRSA", 1024),
  SHA512_256_RSA_1024("SHA512/256withRSA", 1024),

  NONE_RSA_2048("NONEwithRSA", 2048),
  MD2_RSA_2048("MD2withRSA", 2048),
  MD5_RSA_2048("MD5withRSA", 2048),
  SHA1_RSA_2048("SHA1withRSA", 2048),
  SHA224_RSA_2048("SHA224withRSA", 2048),
  SHA256_RSA_2048("SHA256withRSA", 2048),
  SHA384_RSA_2048("SHA384withRSA", 2048),
  SHA512_RSA_2048("SHA512withRSA", 2048),
  SHA512_224_RSA_2048("SHA512/224withRSA", 2048),
  SHA512_256_RSA_2048("SHA512/256withRSA", 2048),

  NONE_RSA_3072("NONEwithRSA", 3072),
  MD2_RSA_3072("MD2withRSA", 3072),
  MD5_RSA_3072("MD5withRSA", 3072),
  SHA1_RSA_3072("SHA1withRSA", 3072),
  SHA224_RSA_3072("SHA224withRSA", 3072),
  SHA256_RSA_3072("SHA256withRSA", 3072),
  SHA384_RSA_3072("SHA384withRSA", 3072),
  SHA512_RSA_3072("SHA512withRSA", 3072),
  SHA512_224_RSA_3072("SHA512/224withRSA", 3072),
  SHA512_256_RSA_3072("SHA512/256withRSA", 3072),

  NONE_ECDSA_256("NONEwithECDSA", 256),
  SHA1_ECDSA_256("SHA1withECDSA", 256),
  SHA224_ECDSA_256("SHA224withECDSA", 256),
  SHA256_ECDSA_256("SHA256withECDSA", 256),
  SHA384_ECDSA_256("SHA384withECDSA", 256),
  SHA512_ECDSA_256("SHA512withECDSA", 256),

  NONE_ECDSA_384("NONEwithECDSA", 384),
  SHA1_ECDSA_384("SHA1withECDSA", 384),
  SHA224_ECDSA_384("SHA224withECDSA", 384),
  SHA256_ECDSA_384("SHA256withECDSA", 384),
  SHA384_ECDSA_384("SHA384withECDSA", 384),
  SHA512_ECDSA_384("SHA512withECDSA", 384),

  NONE_ECDSA_521("NONEwithECDSA", 521),
  SHA1_ECDSA_521("SHA1withECDSA", 521),
  SHA224_ECDSA_521("SHA224withECDSA", 521),
  SHA256_ECDSA_521("SHA256withECDSA", 521),
  SHA384_ECDSA_521("SHA384withECDSA", 521),
  SHA512_ECDSA_521("SHA512withECDSA", 521);
  //@formatter:on

  private final String algorithm;

  private final int keyBits;

  SignatureProviderFactory(String algorithm, int keyBits) {
    this.algorithm = algorithm;
    this.keyBits = keyBits;
  }

  public KeyPair createKeyPair() {
    try {
      if (algorithm.endsWith("RSA")) {
        return Keys.generateKeyPair("RSA", keyBits, SecureRandom.getInstance("SHA1PRNG"));
      } else {
        return Keys.generateKeyPair("EC", keyBits, SecureRandom.getInstance("SHA1PRNG"));
      }
    } catch (NoSuchAlgorithmException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public DigestProvider createProvider(KeyPair keyPair) {
    return new SignatureProviderTemplate(algorithm, keyPair);
  }

  /**
   * corant-modules-security-shared
   *
   * @author bingo 下午12:58:40
   *
   */
  static final class SignatureProviderTemplate extends AbstractSignatureProvider {

    SignatureProviderTemplate(String algorithm, KeyPair keyPair) {
      super(algorithm, keyPair);
    }
  }

}

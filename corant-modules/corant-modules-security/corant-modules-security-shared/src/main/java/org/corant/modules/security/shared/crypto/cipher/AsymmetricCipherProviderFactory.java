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

import static org.corant.shared.util.Strings.split;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import org.corant.modules.security.shared.crypto.Keys;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:52:56
 */
public enum AsymmetricCipherProviderFactory {

  //@formatter:off
  // asymmetric
  RSA_1024_ECB_OAEP_MD5_MGF1("RSA/ECB/OAEPWithMD5AndMGF1Padding", 1024),
  RSA_1024_ECB_OAEP_SHA1_MGF1("RSA/ECB/OAEPWithSHA1AndMGF1Padding", 1024),
  RSA_1024_ECB_OAEP256_MGF1("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", 1024),
  RSA_1024_ECB_OAEP384_MGF1("RSA/ECB/OAEPWithSHA-384AndMGF1Padding", 1024),
  RSA_1024_ECB_OAEP512_224_MGF1("RSA/ECB/OAEPWithSHA-512/224AndMGF1Padding", 1024),
  RSA_1024_ECB_OAEP512_256_MGF1("RSA/ECB/OAEPWithSHA-512/256AndMGF1Padding", 1024),
  RSA_1024_ECB_PKCS1("RSA/ECB/PKCS1Padding", 1024),

  RSA_2048_ECB_OAEP_MD5_MGF1("RSA/ECB/OAEPWithMD5AndMGF1Padding", 2048),
  RSA_2048_ECB_OAEP_SHA1_MGF1("RSA/ECB/OAEPWithSHA1AndMGF1Padding", 2048),
  RSA_2048_ECB_OAEP256_MGF1("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", 2048),
  RSA_2048_ECB_OAEP384_MGF1("RSA/ECB/OAEPWithSHA-384AndMGF1Padding", 2048),
  RSA_2048_ECB_OAEP512_224_MGF1("RSA/ECB/OAEPWithSHA-512/224AndMGF1Padding", 2048),
  RSA_2048_ECB_OAEP512_256_MGF1("RSA/ECB/OAEPWithSHA-512/256AndMGF1Padding", 2048),
  RSA_2048_ECB_PKCS1("RSA/ECB/PKCS1Padding", 2048),

  RSA_3072_ECB_OAEP_MD5_MGF1("RSA/ECB/OAEPWithMD5AndMGF1Padding", 3072),
  RSA_3072_ECB_OAEP_SHA1_MGF1("RSA/ECB/OAEPWithSHA1AndMGF1Padding", 3072),
  RSA_3072_ECB_OAEP256_MGF1("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", 3072),
  RSA_3072_ECB_OAEP384_MGF1("RSA/ECB/OAEPWithSHA-384AndMGF1Padding", 3072),
  RSA_3072_ECB_OAEP512_224_MGF1("RSA/ECB/OAEPWithSHA-512/224AndMGF1Padding", 3072),
  RSA_3072_ECB_OAEP512_256_MGF1("RSA/ECB/OAEPWithSHA-512/256AndMGF1Padding", 3072),
  RSA_3072_ECB_PKCS1("RSA/ECB/PKCS1Padding", 3072);
  //@formatter:on

  private final String transformation;

  private final String algorithm;
  private final int keyBits;

  AsymmetricCipherProviderFactory(String transformation, int keyBits) {
    this.transformation = transformation;
    algorithm = split(transformation, "/", true, true)[0];
    this.keyBits = keyBits;
  }

  public KeyPair createKeyPair() {
    try {
      return Keys.generateKeyPair(algorithm, keyBits, SecureRandom.getInstance("SHA1PRNG"));
    } catch (NoSuchAlgorithmException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public JCACipherProvider createProvider(KeyPair keyPair) {
    return new AsymmetricCipherProviderTemplate(transformation, keyPair.getPublic(),
        keyPair.getPrivate());
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public int getKeyBits() {
    return keyBits;
  }

  public String getTransformation() {
    return transformation;
  }

  /**
   * corant-modules-security-shared
   *
   * @author bingo 下午1:00:44
   *
   */
  static final class AsymmetricCipherProviderTemplate extends AsymmetricCipherProvider {

    AsymmetricCipherProviderTemplate(String algorithm, PublicKey publicKey, PrivateKey privateKey) {
      super(algorithm, publicKey, privateKey);
    }
  }

}

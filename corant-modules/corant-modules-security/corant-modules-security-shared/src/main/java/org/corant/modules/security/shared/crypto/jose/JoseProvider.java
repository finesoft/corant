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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import javax.crypto.SecretKey;
import org.corant.modules.security.shared.crypto.Keys;
import org.corant.modules.security.shared.crypto.jose.algorithm.KeyManagementAlgorithm;
import org.corant.modules.security.shared.crypto.jose.algorithm.SignatureAlgorithm;
import org.corant.shared.resource.Resource;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午7:45:01
 *
 */
public interface JoseProvider {

  static KeyPair generateKeyManagementKeyPair(KeyManagementAlgorithm algo) {
    shouldBeTrue(algo != null && algo.isAsymmetric());
    return Keys.generateKeyPair(algo.getKeyFactoryAlgorithm(),
        algo.getKeyFactoryDefaultKeyBitSize());
  }

  static SecretKey generateKeyManagementSecretKey(KeyManagementAlgorithm algo) {
    shouldBeTrue(algo != null && !algo.isAsymmetric());
    return Keys.generateSecretKey(algo.getKeyFactoryAlgorithm(),
        algo.getKeyFactoryDefaultKeyBitSize());
  }

  static KeyPair generateSignKeyPair(SignatureAlgorithm algo) {
    shouldBeTrue(algo != null && algo.isAsymmetric());
    return Keys.generateKeyPair(algo.getKeyFactoryAlgorithm(),
        algo.getKeyFactoryDefaultKeyBitSize());
  }

  static SecretKey generateSignSecretKey(SignatureAlgorithm algo) {
    shouldBeTrue(algo != null && !algo.isAsymmetric());
    return Keys.generateSecretKeySpec(algo.getKeyFactoryAlgorithm(),
        algo.getKeyFactoryDefaultKeyBitSize());
  }

  static PrivateKey readKeyManagementPrivateKey(Resource resource) {
    return readKeyManagementPrivateKey(resource, KeyManagementAlgorithm.RSA_OAEP);
  }

  static PrivateKey readKeyManagementPrivateKey(Resource resource, KeyManagementAlgorithm algo) {
    return Keys.readPrivateKey(resource, algo.getKeyFactoryAlgorithm());
  }

  static PrivateKey readKeyManagementPrivateKey(String pemEncoded) {
    return readKeyManagementPrivateKey(pemEncoded, KeyManagementAlgorithm.RSA_OAEP);
  }

  static PrivateKey readKeyManagementPrivateKey(String pemEncoded, KeyManagementAlgorithm algo) {
    return Keys.decodePrivateKey(pemEncoded, algo.getKeyFactoryAlgorithm());
  }

  static PublicKey readKeyManagementPublicKey(Resource resource) {
    return readKeyManagementPublicKey(resource, KeyManagementAlgorithm.RSA_OAEP);
  }

  static PublicKey readKeyManagementPublicKey(Resource resource, KeyManagementAlgorithm algo) {
    return Keys.readPublicKey(resource, algo.getKeyFactoryAlgorithm());
  }

  static PublicKey readKeyManagementPublicKey(String pemEncoded) {
    return readKeyManagementPublicKey(pemEncoded, KeyManagementAlgorithm.RSA_OAEP);
  }

  static PublicKey readKeyManagementPublicKey(String pemEncoded, KeyManagementAlgorithm algo) {
    return Keys.decodePublicKey(pemEncoded, algo.getKeyFactoryAlgorithm());
  }

  static PrivateKey readSignPrivateKey(Resource resource) {
    return readSignPrivateKey(resource, SignatureAlgorithm.RS256);
  }

  static PrivateKey readSignPrivateKey(Resource resource, SignatureAlgorithm algo) {
    return Keys.readPrivateKey(resource, algo.getKeyFactoryAlgorithm());
  }

  static PrivateKey readSignPrivateKey(String pemEncoded) {
    return readSignPrivateKey(pemEncoded, SignatureAlgorithm.RS256);
  }

  static PrivateKey readSignPrivateKey(String pemEncoded, SignatureAlgorithm algo) {
    return Keys.decodePrivateKey(pemEncoded, algo.getKeyFactoryAlgorithm());
  }

  static PublicKey readSignPublicKey(Resource resource) {
    return readSignPublicKey(resource, SignatureAlgorithm.RS256);
  }

  static PublicKey readSignPublicKey(Resource resource, SignatureAlgorithm algo) {
    return Keys.readPublicKey(resource, algo.getKeyFactoryAlgorithm());
  }

  static PublicKey readSignPublicKey(String pemEncoded) {
    return readSignPublicKey(pemEncoded, SignatureAlgorithm.RS256);
  }

  static PublicKey readSignPublicKey(String pemEncoded, SignatureAlgorithm algo) {
    return Keys.decodePublicKey(pemEncoded, algo.getKeyFactoryAlgorithm());
  }

  Map<String, Object> decode(String data, boolean verify);

  String encode(String claimsJson);

  JoseEncryptionProvider getEncryptionProvider();

  ProtectionLevel getProtectionLevel();

  JoseSignatureProvider getSignatureProvider();

  enum ProtectionLevel {
    SIGN, ENCRYPT, SIGN_ENCRYPT
  }
}

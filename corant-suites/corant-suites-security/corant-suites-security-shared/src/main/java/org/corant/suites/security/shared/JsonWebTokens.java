/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.suites.security.shared;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.function.Consumer;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

/**
 * corant-suites-security-shared
 *
 * @author bingo 14:55:39
 *
 */
public class JsonWebTokens {

  public static String grantJWTAccessToken(PublicJsonWebKey key, Consumer<JwtClaims> setting,
      String algo) throws JoseException {
    JwtClaims claims = new JwtClaims();
    if (setting != null) {
      setting.accept(claims);
    }
    JsonWebSignature jws = new JsonWebSignature();
    jws.setPayload(claims.toJson());
    jws.setKey(key.getPrivateKey());
    jws.setKeyIdHeaderValue(key.getKeyId());
    jws.setAlgorithmHeaderValue(algo);
    return jws.getCompactSerialization();
  }

  public static String grantJWTRSASHA256AccessToken(String rsaPublicKeyPem, String rsaPrivateKeyPem,
      String sha256keyId, Consumer<JwtClaims> setting)
      throws JoseException, GeneralSecurityException {
    RsaJsonWebKey rsaJsonWebKey =
        new RsaJsonWebKey((RSAPublicKey) Keys.decodePublicKey(rsaPublicKeyPem, "RSA"));
    rsaJsonWebKey.setKeyId(sha256keyId);
    rsaJsonWebKey.setPrivateKey(Keys.decodePrivateKey(rsaPrivateKeyPem, "RSA"));
    JwtClaims claims = new JwtClaims();
    if (setting != null) {
      setting.accept(claims);
    }
    JsonWebSignature jws = new JsonWebSignature();
    jws.setPayload(claims.toJson());
    jws.setKey(rsaJsonWebKey.getPrivateKey());
    jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
    return jws.getCompactSerialization();
  }

}

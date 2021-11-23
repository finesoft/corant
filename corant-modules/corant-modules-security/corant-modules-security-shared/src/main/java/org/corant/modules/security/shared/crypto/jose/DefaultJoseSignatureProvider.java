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

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.security.Key;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.corant.modules.security.shared.crypto.jose.algorithm.SignatureAlgorithm;
import org.corant.shared.exception.CorantRuntimeException;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午2:47:00
 *
 */
public class DefaultJoseSignatureProvider implements JoseSignatureProvider {

  protected SecretKey secretKey;

  protected KeyPair keyPair;

  protected Map<String, Object> headers = new HashMap<>();

  protected SignatureAlgorithm algorithm;

  protected boolean symmetric = false;

  public DefaultJoseSignatureProvider(KeyPair keyPair, SignatureAlgorithm algorithm) {
    shouldBeTrue(algorithm.isAsymmetric());
    this.keyPair = keyPair;
    this.algorithm = algorithm;
    symmetric = !algorithm.isAsymmetric();
  }

  public DefaultJoseSignatureProvider(SecretKey key, SignatureAlgorithm algorithm) {
    shouldBeFalse(algorithm.isAsymmetric());
    secretKey = key;
    this.algorithm = algorithm;
    symmetric = !algorithm.isAsymmetric();
  }

  @Override
  public String getAlgorithmName() {
    return algorithm.getAlgorithmName();
  }

  @Override
  public Key getVerificationKey() {
    if (symmetric) {
      return secretKey;
    } else {
      return keyPair.getPublic();
    }
  }

  @Override
  public Map<String, Object> parse(String signed, boolean verify) {
    try {
      JsonWebSignature jws = new JsonWebSignature();
      if (symmetric) {
        jws.setKey(secretKey);
      } else {
        jws.setKey(keyPair.getPublic());
      }
      jws.setCompactSerialization(signed);
      if (verify) {
        jws.verifySignature();
      }
      return JwtClaims.parse(jws.getPayload()).getClaimsMap();
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public String sign(String claimsJson) {
    JsonWebSignature jws = new JsonWebSignature();
    headers.forEach((k, v) -> jws.setHeader(k.toString(), v));
    if (!headers.containsKey("typ")) {
      jws.setHeader("typ", "JWT");
    }
    jws.setAlgorithmHeaderValue(algorithm.getAlgorithmName());
    jws.setPayload(shouldNotBlank(claimsJson));
    if (symmetric) {
      jws.setKey(secretKey);
    } else {
      jws.setKey(keyPair.getPrivate());
    }
    try {
      return jws.getCompactSerialization();
    } catch (Exception ex) {
      throw new CorantRuntimeException(ex);
    }
  }

}

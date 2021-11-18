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

import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午2:47:00
 *
 */
public class DefaultJWTSignatureProvider implements JWTSignatureProvider {

  protected Key key;

  protected Map<String, Object> headers = new HashMap<>();

  protected SignatureAlgorithm algorithm;

  @Override
  public Map<String, Object> parse(String signed, boolean verify) {
    try {
      JsonWebSignature jws = new JsonWebSignature();
      jws.setKey(key);
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
    jws.setKey(key);
    try {
      return jws.getCompactSerialization();
    } catch (Exception ex) {
      throw new CorantRuntimeException(ex);
    }
  }

}

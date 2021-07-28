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
package org.corant.modules.security.shared.jose;

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toArray;
import static org.corant.shared.util.Conversions.toFloat;
import static org.corant.shared.util.Conversions.toList;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.areEqual;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import org.corant.modules.security.shared.crypto.Keys;
import org.corant.shared.exception.CorantRuntimeException;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.ReservedClaimNames;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 14:55:39
 *
 */
public class JsonWebTokens {

  public static RsaJsonWebKey createRSASHA256JsonWebKey(String rsaPublicKeyPem,
      String rsaPrivateKeyPem, String sha256keyId) throws GeneralSecurityException {
    RsaJsonWebKey rsaJsonWebKey =
        new RsaJsonWebKey((RSAPublicKey) Keys.decodePublicKey(rsaPublicKeyPem, "RSA"));
    rsaJsonWebKey.setKeyId(sha256keyId);
    rsaJsonWebKey.setPrivateKey(Keys.decodePrivateKey(rsaPrivateKeyPem, "RSA"));
    return rsaJsonWebKey;
  }

  public static String generateJWT(PublicJsonWebKey key, Consumer<JwtClaims> setting, String algo)
      throws JoseException {
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

  public static String generateRSASHA256JWT(String rsaPublicKeyPem, String rsaPrivateKeyPem,
      String sha256keyId, Consumer<JwtClaims> setting)
      throws JoseException, GeneralSecurityException {
    return generateJWT(createRSASHA256JsonWebKey(rsaPublicKeyPem, rsaPrivateKeyPem, sha256keyId),
        setting, AlgorithmIdentifiers.RSA_USING_SHA256);
  }

  public static String generateRSASHA256JWT(String rsaPublicKeyPem, String rsaPrivateKeyPem,
      String sha256keyId, Object... claimKeyValues) {
    Map<String, Object> claims = mapOf(claimKeyValues);
    try {
      return generateRSASHA256JWT(rsaPublicKeyPem, rsaPrivateKeyPem, sha256keyId, c -> {
        claims.forEach((k, v) -> {
          if (areEqual(k, ReservedClaimNames.ISSUER)) {
            c.setIssuer(shouldNotNull(v).toString());
          } else if (areEqual(k, ReservedClaimNames.EXPIRATION_TIME)) {
            c.setExpirationTime(NumericDate
                .fromMilliseconds(shouldNotNull(toObject(v, Instant.class)).toEpochMilli()));
          } else if (areEqual(k, ReservedClaimNames.EXPIRATION_TIME + "-mins")) {
            c.setExpirationTimeMinutesInTheFuture(shouldNotNull(toFloat(v)));
          } else if (areEqual(k, ReservedClaimNames.ISSUED_AT)) {
            c.setIssuedAt(NumericDate
                .fromMilliseconds(shouldNotNull(toObject(v, Instant.class)).toEpochMilli()));
          } else if (areEqual(k, ReservedClaimNames.NOT_BEFORE)) {
            c.setNotBefore(NumericDate
                .fromMilliseconds(shouldNotNull(toObject(v, Instant.class)).toEpochMilli()));
          } else if (areEqual(k, ReservedClaimNames.SUBJECT)) {
            c.setSubject(shouldNotNull(v).toString());
          } else if (areEqual(k, ReservedClaimNames.AUDIENCE)) {
            c.setAudience(shouldNotNull(v).toString());
          } else if (areEqual(k, ReservedClaimNames.JWT_ID)) {
            c.setJwtId(shouldNotNull(v).toString());
          } else if (v instanceof Collection) {
            c.setStringListClaim(k, toList(v, String.class));
          } else if (v instanceof Object[]) {
            c.setStringListClaim(k, toArray(v, String.class));
          } else if (v instanceof String) {
            c.setStringClaim(k, (String) v);
          } else {
            c.setClaim(k, v);
          }
        });
        try {
          if (c.getIssuedAt() == null) {
            c.setIssuedAtToNow();
          }
          if (c.getExpirationTime() == null || c.getNotBefore() == null) {
            c.setExpirationTime(NumericDate.fromSeconds(c.getIssuedAt().getValue() + 30 * 60));
          }
        } catch (MalformedClaimException e) {
          throw new CorantRuntimeException(e);
        }
      });
    } catch (JoseException | GeneralSecurityException e) {
      throw new CorantRuntimeException(e);
    }

  }

  public static JwtClaims getJWTClaims(Key key, String jwt) throws InvalidJwtException {
    return new JwtConsumerBuilder().setVerificationKey(key).build().processToClaims(jwt);
  }

  public static JwtClaims getRSASHA256JWTClaims(String rsaPublicKeyPem, String rsaPrivateKeyPem,
      String sha256keyId, String jwt) throws InvalidJwtException, GeneralSecurityException {
    return new JwtConsumerBuilder()
        .setVerificationKey(
            createRSASHA256JsonWebKey(rsaPublicKeyPem, rsaPrivateKeyPem, sha256keyId).getKey())
        .build().processToClaims(jwt);
  }

}

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
import static org.corant.shared.util.Strings.defaultString;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import org.corant.modules.security.shared.crypto.KeyUtils;
import org.corant.shared.exception.CorantRuntimeException;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.EllipticCurveJsonWebKey;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.ReservedClaimNames;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
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
        new RsaJsonWebKey((RSAPublicKey) KeyUtils.decodePublicKey(rsaPublicKeyPem, "RSA"));
    rsaJsonWebKey.setKeyId(sha256keyId);
    rsaJsonWebKey.setPrivateKey(KeyUtils.decodePrivateKey(rsaPrivateKeyPem, "RSA"));
    return rsaJsonWebKey;
  }

  /**
   * Producing a nested (signed and encrypted) JWT
   *
   * @param signKey the signature key(sender key) that will be used for signing and verification of
   *        the JWT, wrapped in a JWK
   * @param signAlgo the signature algorithm on the JWT/JWS that will integrity protect the claims,
   *        default is 'ES256'
   * @param decryptKey the decryption key(receiver key) that will be used for encryption and
   *        decryption of the JWT'
   * @param keyEncryptAlgo the key encryption algorithm for the output of the ECDH-ES key agreement
   *        will encrypt a randomly generated content encryption key, default is 'ECDH-ES+A256KW'
   * @param payloadEncryptAlgo the payload encryption algorithm is used to encrypt the payload,
   *        default is 'A128CBC-HS256'
   * @param setting the claims setting
   * @throws JoseException generateECJWT
   */
  public static String generateECJWT(EllipticCurveJsonWebKey signKey, String signAlgo,
      EllipticCurveJsonWebKey decryptKey, String keyEncryptAlgo, String payloadEncryptAlgo,
      Consumer<JwtClaims> setting) throws JoseException {
    JwtClaims claims = new JwtClaims();
    if (setting != null) {
      setting.accept(claims);
    }
    // A JWT is a JWS and/or a JWE with JSON claims as the payload.
    // In this method it is a JWS nested inside a JWE So we first create a JsonWebSignature object.
    JsonWebSignature jws = new JsonWebSignature();
    jws.setPayload(claims.toJson()); // The payload of the JWS is JSON content of the JWT Claims
    jws.setKey(signKey.getPrivateKey());// The JWT is signed using the sender's private key
    jws.setKeyIdHeaderValue(signKey.getKeyId());
    // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
    jws.setAlgorithmHeaderValue(
        defaultString(signAlgo, AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256));
    // Sign the JWS and produce the compact serialization, which will be the inner JWT/JWS
    // representation, which is a string consisting of three dot ('.') separated
    // base64url-encoded parts in the form Header.Payload.Signature
    String innerJwt = jws.getCompactSerialization();
    // The outer JWT is a JWE
    JsonWebEncryption jwe = new JsonWebEncryption();
    // The output of the ECDH-ES key agreement will encrypt a randomly generated content encryption
    // key
    jwe.setAlgorithmHeaderValue(
        defaultString(keyEncryptAlgo, KeyManagementAlgorithmIdentifiers.ECDH_ES_A256KW));
    // The content encryption key is used to encrypt the payload with a composite AES-CBC / HMAC
    // SHA2 encryption algorithm
    jwe.setEncryptionMethodHeaderParameter(defaultString(payloadEncryptAlgo,
        ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256));
    // We encrypt to the receiver using their public key
    jwe.setKey(decryptKey.getPublicKey());
    jwe.setKeyIdHeaderValue(decryptKey.getKeyId());
    // A nested JWT requires that the cty (Content Type) header be set to "JWT" in the outer JWT
    jwe.setContentTypeHeaderValue("JWT");
    // The inner JWT is the payload of the outer JWT
    jwe.setPayload(innerJwt);
    // Produce the JWE compact serialization, which is the complete JWT/JWE representation,
    // which is a string consisting of five dot ('.') separated
    // base64url-encoded parts in the form Header.EncryptedKey.IV.Ciphertext.AuthenticationTag
    return jwe.getCompactSerialization();
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
          } else if (v != null && v.getClass().isArray()) {
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

  /**
   * Producing a nested (signed and encrypted) JWT
   *
   * @param ecJWT the elliptic curve encrypt JWT to parse
   *
   * @param signKey the signature key(sender key) that will be used for signing and verification of
   *        the JWT, wrapped in a JWK
   * @param signAlgo the signature algorithm on the JWT/JWS that will integrity protect the claims,
   *        default is 'ES256'
   * @param decryptKey the decryption key(receiver key) that will be used for encryption and
   *        decryption of the JWT'
   * @param keyEncryptAlgo the key encryption algorithm for the output of the ECDH-ES key agreement
   *        will encrypt a randomly generated content encryption key, default is 'ECDH-ES+A256KW'
   * @param payloadEncryptAlgo the payload encryption algorithm is used to encrypt the payload,
   *        default is 'A128CBC-HS256'
   * @param builderSetter the claims consumer setting
   * @throws InvalidJwtException
   */
  public static JwtClaims getECJWTClaims(String ecJWT, EllipticCurveJsonWebKey signKey,
      String signAlgo, EllipticCurveJsonWebKey decryptKey, String keyEncryptAlgo,
      String payloadEncryptAlgo, Consumer<JwtConsumerBuilder> builderSetter)
      throws InvalidJwtException {

    return getECJWTConsumer(signKey, signAlgo, decryptKey, keyEncryptAlgo, payloadEncryptAlgo,
        builderSetter).processToClaims(ecJWT);
  }

  public static JwtConsumer getECJWTConsumer(EllipticCurveJsonWebKey signKey, String signAlgo,
      EllipticCurveJsonWebKey decryptKey, String keyEncryptAlgo, String payloadEncryptAlgo,
      Consumer<JwtConsumerBuilder> builderSetter) throws InvalidJwtException {
    // Use JwtConsumerBuilder to construct an appropriate JwtConsumer, which will
    // be used to validate and process the JWT.
    // The specific validation requirements for a JWT are context dependent, however,
    // it typically advisable to require a (reasonable) expiration time, a trusted issuer, and
    // and audience that identifies your system as the intended recipient.
    // It is also typically good to allow only the expected algorithm(s) in the given context
    AlgorithmConstraints jwsAlgConstraints = new AlgorithmConstraints(ConstraintType.PERMIT,
        defaultString(signAlgo, AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256));

    AlgorithmConstraints jweAlgConstraints = new AlgorithmConstraints(ConstraintType.PERMIT,
        defaultString(keyEncryptAlgo, KeyManagementAlgorithmIdentifiers.ECDH_ES_A256KW));

    AlgorithmConstraints jweEncConstraints =
        new AlgorithmConstraints(ConstraintType.PERMIT, defaultString(payloadEncryptAlgo,
            ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256));
    JwtConsumerBuilder builder =
        new JwtConsumerBuilder().setDecryptionKey(decryptKey.getPrivateKey())
            // verify the signature with the public key
            .setVerificationKey(signKey.getPublicKey())
            // limits the acceptable signature algorithm
            .setJwsAlgorithmConstraints(jwsAlgConstraints)
            // limits acceptable encryption key establishment algorithm(s)
            .setJweAlgorithmConstraints(jweAlgConstraints)
            // limits acceptable content encryption algorithm(s)
            .setJweContentEncryptionAlgorithmConstraints(jweEncConstraints);
    if (builderSetter != null) {
      builderSetter.accept(builder);
    }
    return builder.build();
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

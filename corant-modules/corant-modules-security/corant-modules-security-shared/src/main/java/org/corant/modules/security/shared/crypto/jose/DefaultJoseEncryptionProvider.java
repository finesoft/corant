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
import static org.corant.shared.util.Assertions.shouldNoneNull;
import java.security.Key;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.corant.modules.security.shared.crypto.jose.algorithm.ContentEncryptionAlgorithm;
import org.corant.modules.security.shared.crypto.jose.algorithm.KeyManagementAlgorithm;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:50:52
 *
 */
public class DefaultJoseEncryptionProvider implements JoseEncryptionProvider {

  protected KeyManagementAlgorithm keyManagementAlgorithm;

  protected ContentEncryptionAlgorithm contentEncryptionAlgorithm;

  protected String keyId;

  protected Map<String, Object> headers = new HashMap<>();

  protected KeyPair keyManagementKeyPair;

  protected SecretKey keyManagementSecretKey;

  public DefaultJoseEncryptionProvider(KeyManagementAlgorithm keyManagementAlgorithm,
      KeyPair keyManagementKeyPair, String keyId,
      ContentEncryptionAlgorithm contentEncryptionAlgorithm) {
    this(keyManagementAlgorithm, contentEncryptionAlgorithm, keyId, keyManagementKeyPair, null);
  }

  public DefaultJoseEncryptionProvider(KeyManagementAlgorithm keyManagementAlgorithm,
      SecretKey keyManagementSecretKey, String keyId,
      ContentEncryptionAlgorithm contentEncryptionAlgorithm) {
    this(keyManagementAlgorithm, contentEncryptionAlgorithm, keyId, null, keyManagementSecretKey);
  }

  protected DefaultJoseEncryptionProvider() {}

  protected DefaultJoseEncryptionProvider(KeyManagementAlgorithm keyManagementAlgorithm,
      ContentEncryptionAlgorithm contentEncryptionAlgorithm, String keyId,
      KeyPair keyManagementKeyPair, SecretKey keyManagementSecretKey) {
    shouldNoneNull(keyManagementAlgorithm, contentEncryptionAlgorithm);
    if (keyManagementAlgorithm.isAsymmetric()) {
      shouldBeTrue(keyManagementKeyPair != null && keyManagementSecretKey == null);
    } else {
      shouldBeTrue(keyManagementKeyPair == null && keyManagementSecretKey != null);
    }
    this.keyManagementAlgorithm = keyManagementAlgorithm;
    this.contentEncryptionAlgorithm = contentEncryptionAlgorithm;
    this.keyId = keyId;
    this.keyManagementKeyPair = keyManagementKeyPair;
    this.keyManagementSecretKey = keyManagementSecretKey;
    headers.put("alg", keyManagementAlgorithm.getAlgorithmName());
    headers.put("enc", contentEncryptionAlgorithm.getAlgorithmName());
    if (this.keyId != null) {
      headers.put("kid", this.keyId);
    }
  }

  @Override
  public Map<String, Object> decrypt(String data, Pair<Key, String> signatures, boolean verify) {
    String usedData = data;
    JwtConsumerBuilder builder = new JwtConsumerBuilder();
    if (!verify) {
      builder.setSkipAllDefaultValidators();
    }
    if (signatures != null) {
      usedData = decryptSignedData(usedData);
      builder.setVerificationKey(signatures.left());
      builder.setJwsAlgorithmConstraints(
          new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.PERMIT, signatures.right()));
    } else {
      builder.setEnableRequireEncryption();
      builder.setDisableRequireSignature();
      if (keyManagementKeyPair != null) {
        builder.setDecryptionKey(keyManagementKeyPair.getPrivate());
      } else {
        builder.setDecryptionKey(keyManagementSecretKey);
      }
      builder.setJweAlgorithmConstraints(new AlgorithmConstraints(
          AlgorithmConstraints.ConstraintType.PERMIT, getKeyManagementAlgorithmName()));
    }
    try {
      return builder.build().processToClaims(usedData).getClaimsMap();
    } catch (InvalidJwtException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public String encrypt(String claimsJson, boolean innerSigned) {
    final JsonWebEncryption jwe = new JsonWebEncryption();
    jwe.setPlaintext(claimsJson);
    headers.forEach(jwe.getHeaders()::setObjectHeaderValue);
    if (innerSigned && !headers.containsKey("cty")) {
      jwe.getHeaders().setObjectHeaderValue("cty", "JWT");
    }
    jwe.setAlgorithmHeaderValue(getKeyManagementAlgorithmName());
    jwe.setEncryptionMethodHeaderParameter(getContentEncryptionAlgorithmName());
    if (keyManagementKeyPair != null) {
      if (keyManagementKeyPair.getPublic() instanceof RSAPublicKey
          && getKeyManagementAlgorithmName()
              .startsWith(KeyManagementAlgorithm.RSA_OAEP.getAlgorithmName())
          && ((RSAPublicKey) keyManagementKeyPair.getPublic()).getModulus().bitLength() < 2048) {
        throw new NotSupportedException();
      }
      jwe.setKey(keyManagementKeyPair.getPublic());
    } else {
      jwe.setKey(keyManagementSecretKey);
    }
    try {
      return jwe.getCompactSerialization();
    } catch (JoseException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

  @Override
  public String getContentEncryptionAlgorithmName() {
    return contentEncryptionAlgorithm.getAlgorithmName();
  }

  @Override
  public Key getDecriptionKey() {
    if (keyManagementKeyPair != null) {
      return keyManagementKeyPair.getPrivate();
    } else {
      return keyManagementSecretKey;
    }
  }

  @Override
  public Key getEncriptionKey() {
    if (keyManagementKeyPair != null) {
      return keyManagementKeyPair.getPublic();
    } else {
      return keyManagementSecretKey;
    }
  }

  @Override
  public String getKeyManagementAlgorithmName() {
    return keyManagementAlgorithm.getAlgorithmName();
  }

  protected String decryptSignedData(String data) {
    try {
      JsonWebEncryption jwe = new JsonWebEncryption();
      jwe.setAlgorithmConstraints(new AlgorithmConstraints(
          AlgorithmConstraints.ConstraintType.PERMIT, getKeyManagementAlgorithmName()));
      if (keyManagementKeyPair != null) {
        jwe.setKey(keyManagementKeyPair.getPrivate());
      } else {
        jwe.setKey(keyManagementSecretKey);
      }
      jwe.setCompactSerialization(data);
      if (!"JWT".equals(jwe.getContentTypeHeaderValue())) {
        throw new CorantRuntimeException();// Ill header
      }
      return jwe.getPlaintextString();
    } catch (JoseException e) {
      throw new CorantRuntimeException(e);
    }
  }

}

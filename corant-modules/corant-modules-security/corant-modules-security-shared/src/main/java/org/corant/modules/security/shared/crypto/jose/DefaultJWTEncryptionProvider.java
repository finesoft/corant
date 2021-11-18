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

import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.lang.JoseException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:50:52
 *
 */
public class DefaultJWTEncryptionProvider {

  protected KeyEncryptionAlgorithm keyEncryptionAlgorithm;

  protected ContentEncryptionAlgorithm contentEncryptionAlgorithm =
      ContentEncryptionAlgorithm.A256GCM;

  protected String keyId;

  protected Key key;

  protected Map<String, Object> headers = new HashMap<>();

  public DefaultJWTEncryptionProvider(KeyEncryptionAlgorithm keyEncryptionAlgorithm,
      ContentEncryptionAlgorithm contentEncryptionAlgorithm, String keyId, Key key) {
    this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
    this.contentEncryptionAlgorithm = contentEncryptionAlgorithm;
    this.keyId = keyId;
    this.key = key;
    if (keyEncryptionAlgorithm != null) {
      headers.put("alg", keyEncryptionAlgorithm.getAlgorithmName());
    }
    if (contentEncryptionAlgorithm != null) {
      headers.put("enc", contentEncryptionAlgorithm.getAlgorithmName());
    }
    if (keyId != null) {
      headers.put("kid", keyId);
    }
  }

  protected DefaultJWTEncryptionProvider() {}

  public String encrypt(String claimsJson, boolean innerSigned) {
    final JsonWebEncryption jwe = new JsonWebEncryption();
    jwe.setPlaintext(claimsJson);
    headers.forEach(jwe.getHeaders()::setObjectHeaderValue);
    if (innerSigned && !headers.containsKey("cty")) {
      jwe.getHeaders().setObjectHeaderValue("cty", "JWT");
    }
    jwe.setAlgorithmHeaderValue(keyEncryptionAlgorithm.getAlgorithmName());
    jwe.setEncryptionMethodHeaderParameter(contentEncryptionAlgorithm.getAlgorithmName());
    if (key instanceof RSAPublicKey
        && keyEncryptionAlgorithm.getAlgorithmName()
            .startsWith(KeyEncryptionAlgorithm.RSA_OAEP.getAlgorithmName())
        && ((RSAPublicKey) key).getModulus().bitLength() < 2048) {
      throw new NotSupportedException();
    }
    jwe.setKey(key);
    try {
      return jwe.getCompactSerialization();
    } catch (JoseException ex) {
      throw new CorantRuntimeException(ex);
    }
  }

}

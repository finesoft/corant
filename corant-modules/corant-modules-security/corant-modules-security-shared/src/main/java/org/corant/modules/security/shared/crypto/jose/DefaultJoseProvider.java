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

import java.util.Map;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午7:51:27
 *
 */
public class DefaultJoseProvider implements JoseProvider {

  protected ProtectionLevel protectionLevel;

  protected JoseSignatureProvider signatureProvider;

  protected JoseEncryptionProvider encryptionProvider;

  public DefaultJoseProvider(ProtectionLevel protectionLevel,
      JoseSignatureProvider signatureProvider, JoseEncryptionProvider encryptionProvider) {
    this.signatureProvider = signatureProvider;
    this.encryptionProvider = encryptionProvider;
    this.protectionLevel = protectionLevel;
  }

  protected DefaultJoseProvider() {}

  @Override
  public Map<String, Object> decode(String data, boolean verify) {
    if (protectionLevel == ProtectionLevel.SIGN) {
      return signatureProvider.parse(data, verify);
    } else if (protectionLevel == ProtectionLevel.ENCRYPT) {
      return encryptionProvider.decrypt(data, null, verify);
    } else {
      return encryptionProvider.decrypt(data,
          Pair.of(signatureProvider.getVerificationKey(), signatureProvider.getAlgorithmName()),
          verify);
    }
  }

  @Override
  public String encode(String claimsJson) {
    if (protectionLevel == ProtectionLevel.SIGN) {
      return signatureProvider.sign(claimsJson);
    } else if (protectionLevel == ProtectionLevel.ENCRYPT) {
      return encryptionProvider.encrypt(claimsJson, false);
    } else {
      return encryptionProvider.encrypt(signatureProvider.sign(claimsJson), true);
    }
  }

  @Override
  public JoseEncryptionProvider getEncryptionProvider() {
    return encryptionProvider;
  }

  @Override
  public ProtectionLevel getProtectionLevel() {
    return protectionLevel;
  }

  @Override
  public JoseSignatureProvider getSignatureProvider() {
    return signatureProvider;
  }

}

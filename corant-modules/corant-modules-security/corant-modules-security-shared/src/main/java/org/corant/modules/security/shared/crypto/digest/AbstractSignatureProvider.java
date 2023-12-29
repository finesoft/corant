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
package org.corant.modules.security.shared.crypto.digest;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.SignatureException;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午11:28:06
 */
public abstract class AbstractSignatureProvider implements DigestProvider {

  protected final String algorithm;
  protected final KeyPair keyPair;

  protected AbstractSignatureProvider(String algorithm, KeyPair keyPair) {
    this.algorithm = shouldNotBlank(algorithm, "The algorithm can't empty!");
    this.keyPair = shouldNotNull(keyPair, "The key pair can't empty!");
  }

  @Override
  public Object encode(Object data) {
    Signature signature = DigestProvider.getSignature(algorithm, getProvider());
    try {
      signature.initSign(keyPair.getPrivate());
      signature.update((byte[]) data);
      return signature.sign();
    } catch (SignatureException | InvalidKeyException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public String getName() {
    return algorithm;
  }

  @Override
  public boolean validate(Object input, Object criterion) {
    Signature signature = DigestProvider.getSignature(algorithm, getProvider());
    try {
      signature.initVerify(keyPair.getPublic());
      signature.update((byte[]) input);
      return signature.verify((byte[]) criterion);
    } catch (SignatureException | InvalidKeyException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected Object getProvider() {
    return null;
  }

}

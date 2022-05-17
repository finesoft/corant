/*
 * Copyright (c) 2013-2022, Bingo.Chen (finesoft@gmail.com).
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
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.util.Arrays;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:24:08
 *
 */
public class AbstractHMACProvider implements DigestProvider {

  protected final byte[] secret;
  protected final String algorithm;

  public AbstractHMACProvider(String algorithm, byte[] secret) {
    this.algorithm = shouldNotBlank(algorithm, "The algorithm can't empty!");
    this.secret = Arrays.copyOf(shouldNotEmpty(secret, "The secret can't empty!"), secret.length);
  }

  @Override
  public byte[] encode(Object data) {
    try {
      Mac mac = DigestProvider.getMac(algorithm, getProvider());
      mac.init(new SecretKeySpec(secret, algorithm));
      return mac.doFinal((byte[]) data);
    } catch (InvalidKeyException e) {
      throw new CorantRuntimeException(e, "Could not create hmac digest, algorithm %s", algorithm);
    }
  }

  @Override
  public String getName() {
    return algorithm;
  }

  @Override
  public boolean validate(Object input, Object criterion) {
    return MessageDigest.isEqual(encode(input), (byte[]) criterion);
  }

  protected Object getProvider() {
    return null;
  }

}

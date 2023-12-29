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
package org.corant.modules.security.shared.crypto;

import java.util.function.Supplier;
import org.corant.modules.security.shared.crypto.digest.DigestProvider;
import org.corant.modules.security.shared.crypto.digest.SignatureProviderFactory;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:40:54
 */
public class SignatureProviderTest extends TestCase {

  @Test
  public void test() {
    for (SignatureProviderFactory fac : SignatureProviderFactory.values()) {
      testBytesSignatureProvider(() -> fac.createProvider(fac.createKeyPair()),
          () -> fac.createProvider(fac.createKeyPair()),
          () -> fac.createProvider(fac.createKeyPair()));
    }
  }

  void testBytesSignatureProvider(final Supplier<DigestProvider> s1,
      final Supplier<DigestProvider> s2, final Supplier<DigestProvider> s3) {
    String data = "123456";
    byte[] bytes = data.getBytes();
    DigestProvider provider = s1.get();
    byte[] encoded = (byte[]) provider.encode(bytes);
    assertTrue(provider.validate(bytes, encoded));
    // System.out.println(encoded);
    provider = s2.get();
    encoded = (byte[]) provider.encode(bytes);
    assertTrue(provider.validate(bytes, encoded));
    // System.out.println(encoded);
    provider = s3.get();
    encoded = (byte[]) provider.encode(bytes);
    assertTrue(provider.validate(bytes, encoded));
    // System.out.println(encoded);
  }

}

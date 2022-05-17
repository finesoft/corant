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

import org.corant.modules.security.shared.crypto.digest.AbstractHMACProvider;
import org.corant.modules.security.shared.crypto.digest.HMACProviderFactory;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:40:54
 *
 */
public class HMACProviderTest extends TestCase {

  @Test
  public void test() {
    for (HMACProviderFactory fac : HMACProviderFactory.values()) {
      testHMACProvider(new AbstractHMACProvider[] {fac.createProvider("bingo".getBytes()),
          fac.createProvider("jimmy".getBytes()), fac.createProvider("anncy".getBytes()),
          fac.createProvider("Arsenal".getBytes())});
    }
  }

  void testHMACProvider(final AbstractHMACProvider[] providers) {
    String data = "123456";
    for (AbstractHMACProvider provider : providers) {
      byte[] encoded = provider.encode(data.getBytes());
      assertTrue(provider.validate(data.getBytes(), encoded));
    }
  }

}

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

import static org.junit.Assert.assertArrayEquals;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.IntStream;
import org.corant.modules.security.shared.crypto.cipher.AsymmetricCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.AsymmetricCipherProviderFactory;
import org.corant.modules.security.shared.crypto.cipher.JCACipherProvider;
import org.corant.modules.security.shared.crypto.cipher.SymmetricCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.SymmetricCipherProviderFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.FileUtils;
import org.corant.shared.util.Randoms;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午5:17:43
 *
 */
public class JCACipherProviderTest extends TestCase {

  private static final String content = "Fake content for testing! bingo~bingo~chen";

  @Test
  public void testASymmetric() {
    AsymmetricCipherProvider provider;
    for (AsymmetricCipherProviderFactory fac : AsymmetricCipherProviderFactory.values()) {
      provider = (AsymmetricCipherProvider) fac.createProvider(fac.createKeyPair());
      testJCA("", provider);
      if (fac.getKeyBits() > 2048) {
        testJCA(content, provider);
      } else {
        testJCA(content.substring(0, 30), provider);
      }
    }
  }

  @Test
  public void testSymmetric() {
    SymmetricCipherProvider provider;
    for (SymmetricCipherProviderFactory fac : SymmetricCipherProviderFactory.values()) {
      provider = (SymmetricCipherProvider) fac.createProvider(fac.createKey());
      System.out.println(fac);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  @Test
  public void testSymmetricStream() throws IOException {
    SymmetricCipherProvider provider;
    for (SymmetricCipherProviderFactory fac : SymmetricCipherProviderFactory.values()) {
      provider = (SymmetricCipherProvider) fac.createProvider(fac.createKey());
      try (InputStream fis = new FileInputStream("d:/corant-1.9.0_20220505.txt");
          OutputStream fos = new FileOutputStream("d:/" + fac.name() + "_en.txt")) {
        provider.encrypt(fis, fos);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
      try (InputStream fis = new FileInputStream("d:/" + fac.name() + "_en.txt");
          OutputStream fos = new FileOutputStream("d:/" + fac.name() + "_de.txt")) {
        provider.decrypt(fis, fos);
      } catch (IOException e) {
        throw new CorantRuntimeException(e);
      }
      assertTrue(FileUtils.isSameContent(new File("d:/corant-1.9.0_20220505.txt"),
          new File("d:/" + fac.name() + "_de.txt")));
    }
  }

  void testJCA(String content, JCACipherProvider provider) {
    final byte[] contentBytes = content.getBytes();
    byte[] encrypted = provider.encrypt(contentBytes);
    byte[] decrypted = provider.decrypt(encrypted);
    assertArrayEquals(decrypted, contentBytes);
    ByteArrayInputStream is = new ByteArrayInputStream(contentBytes);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ByteArrayOutputStream decryptedOs = new ByteArrayOutputStream();
    provider.encrypt(is, os);
    provider.decrypt(new ByteArrayInputStream(os.toByteArray()), decryptedOs);
    assertArrayEquals(decryptedOs.toByteArray(), contentBytes);
  }
}

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
import java.util.stream.IntStream;
import org.corant.modules.security.shared.crypto.cipher.AESCBCCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.AESCTRCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.AESGCMCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.BlowfishCBCCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.BlowfishCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.SM4CBCCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.SM4ECBCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.SymmetricCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.TripleDESCTRCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.TripleDESECBCipherProvider;
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
  public void testAESCBC() {
    SymmetricCipherProvider provider;
    for (Integer i : AESCBCCipherProvider.ALLOW_KEY_BIT_SIZES) {
      byte[] keyBytes = Keys.generateSecretKey(AESCBCCipherProvider.ALGORITHM, i).getEncoded();
      provider = new AESCBCCipherProvider(keyBytes);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  @Test
  public void testAESCTR() {
    SymmetricCipherProvider provider;
    for (Integer i : AESCTRCipherProvider.ALLOW_KEY_BIT_SIZES) {
      byte[] keyBytes = Keys.generateSecretKey(AESCTRCipherProvider.ALGORITHM, i).getEncoded();
      provider = new AESCTRCipherProvider(keyBytes);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  @Test
  public void testAESGMC() {
    SymmetricCipherProvider provider;
    for (Integer i : AESGCMCipherProvider.ALLOW_KEY_BIT_SIZES) {
      byte[] keyBytes = Keys.generateSecretKey(AESGCMCipherProvider.ALGORITHM, i).getEncoded();
      provider = new AESGCMCipherProvider(keyBytes);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  @Test
  public void testBlowfish() {
    SymmetricCipherProvider provider;
    for (Integer i : BlowfishCipherProvider.ALLOW_KEY_BIT_SIZES) {
      byte[] keyBytes = Keys.generateSecretKey(BlowfishCipherProvider.ALGORITHM, i).getEncoded();
      provider = new BlowfishCipherProvider(keyBytes);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  @Test
  public void testBlowfishCBC() {
    SymmetricCipherProvider provider;
    for (Integer i : BlowfishCBCCipherProvider.ALLOW_KEY_BIT_SIZES) {
      byte[] keyBytes = Keys.generateSecretKey(BlowfishCBCCipherProvider.ALGORITHM, i).getEncoded();
      provider = new BlowfishCBCCipherProvider(keyBytes);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  @Test
  public void testSM4CBC() {
    SymmetricCipherProvider provider;
    for (Integer i : SM4CBCCipherProvider.ALLOW_KEY_BIT_SIZES) {
      byte[] keyBytes = Keys.generateSecretKey(Providers.BOUNCYCASTLE_PROVIDER,
          SM4CBCCipherProvider.ALGORITHM, i, null).getEncoded();
      provider = new SM4CBCCipherProvider(keyBytes);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  @Test
  public void testSM4ECB() {
    SymmetricCipherProvider provider;
    for (Integer i : SM4ECBCipherProvider.ALLOW_KEY_BIT_SIZES) {
      byte[] keyBytes = Keys.generateSecretKey(Providers.BOUNCYCASTLE_PROVIDER,
          SM4ECBCipherProvider.ALGORITHM, i, null).getEncoded();
      provider = new SM4ECBCipherProvider(keyBytes);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  @Test
  public void testTripleDESCTR() {
    SymmetricCipherProvider provider;
    for (Integer i : TripleDESCTRCipherProvider.ALLOW_KEY_BIT_SIZES) {
      byte[] keyBytes =
          Keys.generateSecretKey(TripleDESCTRCipherProvider.ALGORITHM, i).getEncoded();
      provider = new TripleDESCTRCipherProvider(keyBytes);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  @Test
  public void testTripleDESECB() {
    SymmetricCipherProvider provider;
    for (Integer i : TripleDESECBCipherProvider.ALLOW_KEY_BIT_SIZES) {
      byte[] keyBytes =
          Keys.generateSecretKey(TripleDESECBCipherProvider.ALGORITHM, i).getEncoded();
      provider = new TripleDESECBCipherProvider(keyBytes);
      testJCA("", provider);
      testJCA(content, provider);
      final SymmetricCipherProvider itp = provider;
      IntStream.range(0, 20).forEach(x -> testJCA(content.repeat(Randoms.randomInt(x, 1000)), itp));
    }
  }

  void testJCA(String content, SymmetricCipherProvider provider) {
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

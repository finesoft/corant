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
package org.corant.modules.security.shared.crypto.hash;

import static org.junit.Assert.assertArrayEquals;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.Key;
import org.corant.modules.security.shared.crypto.cipher.AESCBCCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.AESCTRCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.AESGCMCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.BlowfishCBCCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.BlowfishCipherProvider;
import org.corant.modules.security.shared.crypto.cipher.JCACipherProvider;
import org.corant.modules.security.shared.crypto.cipher.TripleDESCTR;
import org.corant.modules.security.shared.crypto.cipher.TripleDESECB;
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
    JCACipherProvider provider;
    provider = new AESCBCCipherProvider();
    tesJCA("", provider);
    tesJCA(content, provider);
    tesJCA(content.repeat(1000), provider);
    for (Integer i : AESCBCCipherProvider.ALLOW_KEY_BIT_SIZES) {
      provider = new AESCBCCipherProvider(i);
      tesJCA("", provider);
      tesJCA(content, provider);
      tesJCA(content.repeat(1000), provider);
    }
  }

  @Test
  public void testAESCTR() {
    JCACipherProvider provider;
    provider = new AESCTRCipherProvider();
    tesJCA("", provider);
    tesJCA(content, provider);
    tesJCA(content.repeat(1000), provider);
    for (Integer i : AESCTRCipherProvider.ALLOW_KEY_BIT_SIZES) {
      provider = new AESCTRCipherProvider(i);
      tesJCA("", provider);
      tesJCA(content, provider);
      tesJCA(content.repeat(1000), provider);
    }
  }

  @Test
  public void testAESGMC() {
    JCACipherProvider provider;
    provider = new AESGCMCipherProvider();
    tesJCA("", provider);
    tesJCA(content, provider);
    tesJCA(content.repeat(1000), provider);
  }

  @Test
  public void testBlowfish() {
    JCACipherProvider provider;
    provider = new BlowfishCipherProvider();
    tesJCA("", provider);
    tesJCA(content, provider);
    tesJCA(content.repeat(1000), provider);
  }

  @Test
  public void testBlowfishCBC() {
    JCACipherProvider provider;
    provider = new BlowfishCBCCipherProvider();
    tesJCA("", provider);
    tesJCA(content, provider);
    tesJCA(content.repeat(1000), provider);
  }

  @Test
  public void testTripleDESCTR() {
    JCACipherProvider provider;
    provider = new TripleDESCTR();
    tesJCA("", provider);
    tesJCA(content, provider);
    tesJCA(content.repeat(1000), provider);
    for (Integer i : TripleDESCTR.ALLOW_KEY_BIT_SIZES) {
      provider = new TripleDESCTR(i);
      tesJCA("", provider);
      tesJCA(content, provider);
      tesJCA(content.repeat(1000), provider);
    }
  }

  @Test
  public void testTripleDESECB() {
    JCACipherProvider provider;
    provider = new TripleDESECB();
    tesJCA("", provider);
    tesJCA(content, provider);
    tesJCA(content.repeat(1000), provider);
    for (Integer i : TripleDESECB.ALLOW_KEY_BIT_SIZES) {
      provider = new TripleDESECB(i);
      tesJCA("", provider);
      tesJCA(content, provider);
      tesJCA(content.repeat(1000), provider);
    }
  }

  void tesJCA(String content, JCACipherProvider provider) {
    final byte[] contentBytes = content.getBytes();
    Key key = JCACipherProvider.generateKey(provider);
    byte[] keyBytes = key.getEncoded();
    byte[] encrypted = provider.encrypt(contentBytes, keyBytes);
    byte[] decrypted = provider.decrypt(encrypted, keyBytes);
    assertArrayEquals(decrypted, contentBytes);
    ByteArrayInputStream is = new ByteArrayInputStream(contentBytes);
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    ByteArrayOutputStream decryptedOs = new ByteArrayOutputStream();
    byte[] xkeyBytes = keyBytes.clone();
    provider.encrypt(is, os, xkeyBytes);
    provider.decrypt(new ByteArrayInputStream(os.toByteArray()), decryptedOs, xkeyBytes);
    assertArrayEquals(decryptedOs.toByteArray(), contentBytes);
  }
}

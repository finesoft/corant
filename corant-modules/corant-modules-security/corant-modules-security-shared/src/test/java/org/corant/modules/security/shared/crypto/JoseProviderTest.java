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

import java.security.KeyPair;
import java.util.Map;
import javax.crypto.SecretKey;
import org.corant.modules.security.shared.crypto.jose.DefaultJoseEncryptionProvider;
import org.corant.modules.security.shared.crypto.jose.DefaultJoseProvider;
import org.corant.modules.security.shared.crypto.jose.DefaultJoseSignatureProvider;
import org.corant.modules.security.shared.crypto.jose.JoseEncryptionProvider;
import org.corant.modules.security.shared.crypto.jose.JoseProvider;
import org.corant.modules.security.shared.crypto.jose.JoseProvider.ProtectionLevel;
import org.corant.modules.security.shared.crypto.jose.JoseSignatureProvider;
import org.corant.modules.security.shared.crypto.jose.algorithm.ContentEncryptionAlgorithm;
import org.corant.modules.security.shared.crypto.jose.algorithm.KeyManagementAlgorithm;
import org.corant.modules.security.shared.crypto.jose.algorithm.SignatureAlgorithm;
import org.jose4j.jwt.JwtClaims;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:46:21
 *
 */
public class JoseProviderTest extends TestCase {

  private int decodeTimes = 1;

  @Test
  public void testEncryptAndDecrypt() {
    for (KeyManagementAlgorithm a : KeyManagementAlgorithm.values()) {
      JoseEncryptionProvider jep = null;
      for (ContentEncryptionAlgorithm ca : ContentEncryptionAlgorithm.values()) {
        if (a.isAsymmetric()) {
          KeyPair kp = JoseProvider.generateKeyManagementKeyPair(a);
          jep = new DefaultJoseEncryptionProvider(a, kp, null, ca);
        } else {
          SecretKey kp = Keys.generateSecretKeySpec(a.getKeyFactoryAlgorithm(),
              a.getKeyFactoryDefaultKeyBitSize());
          jep = new DefaultJoseEncryptionProvider(a, kp, null, ca);
        }
        testProvider(new DefaultJoseProvider(ProtectionLevel.ENCRYPT, null, jep));
      }
    }
  }

  @Test
  public void testSignAndParse() {
    for (SignatureAlgorithm sa : SignatureAlgorithm.values()) {
      JoseSignatureProvider sp;
      if (sa.isAsymmetric()) {
        KeyPair keyPair = JoseProvider.generateSignKeyPair(sa);
        sp = new DefaultJoseSignatureProvider(keyPair, sa);
      } else {
        SecretKey key = JoseProvider.generateSignSecretKey(sa);
        sp = new DefaultJoseSignatureProvider(key, sa);
      }
      testProvider(new DefaultJoseProvider(ProtectionLevel.SIGN, sp, null));
    }
  }

  @Test
  public void testSignEncryptAndDecrypt() {
    for (KeyManagementAlgorithm a : KeyManagementAlgorithm.values()) {
      JoseEncryptionProvider jep = null;
      for (ContentEncryptionAlgorithm ca : ContentEncryptionAlgorithm.values()) {
        if (a.isAsymmetric()) {
          KeyPair kp = JoseProvider.generateKeyManagementKeyPair(a);
          jep = new DefaultJoseEncryptionProvider(a, kp, null, ca);
        } else {
          SecretKey kp = Keys.generateSecretKeySpec(a.getKeyFactoryAlgorithm(),
              a.getKeyFactoryDefaultKeyBitSize());
          jep = new DefaultJoseEncryptionProvider(a, kp, null, ca);
        }
        for (SignatureAlgorithm sa : SignatureAlgorithm.values()) {
          JoseSignatureProvider sp;
          if (sa.isAsymmetric()) {
            KeyPair keyPair = JoseProvider.generateSignKeyPair(sa);
            sp = new DefaultJoseSignatureProvider(keyPair, sa);
          } else {
            SecretKey key = JoseProvider.generateSignSecretKey(sa);
            sp = new DefaultJoseSignatureProvider(key, sa);
          }
          testProvider(new DefaultJoseProvider(ProtectionLevel.SIGN_ENCRYPT, sp, jep));
        }
      }
    }
  }

  void testProvider(JoseProvider provider) {
    String sub = "bin.chen";
    String iss = "corant.org";
    JwtClaims claims = new JwtClaims();
    claims.setIssuedAtToNow();
    claims.setIssuer(iss);
    claims.setSubject(sub);
    claims.setExpirationTimeMinutesInTheFuture(30.0f);
    String data = provider.encode(claims.toJson());
    long t1 = System.currentTimeMillis();
    for (int i = 0; i < decodeTimes; i++) {
      Map<String, Object> map = provider.decode(data, false);
      assertEquals(map.get("iss"), iss);
      assertEquals(map.get("sub"), sub);
    }
    long t2 = System.currentTimeMillis();
    float t = (t2 - t1) / (float) decodeTimes;
    if (provider.getProtectionLevel() == ProtectionLevel.SIGN) {
      System.out.printf("TIME_USE\t%-16f\tSA\t%-32s%n", t,
          provider.getSignatureProvider().getAlgorithmName());
    } else if (provider.getProtectionLevel() == ProtectionLevel.ENCRYPT) {
      System.out.printf("TIME_USE\t%-16f\tKA\t%-32s\tCA\t%-32s %n", t,
          provider.getEncryptionProvider().getKeyManagementAlgorithmName(),
          provider.getEncryptionProvider().getContentEncryptionAlgorithmName());
    } else {
      System.out.printf("TIME_USE\t%-16f\tKA\t%-32s\tCA\t%-32s\tSA\t%-32s%n", t,
          provider.getEncryptionProvider().getKeyManagementAlgorithmName(),
          provider.getEncryptionProvider().getContentEncryptionAlgorithmName(),
          provider.getSignatureProvider().getAlgorithmName());
    }

  }
}

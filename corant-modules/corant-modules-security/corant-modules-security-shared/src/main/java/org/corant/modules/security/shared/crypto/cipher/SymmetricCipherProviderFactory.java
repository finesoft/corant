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
package org.corant.modules.security.shared.crypto.cipher;

import static org.corant.shared.util.Strings.split;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.GCMParameterSpec;
import org.corant.modules.security.shared.crypto.Keys;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:52:56
 *
 */
public enum SymmetricCipherProviderFactory {

  //@formatter:off
  // symmetric
  // AES_128_IV_GCM_PKCS5("AES/GCM/PKCS5Padding", 128, 96), --GCM is a stream cipher mode and does not use padding
  // AES_192_IV_GCM_PKCS5("AES/GCM/PKCS5Padding", 192, 96), --GCM is a stream cipher mode and does not use padding
  // AES_256_IV_GCM_PKCS5("AES/GCM/PKCS5Padding", 256, 96), --GCM is a stream cipher mode and does not use padding

  AES_128_IV_GCM("AES/GCM/NoPadding", 128, 96),
  AES_192_IV_GCM("AES/GCM/NoPadding", 192, 96),
  AES_256_IV_GCM("AES/GCM/NoPadding", 256, 96),

  AES_128_IV_CTR_PKCS5("AES/CTR/PKCS5Padding", 128, 0),
  AES_192_IV_CTR_PKCS5("AES/CTR/PKCS5Padding", 192, 0),
  AES_256_IV_CTR_PKCS5("AES/CTR/PKCS5Padding", 256, 0),

  AES_128_IV_CTR("AES/CTR/NoPadding", 128, 0),
  AES_192_IV_CTR("AES/CTR/NoPadding", 192, 0),
  AES_256_IV_CTR("AES/CTR/NoPadding", 256, 0),

  AES_128_IV_OFB_PKCS5("AES/OFB/PKCS5Padding", 128, 0),
  AES_192_IV_OFB_PKCS5("AES/OFB/PKCS5Padding", 192, 0),
  AES_256_IV_OFB_PKCS5("AES/OFB/PKCS5Padding", 256, 0),

  AES_128_IV_OFB("AES/OFB/NoPadding", 128, 0),
  AES_192_IV_OFB("AES/OFB/NoPadding", 192, 0),
  AES_256_IV_OFB("AES/OFB/NoPadding", 256, 0),

  AES_128_IV_CBC_PKCS5("AES/CBC/PKCS5Padding", 128, 0),
  AES_192_IV_CBC_PKCS5("AES/CBC/PKCS5Padding", 192, 0),
  AES_256_IV_CBC_PKCS5("AES/CBC/PKCS5Padding", 256, 0),

  AES_128_IV_PCBC_PKCS5("AES/PCBC/PKCS5Padding", 128, 0),
  AES_192_IV_PCBC_PKCS5("AES/PCBC/PKCS5Padding", 192, 0),
  AES_256_IV_PCBC_PKCS5("AES/PCBC/PKCS5Padding", 256, 0),

  AES_128_IV_CFB("AES/CFB/NoPadding", 128, 0),
  AES_192_IV_CFB("AES/CFB/NoPadding", 192, 0),
  AES_256_IV_CFB("AES/CFB/NoPadding", 256, 0),

  AES_128_ECB_PKCS5("AES/ECB/PKCS5Padding", 128, -1),
  AES_192_ECB_PKCS5("AES/ECB/PKCS5Padding", 192, -1),
  AES_256_ECB_PKCS5("AES/ECB/PKCS5Padding", 256, -1),

  BLOWFISH_128("Blowfish", 128, -1),
  BLOWFISH_192("Blowfish", 192, -1),
  BLOWFISH_256("Blowfish", 256, -1),
  BLOWFISH_288("Blowfish", 320, -1),
  BLOWFISH_392("Blowfish", 384, -1),
  BLOWFISH_448("Blowfish", 448, -1),

  BLOWFISH_128_IV_CBC_PKCS5("Blowfish/CBC/PKCS5Padding", 128, 0),
  BLOWFISH_192_IV_CBC_PKCS5("Blowfish/CBC/PKCS5Padding", 192, 0),
  BLOWFISH_256_IV_CBC_PKCS5("Blowfish/CBC/PKCS5Padding", 256, 0),
  BLOWFISH_288_IV_CBC_PKCS5("Blowfish/CBC/PKCS5Padding", 320, 0),
  BLOWFISH_392_IV_CBC_PKCS5("Blowfish/CBC/PKCS5Padding", 384, 0),
  BLOWFISH_448_IV_CBC_PKCS5("Blowfish/CBC/PKCS5Padding", 448, 0),

  CHA_CHA_20("ChaCha20", 256, 96),
  // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8249844
  //CHA_CHA_20_POLY1305("ChaCha20-Poly1305/None/NoPadding", 256, 96),

  DESEDE_112_IV_CTR("DESede/CTR/NoPadding", 112, 0),
  DESEDE_168_IV_CTR("DESede/CTR/NoPadding", 168, 0),
  DESEDE_112_IV_CTR_PKCS5("DESede/CTR/PKCS5Padding", 112, 0),
  DESEDE_168_IV_CTR_PKCS5("DESede/CTR/PKCS5Padding", 168, 0),

  DESEDE_112_IV_OFB("DESede/OFB/NoPadding", 112, 0),
  DESEDE_168_IV_OFB("DESede/OFB/NoPadding", 168, 0),
  DESEDE_112_IV_OFB_PKCS5("DESede/OFB/PKCS5Padding", 112, 0),
  DESEDE_168_IV_OFB_PKCS5("DESede/OFB/PKCS5Padding", 168, 0),

  DESEDE_112_IV_CBC_PKCS5("DESede/CBC/PKCS5Padding", 112, 0),
  DESEDE_168_IV_CBC_PKCS5("DESede/CBC/PKCS5Padding", 168, 0),

  SM4("SM4", 128, -1),
  SM4_IV_GCM("SM4/GCM/NoPadding", 128, 96),
  SM4_IV_CBC_PKCS5("SM4/CBC/PKCS5Padding", 128, 0),
  SM4_IV_CFB_PKCS5("SM4/CFB/NoPadding", 128, 0),
  SM4_ECB_PKCS5("SM4/ECB/PKCS5Padding", 128, -1),
  SM4_IV_CTR_PKCS5("SM4/CTR/PKCS5Padding", 128, 0),
  SM4_IV_CTR("SM4/CTR/NoPadding", 128, 0),
  SM4_IV_OFB_PKCS5("SM4/OFB/PKCS5Padding", 128, 0),
  SM4_IV_OFB("SM4/OFB/NoPadding", 128, 0);
  //@formatter:on

  private final String transformation;
  private final String algorithm;
  private final int keyBits;
  private final int ivBits;

  SymmetricCipherProviderFactory(String transformation, int keyBits, int ivBits) {
    this.transformation = transformation;
    algorithm = split(transformation, "/", true, true)[0];
    this.keyBits = keyBits;
    this.ivBits = ivBits;
  }

  public Key createKey() {
    return Keys.generateSecretKey(getAlgorithm(), keyBits);
  }

  public JCACipherProvider createProvider(Key key) {
    return new SymmetricCipherProvider(algorithm, key.getEncoded(), ivBits) {
      @Override
      protected AlgorithmParameterSpec createParameterSpec(byte[] iv, boolean streaming) {
        if (transformation.contains("/GCM/") && iv.length > 0) {
          return new GCMParameterSpec(128, iv);
        } else if ("ChaCha20".equals(transformation) && iv.length > 0) {
          return new ChaCha20ParameterSpec(iv, 1);
        }
        return super.createParameterSpec(iv, streaming);
      }

      @Override
      protected String getTransformation() {
        return transformation;
      }

    };
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public int getIvBits() {
    return ivBits;
  }

  public int getKeyBits() {
    return keyBits;
  }

  public String getTransformation() {
    return transformation;
  }
}

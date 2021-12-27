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
package org.corant.modules.security.shared.crypto.cipher;

import static org.corant.shared.util.Sets.immutableSetOf;
import java.util.Set;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午3:50:26
 *
 */
public class AESCBCCipherProvider extends SymmetricCipherProvider {
  public static final String ALGORITHM = "AES";
  public static final String TRANSFORMATION = ALGORITHM + "/CBC/PKCS5Padding";
  public static final Set<Integer> ALLOW_KEY_BIT_SIZES = immutableSetOf(128, 192, 256);
  public static final int IV_BIT_SIZE = 128;// always 128

  public AESCBCCipherProvider(byte[] key) {
    super(ALGORITHM, key, IV_BIT_SIZE);
  }

  @Override
  protected String getTransformation() {
    return TRANSFORMATION;
  }

}

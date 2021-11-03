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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Sets.immutableSetOf;
import java.util.Set;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午11:35:46
 *
 */
public class TripleDESECB extends JCACipherProvider {

  public static final String ALGORITHM = "DESede";
  public static final String TRANSFORMATION = ALGORITHM + "/ECB/NoPadding";
  public static final Set<Integer> ALLOW_KEY_SIZES = immutableSetOf(112, 168);
  public static final int IV_SIZE = 0;// not support

  public TripleDESECB() {
    this(112);
  }

  public TripleDESECB(int keySize) {
    super(ALGORITHM, keySize, IV_SIZE);
  }

  @Override
  protected void checkSize(int keySize, int ivSize) {
    shouldBeTrue(ivSize == IV_SIZE && ALLOW_KEY_SIZES.contains(keySize));
  }

}

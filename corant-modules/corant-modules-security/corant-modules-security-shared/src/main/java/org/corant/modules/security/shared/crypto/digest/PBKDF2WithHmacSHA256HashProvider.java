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
package org.corant.modules.security.shared.crypto.digest;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午6:07:56
 *
 */
public class PBKDF2WithHmacSHA256HashProvider extends PBKDF2HashProvider {

  public static final String ALGORITHM = "PBKDF2WithHmacSHA256";

  /**
   * Use {@code 'PBKDF2WithHmacSHA256'} as the secret-key algorithm and 27500 iterations times to
   * create an instance.
   */
  public PBKDF2WithHmacSHA256HashProvider() {
    super(ALGORITHM, 27500);
  }

  /**
   * Use {@code 'PBKDF2WithHmacSHA256'} as the secret-key algorithm and the given iterations times
   * to create an instance.
   *
   * @param iterations the iterations times, the minimum value is 1024
   */
  public PBKDF2WithHmacSHA256HashProvider(int iterations) {
    super(ALGORITHM, iterations);
  }

  /**
   * Use {@code 'PBKDF2WithHmacSHA256'} as the secret-key algorithm and the given iterations times
   * and salt bits size and derived key bits size to create an instance.
   *
   * @param iterations the iterations times, the minimum value is 1024
   * @param saltSize the salt bits size, the minimum value is 128
   * @param derivedKeySize the derived key bits size, the minimum value is 512
   */
  public PBKDF2WithHmacSHA256HashProvider(int iterations, int saltSize, int derivedKeySize) {
    super(ALGORITHM, iterations, saltSize, derivedKeySize);
  }

}

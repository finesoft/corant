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
 * @author bingo 上午10:36:39
 *
 */
public class SHA256HashProvider extends AbstractHashProvider {

  public static final String ALGORITHM = "SHA-256";

  public SHA256HashProvider() {
    super(ALGORITHM, 27500);
  }

  public SHA256HashProvider(int iterations) {
    super(ALGORITHM, iterations);
  }

  public SHA256HashProvider(int iterations, int saltBitSize) {
    super(ALGORITHM, iterations, saltBitSize);
  }

}
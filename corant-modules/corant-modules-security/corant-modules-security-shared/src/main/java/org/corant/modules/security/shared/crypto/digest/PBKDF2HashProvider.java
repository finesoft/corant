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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.max;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.spec.PBEKeySpec;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午8:24:08
 *
 */
public abstract class PBKDF2HashProvider extends AbstractHashProvider {

  public static final int DEFAULT_DERIVED_KEY_SIZE = 512;

  protected final int derivedKeyBitSize;

  /**
   * Specify the secret-key algorithm name and the number of hash iterations times to create an
   * instance.
   *
   * @param algorithm the standard secret-key PBKDF2 algorithm name, can't empty
   * @param iterations the iterations times, the minimum value is 1024
   */
  protected PBKDF2HashProvider(String algorithm, int iterations) {
    this(algorithm, iterations, 0, 0);
  }

  /**
   * Specify the secret-key algorithm name and the number of hash iterations times and salt bits
   * size and derived key bits size to create an instance.
   *
   * @param algorithm the standard secret-key PBKDF2 algorithm name, can't empty
   * @param iterations the iterations times, the minimum value is 1024
   * @param saltBitSize the salt bits size, the minimum value is 128
   * @param derivedKeyBitSize the derived key bits size, the minimum value is 512
   */
  protected PBKDF2HashProvider(String algorithm, int iterations, int saltBitSize,
      int derivedKeyBitSize) {
    super(algorithm, iterations, saltBitSize);
    this.derivedKeyBitSize = max(DEFAULT_DERIVED_KEY_SIZE, derivedKeyBitSize);
    shouldBeTrue(this.derivedKeyBitSize % Byte.SIZE == 0,
        "The derived key bits size error must be divisible by 8.");
    shouldNotNull(getSecretKeyFactory(algorithm, getProvider()));// for checking
  }

  public int getDerivedKeyBitSize() {
    return derivedKeyBitSize;
  }

  /**
   * Encode the given input string to hash digested bytes.
   *
   * @param input the input string that will be encoded
   * @param algorithm the standard secret-key algorithm name, can't empty
   * @param iterations the iterations times
   * @param salt the salt bytes
   * @return encode
   */
  @Override
  protected byte[] encode(Object input, String algorithm, int iterations, byte[] salt) {
    KeySpec spec =
        new PBEKeySpec(((String) input).toCharArray(), salt, iterations, derivedKeyBitSize);
    try {
      return getSecretKeyFactory(algorithm, getProvider()).generateSecret(spec).getEncoded();
    } catch (InvalidKeySpecException e) {
      throw new CorantRuntimeException(e, "Input could not be encoded");
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

}

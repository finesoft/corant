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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.corant.shared.util.Bytes;

/**
 * corant-modules-security-shared
 * <p>
 * A POJO class used to represent hash metadata info and result. A HashInfo object contains
 * algorithm name, iterations, salt bytes and digested bytes. This class provide a serialization and
 * de-serialization layout by default.
 *
 * <pre>
 * Default bytes array serialization layout:
 *
 * [<b>ANL</b>(int-4bytes), <b>ITE</b>(int-4bytes), <b>SAL</b>(int-4bytes), <b>DCL</b>(int-4bytes), <b>ANB</b>, <b>SAB</b>, <b>DCB</b>]
 *
 * <b>ANL</b>: algorithm name bytes (UTF-8) length integer 4 bytes
 * <b>ITE</b>: iterations integer 4 bytes
 * <b>SAL</b>: salt bytes length integer 4 bytes
 * <b>DCL</b>: digested content bytes length integer 4 bytes
 * <b>ANB</b>: algorithm name bytes (UTF-8)
 * <b>SAB</b>: salt bytes
 * <b>DCB</b>: digested content bytes
 * </pre>
 * </p>
 *
 * @author bingo 下午2:07:59
 */
public class HashInfo {

  protected String algorithm;
  protected int iterations;
  protected byte[] salt;
  protected byte[] digested;

  public HashInfo(String algorithm, int iterations, byte[] salt, byte[] digested) {
    this.algorithm = algorithm;
    this.iterations = iterations;
    this.salt = salt;
    this.digested = digested;
  }

  protected HashInfo() {}

  public static HashInfo fromDefaultLayoutBytes(byte[] bytes) {
    HashInfo info = new HashInfo();
    int next = 0;
    int algoSize = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    info.iterations = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    int saltSize = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4)) << 3;
    int digestSize = Bytes.toInt(Arrays.copyOfRange(bytes, next, next += 4));
    info.algorithm =
        new String(Arrays.copyOfRange(bytes, next, next += algoSize), StandardCharsets.UTF_8);
    info.salt = Arrays.copyOfRange(bytes, next, next += saltSize >>> 3);
    info.digested = Arrays.copyOfRange(bytes, next, next + digestSize);
    return info;
  }

  public static byte[] toDefaultLayoutBytes(HashInfo info) {
    byte[] algoNameBytes = info.algorithm.getBytes(StandardCharsets.UTF_8);
    byte[] bytes =
        new byte[(4 << 2) + algoNameBytes.length + info.salt.length + info.digested.length];
    // header length info
    int next = 0;
    System.arraycopy(Bytes.toBytes(algoNameBytes.length), 0, bytes, next, 4);
    System.arraycopy(Bytes.toBytes(info.iterations), 0, bytes, next += 4, 4);
    System.arraycopy(Bytes.toBytes(info.salt.length), 0, bytes, next += 4, 4);
    System.arraycopy(Bytes.toBytes(info.digested.length), 0, bytes, next += 4, 4);
    // body content info
    System.arraycopy(algoNameBytes, 0, bytes, next += 4, algoNameBytes.length);
    System.arraycopy(info.salt, 0, bytes, next += algoNameBytes.length, info.salt.length);
    System.arraycopy(info.digested, 0, bytes, next + info.salt.length, info.digested.length);
    return bytes;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public byte[] getDigested() {
    return digested;
  }

  public int getIterations() {
    return iterations;
  }

  public byte[] getSalt() {
    return salt;
  }

  /**
   * Returns salt bits size
   */
  public int getSaltSize() {
    return salt.length << 3;
  }

  @Override
  public String toString() {
    return "HashInfo [algorithm=" + getAlgorithm() + ", iterations=" + getIterations()
        + ", saltSize=" + getSaltSize() + "]";
  }
}

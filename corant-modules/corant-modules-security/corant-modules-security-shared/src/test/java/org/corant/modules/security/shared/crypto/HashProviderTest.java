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

import static org.corant.shared.util.Iterables.range;
import static org.corant.shared.util.Objects.min;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.corant.modules.security.shared.crypto.digest.HashProvider;
import org.corant.modules.security.shared.crypto.digest.MD5HashProvider;
import org.corant.modules.security.shared.crypto.digest.PBKDF2WithHmacSHA1HashProvider;
import org.corant.modules.security.shared.crypto.digest.PBKDF2WithHmacSHA256HashProvider;
import org.corant.modules.security.shared.crypto.digest.PBKDF2WithHmacSHA512HashProvider;
import org.corant.modules.security.shared.crypto.digest.SHA256HashProvider;
import org.corant.modules.security.shared.crypto.digest.SHA384HashProvider;
import org.corant.modules.security.shared.crypto.digest.SHA512HashProvider;
import org.corant.modules.security.shared.crypto.digest.SM3HashProvider;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:40:54
 *
 */
public class HashProviderTest extends TestCase {

  @Test
  public void testMD5Provider() {
    testBytesHashProvider(MD5HashProvider::new, () -> new MD5HashProvider(10000),
        () -> new MD5HashProvider(10000, 256));
  }

  @Test
  public void testPBKDF2HashProvider() {
    testPBKDHashProvider(PBKDF2WithHmacSHA1HashProvider::new,
        () -> new PBKDF2WithHmacSHA1HashProvider(10000),
        () -> new PBKDF2WithHmacSHA1HashProvider(10000, 256, 1024));
    testPBKDHashProvider(PBKDF2WithHmacSHA256HashProvider::new,
        () -> new PBKDF2WithHmacSHA256HashProvider(10000),
        () -> new PBKDF2WithHmacSHA256HashProvider(10000, 256, 1024));
    testPBKDHashProvider(PBKDF2WithHmacSHA512HashProvider::new,
        () -> new PBKDF2WithHmacSHA512HashProvider(10000),
        () -> new PBKDF2WithHmacSHA512HashProvider(10000, 256, 1024));
  }

  // @Test
  public void testPBKDF2HashProviderTimeuse() throws InterruptedException {
    testHashProviderTimeuse(PBKDF2WithHmacSHA1HashProvider::new, 100, 4);
    testHashProviderTimeuse(PBKDF2WithHmacSHA256HashProvider::new, 100, 4);
    testHashProviderTimeuse(PBKDF2WithHmacSHA512HashProvider::new, 100, 4);
  }

  @Test
  public void testSHA256Provider() {
    testBytesHashProvider(SHA256HashProvider::new, () -> new SHA256HashProvider(10000),
        () -> new SHA256HashProvider(10000, 256));
  }

  @Test
  public void testSHA384Provider() {
    testBytesHashProvider(SHA384HashProvider::new, () -> new SHA384HashProvider(10000),
        () -> new SHA384HashProvider(10000, 256));
  }

  @Test
  public void testSHA512Provider() {
    testBytesHashProvider(SHA512HashProvider::new, () -> new SHA512HashProvider(10000),
        () -> new SHA512HashProvider(10000, 256));
  }

  @Test
  public void testSM3Provider() {
    testBytesHashProvider(SM3HashProvider::new, () -> new SM3HashProvider(10000),
        () -> new SM3HashProvider(10000, 256));
  }

  void testBytesHashProvider(final Supplier<HashProvider> s1, final Supplier<HashProvider> s2,
      final Supplier<HashProvider> s3) {
    String data = "123456";
    byte[] bytes = data.getBytes();
    HashProvider provider = s1.get();
    String encoded = provider.encode(bytes).toString();
    assertTrue(provider.validate(bytes, encoded));
    // System.out.println(encoded);
    provider = s2.get();
    encoded = provider.encode(bytes).toString();
    assertTrue(provider.validate(bytes, encoded));
    // System.out.println(encoded);
    provider = s3.get();
    encoded = provider.encode(bytes).toString();
    assertTrue(provider.validate(bytes, encoded));
    // System.out.println(encoded);
  }

  void testHashProviderTimeuse(final Supplier<HashProvider> s1, int timesPerThread, int threads)
      throws InterruptedException {
    final String data = UUID.randomUUID().toString();
    final HashProvider provider = s1.get();
    IntStream.of(min(32, timesPerThread)).forEach(i -> provider.encode(data));
    final CountDownLatch latch = new CountDownLatch(threads);
    final long st = System.currentTimeMillis();
    for (int i : range(threads)) {
      new Thread(() -> {
        for (int t = 0; t < timesPerThread; t++) {
          provider.encode(data);
        }
        latch.countDown();
      }, "t-" + i).start();
    }
    latch.await();
    System.out.format("%s threads: %d, times-per-thread: %d, timeuse(ms): %s%n",
        provider.getClass().getSimpleName(), threads, timesPerThread,
        System.currentTimeMillis() - st).flush();
  }

  void testPBKDHashProvider(final Supplier<HashProvider> s1, final Supplier<HashProvider> s2,
      final Supplier<HashProvider> s3) {
    String data = "123456";
    HashProvider provider = s1.get();
    String encoded = provider.encode(data).toString();
    assertTrue(provider.validate(data, encoded));
    // System.out.println(encoded);
    provider = s2.get();
    encoded = provider.encode(data).toString();
    assertTrue(provider.validate(data, encoded));
    // System.out.println(encoded);
    provider = s3.get();
    encoded = provider.encode(data).toString();
    assertTrue(provider.validate(data, encoded));
    // System.out.println(encoded);
  }
}

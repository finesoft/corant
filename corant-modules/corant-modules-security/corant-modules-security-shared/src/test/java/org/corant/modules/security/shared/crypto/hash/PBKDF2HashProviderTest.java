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

import static org.corant.shared.util.Iterables.range;
import static org.corant.shared.util.Objects.min;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-modules-security-shared
 *
 * @author bingo 下午7:14:36
 *
 */
public class PBKDF2HashProviderTest extends TestCase {

  @Test
  public void testPBKDF2HashProvider() {
    testPBKDF2HashProvider(PBKDF2WithHmacSHA1HashProvider::new,
        () -> new PBKDF2WithHmacSHA1HashProvider(10000),
        () -> new PBKDF2WithHmacSHA1HashProvider(10000, 256, 1024));
    testPBKDF2HashProvider(PBKDF2WithHmacSHA256HashProvider::new,
        () -> new PBKDF2WithHmacSHA256HashProvider(10000),
        () -> new PBKDF2WithHmacSHA256HashProvider(10000, 256, 1024));
    testPBKDF2HashProvider(PBKDF2WithHmacSHA512HashProvider::new,
        () -> new PBKDF2WithHmacSHA512HashProvider(10000),
        () -> new PBKDF2WithHmacSHA512HashProvider(10000, 256, 1024));
  }

  @Test
  public void testPBKDF2HashProviderTimeuse() throws InterruptedException {
    testPBKDF2HashProviderTimeuse(PBKDF2WithHmacSHA1HashProvider::new, 100, 4);
    testPBKDF2HashProviderTimeuse(PBKDF2WithHmacSHA256HashProvider::new, 100, 4);
    testPBKDF2HashProviderTimeuse(PBKDF2WithHmacSHA512HashProvider::new, 100, 4);
  }

  void testPBKDF2HashProvider(final Supplier<PBKDF2HashProvider> s1,
      final Supplier<PBKDF2HashProvider> s2, final Supplier<PBKDF2HashProvider> s3) {
    String data = "123456";
    HashProvider provider = s1.get();
    String encoded = provider.encode(data).toString();
    assertTrue(provider.validate(data, encoded));
    provider = s2.get();
    encoded = provider.encode(data).toString();
    assertTrue(provider.validate(data, encoded));
    provider = s3.get();
    encoded = provider.encode(data).toString();
    assertTrue(provider.validate(data, encoded));
  }

  void testPBKDF2HashProviderTimeuse(final Supplier<PBKDF2HashProvider> s1, int timesPerThread,
      int threads) throws InterruptedException {
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

}

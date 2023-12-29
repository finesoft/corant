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
import org.corant.modules.security.shared.crypto.digest.DigestProvider;
import org.corant.modules.security.shared.crypto.digest.HashProviderFactory;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-modules-security-shared
 *
 * @author bingo 上午10:40:54
 */
public class HashProviderTest extends TestCase {

  @Test
  public void test() {
    for (HashProviderFactory fac : HashProviderFactory.values()) {
      testBytesHashProvider(() -> fac.createProvider(0, 0), () -> fac.createProvider(10000, 0),
          () -> fac.createProvider(10000, 256));
    }
  }

  void testBytesHashProvider(final Supplier<DigestProvider> s1, final Supplier<DigestProvider> s2,
      final Supplier<DigestProvider> s3) {
    String data = "123456";
    byte[] bytes = data.getBytes();
    DigestProvider provider = s1.get();
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

  void testHashProviderTimeuse(final Supplier<DigestProvider> s1, int timesPerThread, int threads)
      throws InterruptedException {
    final String data = UUID.randomUUID().toString();
    final DigestProvider provider = s1.get();
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

  void testPBKDHashProvider(final Supplier<DigestProvider> s1, final Supplier<DigestProvider> s2,
      final Supplier<DigestProvider> s3) {
    String data = "123456";
    DigestProvider provider = s1.get();
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

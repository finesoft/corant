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
package org.corant.shared.ubiquity;

import static org.junit.Assert.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.corant.shared.ubiquity.Atomics.AtomicDouble;
import org.corant.shared.ubiquity.Atomics.AtomicFloat;
import org.junit.Test;

/**
 * corant-shared
 *
 * @author bingo 下午8:37:11
 */
public class AtomicsTest {

  @Test
  public void testGetAndSetDouble() throws InterruptedException {
    final int workers = 4;
    final int iterates = 10000;
    double result = workers * iterates;
    AtomicDouble af = new AtomicDouble();
    final CountDownLatch latch = new CountDownLatch(workers);
    final ExecutorService es = Executors.newFixedThreadPool(workers);
    for (int i = 0; i < workers; i++) {
      es.submit(() -> {
        for (int j = 0; j < iterates; j++) {
          af.incrementAndGet();
        }
        latch.countDown();
      });
    }
    latch.await();
    assertTrue(Double.compare(result, af.get()) == 0);
  }

  @Test
  public void testGetAndSetFloat() throws InterruptedException {
    final int workers = 4;
    final int iterates = 10000;
    float result = workers * iterates;
    AtomicFloat af = new AtomicFloat();
    final CountDownLatch latch = new CountDownLatch(workers);
    final ExecutorService es = Executors.newFixedThreadPool(workers);
    for (int i = 0; i < workers; i++) {
      es.submit(() -> {
        for (int j = 0; j < iterates; j++) {
          af.incrementAndGet();
        }
        latch.countDown();
      });
    }
    latch.await();
    assertTrue(Float.compare(result, af.get()) == 0);
  }

}

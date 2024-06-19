/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.util;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.corant.shared.retry.BackoffStrategy.BackoffAlgorithm;
import org.corant.shared.retry.BackoffStrategy.BackoffStrategyBuilder;
import org.corant.shared.retry.RetryStrategy.MaxAttemptsRetryStrategy;
import org.corant.shared.retry.RetryStrategy.ThrowableClassifierRetryStrategy;
import org.corant.shared.retry.Retryer;
import org.junit.Assert;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * corant-shared
 *
 * @author bingo 14:28:25
 */
public class RetryTest extends TestCase {

  final AtomicInteger counter = new AtomicInteger();
  final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);

  @Test
  public void testAsyncFailure() {
    Retryer retryer = Retry.asynchronousRetryer(executorService)
        .backoffStrategy(new BackoffStrategyBuilder().algorithm(BackoffAlgorithm.EXPO_EQUAL_JITTER)
            .baseDuration(Duration.ofSeconds(1)).maxDuration(Duration.ofSeconds(4)).build())
        .retryStrategy(new MaxAttemptsRetryStrategy(3));
    Assert.assertThrows(ActionException.class, () -> retryer.invoke(() -> testAction(5)));
  }

  @Test
  public void testAsyncSuccessfully() {
    counter.set(0);
    Retryer retryer = Retry.asynchronousRetryer(executorService)
        .backoffStrategy(new BackoffStrategyBuilder().algorithm(BackoffAlgorithm.FIXED)
            .baseDuration(Duration.ofSeconds(1)).maxDuration(Duration.ofSeconds(30)).build())
        .retryStrategy(new MaxAttemptsRetryStrategy(15));
    int result = retryer.invoke(() -> testAction(15));
    assertEquals(result, 15);
  }

  @Test
  public void testSyncAbortOnSuccessfully() {
    counter.set(0);
    @SuppressWarnings("unchecked")
    Retryer retryer = Retry.synchronousRetryer()
        .backoffStrategy(new BackoffStrategyBuilder().algorithm(BackoffAlgorithm.FIXED)
            .baseDuration(Duration.ofSeconds(1)).maxDuration(Duration.ofSeconds(30)).build())
        .retryStrategy(new ThrowableClassifierRetryStrategy().abortOn(ActionException.class));
    Assert.assertThrows(ActionException.class, () -> retryer.invoke(() -> testAction(5)));
  }

  @Test
  public void testSyncFailure() {
    counter.set(0);
    Retryer retryer = Retry.synchronousRetryer()
        .backoffStrategy(new BackoffStrategyBuilder().algorithm(BackoffAlgorithm.EXPO_EQUAL_JITTER)
            .baseDuration(Duration.ofSeconds(1)).maxDuration(Duration.ofSeconds(4)).build())
        .retryStrategy(new MaxAttemptsRetryStrategy(3));
    Assert.assertThrows(ActionException.class, () -> retryer.invoke(() -> testAction(5)));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSyncRetryOnSuccessfully() {
    counter.set(0);
    Retryer retryer = Retry.synchronousRetryer()
        .backoffStrategy(new BackoffStrategyBuilder().algorithm(BackoffAlgorithm.FIXED)
            .baseDuration(Duration.ofSeconds(1)).maxDuration(Duration.ofSeconds(30)).build())
        .retryStrategy(new ThrowableClassifierRetryStrategy().retryOn(ActionException.class));
    int result = retryer.invoke(() -> testAction(15));
    assertEquals(result, 15);
  }

  @Test
  public void testSyncSuccessfully() {
    counter.set(0);
    Retryer retryer = Retry.synchronousRetryer()
        .backoffStrategy(new BackoffStrategyBuilder().algorithm(BackoffAlgorithm.FIXED)
            .baseDuration(Duration.ofSeconds(1)).maxDuration(Duration.ofSeconds(30)).build())
        .retryStrategy(new MaxAttemptsRetryStrategy(15));
    int result = retryer.invoke(() -> testAction(15));
    assertEquals(result, 15);
  }

  int testAction(int c) {
    Threads.tryThreadSleep(Randoms.randomInt(100, 1000));
    int result = counter.incrementAndGet();
    if (result < c) {
      throw new ActionException();
    }
    return result;
  }

  public static class ActionException extends RuntimeException {
    private static final long serialVersionUID = 3036684880578210207L;

    public ActionException() {
      super(Thread.currentThread().getName() + "-" + Thread.currentThread().getId()
          + " action error");
    }

  }
}

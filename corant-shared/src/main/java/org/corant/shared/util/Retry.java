/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Functions.asCallable;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;
import org.corant.shared.retry.AsynchronousRetryer;
import org.corant.shared.retry.BackoffStrategy.FixedBackoffStrategy;
import org.corant.shared.retry.RetryStrategy.MaxAttemptsRetryStrategy;
import org.corant.shared.retry.RetryStrategy.ThrowableClassifierRetryStrategy;
import org.corant.shared.retry.SynchronousRetryer;

/**
 * corant-shared
 *
 * @author bingo 10:02:42
 *
 */
public class Retry {

  /**
   * Create asynchronous retryer for more granular control retry mechanism with the given executor
   *
   * @param executor the schedule executor service use in retry
   * @return an asynchronous retryer
   */
  public static AsynchronousRetryer asynchronousRetryer(ScheduledExecutorService executor) {
    return new AsynchronousRetryer(shouldNotNull(executor));
  }

  /**
   * Try to execute runnable asynchronously
   *
   * @param executor the schedule executor service use in retry
   * @param maxAttempts Total number of attempts to execute the {@code runnable}
   * @param interval Time interval between two attempts
   * @param runnable The work unit
   * @param abortOn when a throwable of the given throwable classifier occurs during the retry
   *        process, terminate the retry
   */
  @SafeVarargs
  public static void executeAsync(ScheduledExecutorService executor, int maxAttempts,
      Duration interval, Runnable runnable, Class<? extends Throwable>... abortOn) {
    new AsynchronousRetryer(executor).backoffStrategy(new FixedBackoffStrategy(interval))
        .retryStrategy(new MaxAttemptsRetryStrategy(maxAttempts)
            .and(new ThrowableClassifierRetryStrategy().abortOn(abortOn)))
        .execute(runnable);
  }

  /**
   * Try to execute and supply a result asynchronously
   *
   * @param <T> The result type
   * @param executor the schedule executor service use in retry
   * @param maxAttempts Total number of attempts to execute the {@code supplier}
   * @param interval Time interval between two attempts
   * @param supplier The work unit
   * @param abortOn when a throwable of the given throwable classifier occurs during the retry
   *        process, terminate the retry
   * @return The result
   */
  @SafeVarargs
  public static <T> Future<T> executeAsync(ScheduledExecutorService executor, int maxAttempts,
      Duration interval, Supplier<T> supplier, Class<? extends Throwable>... abortOn) {
    return new AsynchronousRetryer(executor).backoffStrategy(new FixedBackoffStrategy(interval))
        .retryStrategy(new MaxAttemptsRetryStrategy(maxAttempts)
            .and(new ThrowableClassifierRetryStrategy().abortOn(abortOn)))
        .execute(asCallable(supplier));
  }

  /**
   * Try to execute and return a result synchronously
   *
   * @param <T> the result type
   * @param maxAttempts Total number of attempts to execute the {@code runnable}
   * @param interval Time interval between two attempts
   * @param function The work unit, the parameter of this is the number of retry attempts
   * @param abortOn when a throwable of the given throwable classifier occurs during the retry
   *        process, terminate the retry
   * @return The result
   */
  @SafeVarargs
  public static <T> T executeSync(int maxAttempts, Duration interval, Function<Integer, T> function,
      Class<? extends Throwable>... abortOn) {
    return new SynchronousRetryer().backoffStrategy(new FixedBackoffStrategy(interval))
        .retryStrategy(new MaxAttemptsRetryStrategy(maxAttempts)
            .and(new ThrowableClassifierRetryStrategy().abortOn(abortOn)))
        .execute(function);
  }

  /**
   * Try to execute runnable synchronously
   *
   * @param maxAttempts Total number of attempts to execute the {@code runnable}
   * @param interval Time interval between two attempts
   * @param runnable The work unit
   * @param abortOn when a throwable of the given throwable classifier occurs during the retry
   *        process, terminate the retry
   */
  @SafeVarargs
  public static void executeSync(int maxAttempts, Duration interval, Runnable runnable,
      Class<? extends Throwable>... abortOn) {
    new SynchronousRetryer().backoffStrategy(new FixedBackoffStrategy(interval))
        .retryStrategy(new MaxAttemptsRetryStrategy(maxAttempts)
            .and(new ThrowableClassifierRetryStrategy().abortOn(abortOn)))
        .execute(runnable);
  }

  /**
   * Try to execute and supply a result synchronously
   *
   * @param <T> The result type
   * @param maxAttempts Total number of attempts to execute the {@code supplier}
   * @param interval Time interval between two attempts
   * @param supplier The work unit
   * @param abortOn when a throwable of the given throwable classifier occurs during the retry
   *        process, terminate the retry
   * @return The result
   */
  @SafeVarargs
  public static <T> T executeSync(int maxAttempts, Duration interval, Supplier<T> supplier,
      Class<? extends Throwable>... abortOn) {
    return new SynchronousRetryer().backoffStrategy(new FixedBackoffStrategy(interval))
        .retryStrategy(new MaxAttemptsRetryStrategy(maxAttempts)
            .and(new ThrowableClassifierRetryStrategy().abortOn(abortOn)))
        .execute(asCallable(supplier));
  }

  /**
   * Create retryer for more granular control retry mechanism.
   *
   * @return retryer
   */
  public static SynchronousRetryer synchronousRetryer() {
    return new SynchronousRetryer();
  }

}

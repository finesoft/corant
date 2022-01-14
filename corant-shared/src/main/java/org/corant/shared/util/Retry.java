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

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Objects.min;
import static org.corant.shared.util.Sets.immutableSetOf;
import static org.corant.shared.util.Strings.defaultString;
import java.io.Serializable;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.Futures.SimpleFuture;

/**
 * corant-shared
 *
 * @author bingo 10:02:42
 *
 */
public class Retry {

  static final Logger logger = Logger.getLogger(Retry.class.toString());

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
   * Use Exponential Backoff algorithm to compute the delay. The backoff factor accepted by this
   * method must be greater than 1 or null, default is 2.0.
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   */
  public static long computeExpoBackoff(double backoffFactor, long cap, long base, int attempts) {
    long result = min(cap, base * (long) Math.pow(backoffFactor, attempts));
    return result > 0 ? result : cap;
  }

  /**
   * Use Exponential Backoff Decorr algorithm to compute the delay. The backoff factor accepted by
   * this method must be greater than 1 or null, default is 2.0.
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   */
  public static long computeExpoBackoffDecorr(long cap, long base, int attempts, long sleep) {
    long result = min(cap, Randoms.randomLong(base, (sleep <= 0 ? base : sleep) * 3));
    return result > 0 ? result : cap;
  }

  /**
   * Use Exponential Backoff Equal Jitter algorithm to compute the delay. The backoff factor
   * accepted by this method must be greater than 1 or null, default is 2.0.
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   */
  public static long computeExpoBackoffEqualJitter(double backoffFactor, long cap, long base,
      int attempts) {
    long expoBackoff = computeExpoBackoff(backoffFactor, cap, base, attempts);
    long temp = expoBackoff >>> 1;
    return temp + Randoms.randomLong(temp);
  }

  /**
   * Use Exponential Backoff Full Jitter algorithm to compute the delay. The backoff factor accepted
   * by this method must be greater than 1 or null, default is 2.0.
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   */
  public static long computeExpoBackoffFullJitter(double backoffFactor, long cap, long base,
      int attempts) {
    long expoBackoff = computeExpoBackoff(backoffFactor, cap, base, attempts);
    return Randoms.randomLong(expoBackoff);
  }

  /**
   * Try to execute and return a result
   *
   * @param <T> the result type
   * @param times Total number of attempts to execute the {@code runnable}
   * @param interval Time interval between two attempts
   * @param runnable The work unit, the parameter of this is the number of retry attempts
   * @return The result
   */
  public static <T> T execute(int times, Duration interval, Function<Integer, T> runnable) {
    return new Retryer().times(times).noBackoff(interval).execute(runnable);
  }

  /**
   * Try to execute runnable
   *
   * @param times Total number of attempts to execute the {@code runnable}
   * @param interval Time interval between two attempts
   * @param runnable The work unit
   */
  public static void execute(int times, Duration interval, Runnable runnable) {
    new Retryer().times(times).noBackoff(interval).execute(runnable);
  }

  /**
   * Try to execute and supply a result
   *
   * @param <T> The result type
   * @param times Total number of attempts to execute the {@code supplier}
   * @param interval Time interval between two attempts
   * @param supplier The work unit
   * @return The result
   */
  public static <T> T execute(int times, Duration interval, Supplier<T> supplier) {
    return new Retryer().times(times).noBackoff(interval).execute(supplier);
  }

  /**
   * Create retryer for more granular control retry mechanism.
   *
   * @return retryer
   */
  public static Retryer retryer() {
    return new Retryer();
  }

  /**
   * corant-shared
   *
   * @author bingo 10:41:29
   *
   */
  @SuppressWarnings("unchecked")
  public static abstract class AbstractRetryer<R extends AbstractRetryer<R>> {

    protected int times = 8;
    protected RetryInterval interval = RetryInterval.noBackoff(Duration.ofMillis(2000L));
    protected Set<Class<? extends Throwable>> retryOn = emptySet();
    protected Set<Class<? extends Throwable>> abortOn = emptySet();
    protected BiPredicate<Integer, Throwable> predicate;
    protected Supplier<Boolean> breaker = () -> true;

    protected static void logRetry(Throwable e, int attempt, long wait) {
      if (wait >= 0) {
        logger.log(Level.WARNING, e, () -> String.format(
            "An exception [%s] occurred during execution, enter the retry phase, the retry attempt [%s], interval [%s], message : [%s]",
            e.getClass().getName(), attempt, wait, defaultString(e.getMessage(), "unknown")));
      } else {
        logger.log(Level.WARNING, e, () -> String.format(
            "An exception [%s] occurred during execution, interrupt retry, the retry attempt [%s], message : [%s]",
            e.getClass().getName(), attempt, defaultString(e.getMessage(), "unknown")));
      }
    }

    public R abortOn(Class<? extends Throwable>... on) {
      this.abortOn = immutableSetOf(on);
      return (R) this;
    }

    public R abortOn(Set<Class<? extends Throwable>> on) {
      this.abortOn = on != null ? unmodifiableSet(shouldNotNull(on)) : emptySet();
      return (R) this;
    }

    /**
     * The breaker, it will be called every attempt, if the breaker returns false, it will not enter
     * the next attempt. Mainly used to terminate the attempt early when the external environment
     * changes.
     *
     * @param breaker force interrupt retry process.
     */
    public R breaker(final Supplier<Boolean> breaker) {
      if (breaker != null) {
        this.breaker = breaker;
      }
      return (R) this;
    }

    /**
     * @see RetryInterval#expoBackoff(Duration, Duration, Double)
     */
    public R expoBackoff(final Duration interval, final Duration maxInterval,
        final Double backoffFactor) {
      this.interval = RetryInterval.expoBackoff(interval, maxInterval, backoffFactor);
      return (R) this;
    }

    /**
     * @see RetryInterval#expoBackoffDecorr(Duration, Duration, Double)
     */
    public R expoBackoffDecorr(final Duration interval, final Duration maxInterval,
        final Double backoffFactor) {
      this.interval = RetryInterval.expoBackoffDecorr(interval, maxInterval, backoffFactor);
      return (R) this;
    }

    /**
     * @see RetryInterval#expoBackoffEqualJitter(Duration, Duration, Double)
     */
    public R expoBackoffEqualJitter(final Duration interval, final Duration maxInterval,
        final Double backoffFactor) {
      this.interval = RetryInterval.expoBackoffEqualJitter(interval, maxInterval, backoffFactor);
      return (R) this;
    }

    /**
     * @see RetryInterval#expoBackoffFullJitter(Duration, Duration, Double)
     */
    public R expoBackoffFullJitter(final Duration interval, final Duration maxInterval,
        final Double backoffFactor) {
      this.interval = RetryInterval.expoBackoffFullJitter(interval, maxInterval, backoffFactor);
      return (R) this;
    }

    /**
     * Set the retry interval
     */
    public R interval(RetryInterval interval) {
      this.interval = shouldNotNull(interval);
      return (R) this;
    }

    /**
     * No backoff, the same interval between each retry.
     *
     * @param interval the retry interval
     */
    public R noBackoff(final Duration interval) {
      this.interval = RetryInterval.noBackoff(interval);
      return (R) this;
    }

    public R predicate(BiPredicate<Integer, Throwable> predicate) {
      this.predicate = predicate;
      return (R) this;
    }

    public R retryOn(Class<? extends Throwable>... on) {
      this.retryOn = immutableSetOf(on);
      return (R) this;
    }

    public R retryOn(Set<Class<? extends Throwable>> on) {
      this.retryOn = on != null ? unmodifiableSet(shouldNotNull(on)) : emptySet();
      return (R) this;
    }

    /**
     * Set the max retry times, if the given times is less than or equals zero means that retry for
     * ever.
     *
     * @param times the max retry times
     */
    public R times(final int times) {
      this.times = times < 0 ? Integer.MAX_VALUE : max(1, times);
      return (R) this;
    }

    protected boolean continueIfThrowable(Throwable throwable, int attempts) {
      if (predicate != null) {
        return predicate.test(attempts, throwable);
      }
      Class<? extends Throwable> throwableClass = throwable.getClass();
      if (isEmpty(abortOn)) {
        return isEmpty(retryOn)
            || retryOn.stream().anyMatch(c -> c.isAssignableFrom(throwableClass));
      } else {
        return abortOn.stream().noneMatch(c -> c.isAssignableFrom(throwableClass))
            && (isEmpty(retryOn)
                || retryOn.stream().anyMatch(c -> c.isAssignableFrom(throwableClass)));
      }
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:24:56
   *
   */
  public static class AsynchronousRetryer extends AbstractRetryer<AsynchronousRetryer> {

    protected final ScheduledExecutorService executor;

    public AsynchronousRetryer(ScheduledExecutorService executor) {
      this.executor = executor;
    }

    public <T> Future<T> execute(Callable<T> callable) {
      final SimpleFuture<T> future = new SimpleFuture<>();
      executor.schedule(new AsynchronousRetryTask<>(callable, future, this, new AtomicInteger(0)),
          0, TimeUnit.MILLISECONDS);
      return future;
    }

    public void execute(Runnable runnable) {
      final SimpleFuture<Object> future = new SimpleFuture<>();
      final Callable<Object> callable = () -> {
        runnable.run();
        return null;
      };
      final AsynchronousRetryTask<Object> task =
          new AsynchronousRetryTask<>(callable, future, this, new AtomicInteger(0));
      executor.schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    public <T> Future<T> execute(Supplier<T> supplier) {
      final SimpleFuture<T> future = new SimpleFuture<>();
      executor.schedule(
          new AsynchronousRetryTask<>(supplier::get, future, this, new AtomicInteger(0)), 0,
          TimeUnit.MILLISECONDS);
      return future;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午9:22:07
   *
   */
  public static class AsynchronousRetryTask<T> implements Runnable {
    final SimpleFuture<T> future;
    final Callable<T> callable;
    final AsynchronousRetryer retryer;
    final AtomicInteger attempts;

    AsynchronousRetryTask(Callable<T> callable, SimpleFuture<T> future, AsynchronousRetryer retryer,
        AtomicInteger attempts) {
      this.future = future;
      this.callable = callable;
      this.retryer = retryer;
      this.attempts = attempts;
    }

    @Override
    public void run() {
      Throwable throwable = null;
      T result = null;
      try {
        if (preRun()) {
          result = callable.call();
        }
      } catch (Throwable t) {
        throwable = t;
      } finally {
        postRun(result, throwable);
      }
    }

    protected void postRun(T result, Throwable t) {
      if (t != null) {
        final int currentAttempts = attempts.get();
        try {
          // check the breaker
          if (retryer.breaker != null && !retryer.breaker.get()) {
            future.cancel(true);
            return;
          }
          // check predicates and exception types
          if (!retryer.continueIfThrowable(t, currentAttempts)) {
            if (future.isCancelled()) {
              return;
            } else {
              future.failure(t);
              AbstractRetryer.logRetry(t, currentAttempts, -1);
              return;
            }
          }
          // check max retry times
          if (currentAttempts < retryer.times) {
            final long delayMillis = retryer.interval.calculateMillis(currentAttempts);
            final AsynchronousRetryTask<T> next =
                new AsynchronousRetryTask<>(callable, future, retryer, attempts);
            retryer.executor.schedule(next, delayMillis, TimeUnit.MILLISECONDS);
            AbstractRetryer.logRetry(t, currentAttempts, delayMillis);
          } else if (!future.isCancelled()) {
            AbstractRetryer.logRetry(t, currentAttempts, -1);
            future.failure(t);
          }
        } catch (Throwable e) {
          // exception occurred on checking
          AbstractRetryer.logRetry(t, currentAttempts, -1);
          future.failure(t);
        }
      } else if (!future.isCancelled()) {
        future.success(result);
      }
    }

    protected boolean preRun() {
      if (retryer.breaker != null && !retryer.breaker.get()) {
        future.cancel(true);
        return false;
      }
      attempts.incrementAndGet();
      return true;
    }
  }

  /**
   * corant-shared
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   * @author bingo 9:37:51
   *
   */
  public enum BackoffAlgorithm {
    NONE, EXPO, EXPO_DECORR, EXPO_EQUAL_JITTER, EXPO_FULL_JITTER
  }

  /**
   * corant-shared
   *
   * @author bingo 11:23:16
   *
   */
  public static class DefaultRetryInterval implements RetryInterval, Serializable {

    private static final long serialVersionUID = 1960222218031605834L;

    protected final BackoffAlgorithm backoffAlgo;
    protected final Duration interval;
    protected final Duration maxInterval;
    protected final double backoffFactor;
    protected volatile long base;

    public DefaultRetryInterval(BackoffAlgorithm backoffAlgo, Duration interval,
        Duration maxInterval, Double backoffFactor) {
      this.backoffAlgo = shouldNotNull(backoffAlgo, "The retry interval algo can't null!");
      shouldBeTrue(interval != null && interval.toMillis() >= 0, "The retry interval error!");
      this.interval = interval;
      if (backoffAlgo != BackoffAlgorithm.NONE) {
        shouldBeTrue(maxInterval != null && maxInterval.toMillis() >= 0
            && maxInterval.compareTo(interval) > 0, "The retry interval error!");
        if (backoffFactor != null) {
          shouldBeTrue(backoffFactor.doubleValue() > 1,
              "The retry backoff must greater then 1 or null");
        }
      }
      this.maxInterval = maxInterval;
      this.backoffFactor = defaultObject(backoffFactor, 2.0);
      reset();
    }

    @Override
    public long calculateMillis(int attempts) {
      if (attempts <= 1) {
        return base;
      }
      switch (backoffAlgo) {
        case EXPO:
          return computeExpoBackoff(backoffFactor, maxInterval.toMillis(), base, attempts);
        case EXPO_DECORR:
          synchronized (this) {
            return base = computeExpoBackoffDecorr(maxInterval.toMillis(), interval.toMillis(),
                attempts, base);
          }
        case EXPO_EQUAL_JITTER:
          return computeExpoBackoffEqualJitter(backoffFactor, maxInterval.toMillis(), base,
              attempts);
        case EXPO_FULL_JITTER:
          return computeExpoBackoffFullJitter(backoffFactor, maxInterval.toMillis(), base,
              attempts);
        default:
          return base;
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      DefaultRetryInterval other = (DefaultRetryInterval) obj;
      if (backoffAlgo != other.backoffAlgo) {
        return false;
      }
      if (Double.doubleToLongBits(backoffFactor) != Double.doubleToLongBits(other.backoffFactor)) {
        return false;
      }
      if (interval == null) {
        if (other.interval != null) {
          return false;
        }
      } else if (!interval.equals(other.interval)) {
        return false;
      }
      if (maxInterval == null) {
        return other.maxInterval == null;
      } else {
        return maxInterval.equals(other.maxInterval);
      }
    }

    public BackoffAlgorithm getBackoffAlgo() {
      return backoffAlgo;
    }

    public double getBackoffFactor() {
      return backoffFactor;
    }

    public long getBase() {
      return base;
    }

    public Duration getInterval() {
      return interval;
    }

    public Duration getMaxInterval() {
      return maxInterval;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (backoffAlgo == null ? 0 : backoffAlgo.hashCode());
      long temp;
      temp = Double.doubleToLongBits(backoffFactor);
      result = prime * result + (int) (temp ^ temp >>> 32);
      result = prime * result + (interval == null ? 0 : interval.hashCode());
      return prime * result + (maxInterval == null ? 0 : maxInterval.hashCode());
    }

    @Override
    public DefaultRetryInterval reset() {
      base = interval.toMillis();
      return this;
    }
  }

  /**
   * corant-shared
   *
   * @author bingo 10:41:29
   *
   */
  public static class Retryer extends AbstractRetryer<Retryer> {

    public <T> T execute(Function<Integer, T> executable) {
      return doExecute(executable);
    }

    public void execute(final Runnable runnable) {
      doExecute(i -> {
        runnable.run();
        return null;
      });
    }

    public <T> T execute(final Supplier<T> supplier) {
      return doExecute(i -> forceCast(supplier.get()));
    }

    protected <T> T doExecute(final Function<Integer, T> executable) {
      shouldNotNull(executable);
      interval.reset();
      int remaining = times;
      int attempts = 0;
      while (breaker.get()) {
        try {
          return executable.apply(attempts);
        } catch (RuntimeException | AssertionError e) {
          if (!continueIfThrowable(e, attempts)) {
            logRetry(e, attempts, -1);
            throw e;
          }
          remaining--;
          attempts++;
          if (remaining > 0) {
            long wait = interval.calculateMillis(attempts);
            logRetry(e, attempts, wait);
            try {
              if (wait > 0) {
                Thread.sleep(wait);
              }
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              ie.addSuppressed(e);
              throw new CorantRuntimeException(ie);
            }
          } else {
            throw new CorantRuntimeException(e);
          }
        } // end catch
      }
      return null;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 0:47:22
   *
   */
  @FunctionalInterface
  public interface RetryInterval {

    /**
     * Use Exponential Backoff algorithm to compute the delay. The backoff factor accepted by this
     * method must be greater than 1 or null, default is 2.0.
     *
     * @see <a href=
     *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
     *      Backoff And Jitter</a>
     */
    static DefaultRetryInterval expoBackoff(final Duration interval, final Duration maxInterval,
        final Double backoffFactor) {
      return new DefaultRetryInterval(BackoffAlgorithm.EXPO, interval, maxInterval, backoffFactor);
    }

    /**
     * Use Exponential Backoff Decorr algorithm to compute the delay. The backoff factor accepted by
     * this method must be greater than 1 or null, default is 2.0.
     *
     * @see <a href=
     *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
     *      Backoff And Jitter</a>
     */
    static DefaultRetryInterval expoBackoffDecorr(final Duration interval,
        final Duration maxInterval, final Double backoffFactor) {
      return new DefaultRetryInterval(BackoffAlgorithm.EXPO_DECORR, interval, maxInterval,
          backoffFactor);
    }

    /**
     * Use Exponential Backoff Equal Jitter algorithm to compute the delay. The backoff factor
     * accepted by this method must be greater than 1 or null, default is 2.0.
     *
     * @see <a href=
     *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
     *      Backoff And Jitter</a>
     */
    static DefaultRetryInterval expoBackoffEqualJitter(final Duration interval,
        final Duration maxInterval, final Double backoffFactor) {
      return new DefaultRetryInterval(BackoffAlgorithm.EXPO_EQUAL_JITTER, interval, maxInterval,
          backoffFactor);
    }

    /**
     * Use Exponential Backoff Full Jitter algorithm to compute the delay. The backoff factor
     * accepted by this method must be greater than 1 or null, default is 2.0.
     *
     * @see <a href=
     *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
     *      Backoff And Jitter</a>
     */
    static DefaultRetryInterval expoBackoffFullJitter(final Duration interval,
        final Duration maxInterval, final Double backoffFactor) {
      return new DefaultRetryInterval(BackoffAlgorithm.EXPO_FULL_JITTER, interval, maxInterval,
          backoffFactor);
    }

    /**
     * No backoff, the same interval between each retry.
     *
     * @param interval the retry interval duration
     */
    static DefaultRetryInterval noBackoff(Duration interval) {
      shouldBeTrue(interval != null && interval.toMillis() >= 0);
      return new DefaultRetryInterval(BackoffAlgorithm.NONE, interval, null, 0.0);
    }

    /**
     * Calculate millis with attempts
     *
     * @param attempts the current retry attempts
     * @return calculateMillis
     */
    long calculateMillis(int attempts);

    /**
     * Use for clear
     *
     * @return reset
     */
    default RetryInterval reset() {
      return this;
    }
  }
}

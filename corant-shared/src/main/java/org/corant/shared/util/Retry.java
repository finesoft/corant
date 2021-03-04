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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Objects.min;
import static org.corant.shared.util.Strings.defaultString;
import java.io.Serializable;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-shared
 *
 * @author bingo 10:02:42
 *
 */
public class Retry {

  static final Logger logger = Logger.getLogger(Retry.class.toString());

  /**
   * Use Exponential Backoff algorithm to compute the delay. The backoff factor accepted by this
   * method must be greater than 1 or null, default is 2.0.
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   *
   * @param backoffFactor
   * @param cap
   * @param base
   * @param attempts
   * @return computeExpoBackoff
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
   *
   * @param cap
   * @param base
   * @param attempts
   * @param sleep
   * @return computeExpoBackoffDecorr
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
   *
   * @param backoffFactor
   * @param cap
   * @param base
   * @param attempts
   * @return computeExpoBackoffEqualJitter
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
   *
   * @param backoffFactor
   * @param cap
   * @param base
   * @param attempts
   * @return computeExpoBackoffFullJitter
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

    protected DefaultRetryInterval(BackoffAlgorithm backoffAlgo, Duration interval,
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
          return base =
              computeExpoBackoffDecorr(maxInterval.toMillis(), interval.toMillis(), attempts, base);
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
      result = prime * result + (maxInterval == null ? 0 : maxInterval.hashCode());
      return result;
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
  public static class Retryer {

    private int times = 8;
    private RetryInterval interval = RetryInterval.noBackoff(Duration.ofMillis(2000L));
    private BiConsumer<Integer, Throwable> thrower;
    private Supplier<Boolean> breaker = () -> true;

    /**
     * The breaker, it will be called every attempt, if the breaker returns false, it will not enter
     * the next attempt. Mainly used to terminate the attempt early when the external environment
     * changes.
     *
     * @param breaker
     * @return breaker
     */
    public Retryer breaker(final Supplier<Boolean> breaker) {
      if (breaker != null) {
        this.breaker = breaker;
      }
      return this;
    }

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

    /**
     * @see RetryInterval#expoBackoff(Duration, Duration, Double)
     * @param interval
     * @param maxInterval
     * @param backoffFactor
     * @return expoBackoff
     */
    public Retryer expoBackoff(final Duration interval, final Duration maxInterval,
        final Double backoffFactor) {
      this.interval = RetryInterval.expoBackoff(interval, maxInterval, backoffFactor);
      return this;
    }

    /**
     * @see RetryInterval#expoBackoffDecorr(Duration, Duration, Double)
     *
     * @param interval
     * @param maxInterval
     * @param backoffFactor
     * @return expoBackoffDecorr
     */
    public Retryer expoBackoffDecorr(final Duration interval, final Duration maxInterval,
        final Double backoffFactor) {
      this.interval = RetryInterval.expoBackoffDecorr(interval, maxInterval, backoffFactor);
      return this;
    }

    /**
     * @see RetryInterval#expoBackoffEqualJitter(Duration, Duration, Double)
     *
     * @param interval
     * @param maxInterval
     * @param backoffFactor
     * @return expoBackoffEqualJitter
     */
    public Retryer expoBackoffEqualJitter(final Duration interval, final Duration maxInterval,
        final Double backoffFactor) {
      this.interval = RetryInterval.expoBackoffEqualJitter(interval, maxInterval, backoffFactor);
      return this;
    }

    /**
     * @see RetryInterval#expoBackoffFullJitter(Duration, Duration, Double)
     * @param interval
     * @param maxInterval
     * @param backoffFactor
     * @return expoBackoffFullJitter
     */
    public Retryer expoBackoffFullJitter(final Duration interval, final Duration maxInterval,
        final Double backoffFactor) {
      this.interval = RetryInterval.expoBackoffFullJitter(interval, maxInterval, backoffFactor);
      return this;
    }

    public Retryer interval(RetryInterval interval) {
      this.interval = shouldNotNull(interval);
      return this;
    }

    /**
     * No backoff, the same interval between each retry.
     *
     * @param interval
     * @return noBackoff
     */
    public Retryer noBackoff(final Duration interval) {
      this.interval = RetryInterval.noBackoff(interval);
      return this;
    }

    /**
     * The exception handler, if the thrower throw any exceptions the attempt will be breaked.
     *
     * @param thrower
     * @return thrower
     */
    public Retryer thrower(final BiConsumer<Integer, Throwable> thrower) {
      this.thrower = thrower;
      return this;
    }

    /**
     * The max try times
     *
     * @param times
     * @return times
     */
    public Retryer times(final int times) {
      this.times = max(1, times);
      return this;
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
          if (thrower != null) {
            thrower.accept(attempts, e);
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

    void logRetry(Throwable e, int attempt, long wait) {
      logger.log(Level.WARNING, e, () -> String.format(
          "An exception [%s] occurred during execution, enter the retry phase, the retry attempt [%s], interval [%s], message : [%s]",
          e.getClass().getName(), attempt, wait, defaultString(e.getMessage(), "unknown")));
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
     *
     * @param interval
     * @param maxInterval
     * @param backoffFactor
     * @return expoBackoff
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
     * @param interval
     * @param maxInterval
     * @param backoffFactor
     * @return expoBackoffDecorr
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
     * @param interval
     * @param maxInterval
     * @param backoffFactor
     * @return expoBackoffEqualJitter
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
     * @param interval
     * @param maxInterval
     * @param backoffFactor
     * @return expoBackoffFullJitter
     */
    static DefaultRetryInterval expoBackoffFullJitter(final Duration interval,
        final Duration maxInterval, final Double backoffFactor) {
      return new DefaultRetryInterval(BackoffAlgorithm.EXPO_FULL_JITTER, interval, maxInterval,
          backoffFactor);
    }

    /**
     * No backoff, the same interval between each retry.
     *
     * @param interval
     * @return noBackoff
     */
    static DefaultRetryInterval noBackoff(Duration interval) {
      shouldBeTrue(interval != null && interval.toMillis() >= 0);
      return new DefaultRetryInterval(BackoffAlgorithm.NONE, interval, null, 0.0);
    }

    /**
     * Calculate millis with attempts
     *
     * @param attempts
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

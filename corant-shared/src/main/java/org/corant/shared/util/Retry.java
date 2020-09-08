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
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.defaultString;
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
   * Use Exponential backoff + jitter algorithm to compute the delay
   *
   * @param backoffFactor
   * @param base
   * @param attempt
   * @return computeInterval
   */
  public static long computeInterval(double backoffFactor, long base, int attempt) {
    if (backoffFactor > 1) {
      long interval = base * (int) Math.pow(backoffFactor, attempt);
      return Randoms.randomLong(interval);
    } else {
      return base;
    }
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
    return new Retryer().times(times).interval(interval).execute(runnable);
  }

  /**
   * Try to execute runnable
   *
   * @param times Total number of attempts to execute the {@code runnable}
   * @param interval Time interval between two attempts
   * @param runnable The work unit
   */
  public static void execute(int times, Duration interval, Runnable runnable) {
    new Retryer().times(times).interval(interval).execute(runnable);
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
    return new Retryer().times(times).interval(interval).execute(supplier);
  }

  public static Retryer retryer() {
    return new Retryer();
  }

  /**
   * corant-shared
   *
   * @author bingo 10:41:29
   *
   */
  public static class Retryer {

    private int times = 8;
    private long interval = 2000L;
    private double backoff = 0.0;
    private BiConsumer<Integer, Throwable> thrower;
    private Supplier<Boolean> breaker = () -> true;

    /**
     * Use Exponential backoff + jitter algorithm to compute the delay. The backoff factor accepted
     * by this method must be greater than 1 or equal to 0, equal to 0 means that the algorithm is
     * not enabled, and the default is 0.
     *
     * @see <a href=
     *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
     *      backoff + jitter</a>
     * @param backoff
     * @return backoff
     */
    public Retryer backoff(double backoff) {
      if (backoff > 0) {
        shouldBeTrue(backoff > 1);
      }
      this.backoff = backoff;
      return this;
    }

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
      return doExecute((i) -> forceCast(supplier.get()));
    }

    public Retryer interval(final Duration interval) {
      this.interval = interval == null || interval.toMillis() < 0 ? 0L : interval.toMillis();
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

    public Retryer times(final int times) {
      this.times = max(1, times);
      return this;
    }

    protected <T> T doExecute(final Function<Integer, T> executable) {
      shouldNotNull(executable);
      int remaining = times;
      int attempt = 0;
      while (breaker.get()) {
        try {
          return executable.apply(attempt);
        } catch (RuntimeException | AssertionError e) {
          if (thrower != null) {
            thrower.accept(attempt, e);
          }
          remaining--;
          attempt++;
          if (remaining > 0) {
            long wait = computeInterval(backoff, interval, attempt);
            logRetry(e, attempt, wait);
            try {
              if (wait > 0) {
                Thread.sleep(wait);
              }
            } catch (InterruptedException ie) {
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

}

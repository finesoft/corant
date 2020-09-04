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

  public static <T> Retryer<T> retryer() {
    return new Retryer<>();
  }

  public static <T> T execute(int times, Duration interval, Function<Integer, T> runnable) {
    return new Retryer<T>().times(times).interval(interval).task(runnable).execute();
  }

  public static void execute(int times, Duration interval, Runnable runnable) {
    new Retryer<>().times(times).interval(interval).task(runnable).execute();
  }

  public static <T> T execute(int times, Duration interval, Supplier<T> supplier) {
    return new Retryer<T>().times(times).interval(interval).task(supplier).execute();
  }

  /**
   * corant-shared
   *
   * @author bingo 10:41:29
   *
   */
  public static class Retryer<T> {

    private int times = 8;
    private long interval = 2000L;
    private double backoff = 0.0;
    private BiConsumer<Integer, Throwable> thrower;
    private Function<Integer, T> executable;

    private int attempt = 0;

    public Retryer<T> backoff(double backoff) {
      this.backoff = backoff;
      return this;
    }

    public T execute() {

      while (true) {

        try {

          return executable.apply(attempt);

        } catch (RuntimeException | AssertionError e) {

          if (thrower != null) {
            thrower.accept(attempt, e);
          }

          times--;
          attempt++;

          if (times > 0) {
            logRetry(e);
            try {
              if (interval > 0) {
                Thread.sleep(computeInterval(backoff, interval, attempt));
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
    }

    public Retryer<T> interval(Duration interval) {
      this.interval = interval == null || interval.toMillis() < 0 ? 0L : interval.toMillis();
      return this;
    }

    public Retryer<T> task(Function<Integer, T> executable) {
      shouldNotNull(executable);
      this.executable = executable;
      return this;
    }

    public Retryer<T> task(Runnable runnable) {
      shouldNotNull(runnable);
      this.executable = (i) -> {
        runnable.run();
        return null;
      };
      return this;
    }

    public Retryer<T> task(Supplier<T> supplier) {
      shouldNotNull(supplier);
      this.executable = (i) -> {
        return forceCast(supplier.get());
      };
      return this;
    }

    public Retryer<T> thrower(BiConsumer<Integer, Throwable> thrower) {
      this.thrower = thrower;
      return this;
    }

    public Retryer<T> times(int times) {
      this.times = max(1, times);
      return this;
    }

    long computeInterval(double backoffFactor, long base, int attempt) {
      if (backoffFactor > 0) {
        long interval = base * (int) Math.pow(2, attempt);
        return Randoms.randomLong(interval);
      } else {
        return base;
      }
    }

    void logRetry(Throwable e) {
      logger.log(Level.WARNING, e, () -> String.format(
          "An exception [%s] occurred during execution, enter the retry phase, the retry attempts [%s], interval [%s], message : [%s]",
          e.getClass().getName(), attempt, interval, defaultString(e.getMessage(), "unknown")));
    }
  }

}

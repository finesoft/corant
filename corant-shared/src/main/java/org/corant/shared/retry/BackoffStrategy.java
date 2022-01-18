/*
 * Copyright (c) 2013-2022, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.shared.retry;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.min;
import java.time.Duration;
import org.corant.shared.util.Randoms;

/**
 * corant-shared
 *
 * @author bingo 上午10:29:44
 *
 */
@FunctionalInterface
public interface BackoffStrategy {

  BackoffStrategy NON_BACKOFF_STRATEGY = new NonBackoffStrategy();

  long computeBackoffMillis(int attempts);

  default long computeBackoffMillis(RetryContext context) {
    return computeBackoffMillis(context.getAttempts());
  }

  default void reset() {}

  /**
   * corant-shared
   *
   * @see <a href=
   *      "https://aws.amazon.com/cn/blogs/architecture/exponential-backoff-and-jitter/">Exponential
   *      Backoff And Jitter</a>
   * @author bingo 9:37:51
   *
   */
  enum BackoffAlgorithm {
    NON, FIXED, EXPO, EXPO_DECORR, EXPO_EQUAL_JITTER, EXPO_FULL_JITTER
  }

  /**
   * corant-shared
   *
   * @author bingo 上午10:35:18
   *
   */
  class BackoffStrategyBuilder {
    BackoffAlgorithm algorithm = BackoffAlgorithm.NON;
    double factor = 2.0;
    Duration baseDuration;
    Duration maxDuration;

    public BackoffStrategyBuilder algorithm(BackoffAlgorithm algorithm) {
      this.algorithm = defaultObject(algorithm, BackoffAlgorithm.NON);
      return this;
    }

    public BackoffStrategyBuilder baseDuration(Duration baseDuration) {
      this.baseDuration = baseDuration;
      return this;
    }

    public BackoffStrategy build() {
      switch (algorithm) {
        case EXPO:
          return new CappedExpoBackoffStrategy(factor, baseDuration, maxDuration);
        case FIXED:
          return new FixedBackoffStrategy(baseDuration);
        case EXPO_DECORR:
          return new CappedExpoDecorrJitterBackoffStrategy(factor, baseDuration, maxDuration);
        case EXPO_EQUAL_JITTER:
          return new CappedExpoEqualJitterBackoffStrategy(factor, baseDuration, maxDuration);
        case EXPO_FULL_JITTER:
          return new CappedExpoFullJitterBackoffStrategy(factor, baseDuration, maxDuration);
        default:
          return BackoffStrategy.NON_BACKOFF_STRATEGY;
      }
    }

    public BackoffStrategyBuilder factor(double factor) {
      this.factor = factor;
      return this;
    }

    public BackoffStrategyBuilder maxDuration(Duration maxDuration) {
      this.maxDuration = maxDuration;
      return this;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午10:32:30
   *
   */
  class CappedExpoBackoffStrategy implements BackoffStrategy {

    protected volatile double factor = 2.0;
    protected volatile long baseDuration;
    protected volatile long maxDuration = Long.MAX_VALUE;

    public CappedExpoBackoffStrategy() {}

    public CappedExpoBackoffStrategy(double factor, Duration baseDuration, Duration maxDuration) {
      factor(factor).durations(baseDuration, maxDuration);
    }

    @Override
    public long computeBackoffMillis(int attempts) {
      long result = min(maxDuration, baseDuration * (long) Math.pow(factor, attempts));
      return result > 0 ? result : maxDuration;
    }

    public CappedExpoBackoffStrategy durations(Duration baseDuration, Duration maxDuration) {
      shouldBeTrue(baseDuration != null && baseDuration.toMillis() >= 0,
          "The back-off strategy base duration error!");
      this.baseDuration = baseDuration.toMillis();
      if (maxDuration != null) {
        shouldBeTrue(maxDuration.toMillis() >= baseDuration.toMillis(),
            "The back-off strategy max duration error!");
        this.maxDuration = maxDuration.toMillis();
      }
      return this;
    }

    public CappedExpoBackoffStrategy factor(Double factor) {
      if (factor != null) {
        shouldBeTrue(factor > 1, "The retry backoff must greater then 1 or null");
        this.factor = factor;
      } else {
        this.factor = 2.0;
      }
      return this;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午11:27:28
   *
   */
  class CappedExpoDecorrJitterBackoffStrategy extends CappedExpoBackoffStrategy {

    protected volatile long sleep;

    public CappedExpoDecorrJitterBackoffStrategy() {}

    public CappedExpoDecorrJitterBackoffStrategy(double factor, Duration baseDuration,
        Duration maxDuration) {
      super(factor, baseDuration, maxDuration);
    }

    @Override
    public long computeBackoffMillis(int attempts) {
      sleep = min(maxDuration, Randoms.randomLong(baseDuration, sleep * 3));
      return sleep;
    }

    @Override
    public CappedExpoDecorrJitterBackoffStrategy durations(Duration baseDuration,
        Duration maxDuration) {
      super.durations(baseDuration, maxDuration);
      reset();
      return this;
    }

    @Override
    public void reset() {
      sleep = baseDuration;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午10:31:43
   *
   */
  class CappedExpoEqualJitterBackoffStrategy extends CappedExpoBackoffStrategy {

    public CappedExpoEqualJitterBackoffStrategy() {}

    public CappedExpoEqualJitterBackoffStrategy(double factor, Duration baseDuration,
        Duration maxDuration) {
      super(factor, baseDuration, maxDuration);
    }

    @Override
    public long computeBackoffMillis(int attempts) {
      long expoBackoff = super.computeBackoffMillis(attempts);
      long result = expoBackoff >>> 1;
      result = result + Randoms.randomLong(result);
      return result > 0 ? result : maxDuration;
    }

    @Override
    public CappedExpoEqualJitterBackoffStrategy durations(Duration baseDuration,
        Duration maxDuration) {
      super.durations(baseDuration, maxDuration);
      return this;
    }

    @Override
    public CappedExpoEqualJitterBackoffStrategy factor(Double factor) {
      super.factor(factor);
      return this;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午10:31:43
   *
   */
  class CappedExpoFullJitterBackoffStrategy extends CappedExpoBackoffStrategy {

    public CappedExpoFullJitterBackoffStrategy() {}

    public CappedExpoFullJitterBackoffStrategy(double factor, Duration baseDuration,
        Duration maxDuration) {
      super(factor, baseDuration, maxDuration);
    }

    @Override
    public long computeBackoffMillis(int attempts) {
      return Randoms.randomLong(super.computeBackoffMillis(attempts));
    }

    @Override
    public CappedExpoFullJitterBackoffStrategy durations(Duration baseDuration,
        Duration maxDuration) {
      super.durations(baseDuration, maxDuration);
      return this;
    }

    @Override
    public CappedExpoFullJitterBackoffStrategy factor(Double factor) {
      super.factor(factor);
      return this;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午12:01:00
   *
   */
  class FixedBackoffStrategy implements BackoffStrategy {

    protected volatile long duration;

    public FixedBackoffStrategy() {}

    public FixedBackoffStrategy(Duration duration) {
      duration(duration);
    }

    @Override
    public long computeBackoffMillis(int attempts) {
      return duration;
    }

    public FixedBackoffStrategy duration(Duration duration) {
      shouldBeTrue(duration != null && duration.toMillis() >= 0, "The retry duration error!");
      this.duration = duration.toMillis();
      return this;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午10:31:43
   *
   */
  class NonBackoffStrategy implements BackoffStrategy {

    @Override
    public long computeBackoffMillis(int attempts) {
      return 0L;
    }

  }
}

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

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Sets.immutableSetOf;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.corant.shared.util.Objects;

/**
 * corant-shared
 *
 * @author bingo 下午9:00:30
 *
 */
@FunctionalInterface
public interface RetryStrategy {

  RetryStrategy NON_RETRY_STRATEGY = new NonRetryStrategy();

  default RetryStrategy and(Predicate<RetryContext> other) {
    shouldNotNull(other);
    return ctx -> test(ctx) && other.test(ctx);
  }

  default RetryStrategy and(RetryStrategy other) {
    shouldNotNull(other);
    return ctx -> test(ctx) && other.test(ctx);
  }

  default RetryStrategy negate() {
    return ctx -> !test(ctx);
  }

  default RetryStrategy or(Predicate<RetryContext> other) {
    shouldNotNull(other);
    return ctx -> test(ctx) || other.test(ctx);
  }

  default RetryStrategy or(RetryStrategy other) {
    shouldNotNull(other);
    return ctx -> test(ctx) || other.test(ctx);
  }

  boolean test(RetryContext ctx);

  /**
   * corant-shared
   *
   * @author bingo 下午8:56:51
   *
   */
  class AlwaysRetryStrategy implements RetryStrategy {

    @Override
    public boolean test(RetryContext context) {
      return true;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午9:09:29
   *
   */
  class MaxAttemptsRetryStrategy implements RetryStrategy {

    protected int maxAttempts;

    public MaxAttemptsRetryStrategy() {}

    public MaxAttemptsRetryStrategy(int maxAttempts) {
      maxAttempts(maxAttempts);
    }

    public MaxAttemptsRetryStrategy maxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts < 0 ? Integer.MAX_VALUE : max(1, maxAttempts);
      return this;
    }

    @Override
    public boolean test(RetryContext context) {
      return context.getAttempts() <= maxAttempts;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午8:55:44
   *
   */
  class NonRetryStrategy implements RetryStrategy {

    @Override
    public boolean test(RetryContext context) {
      return false;
    }

  }

  /**
   * corant-shared
   *
   * @author bingo 下午9:06:47
   *
   */
  class ThrowableClassifierRetryStrategy implements RetryStrategy {

    protected Set<Class<? extends Throwable>> retryOn = emptySet();
    protected Set<Class<? extends Throwable>> abortOn = emptySet();

    public ThrowableClassifierRetryStrategy() {}

    public ThrowableClassifierRetryStrategy(Set<Class<? extends Throwable>> retryOn,
        Set<Class<? extends Throwable>> abortOn) {
      retryOn(retryOn).abortOn(abortOn);
    }

    @SuppressWarnings("unchecked")
    public ThrowableClassifierRetryStrategy abortOn(Class<? extends Throwable>... on) {
      abortOn = immutableSetOf(on);
      return this;
    }

    public ThrowableClassifierRetryStrategy abortOn(Collection<Class<? extends Throwable>> on) {
      if (on != null) {
        Set<Class<? extends Throwable>> use = new LinkedHashSet<>(on);
        use.removeIf(Objects::isNull);
        abortOn = unmodifiableSet(use);
      } else {
        abortOn = emptySet();
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    public ThrowableClassifierRetryStrategy retryOn(Class<? extends Throwable>... on) {
      retryOn = immutableSetOf(on);
      return this;
    }

    public ThrowableClassifierRetryStrategy retryOn(Collection<Class<? extends Throwable>> on) {
      if (on != null) {
        Set<Class<? extends Throwable>> use = new LinkedHashSet<>(on);
        use.removeIf(Objects::isNull);
        retryOn = unmodifiableSet(use);
      } else {
        retryOn = emptySet();
      }
      return this;
    }

    @Override
    public boolean test(RetryContext context) {
      if (context.getLastThrowable() == null) {
        return true;
      }
      Class<? extends Throwable> throwableClass = context.getLastThrowable().getClass();
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
   *
   * corant-shared
   *
   * @author bingo 下午9:18:46
   *
   */
  class TimeoutRetryStrategy implements RetryStrategy {

    protected Duration duration = Duration.ZERO;

    public TimeoutRetryStrategy() {}

    public TimeoutRetryStrategy(Duration duration) {
      timeout(duration);
    }

    @Override
    public boolean test(RetryContext context) {
      if (duration.equals(Duration.ZERO)) {
        return true;
      }
      Instant timeout = context.getStartTime().plusMillis(duration.toMillis());
      return Instant.now().compareTo(timeout) <= 0;
    }

    public TimeoutRetryStrategy timeout(Duration duration) {
      this.duration = duration == null || duration.isNegative() ? Duration.ZERO : duration;
      return this;
    }

  }
}

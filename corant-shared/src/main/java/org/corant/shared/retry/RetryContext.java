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

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * corant-shared
 *
 * @author bingo 下午11:38:14
 *
 */
public interface RetryContext {

  int getAttempts();

  Throwable getLastThrowable();

  Instant getStartTime();

  /**
   * corant-shared
   *
   * @author bingo 下午11:38:10
   *
   */
  class DefaultRetryContext implements RetryContext {

    protected final AtomicInteger attemptsCounter = new AtomicInteger(0);
    protected volatile Throwable lastThrowable;
    protected volatile Instant startTime;

    @Override
    public int getAttempts() {
      return attemptsCounter.get();
    }

    @Override
    public Throwable getLastThrowable() {
      return lastThrowable;
    }

    @Override
    public Instant getStartTime() {
      return startTime;
    }

    protected AtomicInteger getAttemptsCounter() {
      return attemptsCounter;
    }

    protected DefaultRetryContext initialize() {
      attemptsCounter.set(0);
      lastThrowable = null;
      startTime = Instant.now();
      return this;
    }

    protected DefaultRetryContext setLastThrowable(Throwable lastThrowable) {
      this.lastThrowable = lastThrowable;
      return this;
    }

    protected DefaultRetryContext setStartTime(Instant startTime) {
      this.startTime = startTime;
      return this;
    }

  }
}

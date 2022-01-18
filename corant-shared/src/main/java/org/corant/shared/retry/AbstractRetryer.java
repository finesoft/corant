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

import static java.util.Collections.unmodifiableList;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * corant-shared
 *
 * @author bingo 下午10:46:11
 *
 */
public abstract class AbstractRetryer<R extends AbstractRetryer<R>> implements Retryer {

  protected final Logger logger = Logger.getLogger(getClass().getName());

  protected final List<RetryListener> retryListeners = new ArrayList<>();

  protected volatile BackoffStrategy backoffStrategy = BackoffStrategy.NON_BACKOFF_STRATEGY;

  protected volatile RetryStrategy retryStrategy = RetryStrategy.NON_RETRY_STRATEGY;

  protected volatile RetryPrecondition retryPrecondition = RetryPrecondition.NON_PRECONDITION;

  protected volatile RecoveryCallback recoveryCallback;

  @SuppressWarnings("unchecked")
  public R addRetryListener(RetryListener listener) {
    retryListeners.add(shouldNotNull(listener, "The retry listener can't null!"));
    return (R) this;
  }

  @SuppressWarnings("unchecked")
  public R backoffStrategy(BackoffStrategy backoffStrategy) {
    if (backoffStrategy != null) {
      this.backoffStrategy = backoffStrategy;
    }
    return (R) this;
  }

  @Override
  public BackoffStrategy getBackoffStrategy() {
    return backoffStrategy;
  }

  @Override
  public RecoveryCallback getRecoveryCallback() {
    return recoveryCallback;
  }

  @Override
  public Collection<? extends RetryListener> getRetryListeners() {
    return unmodifiableList(retryListeners);
  }

  @Override
  public RetryPrecondition getRetryPrecondition() {
    return retryPrecondition;
  }

  @Override
  public RetryStrategy getRetryStrategy() {
    return retryStrategy;
  }

  @SuppressWarnings("unchecked")
  public R recoveryCallback(RecoveryCallback recoveryCallback) {
    this.recoveryCallback = recoveryCallback;
    return (R) this;
  }

  @SuppressWarnings("unchecked")
  public R removeRetryListenerIf(Predicate<RetryListener> predicate) {
    retryListeners.removeIf(predicate);
    return (R) this;
  }

  @SuppressWarnings("unchecked")
  public R retryPrecondition(RetryPrecondition retryPrecondition) {
    if (retryPrecondition != null) {
      this.retryPrecondition = retryPrecondition;
    }
    return (R) this;
  }

  @SuppressWarnings("unchecked")
  public R retryStrategy(RetryStrategy retryStrategy) {
    if (retryStrategy != null) {
      this.retryStrategy = retryStrategy;
    }
    return (R) this;
  }

  protected void emitOnRetry(RetryContext context) {
    // if (context.getAttempts() > 0) {
    getRetryListeners().forEach(listener -> {
      try {
        listener.onRetry(getContext());
      } catch (Throwable ex) {
        logger.log(Level.SEVERE, ex, () -> String.format(
            "Retry listener %s handling occurred error, but the retry process continued to execute!",
            listener));
      }
    });
    // }
  }

}

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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Functions.asCallable;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.retry.RetryContext.DefaultRetryContext;
import org.corant.shared.ubiquity.Futures.SimpleFuture;

public class AsynchronousRetryer extends AbstractRetryer<AsynchronousRetryer> {

  protected final ScheduledExecutorService executor;
  protected final DefaultRetryContext context = new DefaultRetryContext();

  public AsynchronousRetryer(ScheduledExecutorService executor) {
    this.executor = shouldNotNull(executor);
  }

  public <T> Future<T> execute(Callable<T> callable) {
    shouldNotNull(callable);
    final SimpleFuture<T> future = new SimpleFuture<>();
    context.initialize();
    executor.schedule(new AsynchronousRetryTask<>(future, callable, this), 0,
        TimeUnit.MILLISECONDS);
    return future;
  }

  @Override
  public void execute(Runnable runnable) {
    shouldNotNull(runnable);
    context.initialize();
    final AsynchronousRetryTask<Object> task =
        new AsynchronousRetryTask<>(new SimpleFuture<>(), asCallable(runnable), this);
    executor.schedule(task, 0, TimeUnit.MILLISECONDS);
  }

  @Override
  public DefaultRetryContext getContext() {
    return context;
  }

  @Override
  public <T> T invoke(Supplier<T> supplier) {
    shouldNotNull(supplier);
    final SimpleFuture<T> future = new SimpleFuture<>();
    context.initialize();
    executor.schedule(new AsynchronousRetryTask<>(future, asCallable(supplier), this), 0,
        TimeUnit.MILLISECONDS);
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new CorantRuntimeException(e);
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
    final DefaultRetryContext context;
    final RetryStrategy retryStrategy;
    final BackoffStrategy backoffStrategy;
    final RetryPrecondition retryPrecondition;
    final Collection<? extends RetryListener> retryListeners;
    final RecoveryCallback recoverCallback;

    public AsynchronousRetryTask(SimpleFuture<T> future, Callable<T> callable,
        AsynchronousRetryer retryer) {
      this.future = future;
      this.callable = callable;
      this.retryer = retryer;
      this.context = retryer.getContext();
      this.retryStrategy = retryer.getRetryStrategy();
      this.backoffStrategy = retryer.getBackoffStrategy();
      this.retryPrecondition = retryer.getRetryPrecondition();
      this.retryListeners = defaultObject(retryer.getRetryListeners(), Collections.emptyList());
      this.recoverCallback = retryer.getRecoveryCallback();
    }

    @Override
    public void run() {
      try {
        if (retryPrecondition.test(context)) {
          if (!future.isDone()) {
            retryer.emitOnRetry(context);
            context.getAttemptsCounter().incrementAndGet();
            T result = callable.call();
            if (future.success(result)) {
              retryer.logger.fine(() -> String.format(
                  "Executed successfully, it has been tried %s times, no more retries.",
                  context.getAttempts()));
            }
          } else if (future.isCancelled()) {
            retryer.logger.fine(() -> String.format(
                "Execution was cancelled by caller, it has been tried %s times, no more retries.",
                context.getAttempts()));
          }
        } else {
          retryer.logger.info(() -> String.format(
              "Cancel execution, unable to meet preconditions, it has been tried %s times",
              context.getAttempts()));
        }
      } catch (Throwable throwable) {
        Throwable currThrowable = throwable;
        try {
          if (retryStrategy.test(context.setLastThrowable(currThrowable))) {
            long wait = backoffStrategy.computeBackoffMillis(context);
            retryer.logger.log(Level.WARNING, currThrowable, () -> String.format(
                "An error occurred in the retrying execution, it has been tried %s times, wait for %s milliseconds and continue to try to execute!",
                context.getAttempts(), wait));
            AsynchronousRetryTask<T> next = new AsynchronousRetryTask<>(future, callable, retryer);
            retryer.executor.schedule(next, wait, TimeUnit.MILLISECONDS);
            currThrowable = null;
          }
        } catch (Exception ee) {
          ee.addSuppressed(currThrowable);
          currThrowable = ee;
        } finally {
          if (currThrowable != null) {
            if (recoverCallback != null) {
              try {
                retryer.logger.log(Level.WARNING, currThrowable, () -> String.format(
                    "An error occurred in the execution, it has been tried %s times, the retrying execution was interrupted, started to execute the recovery callback.",
                    context.getAttempts()));
                T result = forceCast(recoverCallback.recover(context));
                if (future.success(result)) {
                  retryer.logger.log(Level.INFO,
                      () -> "Retry recovery callback executed successfully.");
                }
              } catch (Exception e) {
                e.addSuppressed(currThrowable);
                currThrowable = e;
              }
            }
            if (!future.isDone()) {
              retryer.logger.log(Level.WARNING, currThrowable, () -> String.format(
                  "An error occurred in the execution, it has been tried %s times, no more retries.",
                  context.getAttempts()));
              future.failure(currThrowable);
            }
          }
        }
      }
    }

  }

}

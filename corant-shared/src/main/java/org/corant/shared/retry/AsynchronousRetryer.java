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

import static java.lang.String.format;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Functions.asCallable;
import static org.corant.shared.util.Objects.forceCast;
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
  protected DefaultRetryContext context = new DefaultRetryContext();

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
    } catch (InterruptedException ie) {
      throw new CorantRuntimeException(ie);
    } catch (ExecutionException ee) {
      Throwable t = ee.getCause();
      if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      }
      if (t instanceof Error) {
        throw (Error) t;
      }
      throw new CorantRuntimeException(t);
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
    final RecoveryCallback recoverCallback;

    public AsynchronousRetryTask(SimpleFuture<T> future, Callable<T> callable,
        AsynchronousRetryer retryer) {
      this.future = future;
      this.callable = callable;
      this.retryer = retryer;
      context = retryer.getContext();
      retryStrategy = retryer.getRetryStrategy();
      backoffStrategy = retryer.getBackoffStrategy();
      retryPrecondition = retryer.getRetryPrecondition();
      recoverCallback = retryer.getRecoveryCallback();
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
              retryer.logger.fine(() -> format(
                  "Executed successfully, it has been tried %s times, no more retries.",
                  context.getAttempts()));
            }
          } else if (future.isCancelled()) {
            retryer.logger.fine(() -> format(
                "Execution was cancelled by caller, it has been tried %s times, no more retries.",
                context.getAttempts()));
          }
        } else {
          retryer.logger.info(() -> format(
              "Cancel execution, unable to meet preconditions, it has been tried %s times",
              context.getAttempts()));
        }
      } catch (Throwable throwable) {
        Throwable currentThrowable = throwable;
        try {
          currentThrowable = testContinue(currentThrowable);
        } catch (Exception ee) {
          ee.addSuppressed(currentThrowable);
          currentThrowable = ee;
        } finally {
          if (currentThrowable != null) {
            if (recoverCallback != null) {
              try {
                recover(currentThrowable);
              } catch (Exception e) {
                e.addSuppressed(currentThrowable);
                currentThrowable = e;
              }
            }
            if (!future.isDone()) {
              retryer.logger.log(Level.WARNING, currentThrowable, () -> format(
                  "An error occurred in the execution, it has been tried %s times, no more retries.",
                  context.getAttempts()));
              future.failure(currentThrowable);
            }
          }
        }
      }
    }

    protected void recover(Throwable currentThrowable) throws Exception {
      retryer.logger.log(Level.WARNING, currentThrowable, () -> format(
          "An error occurred in the execution, it has been tried %s times, the retrying execution was interrupted, started to execute the recovery callback.",
          context.getAttempts()));
      T result = forceCast(recoverCallback.recover(context));
      if (future.success(result)) {
        retryer.logger.log(Level.INFO, () -> "Retry recovery callback executed successfully.");
      }
    }

    protected Throwable testContinue(Throwable currentThrowable) {
      if (retryStrategy.test(context.setLastThrowable(currentThrowable))) {
        long wait = backoffStrategy.computeBackoffMillis(context);
        retryer.logger.log(Level.WARNING, currentThrowable, () -> format(
            "An error occurred in the retrying execution, it has been tried %s times, wait for %s milliseconds and continue to try to execute!",
            context.getAttempts(), wait));
        AsynchronousRetryTask<T> next = new AsynchronousRetryTask<>(future, callable, retryer);
        retryer.executor.schedule(next, wait, TimeUnit.MILLISECONDS);
        return null;
      }
      return currentThrowable;
    }

  }

}

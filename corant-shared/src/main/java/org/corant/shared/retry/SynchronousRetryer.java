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
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Throwables.asUncheckedException;
import static org.corant.shared.util.Throwables.rethrow;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.corant.shared.retry.RetryContext.DefaultRetryContext;

/**
 * corant-shared
 *
 * @author bingo 14:17:50
 */
public class SynchronousRetryer extends AbstractRetryer<SynchronousRetryer> {

  protected DefaultRetryContext context = new DefaultRetryContext();

  public <T> T execute(Callable<T> callable) {
    shouldNotNull(callable);
    context.initialize();
    return doExecute(i -> {
      try {
        return callable.call();
      } catch (Exception e) {
        throw asUncheckedException(e);
      }
    });
  }

  public <T> T execute(Function<Integer, T> function) {
    shouldNotNull(function);
    context.initialize();
    return doExecute(function);
  }

  @Override
  public void execute(Runnable runnable) {
    shouldNotNull(runnable);
    context.initialize();
    doExecute(i -> {
      runnable.run();
      return null;
    });
  }

  @Override
  public RetryContext getContext() {
    return context;
  }

  @Override
  public <T> T invoke(Supplier<T> supplier) {
    shouldNotNull(supplier);
    context.initialize();
    return doExecute(i -> supplier.get());
  }

  protected <T> T doExecute(final Function<Integer, T> callable) {
    for (;;) {
      if (!getRetryPrecondition().test(context)) {
        logger.info(() -> format(
            "Cancel execution, unable to meet preconditions, it has been tried %s times",
            context.getAttempts()));
        return null;
      }
      try {
        emitOnRetry(context);
        int attempts = increaseAttempts();
        T result = callable.apply(attempts);
        logger.fine(() -> format(
            "Executed successfully, it has been tried %s times, no more retries.", attempts));
        return result;
      } catch (Throwable throwable) {
        Throwable currentThrowable = throwable;
        try {
          currentThrowable = testContinue(currentThrowable);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          ie.addSuppressed(currentThrowable);
          currentThrowable = ie;
        } catch (Exception ee) {
          ee.addSuppressed(currentThrowable);
          currentThrowable = ee;
        } finally {
          if (currentThrowable != null) {
            if (getRecoveryCallback() != null) {
              try {
                return recover(currentThrowable);
              } catch (Exception e) {
                e.addSuppressed(currentThrowable);
                currentThrowable = e;
              }
            }
            logger.log(Level.WARNING, currentThrowable, () -> format(
                "An error occurred in the execution, it has been tried %s times, and the retrying execution was interrupted.",
                context.getAttempts()));
            rethrow(currentThrowable);
          }
        }
      }
    }
  }

  protected int increaseAttempts() {
    return context.getAttemptsCounter().incrementAndGet();
  }

  protected <T> T recover(Throwable currentThrowable) throws Exception {
    logger.log(Level.WARNING, currentThrowable, () -> format(
        "An error occurred in the execution, it has been tried %s times, the retrying execution was interrupted, started to execute the recovery callback.",
        context.getAttempts()));
    T result = forceCast(getRecoveryCallback().recover(context));
    logger.log(Level.INFO, () -> "Retry recovery callback executed successfully.");
    return result;
  }

  protected Throwable testContinue(Throwable currentThrowable) throws InterruptedException {
    if (getRetryStrategy().test(context.setLastThrowable(currentThrowable))) {
      long wait = getBackoffStrategy().computeBackoffMillis(context);
      logger.log(Level.WARNING, currentThrowable, () -> format(
          "An error occurred in the retrying execution, it has been tried %s times, wait for %s milliseconds and continue to try to execute!",
          context.getAttempts(), wait));
      if (wait > 0) {
        Thread.sleep(wait);
      }
      return null;
    }
    return currentThrowable;
  }
}

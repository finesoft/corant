/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.context.concurrent.executor;

import static java.lang.String.format;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * corant-context
 *
 * @author bingo 下午8:25:51
 */
public class ScheduledThreadPoolExecutorx extends ScheduledThreadPoolExecutor {

  /**
   * @see ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int)
   */
  public ScheduledThreadPoolExecutorx(int corePoolSize) {
    super(corePoolSize);
  }

  /**
   * @see ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, RejectedExecutionHandler)
   */
  public ScheduledThreadPoolExecutorx(int corePoolSize, RejectedExecutionHandler handler) {
    super(corePoolSize, handler);
  }

  /**
   * @see ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, ThreadFactory)
   */
  public ScheduledThreadPoolExecutorx(int corePoolSize, ThreadFactory threadFactory) {
    super(corePoolSize, threadFactory);
  }

  /**
   * @see ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, ThreadFactory,
   *      RejectedExecutionHandler)
   */
  public ScheduledThreadPoolExecutorx(int corePoolSize, ThreadFactory threadFactory,
      RejectedExecutionHandler handler) {
    super(corePoolSize, threadFactory, handler);
  }

  public Future<?> scheduleWithDynamicDelay(DynamicDelayRunnable runnable) {
    if (runnable == null) {
      throw new NullPointerException();
    }
    if (isShutdown()) {
      return null;
    }
    return new DynamicDelayRunnableWrapper<>(runnable).doSchedule();
  }

  public interface DynamicDelayRunnable extends Runnable {
    long getDelay();
  }

  protected class DynamicDelayRunnableWrapper<V> implements Runnable, Future<V> {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final DynamicDelayRunnable task;
    private volatile Future<?> future;
    private volatile boolean cancelled = false;

    public DynamicDelayRunnableWrapper(DynamicDelayRunnable task) {
      this.task = task;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      boolean retval = !isDone();
      cancelled = true;
      if (future != null) {
        future.cancel(mayInterruptIfRunning);
      }
      return retval;
    }

    public DynamicDelayRunnableWrapper<V> doSchedule() {
      long delay = task.getDelay();
      if (delay <= 0) {
        logger.finer(() -> format("Task will not get rescheduled as delay is %s", delay));
      } else {
        future = schedule(this, delay, TimeUnit.MILLISECONDS);
        if (cancelled) {
          future.cancel(true);
        }
      }
      return this;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
      return null;
    }

    @Override
    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return null;
    }

    public Future<?> getFuture() {
      return future;
    }

    @Override
    public boolean isCancelled() {
      return cancelled;
    }

    @Override
    public boolean isDone() {
      return cancelled || future == null || future.isDone();
    }

    @Override
    public void run() {
      try {
        if (cancelled) {
          if (future != null) {
            future.cancel(true);
          }
          return;
        }
        if (future != null && future.isCancelled()) {
          return;
        }
        task.run();
      } catch (Throwable t) {
        logger.log(Level.SEVERE, t, () -> format("Failed running task %s", task));
      }
      if (cancelled) {
        if (future != null) {
          future.cancel(true);
        }
        return;
      }
      if (future != null && future.isCancelled()) {
        return;
      }
      doSchedule();
    }

  }

}

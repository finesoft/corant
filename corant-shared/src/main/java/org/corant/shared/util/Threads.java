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
import static org.corant.shared.util.Functions.uncheckedRunner;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Throwing.ThrowingRunnable;

/**
 * corant-shared
 *
 * @author bingo 下午2:55:59
 *
 */
public class Threads {

  public static final String DAEMON_THREAD_NAME_PREFIX = Names.CORANT.concat("-daemon");
  static final AtomicLong DAEMON_THREAD_ID = new AtomicLong(0);

  public static <V> V callInDaemon(Callable<V> callable) {
    final FutureTask<V> future = new FutureTask<>(shouldNotNull(callable));
    runInDaemon(future);
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static <V> V callInDaemon(String threadName, Callable<V> callable) {
    final FutureTask<V> future = new FutureTask<>(shouldNotNull(callable));
    runInDaemon(threadName, future);
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public static ThreadFactory daemonThreadFactory(final String threadName) {
    return daemonThreadFactory(threadName, Thread.NORM_PRIORITY);
  }

  public static ThreadFactory daemonThreadFactory(final String threadName, final int priority) {
    return r -> {
      // FIXME context propagation
      Thread thread = new Thread(r);
      thread.setName(threadName + '-' + DAEMON_THREAD_ID.incrementAndGet());
      thread.setDaemon(true);
      thread.setPriority(priority);
      return thread;
    };
  }

  public static void delayRunInDaemon(String threadName, Duration delay, Runnable runner) {
    daemonThreadFactory(threadName).newThread(() -> {
      if (delay != null) {
        try {
          TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
      runner.run();
    }).start();
  }

  public static <E extends Throwable> void delayRunInDaemonx(Duration delay,
      ThrowingRunnable<E> runner) {
    delayRunInDaemonx(DAEMON_THREAD_NAME_PREFIX, delay, runner);
  }

  public static <E extends Throwable> void delayRunInDaemonx(String threadName, Duration delay,
      ThrowingRunnable<E> runner) {
    delayRunInDaemon(threadName, delay, uncheckedRunner(runner));
  }

  public static void runInDaemon(Runnable runner) {
    runInDaemon(DAEMON_THREAD_NAME_PREFIX, runner);
  }

  public static void runInDaemon(String threadName, Runnable runner) {
    delayRunInDaemon(threadName, null, runner);
  }

  public static <E extends Throwable> void runInDaemonx(String threadName,
      ThrowingRunnable<E> runner) {
    delayRunInDaemonx(threadName, null, runner);
  }

  public static <E extends Throwable> void runInDaemonx(ThrowingRunnable<E> runner) {
    delayRunInDaemonx(null, runner);
  }

  public static void tryThreadSleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      // Noop! just try...
    }
  }

}

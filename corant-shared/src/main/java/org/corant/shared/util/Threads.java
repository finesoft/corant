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

import static org.corant.shared.util.Functions.uncheckedRunner;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Throwing.ThrowingRunnable;

/**
 * corant-shared
 *
 * @author bingo 下午2:55:59
 *
 */
public class Threads {

  public static final String DAEMON_THREAD_NAME_PREFIX = Names.CORANT.concat("-daemon-");
  static final AtomicLong DAEMON_THREAD_ID = new AtomicLong(0);

  public static <E extends Throwable> void delayRunInDaemon(Duration delay,
      ThrowingRunnable<E> runner) {
    Thread daemonThread = new Thread(() -> {
      if (delay != null) {
        try {
          TimeUnit.MILLISECONDS.sleep(delay.toMillis());
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
      uncheckedRunner(runner).run();
    }, DAEMON_THREAD_NAME_PREFIX + DAEMON_THREAD_ID);
    daemonThread.setDaemon(true);
    daemonThread.start();
  }

  public static void runDaemon(Runnable runner) {
    runDaemon(DAEMON_THREAD_NAME_PREFIX + DAEMON_THREAD_ID.incrementAndGet(), runner);
  }

  public static void runDaemon(String threadName, Runnable runner) {
    Thread daemonThread = new Thread(runner, threadName);
    daemonThread.setDaemon(true);
    daemonThread.start();
  }

  public static <E extends Throwable> void runInDaemon(ThrowingRunnable<E> runner) {
    delayRunInDaemon(null, runner);
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

/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jms.shared;

import static org.corant.shared.util.Objects.max;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.corant.shared.util.Randoms;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Threads;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 10:03:53
 */
public class Test {

  static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(8);
  static final LinkedBlockingDeque<Task> queue = new LinkedBlockingDeque<>();
  static final Thread arrager = new Thread(() -> {
    for (;;) {
      try {
        Task task = queue.take();
        final long ms = max(task.getNextDelay(), 0L);
        executorService.schedule(new Task(new TaskRunnable(task.name, ms), null), ms,
            TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  });

  public static void main(String[] args) throws InterruptedException {
    for (int i = 0; i < 4; i++) {
      executorService.schedule(new Task(new TaskRunnable("task-" + i, 0L), null), 0,
          TimeUnit.MILLISECONDS);
    }
    arrager.start();
    Thread.currentThread().join();
  }

  public static class Task extends FutureTask<Void> {

    final String name;

    public Task(Runnable runnable, Void result) {
      super(runnable, result);
      name = ((TaskRunnable) runnable).name;
    }

    public long getNextDelay() {
      return Randoms.randomLong(-1000, 1000);
    }

    @Override
    protected void done() {
      super.done();
      queue.add(this);
    }

  }

  public static class TaskRunnable implements Runnable {

    final String name;
    final long delay;

    public TaskRunnable(String name, long delay) {
      this.name = name;
      this.delay = delay;
    }

    @Override
    public void run() {
      Threads.tryThreadSleep(Randoms.randomLong(0, 80));
      System.out.println(Strings.joinIfNotBlank("\t", Thread.currentThread().getName(),
          Instant.now(), UUID.randomUUID().toString(), name, delay));
    }

  }
}

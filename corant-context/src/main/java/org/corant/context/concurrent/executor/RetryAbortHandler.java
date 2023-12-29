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

import static org.corant.shared.util.Objects.defaultObject;
import java.time.Duration;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.corant.shared.util.Threads;

/**
 * corant-context
 *
 * @author bingo 上午11:00:38
 */
public class RetryAbortHandler implements RejectedExecutionHandler {

  static final Logger logger = Logger.getLogger(RetryAbortHandler.class.getName());

  final String name;
  final Duration retryDelay;

  public RetryAbortHandler(String name, Duration retryDelay) {
    this.name = name;
    this.retryDelay = defaultObject(retryDelay, () -> Duration.ofSeconds(4L));
  }

  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    if (r != null && !executor.isShutdown() && !executor.isTerminated()
        && !executor.isTerminating()) {
      logger.info(() -> String.format(
          "The task %s was rejected from the executor %s in the executor service %s for the first time and needs to be tried once after %s.",
          r.toString(), executor.toString(), name, retryDelay));
      Threads.runInDaemonx(() -> {
        if (executor.getQueue().offer(r, retryDelay.toMillis(), TimeUnit.MILLISECONDS)) {
          logger.info(() -> String.format(
              "Succeeded in adding the task %s back to the queue of the executor %s in the executor service %s",
              r.toString(), executor.toString(), name));
        } else {
          logger.warning(() -> String.format(
              "Failed to re-add the task %s to the queue of the executor %s in the executor service %s",
              r.toString(), executor.toString(), name));
        }
      });
    }
  }

}

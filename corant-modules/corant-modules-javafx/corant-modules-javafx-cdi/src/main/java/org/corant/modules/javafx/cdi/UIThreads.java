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
package org.corant.modules.javafx.cdi;

import static org.corant.shared.ubiquity.Throwing.uncheckedRunner;
import static org.corant.shared.util.Assertions.shouldNoneNull;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import org.corant.shared.exception.CorantRuntimeException;
import javafx.application.Platform;

/**
 * corant-modules-javafx-cdi
 *
 * @author bingo 下午11:41:07
 *
 */
public class UIThreads {

  public static <T> T callInsideUIAndWait(final Callable<T> callable)
      throws InterruptedException, ExecutionException {
    final FutureTask<T> future = new FutureTask<>(shouldNotNull(callable));
    Platform.runLater(future);
    return future.get();
  }

  public static <T> T callInsideUISync(final Callable<T> callable) {
    FutureTask<T> ft = new FutureTask<>(shouldNotNull(callable));
    runInsideUISync(ft);
    try {
      return ft.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new CorantRuntimeException(e,
          "An error occurred while executing a task inside the UI thread");
    }
  }

  public static boolean isUIThread() {
    return Platform.isFxApplicationThread();
  }

  public static void runInsideUIAndWait(final Runnable runnable)
      throws InterruptedException, ExecutionException {
    final FutureTask<Void> future = new FutureTask<>(shouldNotNull(runnable), null);
    Platform.runLater(future);
    future.get();
  }

  public static void runInsideUIAsync(final Runnable runnable) {
    Platform.runLater(shouldNotNull(runnable));
  }

  public static void runInsideUISync(final Runnable runnable) {
    shouldNotNull(runnable);
    if (isUIThread()) {
      runnable.run();
    } else {
      final FutureTask<Void> task = new FutureTask<>(uncheckedRunner(runnable::run), null);
      Platform.runLater(task);
      try {
        task.get();
      } catch (InterruptedException | ExecutionException e) {
        throw new CorantRuntimeException(e);
      }
    }
  }

  public static void runOutsideUI(final ExecutorService executorService, final Runnable runnable) {
    shouldNotNull(runnable);
    if (!isUIThread()) {
      runnable.run();
    } else {
      shouldNotNull(executorService).execute(uncheckedRunner(runnable::run));
    }
  }

  public static void runOutsideUIAsync(final ExecutorService executorService,
      final Runnable runnable) {
    shouldNoneNull(runnable, executorService);
    executorService.execute(uncheckedRunner(runnable::run));
  }
}

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
package org.corant.modules.jms.shared.receive;

import java.util.concurrent.ScheduledFuture;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午6:19:43
 *
 */
public class MessageReceivingTaskExecution {

  private final ScheduledFuture<?> future;
  private final MessageReceivingTask task;
  private final Runnable cancelledHook;

  /**
   * @param future
   * @param task
   */
  public MessageReceivingTaskExecution(ScheduledFuture<?> future, MessageReceivingTask task) {
    this.future = future;
    this.task = task;
    cancelledHook = null;
  }

  /**
   * @param future
   * @param task
   * @param cancelledHook
   */
  public MessageReceivingTaskExecution(ScheduledFuture<?> future, MessageReceivingTask task,
      Runnable cancelledHook) {
    this.future = future;
    this.task = task;
    this.cancelledHook = cancelledHook;
  }

  public boolean cancel() {
    boolean cancelled = task.cancel();
    if (cancelledHook != null) {
      cancelledHook.run();
    }
    future.cancel(false);
    return cancelled;
  }

  public ScheduledFuture<?> getFuture() {
    return future;
  }
}

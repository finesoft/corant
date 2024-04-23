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
import org.corant.modules.jms.receive.ManagedMessageReceivingTask;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午6:19:43
 */
public class MessageReceivingTaskExecution {

  protected final ManagedMessageReceivingTask task;
  protected final Runnable cancelledHook;
  protected final MessageReceivingMetaData metaData;

  protected volatile ScheduledFuture<?> future;
  protected volatile boolean cancelled;

  public MessageReceivingTaskExecution(MessageReceivingMetaData metaData,
      ManagedMessageReceivingTask task) {
    this(metaData, null, task, null);
  }

  public MessageReceivingTaskExecution(MessageReceivingMetaData metaData, ScheduledFuture<?> future,
      ManagedMessageReceivingTask task) {
    this(metaData, future, task, null);
  }

  public MessageReceivingTaskExecution(MessageReceivingMetaData metaData, ScheduledFuture<?> future,
      ManagedMessageReceivingTask task, Runnable cancelledHook) {
    this.metaData = metaData;
    this.future = future;
    this.task = task;
    this.cancelledHook = cancelledHook;
  }

  public boolean cancel() {
    cancelled = true;
    boolean cancelled = task.cancel();
    if (cancelledHook != null) {
      cancelledHook.run();
    }
    final ScheduledFuture<?> f = future;
    if (f != null) {
      f.cancel(false);
    }
    return cancelled;
  }

  public ScheduledFuture<?> getFuture() {
    return future;
  }

  protected void updateFuture(ScheduledFuture<?> future) {
    if (!cancelled) {
      this.future = future;
    }
  }
}

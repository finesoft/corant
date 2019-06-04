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
package org.corant.suites.concurrency;

import javax.enterprise.concurrent.ManageableThread;

/**
 * corant-suites-concurrency
 *
 * @author bingo 下午7:02:00
 *
 */
public abstract class AbstractManagedThread extends Thread implements ManageableThread {

  volatile boolean shutdown = false;
  final long startTime = System.currentTimeMillis();

  public AbstractManagedThread(Runnable target) {
    super(target);
  }

  public long getStartTime() {
    return startTime;
  }

  @Override
  public boolean isShutdown() {
    return shutdown;
  }

  public void shutdown() {
    shutdown = true;
  }

  abstract String getTaskIdentityName();

  abstract long getTaskRunTime(long now);

  abstract boolean isTaskHung(long now);

}

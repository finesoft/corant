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
package org.corant.context.concurrent.executor;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedExecutorServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;

/**
 * corant-context
 *
 * @author bingo 上午10:22:51
 *
 */
public class DefaultManagedExecutorService extends ManagedExecutorServiceImpl {

  static final Logger logger = Logger.getLogger(DefaultManagedExecutorService.class.getName());

  final Duration awaitTermination;

  /**
   * @param name
   * @param managedThreadFactory
   * @param hungTaskThreshold
   * @param longRunningTasks
   * @param corePoolSize
   * @param maxPoolSize
   * @param keepAliveTime
   * @param keepAliveTimeUnit
   * @param threadLifeTime
   * @param awaitTermination
   * @param contextService
   * @param rejectPolicy
   * @param queue
   */
  public DefaultManagedExecutorService(String name, ManagedThreadFactoryImpl managedThreadFactory,
      long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize,
      long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime,
      Duration awaitTermination, ContextServiceImpl contextService, RejectPolicy rejectPolicy,
      BlockingQueue<Runnable> queue) {
    super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize,
        maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy,
        queue);
    this.awaitTermination = awaitTermination;
    if (rejectPolicy == RejectPolicy.RETRY_ABORT) {
      threadPoolExecutor.setRejectedExecutionHandler(new RetryAbortHandler(name));
    } else {
      threadPoolExecutor.setRejectedExecutionHandler(new AbortHandler(name));
    }
    // TODO Auto-generated constructor stub
  }

  /**
   * @param name
   * @param managedThreadFactory
   * @param hungTaskThreshold
   * @param longRunningTasks
   * @param corePoolSize
   * @param maxPoolSize
   * @param keepAliveTime
   * @param keepAliveTimeUnit
   * @param threadLifeTime
   * @param awaitTermination
   * @param queueCapacity
   * @param contextService
   * @param rejectPolicy
   */
  public DefaultManagedExecutorService(String name, ManagedThreadFactoryImpl managedThreadFactory,
      long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize,
      long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime,
      Duration awaitTermination, int queueCapacity, ContextServiceImpl contextService,
      RejectPolicy rejectPolicy) {
    super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize,
        maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity,
        contextService, rejectPolicy);
    this.awaitTermination = awaitTermination;
    if (rejectPolicy == RejectPolicy.RETRY_ABORT) {
      threadPoolExecutor.setRejectedExecutionHandler(new RetryAbortHandler(name));
    } else {
      threadPoolExecutor.setRejectedExecutionHandler(new AbortHandler(name));
    }
  }

  void stop() {
    try {
      super.shutdown();
      if (awaitTermination != null
          && !super.awaitTermination(awaitTermination.toMillis(), TimeUnit.MILLISECONDS)) {
        logger.log(Level.WARNING,
            () -> String.format("Shutdown managed executor service %s timeout!", name));
      }
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, e,
          () -> String.format("Shutdown managed executor service %s occurred error!", name));
    }
  }
}

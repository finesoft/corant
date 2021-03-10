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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedExecutorServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;

/**
 * corant-suites-concurrency
 *
 * @author bingo 上午10:22:51
 *
 */
public class DefaultManagedExecutorService extends ManagedExecutorServiceImpl {

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
   * @param contextService
   * @param rejectPolicy
   * @param queue
   */
  public DefaultManagedExecutorService(String name, ManagedThreadFactoryImpl managedThreadFactory,
      long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize,
      long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime,
      ContextServiceImpl contextService, RejectPolicy rejectPolicy, BlockingQueue<Runnable> queue) {
    super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize,
        maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy,
        queue);
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
   * @param queueCapacity
   * @param contextService
   * @param rejectPolicy
   */
  public DefaultManagedExecutorService(String name, ManagedThreadFactoryImpl managedThreadFactory,
      long hungTaskThreshold, boolean longRunningTasks, int corePoolSize, int maxPoolSize,
      long keepAliveTime, TimeUnit keepAliveTimeUnit, long threadLifeTime, int queueCapacity,
      ContextServiceImpl contextService, RejectPolicy rejectPolicy) {
    super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize,
        maxPoolSize, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueCapacity,
        contextService, rejectPolicy);
    // TODO Auto-generated constructor stub
  }

}

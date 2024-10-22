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

import static java.lang.String.format;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Throwables.rethrow;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.context.concurrent.ConcurrentExtension;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;

/**
 * corant-context
 *
 * @author bingo 上午10:23:58
 */
public class DefaultManagedScheduledExecutorService extends ManagedScheduledExecutorServiceImpl {

  static final Logger logger =
      Logger.getLogger(DefaultManagedScheduledExecutorService.class.getName());

  final Duration awaitTermination;

  public DefaultManagedScheduledExecutorService(String name,
      ManagedThreadFactoryImpl managedThreadFactory, long hungTaskThreshold,
      boolean longRunningTasks, int corePoolSize, long keepAliveTime, TimeUnit keepAliveTimeUnit,
      long threadLifeTime, Duration awaitTermination, ContextServiceImpl contextService,
      RejectPolicy rejectPolicy, Duration retryDelay) {
    super(name, managedThreadFactory, hungTaskThreshold, longRunningTasks, corePoolSize,
        keepAliveTime, keepAliveTimeUnit, threadLifeTime, contextService, rejectPolicy);
    this.awaitTermination = awaitTermination;
    if (rejectPolicy == RejectPolicy.RETRY_ABORT) {
      threadPoolExecutor.setRejectedExecutionHandler(new RetryAbortHandler(name, retryDelay));
    } else {
      threadPoolExecutor.setRejectedExecutionHandler(new AbortHandler(name));
    }
  }

  @Override
  public void execute(Runnable command) {
    if (command == null) {
      throw new NullPointerException("The runnable can't null!");
    }
    final Runnable useCommand = ConcurrentExtension.ENABLE_EXE_RUNNABLE_LOGGER ? () -> {
      try {
        command.run();
      } catch (Throwable t) {
        logger.log(Level.SEVERE, "Execute runnable occurred error!", t);
        rethrow(t);
      }
    } : command;
    super.execute(useCommand);
  }

  void stop(List<RemainExecutionHandler> handlers) {
    try {
      if (isEmpty(handlers)) {
        super.shutdown();
        if (awaitTermination != null
            && !super.awaitTermination(awaitTermination.toMillis(), TimeUnit.MILLISECONDS)) {
          super.shutdownNow();
          if (!super.awaitTermination(awaitTermination.toMillis(), TimeUnit.MILLISECONDS)) {
            logger.log(Level.WARNING,
                () -> format("Shutdown managed scheduled executor service %s timeout!", name));
          }
        }
      } else {
        List<Runnable> runnable = super.shutdownNow();
        for (RemainExecutionHandler handler : handlers) {
          handler.handle(true, getName(), runnable);
        }
      }
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, e, () -> String
          .format("Shutdown managed scheduled executor service %s occurred error!", name));
      super.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

}

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

import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.max;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.context.concurrent.ConcurrentExtension;
import org.corant.context.concurrent.ManagedExecutorConfig;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings;
import org.corant.shared.util.Threads;
import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService;
import org.glassfish.enterprise.concurrent.AbstractManagedThread;

/**
 * corant-context
 *
 * @author bingo 下午8:33:48
 *
 */
@ApplicationScoped
public class ExecutorServiceManager {

  protected static final Logger logger = Logger.getLogger(ExecutorServiceManager.class.getName());

  protected static final List<DefaultManagedExecutorService> executorService = new ArrayList<>();
  protected static final List<DefaultManagedScheduledExecutorService> scheduledExecutorService =
      new ArrayList<>();

  protected HungLogger hungLogger;

  @Inject
  protected ConcurrentExtension extension;

  public void register(DefaultManagedExecutorService service) {
    executorService.add(service);
    initializeHungLoggerIfNecessary();
  }

  public void register(DefaultManagedScheduledExecutorService service) {
    scheduledExecutorService.add(service);
    initializeHungLoggerIfNecessary();
  }

  protected synchronized void initializeHungLoggerIfNecessary() {
    if (logger.getLevel() == Level.OFF || !ConcurrentExtension.ENABLE_HUNG_TASK_LOGGER
        || hungLogger != null) {
      return;
    }
    long checkPeriod = Stream
        .concat(extension.getExecutorConfigs().getAllWithNames().values().stream(),
            extension.getScheduledExecutorConfigs().getAllWithNames().values().stream())
        .filter(c -> !c.isLongRunningTasks() && c.isValid())
        .map(ManagedExecutorConfig::getHungTaskThreshold).min(Long::compare).orElse(0L);
    if (checkPeriod > 0) {
      checkPeriod = checkPeriod / 2;
      hungLogger = new HungLogger(max(checkPeriod, 16000L));
      hungLogger.start();
    }
  }

  protected synchronized void preContainerStopEvent(@Observes final PreContainerStopEvent event) {
    if (hungLogger != null) {
      hungLogger.terminate();
    }
    for (DefaultManagedExecutorService service : executorService) {
      logger.info(() -> String.format("The managed executor service %s will be shutdown!",
          service.getName()));
      service.stop();
    }
    for (DefaultManagedScheduledExecutorService service : scheduledExecutorService) {
      logger.info(() -> String.format("The managed scheduled executor service %s will be shutdown!",
          service.getName()));
      service.stop();
    }
    executorService.clear();
    scheduledExecutorService.clear();
  }

  /**
   * corant-context
   *
   * @author bingo 下午5:25:42
   *
   */
  public static class HungLogger extends Thread {

    private final Logger logger = Logger.getLogger(HungLogger.class.getName());
    private final boolean dumpStack = logger.isLoggable(Level.FINE);
    private final long checkPeriod;
    private volatile boolean running = true;

    public HungLogger(long checkPeriod) {
      super("corant-es-hung");
      this.checkPeriod = checkPeriod;
      setDaemon(true);
    }

    @Override
    public void run() {
      while (running) {
        Threads.tryThreadSleep(checkPeriod);
        if (!executorService.isEmpty()) {
          for (AbstractManagedExecutorService es : executorService) {
            if (!running) {
              break;
            }
            log(es);
          }
        }
        if (!scheduledExecutorService.isEmpty()) {
          for (AbstractManagedExecutorService es : scheduledExecutorService) {
            if (!running) {
              break;
            }
            log(es);
          }
        }
      }
    }

    void log(AbstractManagedExecutorService es) {
      if (!es.isTerminated() && !es.isShutdown()) {
        final long now = System.currentTimeMillis();
        Collection<AbstractManagedThread> hungThreads = es.getHungThreads();
        if (isNotEmpty(hungThreads)) {
          for (AbstractManagedThread thread : hungThreads) {
            log(es.getName(), thread, now);
          }
        }
      }
    }

    void log(String esname, AbstractManagedThread t, long now) {
      if (dumpStack) {
        logger.warning(() -> String.format(
            "The thread [%s] id [%s] %s in managed executor service %s may suspected of being hung, started at %s, run time %s, the stack:%n\t%s.",
            t.getName(), t.getId(),
            areEqual("null", t.getTaskIdentityName()) ? Strings.EMPTY : t.getTaskIdentityName(),
            esname, Instant.ofEpochMilli(t.getThreadStartTime()), t.getTaskRunTime(now),
            String.join(Strings.NEWLINE.concat(Strings.TAB),
                Arrays.stream(t.getStackTrace()).map(Objects::asString).toArray(String[]::new))));
      } else {
        logger.warning(() -> String.format(
            "The thread [%s] id [%s] %s in managed executor service %s may suspected of being hung, started at %s, run time %s.",
            t.getName(), t.getId(),
            areEqual("null", t.getTaskIdentityName()) ? Strings.EMPTY : t.getTaskIdentityName(),
            esname, Instant.ofEpochMilli(t.getThreadStartTime()), t.getTaskRunTime(now)));
      }
    }

    void terminate() {
      running = false;
    }
  }
}

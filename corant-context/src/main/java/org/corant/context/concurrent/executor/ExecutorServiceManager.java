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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

  protected final List<DefaultManagedExecutorService> executorService =
      new CopyOnWriteArrayList<>();
  protected final List<DefaultManagedScheduledExecutorService> scheduledExecutorService =
      new CopyOnWriteArrayList<>();

  protected final Object monitor = new Object();

  protected HungLogger hungLogger;

  protected volatile boolean running = false;

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

  protected List<DefaultManagedExecutorService> getExecutorService() {
    return executorService;
  }

  protected List<DefaultManagedScheduledExecutorService> getScheduledExecutorService() {
    return scheduledExecutorService;
  }

  protected void initializeHungLoggerIfNecessary() {
    synchronized (monitor) {
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
        checkPeriod = checkPeriod >>> 2;
        final long useCheckPeriod = max(checkPeriod, 2000L);
        hungLogger = new HungLogger(this, useCheckPeriod);
        hungLogger.start();
        logger.info(() -> String.format(
            "Initialized the hung task logger for all managed executor services, check period %sms.",
            useCheckPeriod));
      }
    }
  }

  protected boolean isRunning() {
    return running;
  }

  protected void preContainerStopEvent(@Observes final PreContainerStopEvent event) {
    releaseHungLoggerIfNecessary();
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

  protected void releaseHungLoggerIfNecessary() {
    synchronized (monitor) {
      if (hungLogger != null) {
        logger.fine("Uninstall the hung task logger.");
        while (isRunning()) {
          setRunning(false);
          try {
            hungLogger.interrupt();
            monitor.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "Uninstall the hung task logger raise error!", e);
          }
        }
        hungLogger = null;
        logger.fine("The hung task logger was Uninstalled.");
      }
    }
  }

  protected void setRunning(boolean running) {
    this.running = running;
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
    private final ExecutorServiceManager manager;

    public HungLogger(ExecutorServiceManager manager, long checkPeriod) {
      super("corant-es-hung");
      this.manager = manager;
      this.checkPeriod = checkPeriod;
      this.manager.setRunning(true);
      setDaemon(true);
    }

    @Override
    public void run() {
      try {
        while (manager.isRunning()) {
          Thread.sleep(checkPeriod);
          if (!manager.getExecutorService().isEmpty() && manager.isRunning()) {
            for (AbstractManagedExecutorService es : manager.getExecutorService()) {
              if (!manager.isRunning()) {
                break;
              }
              log(es);
            }
          }
          if (!manager.getScheduledExecutorService().isEmpty() && manager.isRunning()) {
            for (AbstractManagedExecutorService es : manager.getScheduledExecutorService()) {
              if (!manager.isRunning()) {
                break;
              }
              log(es);
            }
          }
        }
      } catch (InterruptedException e) {
        // Noop, here sleeping is forcibly interrupted, that OK!
      } finally {
        synchronized (manager.monitor) {
          logger.fine("The hung task logger is about to exit.");
          if (!manager.isRunning()) {
            manager.monitor.notifyAll();
          } else {
            manager.setRunning(false);
          }
          logger.fine("The hung task logger exits.");
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
        logger.info(() -> String.format(
            "The thread [%s] id [%s] %s in managed executor service %s may suspected of being hung, started at %s, run time %sms, the stack:%n\t%s.",
            t.getName(), t.getId(),
            areEqual("null", t.getTaskIdentityName()) ? Strings.EMPTY : t.getTaskIdentityName(),
            esname, Instant.ofEpochMilli(t.getThreadStartTime()), t.getTaskRunTime(now),
            String.join(Strings.NEWLINE.concat(Strings.TAB),
                Arrays.stream(t.getStackTrace()).map(Objects::asString).toArray(String[]::new))));
      } else {
        logger.info(() -> String.format(
            "The thread [%s] id [%s] %s in managed executor service %s may suspected of being hung, started at %s, run time %sms.",
            t.getName(), t.getId(),
            areEqual("null", t.getTaskIdentityName()) ? Strings.EMPTY : t.getTaskIdentityName(),
            esname, Instant.ofEpochMilli(t.getThreadStartTime()), t.getTaskRunTime(now)));
      }
    }

  }
}

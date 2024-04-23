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
package org.corant.modules.jms.shared.receive;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.corant.modules.jms.shared.receive.MessageReceivingExecutorConfig.getExecutorConfig;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Strings.isBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.kernel.event.PostCorantReadyAsyncEvent;
import org.corant.modules.jms.receive.ManagedMessageReceivingExecutor;
import org.corant.modules.jms.shared.AbstractJMSConfig;
import org.corant.modules.jms.shared.AbstractJMSExtension;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Threads;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午2:37:45
 */
@ApplicationScoped
public class MessageReceivingExecutor implements ManagedMessageReceivingExecutor {

  @Inject
  protected Logger logger;

  @Inject
  protected AbstractJMSExtension extension;

  @Inject
  protected MessageReceivingTaskFactory taskFactory;

  @Inject
  protected MessageReceivingConnections connections;

  @Inject
  @Any
  protected Instance<MessageReceivingMetaDataSupplier> metaDataSuppliers;

  protected volatile boolean running;

  protected List<MessageReceivingMetaData> metaData = new ArrayList<>();

  protected List<MessageReceivingTaskExecution> executions = new ArrayList<>();

  protected Map<String, ScheduledExecutorService> executors = new HashMap<>();

  protected LinkedBlockingQueue<MessageReceivingTaskExecution> queue = new LinkedBlockingQueue<>();

  protected long maxArrangerWaitMs;

  protected TaskArranger arranger;

  @Override
  public synchronized void start() {
    if (running) {
      return;
    }
    logger.info("Starting message receiving tasks...");
    running = true;
    Set<Pair<String, String>> anyCasts = new HashSet<>();
    for (MessageReceivingMetaData meta : metaData) {
      final String id = meta.getConnectionFactoryId();
      if (!meta.isMulticast() && !anyCasts.add(Pair.of(id, meta.getDestination()))) {
        logger.warning(format(
            "Any cast message receiver destination appeared more than once, it should be avoided in general, message receiver [%s].",
            meta));
      }
      AbstractJMSConfig config = AbstractJMSExtension.getConfig(id);
      executions.add(createExecution(config, meta));
      logger.fine(format("Scheduled message receiving task. message receiver [%s].", meta));
    }
    if (!executions.isEmpty()) {
      arranger = new TaskArranger();
      arranger.start();
      logger.info(format("Message receiving schedule arranger started, loop poll wait [%s]ms.",
          maxArrangerWaitMs));
    }
    anyCasts.clear();
    logger.info("Message receiving tasks and executors are started!");
  }

  @Override
  public synchronized void stop() {
    logger.info(() -> "Stopping message receiving tasks...");
    running = false;
    while (!executions.isEmpty()) {
      try {
        executions.remove(0).cancel();
      } catch (Exception e) {
        logger.log(Level.WARNING, e, () -> "Stop message receiving task error!");
      }
    }
    if (arranger != null) {
      while (arranger.isAlive()) {
        Threads.tryThreadSleep(maxArrangerWaitMs);
      }
      arranger = null;
      logger.info(() -> "Message receiving schedule arranger stopped.");
    }
    Iterator<Entry<String, ScheduledExecutorService>> it = executors.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, ScheduledExecutorService> entry = it.next();
      String id = entry.getKey();
      ScheduledExecutorService executorService = entry.getValue();
      long awaitTermination = getExecutorConfig(id).getAwaitTermination().toMillis();
      try {
        executorService.shutdown();
        if (executorService.awaitTermination(awaitTermination, MILLISECONDS)) {
          logger.info(format("Message receiving executor service %s was stopped.", id));
        } else {
          logger.info(format("Message receiving executor service %s terminated timeout.", id));
        }
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, e, () -> format("Can not await [%s] executor service.", id));
        Thread.currentThread().interrupt();
      } finally {
        it.remove();
      }
    }
    connections.shutdown();
    queue.clear();
    logger.info(() -> "Message receiving tasks and executors were stopped!");
  }

  protected MessageReceivingTaskExecution createExecution(AbstractJMSConfig config,
      MessageReceivingMetaData meta) {
    if (meta.isXa()) {
      shouldBeTrue(config.isXa(), "Connection factory doesn't support XA! message receiver [%s].",
          meta);
    }
    String id = config.getConnectionFactoryId();
    ScheduledExecutorService service = shouldNotNull(executors.get(id),
        "Can't find any scheduled executor service! message receiver [%s].", meta);
    MessageReceivingExecutorConfig executorConfig = getExecutorConfig(id);
    MessageReceivingTaskExecution execution =
        new MessageReceivingTaskExecution(meta, taskFactory.create(meta));
    ScheduleFutureTask task = new ScheduleFutureTask(execution, queue);
    long delay = Math.max(0, executorConfig.getInitialDelay().toMillis());
    logger.info(format("Task will be executed after [%s]ms", delay));
    ScheduledFuture<?> future = service.schedule(task, delay, MILLISECONDS);
    execution.updateFuture(future);
    return execution;
  }

  protected synchronized void initialize() {
    metaData.clear();
    extension.getReceiveMethods().stream().map(MessageReceivingMetaData::of)
        .forEach(metaData::addAll);
    if (!metaDataSuppliers.isUnsatisfied()) {
      metaDataSuppliers.stream().forEach(s -> metaData.addAll(s.get()));
    }
    if (metaData.isEmpty()) {
      logger.info(() -> "Can't find any message receive methods");
      return;
    }
    // FIXME check receive and reply recursion
    Set<AbstractJMSConfig> useConfigs = new LinkedHashSet<>();
    Map<String, ? extends AbstractJMSConfig> configs = extension.getConfigs();
    Iterator<MessageReceivingMetaData> metaIt = metaData.listIterator();
    while (metaIt.hasNext()) {
      MessageReceivingMetaData meta = metaIt.next();
      String id = meta.getConnectionFactoryId();
      AbstractJMSConfig config = configs.get(id);
      if (config == null || !config.isEnable()) {
        final String methodName = meta.getMethod().getMethod().toString();
        logger.warning(format(
            "Receiver method [%s] can't be performed, the connection factory [%s] is not available!",
            methodName, id));
        metaIt.remove();
        continue;
      }
      maxArrangerWaitMs = max(maxArrangerWaitMs, meta.getLoopIntervalMs());
      useConfigs.add(config);
    }
    for (AbstractJMSConfig config : useConfigs) {
      String id = config.getConnectionFactoryId();
      MessageReceivingExecutorConfig executorConfig = getExecutorConfig(id);
      int size = executorConfig.getCorePoolSize();
      ThreadFactory tf = new MessageReceivingThreadFactory(id);
      ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(size, tf);
      executor.setRemoveOnCancelPolicy(true);
      executors.put(id, executor);
    }
    logger.info(format("Found %s message receivers that involving %s connection factories.",
        metaData.size(), executors.size()));
  }

  protected void onPostCorantReadyEvent(@ObservesAsync PostCorantReadyAsyncEvent adv) {
    start();
  }

  protected void onPreContainerStopEvent(@Observes final PreContainerStopEvent event) {
    stop();
  }

  @PostConstruct
  protected void postConstruct() {
    initialize();
  }

  /**
   * corant-modules-jms-shared
   *
   * @author bingo 18:25:11
   */
  protected static class MessageReceivingThreadFactory implements ThreadFactory {

    private final static AtomicLong COUNT = new AtomicLong(1);
    private final String name;

    MessageReceivingThreadFactory(String connectionFactoryId) {
      if (isBlank(connectionFactoryId)) {
        name = Names.CORANT_PREFIX + "msg-rec-";
      } else {
        name = Names.CORANT_PREFIX + "msg-rec-" + connectionFactoryId + "-";
      }
    }

    @Override
    public Thread newThread(final Runnable r) {
      return new Thread(r, name + COUNT.getAndIncrement());
    }

  }

  /**
   * corant-modules-jms-shared
   *
   * @author bingo 11:53:22
   */
  protected static class ScheduleFutureTask extends FutureTask<Void> {

    final MessageReceivingTaskExecution execution;
    final Queue<MessageReceivingTaskExecution> queue;

    public ScheduleFutureTask(MessageReceivingTaskExecution execution,
        Queue<MessageReceivingTaskExecution> queue) {
      super(execution.task, null);
      this.execution = execution;
      this.queue = queue;
    }

    @Override
    protected void done() {
      if (!isCancelled()) {
        queue.add(execution);
      }
    }
  }

  /**
   * corant-modules-jms-shared
   *
   * @author bingo 14:18:18
   */
  protected class TaskArranger extends Thread {

    public TaskArranger() {
      setName(Names.CORANT_PREFIX + "msg-rec-arranger");
      setDaemon(true);
    }

    @Override
    public void run() {
      while (running) {
        try {
          final MessageReceivingTaskExecution execution =
              queue.poll(maxArrangerWaitMs, MILLISECONDS);
          if (execution != null) {
            final long delay = max(execution.task.getDelay(MILLISECONDS), 0L);
            if (delay > 0) {
              logger.info(format("Task will be executed after [%s]ms", delay));
            }
            final String id = execution.metaData.getConnectionFactoryId();
            ScheduledFuture<?> future = executors.get(id)
                .schedule(new ScheduleFutureTask(execution, queue), delay, MILLISECONDS);
            execution.updateFuture(future);
          }
        } catch (InterruptedException e) {
          logger.log(Level.WARNING, "Arranger thread interrupted!", e);
          Thread.currentThread().interrupt();
        } catch (Exception e) {
          logger.log(Level.WARNING, "Arranger occurred error!", e);
        }
      }
    }

  }

}

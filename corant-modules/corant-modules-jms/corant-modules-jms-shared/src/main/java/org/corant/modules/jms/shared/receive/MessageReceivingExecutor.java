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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Strings.isBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.corant.config.Configs;
import org.corant.context.ContainerEvents.PreContainerStopEvent;
import org.corant.kernel.event.PostCorantReadyAsyncEvent;
import org.corant.modules.jms.receive.ManagedMessageReceivingExecutor;
import org.corant.modules.jms.receive.ManagedMessageReceivingTask;
import org.corant.modules.jms.shared.AbstractJMSConfig;
import org.corant.modules.jms.shared.AbstractJMSExtension;
import org.corant.shared.normal.Names;
import org.corant.shared.ubiquity.Tuple.Pair;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午2:37:45
 *
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

  protected final Map<AbstractJMSConfig, ScheduledExecutorService> executors = new HashMap<>();

  protected final List<MessageReceivingMetaData> metaDatas = new ArrayList<>();

  protected final List<MessageReceivingTaskExecution> receiveExecutions = new ArrayList<>();

  protected final Map<String, MessageReceivingExecutorConfig> executorConfigs =
      Configs.resolveMulti(MessageReceivingExecutorConfig.class);

  @Override
  public void start() {
    Set<Pair<String, String>> anycasts = new HashSet<>();
    for (final MessageReceivingMetaData meta : metaDatas) {
      if (!meta.isMulticast()
          && !anycasts.add(Pair.of(meta.getConnectionFactoryId(), meta.getDestination()))) {
        logger.warning(() -> String.format(
            "The anycast message receiver destination appeared more than once, it should be avoided in general. message receiver [%s].",
            meta));
      }
      AbstractJMSConfig config = AbstractJMSExtension.getConfig(meta.getConnectionFactoryId());
      if (config != null && config.isEnable()) {
        MessageReceivingTaskExecution execution = createExecution(config, meta);
        receiveExecutions.add(execution);
        logger.fine(
            () -> String.format("Scheduled message receiving task. message receiver [%s].", meta));
      }
    }
    anycasts.clear();
  }

  @Override
  public void stop() {
    logger.info(() -> "Stopping the message receiving tasks...");
    while (!receiveExecutions.isEmpty()) {
      try {
        receiveExecutions.remove(0).cancel();
      } catch (Exception e) {
        logger.log(Level.WARNING, e, () -> "Stop message receiving task error!");
      }
    }
    logger.info(() -> "All message receiving tasks were stopped.");
    logger.info(() -> "Stopping the message receiving executor services.");
    Iterator<Entry<AbstractJMSConfig, ScheduledExecutorService>> it =
        executors.entrySet().iterator();
    while (it.hasNext()) {
      Entry<AbstractJMSConfig, ScheduledExecutorService> entry = it.next();
      try {
        entry.getValue().shutdown();
        entry.getValue().awaitTermination(
            getExecutorConfig(entry.getKey()).getAwaitTermination().toMillis(),
            TimeUnit.MICROSECONDS);
        logger.info(() -> String.format("The message receiving executor service %s was stopped.",
            entry.getKey().getConnectionFactoryId()));
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, e, () -> String.format("Can not await [%s] executor service.",
            entry.getKey().getConnectionFactoryId()));
        Thread.currentThread().interrupt();
      } finally {
        it.remove();
      }
    }
    logger.info(() -> "All message receiving executor services were stopped.");
    connections.shutdown();
    logger.info(() -> "All message receiving connections were released.");
  }

  protected MessageReceivingExecutorConfig getExecutorConfig(AbstractJMSConfig config) {
    return executorConfigs.getOrDefault(config.getConnectionFactoryId(),
        MessageReceivingExecutorConfig.DFLT_INST);
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

  MessageReceivingTaskExecution createExecution(AbstractJMSConfig config,
      MessageReceivingMetaData meta) {
    if (meta.isXa()) {
      shouldBeTrue(config.isXa(),
          "The connection factory doesn't support xa! message receiver [%s].", meta);
    }
    ScheduledExecutorService service = shouldNotNull(executors.get(config),
        "Can't find any scheduled executore service! message receiver [%s].", meta);
    final MessageReceivingExecutorConfig executorConfig = getExecutorConfig(config);
    final ManagedMessageReceivingTask task = taskFactory.create(meta);
    final ScheduledFuture<?> future =
        service.scheduleWithFixedDelay(task, executorConfig.getInitialDelay().toMillis(),
            executorConfig.getDelay().toMillis(), TimeUnit.MICROSECONDS);
    return new MessageReceivingTaskExecution(future, task);
  }

  void initialize() {
    extension.getReceiveMethods().stream().map(MessageReceivingMetaData::of)
        .forEach(metaDatas::addAll);
    if (!metaDataSuppliers.isUnsatisfied()) {
      metaDataSuppliers.stream().forEach(s -> metaDatas.addAll(s.get()));
    }
    if (!metaDatas.isEmpty()) {
      // FIXME check receive and reply recursion
      final Map<String, ? extends AbstractJMSConfig> allConfigs =
          extension.getConfigManager().getAllWithNames();
      Set<AbstractJMSConfig> configs = new HashSet<>();
      Iterator<MessageReceivingMetaData> metaIt = metaDatas.iterator();
      while (metaIt.hasNext()) {
        MessageReceivingMetaData meta = metaIt.next();
        final String connectionFactoryId = meta.getConnectionFactoryId();
        final AbstractJMSConfig config = allConfigs.get(connectionFactoryId);
        if (config == null || !config.isEnable()) {
          logger.warning(() -> String.format(
              "The receiver method %s can't be performed, the connection factory %s is not available!",
              meta.getMethod().getMethod().toString(), connectionFactoryId));
          metaIt.remove();
          continue;
        }
        configs.add(config);
      }

      if (!configs.isEmpty()) {
        configs.forEach(cfg -> {
          MessageReceivingExecutorConfig executorConfig = getExecutorConfig(cfg);
          ScheduledThreadPoolExecutor executor =
              new ScheduledThreadPoolExecutor(executorConfig.getCorePoolSize(),
                  new MessageReceivingThreadFactory(cfg.getConnectionFactoryId()));
          executor.setRemoveOnCancelPolicy(true);
          executors.put(cfg, executor);
        });
      }

      logger.info(
          () -> String.format("Found %s message receivers that involving %s connection factories.",
              metaDatas.size(), executors.size()));
    }
  }

  static class MessageReceivingThreadFactory implements ThreadFactory {

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

}

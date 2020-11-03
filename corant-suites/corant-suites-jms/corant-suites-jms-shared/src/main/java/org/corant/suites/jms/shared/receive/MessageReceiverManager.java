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
package org.corant.suites.jms.shared.receive;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.kernel.event.PreContainerStopEvent;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.corant.shared.util.Sets;
import org.corant.suites.jms.shared.AbstractJMSConfig;
import org.corant.suites.jms.shared.AbstractJMSExtension;
import org.corant.suites.jms.shared.receive.MessageReceiverTaskFactory.CancellableTask;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午2:37:45
 *
 */
@ApplicationScoped
public class MessageReceiverManager {

  @Inject
  protected Logger logger;

  @Inject
  protected AbstractJMSExtension extesion;

  @Inject
  protected MessageReceiverTaskFactory taskFactory;

  protected final Map<AbstractJMSConfig, ScheduledExecutorService> executorServices =
      new HashMap<>();

  protected final Set<MessageReceiverMetaData> receiveMetaDatas = Sets.newConcurrentHashSet();

  protected final List<MessageReceiverTaskExecution> receiveControllers = new ArrayList<>();

  protected synchronized void beforeShutdown(@Observes final PreContainerStopEvent event) {
    logger.fine(() -> "Stop the message receiver tasks.");
    while (!receiveControllers.isEmpty()) {
      try {
        receiveControllers.remove(0).cancel();
      } catch (Exception e) {
        logger.log(Level.WARNING, e, () -> "Stop message receiver task error!");
      }
    }
    logger.fine(() -> "Shut down the message receiver executor services.");
    Iterator<Entry<AbstractJMSConfig, ScheduledExecutorService>> it =
        executorServices.entrySet().iterator();
    while (it.hasNext()) {
      Entry<AbstractJMSConfig, ScheduledExecutorService> entry = it.next();
      try {
        entry.getValue().shutdown();
        entry.getValue().awaitTermination(
            entry.getKey().getReceiverExecutorAwaitTermination().toMillis(), TimeUnit.MICROSECONDS);
      } catch (InterruptedException e) {
        logger.log(Level.WARNING, e, () -> String.format("Can not await [%s] executor service.",
            entry.getKey().getConnectionFactoryId()));
        Thread.currentThread().interrupt();
      } finally {
        it.remove();
      }
    }
  }

  protected void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv) {
    Set<Pair<String, String>> anycasts = new HashSet<>();
    for (final MessageReceiverMetaData metaData : receiveMetaDatas) {
      if (!metaData.isMulticast()
          && !anycasts.add(Pair.of(metaData.getConnectionFactoryId(), metaData.getDestination()))) {
        logger.warning(() -> String.format(
            "The anycast message receiver with same connection factory id and same destination appeared more than once, it should be avoided in general. message receiver [%s].",
            metaData));
      }
      final AbstractJMSConfig cfg =
          AbstractJMSExtension.getConfig(metaData.getConnectionFactoryId());
      if (cfg != null && cfg.isEnable()) {
        if (metaData.xa()) {
          shouldBeTrue(cfg.isXa(),
              "Can not schedule xa message receiver task, the connection factory [%s] not supported! message receiver [%s].",
              cfg.getConnectionFactoryId(), metaData);
        }
        ScheduledExecutorService ses = shouldNotNull(executorServices.get(cfg),
            "Can not schedule message receiver task, connection factory id [%s] not found. message receiver [%s].",
            cfg.getConnectionFactoryId(), metaData);
        CancellableTask task = taskFactory.create(metaData);
        ScheduledFuture<?> future =
            ses.scheduleWithFixedDelay(task, cfg.getReceiveTaskInitialDelay().toMillis(),
                cfg.getReceiveTaskDelay().toMillis(), TimeUnit.MICROSECONDS);
        receiveControllers.add(new MessageReceiverTaskExecution(future, task));
        logger.fine(() -> String.format(
            "Scheduled message receiver task, connection factory id [%s], destination [%s], initial delay [%s]Ms",
            metaData.getConnectionFactoryId(), metaData.getDestination(),
            cfg.getReceiveTaskInitialDelay()));
      }
    }
    anycasts.clear();
  }

  @PostConstruct
  protected void postConstruct() {
    extesion.getReceiveMethods().stream().map(MessageReceiverMetaData::of)
        .forEach(receiveMetaDatas::addAll);
    if (!receiveMetaDatas.isEmpty()) {
      extesion.getConfigManager().getAllWithNames().values().forEach(cfg -> {
        if (cfg != null && cfg.isEnable()) {
          ScheduledThreadPoolExecutor executor =
              new ScheduledThreadPoolExecutor(cfg.getReceiveTaskThreads());
          executor.setRemoveOnCancelPolicy(true);
          executorServices.put(cfg, executor);
        }
      });
      logger.info(
          () -> String.format("Find %s message receivers that involving %s connection factories.",
              receiveMetaDatas.size(), executorServices.size()));
    }
  }

}

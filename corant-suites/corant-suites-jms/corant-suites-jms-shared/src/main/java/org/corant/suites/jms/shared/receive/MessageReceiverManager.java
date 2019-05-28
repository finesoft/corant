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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.suites.jms.shared.AbstractJMSConfig;
import org.corant.suites.jms.shared.AbstractJMSExtension;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午2:37:45
 *
 */
@ApplicationScoped
public class MessageReceiverManager {

  @Inject
  Logger logger;

  @Inject
  AbstractJMSExtension extesion;

  final Map<Object, ScheduledExecutorService> executorServices = new HashMap<>();

  final Set<MessageReceiverMetaData> receiveMetaDatas =
      Collections.newSetFromMap(new ConcurrentHashMap<MessageReceiverMetaData, Boolean>());

  protected MessageReceiverTask buildTask(MessageReceiverMetaData metaData) {
    return new MessageReceiverTask(metaData);
  }

  void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv) {
    for (final MessageReceiverMetaData metaData : receiveMetaDatas) {
      final AbstractJMSConfig cfg = defaultObject(
          extesion.getConfig(metaData.getConnectionFactoryId()), AbstractJMSConfig.DFLT_INSTANCE);
      ScheduledExecutorService ses =
          shouldNotNull(executorServices.get(cfg.getConnectionFactoryId()),
              "Can not find any executor service for connection factory id [%s].",
              cfg.getConnectionFactoryId());
      ses.scheduleWithFixedDelay(buildTask(metaData), cfg.getReceiveTaskInitialDelayMs(),
          cfg.getReceiveTaskDelayMs(), TimeUnit.MICROSECONDS);
    }
  }

  @PostConstruct
  void postConstruct() {
    extesion.getReceiveMethods().stream().map(MessageReceiverMetaData::of)
        .forEach(receiveMetaDatas::addAll);
    extesion.getConfigs().keySet().forEach(cfId -> executorServices.put(cfId,
        Executors.newScheduledThreadPool(extesion.getConfig(cfId).getReceiveTaskThreads())));
    logger.info(
        () -> String.format("Find %s message receivers that involving %s connection factories.",
            receiveMetaDatas.size(), executorServices.size()));
  }

  @PreDestroy
  void preDestroy() {
    executorServices.values().forEach(es -> {
      es.shutdownNow().forEach(r -> {
        if (r instanceof MessageReceiverTask) {
          MessageReceiverTask.class.cast(r).release(true);
        }
      });
    });
  }
}

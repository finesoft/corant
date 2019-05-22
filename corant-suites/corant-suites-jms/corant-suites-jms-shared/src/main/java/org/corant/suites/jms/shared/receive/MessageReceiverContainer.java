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

import java.util.Collections;
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
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.transaction.TransactionManager;
import org.corant.kernel.event.PostCorantReadyEvent;
import org.corant.suites.jms.shared.AbstractJMSExtension;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午2:37:45
 *
 */
@ApplicationScoped
public class MessageReceiverContainer {

  @Inject
  Logger logger;

  @Inject
  AbstractJMSExtension extesion;

  @Inject
  BeanManager beanManager;

  @Inject
  TransactionManager transactionManager;

  @Inject
  @ConfigProperty(name = "jms.enable", defaultValue = "true")
  boolean enable;

  @Inject
  @ConfigProperty(name = "jms.receiver.executor.initialDelayMs", defaultValue = "0")
  Integer esInitialDelay;

  @Inject
  @ConfigProperty(name = "jms.receiver.executor.initialDelayMs", defaultValue = "1000")
  Integer esDelay;

  @Inject
  @ConfigProperty(name = "jms.receiver.executor.threads", defaultValue = "4")
  Integer esThreads;

  ScheduledExecutorService executorService;

  Set<MessageReceiverMetaData> receiverMetaDatas =
      Collections.newSetFromMap(new ConcurrentHashMap<MessageReceiverMetaData, Boolean>());

  void onPostCorantReadyEvent(@Observes PostCorantReadyEvent adv) {
    for (final MessageReceiverMetaData metaData : receiverMetaDatas) {
      executorService.scheduleWithFixedDelay(new MessageReceiveTask(metaData),
          esInitialDelay.intValue(), esDelay.intValue(), TimeUnit.MICROSECONDS);
    }
  }

  @PostConstruct
  void postConstruct() {
    if (!enable) {
      logger.info(() -> "JMS not enable!");
      return;
    }
    extesion.receiverMethods().stream().map(MessageReceiverMetaData::of)
        .forEach(receiverMetaDatas::addAll);
    executorService = Executors.newScheduledThreadPool(esThreads);
  }

  @PreDestroy
  void preDestroy() {
    executorService.shutdownNow().forEach(r -> {
      if (r instanceof MessageReceiveTask) {
        MessageReceiveTask.class.cast(r).release(true);
      }
    });
  }
}

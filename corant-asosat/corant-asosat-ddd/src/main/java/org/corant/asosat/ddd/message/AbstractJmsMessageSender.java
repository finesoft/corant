/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.ddd.message;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.Message.ExchangedMessage;

/**
 * @author bingo 上午11:58:38
 *
 */
@ApplicationScoped
@InfrastructureServices
public abstract class AbstractJmsMessageSender implements MessageSender {

  @Inject
  private JMSContext context;
  private final Map<String, Queue> queues = new HashMap<>();
  private JMSProducer producer;

  public AbstractJmsMessageSender() {}

  @Override
  public boolean send(ExchangedMessage message) throws Exception {
    return convertAndSend(queues.get(message.queueName()), message);
  }

  protected boolean convertAndSend(Queue queue, ExchangedMessage message) {
    getProducer().send(queue, message);
    return true;
  }

  protected JMSContext getContext() {
    return context;
  }

  protected JMSProducer getProducer() {
    return producer;
  }

  protected abstract Set<String> getQueueNames();


  protected Map<String, Queue> getQueues() {
    return queues;
  }

  @PostConstruct
  void init() {
    getQueueNames().forEach(queue -> {
      queues.put(queue, context.createQueue(queue));
    });
    producer = context.createProducer();
  }

}

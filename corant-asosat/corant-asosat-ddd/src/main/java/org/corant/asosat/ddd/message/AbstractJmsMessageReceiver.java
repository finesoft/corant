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

import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.Message.ExchangedMessage;
import org.corant.suites.ddd.message.MessageService;

/**
 * @author bingo 下午2:32:37
 *
 */
@ApplicationScoped
@InfrastructureServices
public abstract class AbstractJmsMessageReceiver implements MessageListener, MessageReceiver {

  @Inject
  protected MessageService service;
  @Inject
  protected JMSContext context;

  public AbstractJmsMessageReceiver() {}

  @Override
  public void onMessage(Message message) {
    this.receive(convert(message));
  }

  @Override
  public void receive(ExchangedMessage message) {
    service.receive(message);
  }

  protected abstract ExchangedMessage convert(Message message);

  protected abstract Set<String> getQueues();

  @PostConstruct
  void init() {
    getQueues().forEach(queue -> {
      context.createConsumer(context.createQueue(queue)).setMessageListener(this);
    });
  }
}

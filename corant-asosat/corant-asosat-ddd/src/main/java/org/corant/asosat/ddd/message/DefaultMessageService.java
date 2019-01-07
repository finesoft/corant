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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.corant.asosat.ddd.pattern.interceptor.Asynchronous;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.message.Message.ExchangedMessage;
import org.corant.suites.ddd.message.MessageService;

/**
 * @author bingo 上午10:51:18
 *
 */
@ApplicationScoped
@InfrastructureServices
public class DefaultMessageService implements MessageService {

  @Inject
  protected Logger logger;

  @Inject
  protected MessageConvertor convertor;

  @Inject
  protected MessageStroage stroage;

  @Inject
  protected ExchangedMessageHandler exchangeMessageHandler;

  @Inject
  protected MessageSender sender;

  public DefaultMessageService() {}

  @Override
  public MessageConvertor getConvertor() {
    return convertor;
  }

  @Asynchronous(fair = false)
  @Override
  public void receive(ExchangedMessage msg) {
    if (msg != null) {
      exchangeMessageHandler.handle(msg);
    } else {
      logger.warn(() -> "Can not find exchanged message handler!");
    }
  }

  @Asynchronous(fair = false) // FIXME ordered
  @Override
  public void send(ExchangedMessage msg) {
    if (msg != null) {
      try {
        sender.send(msg);
      } catch (Exception e) {
        throw new GeneralRuntimeException(e, "");// FIXME MSG
      }
    } else {
      logger.warn(() -> "Can not find message channel!");
    }
  }

  @Override
  public Message store(Message message) {
    stroage.store(message);
    return message;
  }
}

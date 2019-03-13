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
package org.corant.suites.ddd.message;

import java.util.logging.Logger;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.Message.ExchangedMessage;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午3:45:49
 *
 */
public interface MessageService {

  static MessageService empty() {
    return EmptyMessageService.INSTANCE;
  }

  MessageConvertor getConvertor();

  void receive(ExchangedMessage message);

  void send(ExchangedMessage messages);

  Message store(Message message);

  static class EmptyMessageConvertor implements MessageConvertor {

    public static final MessageConvertor INSTANCE = new EmptyMessageConvertor();

    protected final transient Logger logger = Logger.getLogger(this.getClass().toString());

    @Override
    public Message from(ExchangedMessage message) {
      logger.warning(
          () -> "The message convertor is an empty implementation that does not really implement from");
      return null;
    }

    @Override
    public ExchangedMessage to(Message message) {
      logger.warning(
          () -> "The message convertor is an empty implementation that does not really implement to");
      return null;
    }

  }

  static class EmptyMessageService implements MessageService {

    public static final MessageService INSTANCE = new EmptyMessageService();

    protected final transient Logger logger = Logger.getLogger(this.getClass().toString());

    @Override
    public MessageConvertor getConvertor() {
      return EmptyMessageConvertor.INSTANCE;
    }

    @Override
    public void receive(ExchangedMessage message) {
      logger.warning(
          () -> "The message service is an empty implementation that does not really implement receive");
    }

    @Override
    public void send(ExchangedMessage messages) {
      logger.warning(
          () -> "The message service is an empty implementation that does not really implement send");
    }

    @Override
    public Message store(Message message) {
      logger.warning(
          () -> "The message service is an empty implementation that does not really implement store");
      return message;
    }

  }

  /**
   * @author bingo 下午12:27:57
   *
   */
  @InfrastructureServices
  public interface MessageConvertor {

    Message from(ExchangedMessage message);

    ExchangedMessage to(Message message);
  }

  /**
   * corant-suites-ddd
   *
   * @author bingo 下午10:26:09
   *
   */
  public interface MessageStroage {

    void store(Message message);
  }
}

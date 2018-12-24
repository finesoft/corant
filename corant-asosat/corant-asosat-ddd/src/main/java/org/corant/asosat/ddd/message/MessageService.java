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
package org.corant.asosat.ddd.message;

import org.corant.asosat.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.asosat.ddd.message.Message.ExchangedMessage;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午3:45:49
 *
 */
public interface MessageService {

  MessageConvertor getConvertor();

  void receive(ExchangedMessage message);

  void send(ExchangedMessage messages);

  Message store(Message message);

  /**
   * @author bingo 下午12:27:57
   *
   */
  @InfrastructureServices
  public static interface MessageConvertor {

    Message from(ExchangedMessage message);

    ExchangedMessage to(Message message);
  }

  /**
   * asosat-kernel
   *
   * @author bingo 下午10:26:09
   *
   */
  public static interface MessageStroage {

    void store(Message message);
  }
}

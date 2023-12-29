/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jms.send;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * corant-modules-jms-api
 *
 * @author bingo 上午11:55:37
 */
public class GroupMessageSender implements MessageSender {

  final List<MessageSender> dispatchers;

  public GroupMessageSender(List<MessageSender> dispatchers) {
    this.dispatchers = dispatchers;
  }

  @Override
  public void send(byte[] message) {
    for (MessageSender dispatcher : dispatchers) {
      dispatcher.send(message);
    }
  }

  @Override
  public void send(Map<String, Object> message) {
    for (MessageSender dispatcher : dispatchers) {
      dispatcher.send(message);
    }
  }

  @Override
  public void send(Serializable message) {
    for (MessageSender dispatcher : dispatchers) {
      dispatcher.send(message);
    }
  }

  @Override
  public void send(String message) {
    for (MessageSender dispatcher : dispatchers) {
      dispatcher.send(message);
    }
  }

  @Override
  public void send(String marshallerName, Object... messages) {
    for (MessageSender dispatcher : dispatchers) {
      dispatcher.send(marshallerName, messages);
    }
  }

}

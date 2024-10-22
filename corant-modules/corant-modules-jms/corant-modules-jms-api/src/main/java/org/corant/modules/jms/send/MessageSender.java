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

import static org.corant.shared.util.Streams.copy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * corant-modules-jms-api
 *
 * @author bingo 上午11:52:26
 */
public interface MessageSender {

  /**
   * Send the given input stream message
   *
   * @param message message to be sent
   * @throws IOException if error occur
   */
  default void send(InputStream message) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      copy(message, buffer);
      byte[] bytes = buffer.toByteArray();
      sendBytes(bytes);
    }
  }

  /**
   * Send the given serializable message, the message serialization use the default java object
   * marshaller, subclass may change the marshaller.
   *
   * @param message the message to be sent
   */
  void send(Serializable message);

  /**
   * Send the given messages with the given marshaller name
   *
   * @param marshallerName the name of message marshaller, the default marshaller is java object
   *        serialization
   * @param messages the message to be sent
   */
  void send(String marshallerName, Object... messages);

  /**
   * Send the given bytes array message, the bytes array message will be used to create a
   * {@code BytesMessage}
   *
   * @param message the bytes array message to be sent
   */
  void sendBytes(byte[] message);

  /**
   * Send the given map message, the map message will be used to create a {@code MapMessage}
   *
   * @param message the map message to be sent
   */
  void sendMap(Map<String, Object> message);

  /**
   * Send the given text message, the map message will be used to create a {@code TextMessage}
   *
   * @param message the text message to be sent
   */
  void sendText(String message);
}

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
package org.corant.suites.jms.shared.send;

import static org.corant.Corant.instance;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.StreamUtils.copy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Message;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.annotation.MessageSend;
import org.corant.suites.jms.shared.context.JMSContextProducer;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:17:57
 *
 */
public interface MessageSender {

  void send(byte[] message);

  default void send(InputStream message) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      copy(message, buffer);
      byte[] bytes = buffer.toByteArray();
      send(bytes);
    }
  }

  void send(Map<String, Object> message);

  void send(Message message);

  void send(Serializable message);

  void send(String message);

  /**
   * corant-suites-jms-artemis
   *
   * @author bingo 下午4:29:24
   *
   */
  public static class MessageSenderImpl implements MessageSender {

    protected final boolean multicast;
    protected final String destination;
    protected final String connectionFactoryId;
    protected final int sessionMode;

    public MessageSenderImpl(MessageSend mpl) {
      multicast = mpl.multicast();
      destination = shouldNotNull(mpl.destination());
      connectionFactoryId = mpl.connectionFactoryId();
      sessionMode = mpl.sessionMode();
    }

    @Override
    public void send(byte[] message) {
      doSend(message);
    }

    @Override
    public void send(InputStream message) throws IOException {
      try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
        copy(message, buffer);
        byte[] bytes = buffer.toByteArray();
        send(bytes);
      }
    }

    @Override
    public void send(Map<String, Object> message) {
      doSend(message);
    }

    @Override
    public void send(Message message) {
      doSend(message);
    }

    @Override
    public void send(Serializable message) {
      doSend(message);
    }

    @Override
    public void send(String message) {
      doSend(message);
    }

    @SuppressWarnings({"unchecked"})
    void doSend(Object message) {
      final JMSContextProducer ctxProducer = instance().select(JMSContextProducer.class).get();
      final JMSContext jmsc = ctxProducer.create(connectionFactoryId, sessionMode);
      try {
        Destination d = multicast ? jmsc.createTopic(destination) : jmsc.createQueue(destination);
        JMSProducer p = jmsc.createProducer();
        if (message instanceof String) {
          p.send(d, (String) message);
        } else if (message instanceof Message) {
          p.send(d, (Message) message);
        } else if (message instanceof Map) {
          p.send(d, (Map<String, Object>) message);
        } else if (message instanceof byte[]) {
          p.send(d, (byte[]) message);
        } else if (message instanceof Serializable) {
          p.send(d, (Serializable) message);
        }
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
  }
}

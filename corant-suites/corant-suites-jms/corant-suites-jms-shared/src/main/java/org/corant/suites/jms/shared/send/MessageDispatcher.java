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

import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.StreamUtils.copy;
import static org.corant.suites.cdi.Instances.resolveApply;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSProducer;
import javax.jms.JMSSessionMode;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.annotation.MessageDispatch;
import org.corant.suites.jms.shared.context.JMSContextProducer;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:17:57
 *
 */
public interface MessageDispatcher {

  void dispatch(byte[] message);

  void dispatch(Map<String, Object> message);

  void dispatch(Message message);

  void dispatch(Serializable message);

  void dispatch(String message);

  default void send(InputStream message) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      copy(message, buffer);
      byte[] bytes = buffer.toByteArray();
      dispatch(bytes);
    }
  }

  /**
   * corant-suites-jms-artemis
   *
   * @author bingo 下午4:29:24
   *
   */
  public static class MessageDispatcherImpl implements MessageDispatcher {

    protected final boolean multicast;
    protected final String destination;
    protected final String connectionFactoryId;
    protected final int sessionMode;

    public MessageDispatcherImpl(JMSDestinationDefinition dann, JMSSessionMode sann) {
      multicast = tryAsClass(dann.description()).isAssignableFrom(Queue.class);
      destination = shouldNotNull(dann.destinationName());
      connectionFactoryId = shouldNotNull(dann.name());
      sessionMode = sann == null ? Session.AUTO_ACKNOWLEDGE : sann.value();
    }

    public MessageDispatcherImpl(MessageDispatch mpl) {
      multicast = mpl.multicast();
      destination = shouldNotNull(mpl.destination());
      connectionFactoryId = mpl.connectionFactoryId();
      sessionMode = mpl.sessionMode();
    }

    @Override
    public void dispatch(byte[] message) {
      doDispatch(message);
    }

    @Override
    public void dispatch(Map<String, Object> message) {
      doDispatch(message);
    }

    @Override
    public void dispatch(Message message) {
      doDispatch(message);
    }

    @Override
    public void dispatch(Serializable message) {
      doDispatch(message);
    }

    @Override
    public void dispatch(String message) {
      doDispatch(message);
    }

    @Override
    public void send(InputStream message) throws IOException {
      try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
        copy(message, buffer);
        byte[] bytes = buffer.toByteArray();
        dispatch(bytes);
      }
    }

    @SuppressWarnings("unchecked")
    void doDispatch(Object message) {
      final JMSContext jmsc =
          resolveApply(JMSContextProducer.class, b -> b.create(connectionFactoryId, sessionMode));
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

  /**
   * corant-suites-jms-shared
   *
   * @author bingo 下午7:45:18
   *
   */
  public static class GroupMessageDispatcherImpl implements MessageDispatcher {

    final List<MessageDispatcher> dispatchers;

    /**
     * @param dispatchers
     */
    protected GroupMessageDispatcherImpl(List<MessageDispatcher> dispatchers) {
      super();
      this.dispatchers = dispatchers;
    }

    @Override
    public void dispatch(byte[] message) {
      for (MessageDispatcher dispatcher : dispatchers) {
        dispatcher.dispatch(message);
      }
    }

    @Override
    public void dispatch(Map<String, Object> message) {
      for (MessageDispatcher dispatcher : dispatchers) {
        dispatcher.dispatch(message);
      }
    }

    @Override
    public void dispatch(Message message) {
      for (MessageDispatcher dispatcher : dispatchers) {
        dispatcher.dispatch(message);
      }
    }

    @Override
    public void dispatch(Serializable message) {
      for (MessageDispatcher dispatcher : dispatchers) {
        dispatcher.dispatch(message);
      }
    }

    @Override
    public void dispatch(String message) {
      for (MessageDispatcher dispatcher : dispatchers) {
        dispatcher.dispatch(message);
      }
    }

  }
}

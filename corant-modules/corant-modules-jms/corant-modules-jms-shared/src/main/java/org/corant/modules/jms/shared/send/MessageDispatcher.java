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
package org.corant.modules.jms.shared.send;

import static org.corant.context.Instances.resolve;
import static org.corant.context.Instances.resolveApply;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Streams.copy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSProducer;
import javax.jms.JMSSessionMode;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import org.corant.config.Configs;
import org.corant.modules.jms.shared.annotation.MessageDispatch;
import org.corant.modules.jms.shared.annotation.MessageSerialization.SerializationSchema;
import org.corant.modules.jms.shared.context.JMSContextProducer;
import org.corant.modules.jms.shared.context.MessageSerializer;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午3:17:57
 *
 */
public interface MessageDispatcher {

  void dispatch(byte[] message);

  default void dispatch(InputStream message) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      copy(message, buffer);
      byte[] bytes = buffer.toByteArray();
      dispatch(bytes);
    }
  }

  void dispatch(Map<String, Object> message);

  void dispatch(Message message);

  default void dispatch(Object object, SerializationSchema serializationSchema) {
    throw new NotSupportedException();
  }

  void dispatch(Serializable message);

  void dispatch(String message);

  /**
   * corant-modules-jms-shared
   *
   * @author bingo 下午7:45:18
   *
   */
  class GroupMessageDispatcherImpl implements MessageDispatcher {

    final List<MessageDispatcher> dispatchers;

    /**
     * @param dispatchers
     */
    protected GroupMessageDispatcherImpl(List<MessageDispatcher> dispatchers) {
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
    public void dispatch(Object object, SerializationSchema serializationSchema) {
      for (MessageDispatcher dispatcher : dispatchers) {
        dispatcher.dispatch(object, serializationSchema);
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

  /**
   * corant-modules-jms-artemis
   *
   * @author bingo 下午4:29:24
   *
   */
  class MessageDispatcherImpl implements MessageDispatcher {

    protected boolean multicast;
    protected String destination;
    protected String connectionFactoryId;
    protected int sessionMode;
    protected int deliveryMode;
    protected long deliveryDelay = -1;
    protected long timeToLive = -1;
    protected Map<String, Object> properties = new HashMap<>();

    public MessageDispatcherImpl(JMSDestinationDefinition dann, JMSSessionMode sann) {
      multicast = Queue.class.isAssignableFrom(tryAsClass(dann.description()));
      destination = shouldNotNull(Configs.assemblyStringConfigProperty(dann.destinationName()));
      connectionFactoryId = shouldNotNull(Configs.assemblyStringConfigProperty(dann.name()));
      sessionMode = sann == null ? Session.AUTO_ACKNOWLEDGE : sann.value();
      deliveryMode = DeliveryMode.PERSISTENT;
    }

    public MessageDispatcherImpl(MessageDispatch mpl) {
      multicast = mpl.multicast();
      destination = shouldNotNull(Configs.assemblyStringConfigProperty(mpl.destination()));
      connectionFactoryId = Configs.assemblyStringConfigProperty(mpl.connectionFactoryId());
      sessionMode = mpl.sessionMode();
      deliveryMode = mpl.deliveryMode();
      deliveryDelay = mpl.deliveryDelay();
      timeToLive = mpl.timeToLive();
      if (isNotEmpty(mpl.properties())) {
        Arrays.stream(mpl.properties())
            .forEach(p -> properties.put(p.name(), toObject(p.value(), p.type())));
      }
    }

    protected MessageDispatcherImpl() {

    }

    @Override
    public void dispatch(byte[] message) {
      doDispatch(message, null);
    }

    @Override
    public void dispatch(InputStream message) throws IOException {
      try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
        copy(message, buffer);
        byte[] bytes = buffer.toByteArray();
        dispatch(bytes);
      }
    }

    @Override
    public void dispatch(Map<String, Object> message) {
      doDispatch(message, null);
    }

    @Override
    public void dispatch(Message message) {
      doDispatch(message, null);
    }

    @Override
    public void dispatch(Object object, SerializationSchema serializationSchema) {
      doDispatch(object, serializationSchema);
    }

    @Override
    public void dispatch(Serializable message) {
      doDispatch(message, null);
    }

    @Override
    public void dispatch(String message) {
      doDispatch(message, null);
    }

    protected void configurateProducer(JMSContext jmsc, JMSProducer producer) {
      producer.setDeliveryMode(deliveryMode);
      if (deliveryDelay > 0) {
        producer.setDeliveryDelay(deliveryDelay);
      }
      if (timeToLive > 0) {
        producer.setTimeToLive(timeToLive);
      }
      if (isNotEmpty(properties)) {
        properties.forEach(producer::setProperty);
      }
    }

    @SuppressWarnings("unchecked")
    protected void doDispatch(JMSContext jmsc, SerializationSchema serializationSchema,
        Object message) {
      try {
        Destination d = multicast ? jmsc.createTopic(destination) : jmsc.createQueue(destination);
        JMSProducer p = jmsc.createProducer();
        configurateProducer(jmsc, p);
        if (serializationSchema != null) {
          p.send(d, resolve(MessageSerializer.class, serializationSchema.qualifier())
              .serialize(jmsc, message));
        } else if (message instanceof String) {
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

    protected void doDispatch(Object message, SerializationSchema serializationSchema) {
      final JMSContext jmsc =
          resolveApply(JMSContextProducer.class, b -> b.create(connectionFactoryId, sessionMode));
      doDispatch(jmsc, serializationSchema, message);
    }
  }
}

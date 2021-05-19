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
import java.util.stream.Stream;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSProducer;
import javax.jms.JMSSessionMode;
import javax.jms.Message;
import javax.jms.Queue;
import org.corant.config.Configs;
import org.corant.modules.jms.shared.annotation.MessageDispatch;
import org.corant.modules.jms.shared.context.JMSContextProducer;
import org.corant.modules.jms.shared.context.MessageSerializer;
import org.corant.modules.jms.shared.context.SerialSchema;
import org.corant.shared.exception.CorantRuntimeException;

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

  void dispatch(Serializable message);

  void dispatch(SerialSchema serialSchema, Object... messages);

  void dispatch(String message);

  /**
   * corant-modules-jms-shared
   *
   * @author bingo 下午7:45:18
   *
   */
  class GroupMessageDispatcherImpl implements MessageDispatcher {

    final List<MessageDispatcher> dispatchers;

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
    public void dispatch(Serializable message) {
      for (MessageDispatcher dispatcher : dispatchers) {
        dispatcher.dispatch(message);
      }
    }

    @Override
    public void dispatch(SerialSchema serializationSchema, Object... messages) {
      for (MessageDispatcher dispatcher : dispatchers) {
        dispatcher.dispatch(serializationSchema, messages);
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
   *
   * setDisableMessageTimestamp setDisableMessageID
   *
   * @author bingo 下午4:29:24
   *
   */
  class MessageDispatcherImpl implements MessageDispatcher {

    protected boolean multicast;
    protected String destination;
    protected String connectionFactoryId;
    protected boolean dupsOkAck = false;
    protected int deliveryMode = DeliveryMode.PERSISTENT;
    protected long deliveryDelay = -1;
    protected long timeToLive = -1;
    protected Map<String, Object> properties = new HashMap<>();

    public MessageDispatcherImpl(JMSDestinationDefinition dann, JMSSessionMode sann) {
      multicast = Queue.class.isAssignableFrom(tryAsClass(dann.description()));
      destination = shouldNotNull(Configs.assemblyStringConfigProperty(dann.destinationName()));
      connectionFactoryId = shouldNotNull(Configs.assemblyStringConfigProperty(dann.name()));
      if (sann.value() == JMSContext.DUPS_OK_ACKNOWLEDGE) {
        dupsOkAck = true;
      }
      deliveryMode = DeliveryMode.PERSISTENT;
    }

    public MessageDispatcherImpl(MessageDispatch mpl) {
      multicast = mpl.multicast();
      destination = shouldNotNull(Configs.assemblyStringConfigProperty(mpl.destination()));
      connectionFactoryId = Configs.assemblyStringConfigProperty(mpl.connectionFactoryId());
      dupsOkAck = mpl.dupsOkAck();
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
      doDispatch(null, new Object[] {message});// FIXME
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
      doDispatch(null, message);
    }

    @Override
    public void dispatch(Serializable message) {
      doDispatch(null, message);
    }

    @Override
    public void dispatch(SerialSchema serialSchema, Object... messages) {
      doDispatch(serialSchema, messages);
    }

    @Override
    public void dispatch(String message) {
      doDispatch(null, message);
    }

    protected void configure(JMSContext jmsc, JMSProducer producer) {
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
    protected void doDispatch(JMSContext jmsc, Destination d, JMSProducer p,
        MessageSerializer serializer, Object message) {
      try {
        if (serializer != null) {
          p.send(d, serializer.serialize(jmsc, message));
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

    protected void doDispatch(JMSContext jmsc, SerialSchema serialSchema, Object... messages) {
      if (isNotEmpty(messages)) {
        try {
          final MessageSerializer serializer = serializer(serialSchema);
          Destination d = multicast ? jmsc.createTopic(destination) : jmsc.createQueue(destination);
          JMSProducer p = jmsc.createProducer();
          configure(jmsc, p);
          for (Object message : messages) {
            doDispatch(jmsc, d, p, serializer, message);
          }
        } catch (Exception e) {
          throw new CorantRuntimeException(e);
        }
      }
    }

    protected void doDispatch(SerialSchema serialSchema, Object... messages) {
      if (isNotEmpty(messages)) {
        final JMSContext jmsc =
            resolveApply(JMSContextProducer.class, b -> b.create(connectionFactoryId, dupsOkAck));
        doDispatch(jmsc, serialSchema, messages);
      }
    }

    protected void doStreamDispatch(JMSContext jmsc, Stream<?> messages,
        SerialSchema serializationSchema) {
      try {
        final MessageSerializer serializer = serializer(serializationSchema);
        Destination d = multicast ? jmsc.createTopic(destination) : jmsc.createQueue(destination);
        JMSProducer p = jmsc.createProducer();
        configure(jmsc, p);
        messages.forEach(message -> doDispatch(jmsc, d, p, serializer, message));
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }

    protected MessageSerializer serializer(SerialSchema serialSchema) {
      return serialSchema != null ? resolve(MessageSerializer.class, serialSchema.qualifier())
          : null;
    }
  }
}

/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.context.Beans.findNamed;
import static org.corant.context.Beans.resolveApply;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Maps.mapOf;
import java.io.Serializable;
import java.util.Map;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSProducer;
import jakarta.jms.Message;
import org.corant.modules.jms.marshaller.MessageMarshaller;
import org.corant.modules.jms.metadata.MessageDispatchMetaData;
import org.corant.modules.jms.send.MessageDispatcher;
import org.corant.modules.jms.shared.context.DefaultJMSContextService;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 11:13:44
 */
public class DefaultMessageDispatcher implements MessageDispatcher {

  protected boolean dupsOkAck;
  protected int deliveryMode;
  protected long deliveryDelay;
  protected long timeToLive;
  protected String marshaller;

  public DefaultMessageDispatcher(boolean dupsOkAck, int deliveryMode, long deliveryDelay,
      long timeToLive, String marshaller) {
    this.dupsOkAck = dupsOkAck;
    this.deliveryMode = deliveryMode;
    this.deliveryDelay = deliveryDelay;
    this.timeToLive = timeToLive;
    this.marshaller = marshaller;
  }

  public DefaultMessageDispatcher(MessageDispatchMetaData meta) {
    this(meta.isDupsOkAck(), meta.getDeliveryMode(), meta.getDeliveryDelay(), meta.getTimeToLive(),
        meta.getMarshaller());
  }

  @Override
  public void dispatch(String connectionFactoryId, String destination, boolean multicast,
      Object message, Map<String, Object> properties) {
    doDispatch(connectionFactoryId, destination, multicast, message, properties,
        marshaller(marshaller));
  }

  @Override
  public void dispatchBytes(String connectionFactoryId, String destination, boolean multicast,
      byte[] message, Object... messageProperties) {
    doDispatch(connectionFactoryId, destination, multicast, message, mapOf(messageProperties),
        null);
  }

  @Override
  public void dispatchMap(String connectionFactoryId, String destination, boolean multicast,
      Map<String, Object> message, Object... messageProperties) {
    doDispatch(connectionFactoryId, destination, multicast, message, mapOf(messageProperties),
        null);
  }

  @Override
  public void dispatchText(String connectionFactoryId, String destination, boolean multicast,
      String message, Object... messageProperties) {
    doDispatch(connectionFactoryId, destination, multicast, message, mapOf(messageProperties),
        null);
  }

  @SuppressWarnings("unchecked")
  protected void doDispatch(JMSContext jmsc, Destination d, JMSProducer p,
      MessageMarshaller marshaller, Object message) {
    try {
      if (message instanceof Message) {
        p.send(d, (Message) message);
      } else if (marshaller != null) {
        p.send(d, marshaller.serialize(jmsc, message));
      } else if (message instanceof String) {
        p.send(d, (String) message);
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

  protected void doDispatch(String connectionFactoryId, String destination, boolean multicast,
      Object message, Map<String, Object> properties, MessageMarshaller serializer) {
    final JMSContext jmsc = resolveApply(DefaultJMSContextService.class,
        b -> b.getJMSContext(connectionFactoryId, dupsOkAck));
    Destination dest = multicast ? jmsc.createTopic(destination) : jmsc.createQueue(destination);
    JMSProducer producer = jmsc.createProducer();
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
    doDispatch(jmsc, dest, producer, serializer, message);
  }

  protected MessageMarshaller marshaller(String marshallerName) {
    return findNamed(MessageMarshaller.class, marshallerName).orElse(null);
  }

}

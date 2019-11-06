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

import static org.corant.kernel.util.Instances.resolve;
import static org.corant.kernel.util.Instances.resolveApply;
import static org.corant.shared.util.AnnotationUtils.findAnnotation;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.ClassUtils.getUserClass;
import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.annotation.MessageSend;
import org.corant.suites.jms.shared.annotation.MessageSend.SerializationSchema;
import org.corant.suites.jms.shared.annotation.MessageSends;
import org.corant.suites.jms.shared.annotation.MessageSerialization.MessageSerializationLiteral;
import org.corant.suites.jms.shared.context.JMSContextProducer;
import org.corant.suites.jms.shared.context.MessageSerializer;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午5:00:02
 *
 */
public abstract class AbstractMessageSender implements MessageSender {

  @Override
  public void send(Serializable... annotatedPayloads) {
    Class<?> payloadClass = getUserClass(shouldNotNull(annotatedPayloads));
    Set<MessageSenderMetaData> sends = resolveMetaDatas(payloadClass);
    shouldBeTrue(isNotEmpty(sends),
        "The payload class %s must include either MessageSend or MessageSends annotaion.");
    for (MessageSenderMetaData send : sends) {
      send(annotatedPayloads, send.getConnectionFactoryId(), send.getDestination(),
          send.isMulticast(), send.getDurableSubscription(), send.getSessionMode(),
          send.getSerialization());
    }

  }

  public void send(Serializable message, String connectionFactoryId, String destination,
      boolean multicast, String durableSubscription, int sessionMode, SerializationSchema schema) {
    MessageSerializer serializer =
        resolve(MessageSerializer.class, MessageSerializationLiteral.of(schema)).get();
    final JMSContext jmsc =
        resolveApply(JMSContextProducer.class, b -> b.create(connectionFactoryId, sessionMode));
    Message body = serializer.serialize(jmsc, shouldNotNull(message));
    try {
      body.setStringProperty(MessageSerializer.MSG_SERIAL_SCHAME, schema.name());
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
    if (multicast) {
      jmsc.createProducer().send(jmsc.createTopic(destination), body);
    } else {
      jmsc.createProducer().send(jmsc.createQueue(destination), body);
    }
  }

  protected Set<MessageSenderMetaData> resolveMetaDatas(Class<?> messageClass) {
    Set<MessageSenderMetaData> sends = new LinkedHashSet<>();
    MessageSends anns = findAnnotation(messageClass, MessageSends.class, false);// FIXME inherit
    if (anns == null) {
      MessageSend ann = findAnnotation(messageClass, MessageSend.class, false);// FIXME inherit
      if (ann != null) {
        sends.add(new MessageSenderMetaData(ann));
      }
    } else {
      listOf(anns.value()).stream().map(MessageSenderMetaData::new).forEach(sends::add);
    }
    return sends;
  }

}

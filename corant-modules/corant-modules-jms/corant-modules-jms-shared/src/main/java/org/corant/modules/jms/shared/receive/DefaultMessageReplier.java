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
package org.corant.modules.jms.shared.receive;

import static org.corant.context.Instances.find;
import static org.corant.context.Instances.resolve;
import static org.corant.modules.jms.shared.MessagePropertyNames.REPLY_MSG_SERIAL_SCHAME;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import org.corant.modules.jms.shared.annotation.MessageSerialization.SerializationSchema;
import org.corant.modules.jms.shared.context.MessageSerializer;
import org.corant.modules.jms.shared.context.SecurityContextPropagator;
import org.corant.modules.jms.shared.context.SecurityContextPropagator.SimpleSecurityContextPropagator;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午11:57:15
 *
 */
public class DefaultMessageReplier implements MessageReplier {

  final MessageReceivingMetaData meta;

  protected DefaultMessageReplier(MessageReceivingMetaData meta) {
    this.meta = meta;
  }

  @Override
  public void reply(Session session, Message originalMessage, Object payload) throws JMSException {
    SecurityContextPropagator sctxs =
        find(SecurityContextPropagator.class).orElse(SimpleSecurityContextPropagator.INSTANCE);
    if (originalMessage != null && originalMessage.getJMSReplyTo() != null) {
      SerializationSchema serialSchema = SerializationSchema.JSON_STRING;
      String desSerialSchema = originalMessage.getStringProperty(REPLY_MSG_SERIAL_SCHAME);
      if (desSerialSchema != null) {
        serialSchema = SerializationSchema.valueOf(desSerialSchema);
      }
      MessageSerializer ms = resolve(MessageSerializer.class, serialSchema.qualifier());
      Message msg = ms.serialize(session, payload);
      String clid;
      if ((clid = originalMessage.getJMSCorrelationID()) != null) {
        msg.setJMSCorrelationID(clid);
      }
      byte[] clids;
      if ((clids = originalMessage.getJMSCorrelationIDAsBytes()) != null) {
        msg.setJMSCorrelationIDAsBytes(clids);
      }
      sctxs.propagate(msg);
      session.createProducer(originalMessage.getJMSReplyTo()).send(msg);
    }
    for (MessageReplyMetaData rd : meta.getReplies()) {
      Destination dest = rd.isMulticast() ? session.createTopic(rd.getDestination())
          : session.createQueue(rd.getDestination());
      MessageSerializer ms = resolve(MessageSerializer.class, rd.getSerialization().qualifier());
      Message msg = ms.serialize(session, payload);
      sctxs.propagate(msg);
      MessageProducer producer = session.createProducer(dest);
      producer.setDeliveryMode(rd.getDeliveryMode());
      producer.send(msg);
    }
  }

}

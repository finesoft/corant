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

import static org.corant.modules.jms.JMSNames.MSG_MARSHAL_SCHEMA_STD_JAVA;
import static org.corant.modules.jms.JMSNames.REPLY_MSG_MARSHAL_SCHEMA;
import static org.corant.modules.jms.JMSNames.SECURITY_CONTEXT_PROPERTY_NAME;
import static org.corant.shared.util.Strings.defaultString;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import org.corant.modules.jms.metadata.MessageReplyMetaData;
import org.corant.modules.jms.receive.ManagedMessageReceiveReplier;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午11:57:15
 */
public class DefaultMessageReplier implements ManagedMessageReceiveReplier {

  final MessageReceivingMetaData meta;
  final MessageReceivingMediator mediator;

  protected DefaultMessageReplier(MessageReceivingMetaData meta,
      MessageReceivingMediator mediator) {
    this.meta = meta;
    this.mediator = mediator;
  }

  @Override
  public void reply(Session session, Message originalMessage, Object payload) throws JMSException {
    if (originalMessage != null) {
      String sctx = originalMessage.getStringProperty(SECURITY_CONTEXT_PROPERTY_NAME);
      if (originalMessage.getJMSReplyTo() != null) {
        // FIXME use original message or no?
        String marshallerName =
            defaultString(originalMessage.getStringProperty(REPLY_MSG_MARSHAL_SCHEMA),
                MSG_MARSHAL_SCHEMA_STD_JAVA);
        Message msg = payload instanceof Message ? (Message) payload
            : mediator.getMessageMarshaller(marshallerName).serialize(session, payload);
        String clid;
        if ((clid = originalMessage.getJMSCorrelationID()) != null) {
          msg.setJMSCorrelationID(clid);
        }
        byte[] clids;
        if ((clids = originalMessage.getJMSCorrelationIDAsBytes()) != null) {
          msg.setJMSCorrelationIDAsBytes(clids);
        }
        if (sctx != null) {
          msg.setStringProperty(SECURITY_CONTEXT_PROPERTY_NAME, sctx);
        }
        session.createProducer(originalMessage.getJMSReplyTo()).send(msg);
      } else {
        for (MessageReplyMetaData rd : meta.getReplies()) {
          Destination dest = rd.isMulticast() ? session.createTopic(rd.getDestination())
              : session.createQueue(rd.getDestination());
          Message msg = payload instanceof Message ? (Message) payload
              : mediator.getMessageMarshaller(rd.getMarshaller()).serialize(session, payload);
          if (sctx != null) {
            msg.setStringProperty(SECURITY_CONTEXT_PROPERTY_NAME, sctx);
          }
          MessageProducer producer = session.createProducer(dest);
          producer.setDeliveryMode(rd.getDeliveryMode());
          producer.send(msg);
        }
      }
    }
  }

}

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
package org.corant.modules.jms.shared.context;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import org.corant.modules.jms.shared.annotation.MessageSerialization;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 上午11:34:02
 *
 */
@ApplicationScoped
@MessageSerialization(schema = SerialSchema.JAVA_BUILTIN)
public class JavaMessageSerializer implements MessageSerializer {

  @SuppressWarnings("unchecked")
  @Override
  public <T> T deserialize(Message message, Class<T> clazz) {
    shouldBeTrue(message instanceof ObjectMessage);
    ObjectMessage objMsg = (ObjectMessage) message;
    try {
      return (T) objMsg.getObject();
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
  }

  @Override
  public Message serialize(JMSContext jmsContext, Object object) {
    ObjectMessage msg = jmsContext.createObjectMessage();
    if (object != null) {
      shouldBeTrue(object instanceof Serializable);
      try {
        msg.setObject((Serializable) object);
      } catch (JMSException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return resolveSchemaProperty(msg, SerialSchema.JAVA_BUILTIN);
  }

  @Override
  public Message serialize(Session session, Object object) {
    try {
      ObjectMessage msg = session.createObjectMessage();
      if (object != null) {
        shouldBeTrue(object instanceof Serializable);
        msg.setObject((Serializable) object);
      }
      return resolveSchemaProperty(msg, SerialSchema.JAVA_BUILTIN);
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
  }
}

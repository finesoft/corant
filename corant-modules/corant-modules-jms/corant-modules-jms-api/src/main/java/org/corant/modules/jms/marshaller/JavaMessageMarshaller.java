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
package org.corant.modules.jms.marshaller;

import static org.corant.modules.jms.JMSNames.MSG_MARSHAL_SCHEMA_STD_JAVA;
import static org.corant.shared.util.Assertions.shouldInstanceOf;
import java.io.Serializable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-jms-api
 *
 * @author bingo 上午11:41:39
 */
@ApplicationScoped
@Named(MSG_MARSHAL_SCHEMA_STD_JAVA)
public class JavaMessageMarshaller implements MessageMarshaller {

  @SuppressWarnings("unchecked")
  @Override
  public <T> T deserialize(Message message, Class<T> clazz) {
    ObjectMessage objMsg = shouldInstanceOf(message, ObjectMessage.class);
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
      try {
        msg.setObject(shouldInstanceOf(object, Serializable.class));
      } catch (JMSException e) {
        throw new CorantRuntimeException(e);
      }
    }
    return resolveSchemaProperty(msg, MSG_MARSHAL_SCHEMA_STD_JAVA);
  }

  @Override
  public Message serialize(Session session, Object object) {
    try {
      ObjectMessage msg = session.createObjectMessage();
      if (object != null) {
        msg.setObject(shouldInstanceOf(object, Serializable.class));
      }
      return resolveSchemaProperty(msg, MSG_MARSHAL_SCHEMA_STD_JAVA);
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
  }

}

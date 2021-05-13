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
package org.corant.modules.jms.shared.context;

import static org.corant.modules.jms.shared.MessagePropertyNames.MSG_SERIAL_SCHAME;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午5:14:28
 *
 */
public interface MessageSerializer {

  <T> T deserialize(Message message, Class<T> clazz);

  default <T extends Message> T resolveSchemaProperty(T message, SerialSchema schema) {
    try {
      message.setStringProperty(MSG_SERIAL_SCHAME, schema.name());
      return message;
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
  }

  Message serialize(JMSContext jmsContext, Object object);

  default Message serialize(Session session, Object object) {
    throw new NotSupportedException();
  }

}

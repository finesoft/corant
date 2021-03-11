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
package org.corant.suites.jms.shared.context;

import static org.corant.suites.jms.shared.MessagePropertyNames.MSG_SERIAL_SCHAME;
import java.io.Serializable;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.jms.shared.annotation.MessageSend.SerializationSchema;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午5:14:28
 *
 */
public interface MessageSerializer {

  <T> T deserialize(Message message, Class<T> clazz);

  default void resolveSchemaProperty(Message message, SerializationSchema schema) {
    try {
      message.setStringProperty(MSG_SERIAL_SCHAME, schema.name());
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
  }

  Message serialize(JMSContext jmsContext, Serializable object);

}

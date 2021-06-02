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
package org.corant.modules.jms.shared.marshaller;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Objects.forceCast;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;
import org.corant.modules.jms.marshaller.MessageMarshaller;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 上午11:34:02
 *
 */
@ApplicationScoped
@Named("MAP")
public class MapMessageMarshaller implements MessageMarshaller {

  @SuppressWarnings("rawtypes")
  @Override
  public <T> T deserialize(Message message, Class<T> clazz) {
    shouldBeTrue(message instanceof MapMessage);
    shouldBeTrue(Map.class.isAssignableFrom(clazz));
    MapMessage mapMsg = (MapMessage) message;
    Map<String, Object> result = new HashMap<>();
    try {
      Enumeration en = mapMsg.getMapNames();
      while (en.hasMoreElements()) {
        Object e = en.nextElement();
        result.put(e.toString(), mapMsg.getObject(e.toString()));
      }
    } catch (JMSException e) {
      throw new CorantRuntimeException(e);
    }
    return forceCast(result);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public Message serialize(JMSContext jmsContext, Object object) {
    MapMessage mapMsg = jmsContext.createMapMessage();
    if (object != null) {
      shouldBeTrue(object instanceof Map);
      ((Map) object).forEach((t, u) -> {
        try {
          mapMsg.setObject(t.toString(), u);
        } catch (JMSException e) {
          throw new CorantRuntimeException(e);
        }
      });
    }
    return resolveSchemaProperty(mapMsg, "MAP");
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public Message serialize(Session session, Object object) {
    try {
      MapMessage mapMsg = session.createMapMessage();
      if (object != null) {
        shouldBeTrue(object instanceof Map);
        ((Map) object).forEach((t, u) -> {
          try {
            mapMsg.setObject(t.toString(), u);
          } catch (JMSException e) {
            throw new CorantRuntimeException(e);
          }
        });
      }
      return resolveSchemaProperty(mapMsg, "MAP");
    } catch (JMSException e1) {
      throw new CorantRuntimeException(e1);
    }
  }
}

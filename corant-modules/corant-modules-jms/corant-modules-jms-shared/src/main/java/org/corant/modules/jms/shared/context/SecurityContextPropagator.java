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

import static org.corant.context.Instances.find;
import static org.corant.modules.jms.JMSNames.SECURITY_CONTEXT_PROPERTY_NAME;
import static org.corant.shared.util.Strings.isNotBlank;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import org.corant.context.security.SecurityContext;
import org.corant.context.security.SecurityContextSerializer;
import org.corant.context.security.SecurityContextSerializer.Base64SecurityContextSerializer;
import org.corant.context.security.SecurityContexts;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午4:04:48
 *
 */
public interface SecurityContextPropagator {

  SecurityContext extract(Message message);

  void propagate(JMSProducer producer);

  void propagate(Message message);

  class SimpleSecurityContextPropagator implements SecurityContextPropagator {

    public static final SimpleSecurityContextPropagator INSTANCE =
        new SimpleSecurityContextPropagator();

    @Override
    public SecurityContext extract(Message message) {
      try {
        if (message != null && message.propertyExists(SECURITY_CONTEXT_PROPERTY_NAME)) {
          String currentSerial = message.getStringProperty(SECURITY_CONTEXT_PROPERTY_NAME);
          if (isNotBlank(currentSerial)) {
            return find(SecurityContextSerializer.class)
                .orElse(Base64SecurityContextSerializer.INSTANCE).deserialize(currentSerial);
          }
        }
        return null;
      } catch (JMSException e) {
        throw new CorantRuntimeException(e);
      }
    }

    @Override
    public void propagate(JMSProducer producer) {
      SecurityContext current;
      if (producer != null && (current = SecurityContexts.getCurrent()) != null) {
        String data = find(SecurityContextSerializer.class)
            .orElse(Base64SecurityContextSerializer.INSTANCE).serialize(current);
        if (data != null) {
          producer.setProperty(SECURITY_CONTEXT_PROPERTY_NAME, data);
        }
      }
    }

    @Override
    public void propagate(Message message) {
      SecurityContext current;
      if (message != null && (current = SecurityContexts.getCurrent()) != null) {
        String data = find(SecurityContextSerializer.class)
            .orElse(Base64SecurityContextSerializer.INSTANCE).serialize(current);
        if (data != null) {
          try {
            message.setStringProperty(SECURITY_CONTEXT_PROPERTY_NAME, data);
          } catch (JMSException e) {
            throw new CorantRuntimeException(e);
          }
        }
      }
    }

  }
}

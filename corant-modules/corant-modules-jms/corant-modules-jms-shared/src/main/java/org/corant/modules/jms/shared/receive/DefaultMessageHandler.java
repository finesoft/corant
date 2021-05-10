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
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.jms.Message;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.context.security.SecurityContext;
import org.corant.context.security.SecurityContexts;
import org.corant.modules.jms.shared.MessagePropertyNames;
import org.corant.modules.jms.shared.annotation.MessageSerialization.MessageSerializationLiteral;
import org.corant.modules.jms.shared.context.MessageSerializer;
import org.corant.modules.jms.shared.context.SecurityContextPropagator;
import org.corant.modules.jms.shared.context.SecurityContextPropagator.SimpleSecurityContextPropagator;
import org.corant.shared.exception.CorantRuntimeException;

public class DefaultMessageHandler implements MessageHandler {

  final ContextualMethodHandler method;
  final Class<?> messageClass;
  final Logger logger = Logger.getLogger(DefaultMessageHandler.class.getName());

  protected DefaultMessageHandler(ContextualMethodHandler method) {
    this.method = method;
    messageClass = method.getMethod().getParameters()[0].getType();
  }

  @Override
  public Object onMessage(Message message) {
    try {
      resolveSecurityContext(message);
      return method.invoke(resolvePayload(message));
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | JMSException e) {
      throw new CorantRuntimeException(e);
    } finally {
      SecurityContexts.setCurrent(null);
    }
  }

  protected Object resolvePayload(Message message) throws JMSException {
    String serialSchema = message.getStringProperty(MessagePropertyNames.MSG_SERIAL_SCHAME);
    if (isNotBlank(serialSchema)) {
      if (!Message.class.isAssignableFrom(messageClass)) {
        MessageSerializer serializer =
            resolve(MessageSerializer.class, MessageSerializationLiteral.of(serialSchema));
        return serializer.deserialize(message, messageClass);
      } else {
        logger.warning(() -> String.format(
            "The message has serialization scheme property, but the message consumer still use the native javax.jms.Message as method %s parameter type.",
            method));
      }
    }
    return message;
  }

  protected void resolveSecurityContext(Message message) {
    try {
      SecurityContext ctx = find(SecurityContextPropagator.class)
          .orElse(SimpleSecurityContextPropagator.INSTANCE).extract(message);
      SecurityContexts.setCurrent(ctx);
    } catch (Exception e) {
      logger.log(Level.SEVERE, e,
          () -> "Resolve security context propagation from message occurred error!");
    }
  }

}

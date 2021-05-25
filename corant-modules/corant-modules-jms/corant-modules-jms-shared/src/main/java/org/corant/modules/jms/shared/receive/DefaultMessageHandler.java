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
import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.jms.Message;
import org.corant.context.proxy.ContextualMethodHandler;
import org.corant.context.security.SecurityContext;
import org.corant.context.security.SecurityContexts;
import org.corant.modules.jms.shared.MessagePropertyNames;
import org.corant.modules.jms.shared.context.SecurityContextPropagator;
import org.corant.modules.jms.shared.context.SerialSchema;
import org.corant.modules.jms.shared.context.SecurityContextPropagator.SimpleSecurityContextPropagator;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午8:48:45
 *
 */
public class DefaultMessageHandler implements MessageHandler {

  protected static final Logger logger = Logger.getLogger(DefaultMessageHandler.class.getName());
  final ContextualMethodHandler method;
  final MessageReceivingMediator mediator;
  final Class<?> messageClass;

  protected DefaultMessageHandler(MessageReceivingMetaData meta,
      MessageReceivingMediator mediator) {
    method = meta.getMethod();
    messageClass = method.getMethod().getParameters()[0].getType();
    this.mediator = mediator;
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
    if (!Message.class.isAssignableFrom(messageClass)) {
      SerialSchema serialSchema = SerialSchema
          .valueOf(shouldNotBlank(message.getStringProperty(MessagePropertyNames.MSG_SERIAL_SCHAME),
              "Resolve message payload occurred error, missing [%s] information message header.",
              MessagePropertyNames.MSG_SERIAL_SCHAME));
      return mediator.getMessageSerializer(serialSchema).deserialize(message, messageClass);
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
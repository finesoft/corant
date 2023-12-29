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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.corant.modules.jms.marshaller.MessageMarshaller;

/**
 * corant-modules-jms-shared
 *
 * @author bingo 下午5:23:31
 */
public interface MessageReceivingMediator {

  boolean checkCancelled();

  MessageMarshaller getMessageMarshaller(String name);

  void onPostMessageHandled(Message message, Session session, Object result) throws JMSException;

  void onReceivingException(Exception e);
}

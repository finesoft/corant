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

import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import org.corant.modules.jms.shared.annotation.MessageSerialization;
import org.corant.modules.jms.shared.annotation.MessageSerialization.MessageSerializationLiteral;

public enum SerialSchema {

  JSON_STRING(TextMessage.class), BINARY(BytesMessage.class), JAVA_BUILTIN(
      ObjectMessage.class), MAP(MapMessage.class), KRYO(BytesMessage.class);

  private final MessageSerialization qualifier;
  private final Class<? extends Message> messageClass;

  SerialSchema(Class<? extends Message> messageClass) {
    qualifier = MessageSerializationLiteral.of(this);
    this.messageClass = messageClass;
  }

  public Class<? extends Message> messageClass() {
    return messageClass;
  }

  public MessageSerialization qualifier() {
    return qualifier;
  }
}
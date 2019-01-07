/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant.asosat.ddd.message;

import org.corant.asosat.ddd.domain.shared.DynamicAttributes.DynamicAttributeMap;
import org.corant.suites.ddd.annotation.stereotype.Messages;
import org.corant.suites.ddd.message.Message.ExchangedMessage;

/**
 * @author bingo 下午4:33:54
 *
 */
@Messages
public class BaseExchangedMessage implements ExchangedMessage {

  private static final long serialVersionUID = -746666508089335396L;

  private BaseMessageIdentifier originalMessage;

  private BaseMessageMetadata metadata;

  private DynamicAttributeMap payload;

  public BaseExchangedMessage() {}

  public BaseExchangedMessage(AbstractBaseMessage message) {
    originalMessage = new BaseMessageIdentifier(message);
    metadata = message.getMetadata();
    payload = message.getPayload();
  }


  @Override
  public BaseMessageMetadata getMetadata() {
    return metadata;
  }

  @Override
  public BaseMessageIdentifier getOriginalMessage() {
    return originalMessage;
  }

  @Override
  public DynamicAttributeMap getPayload() {
    return payload;
  }

  @Override
  public String toString() {
    return "BaseExchangedMessage [metadata=" + metadata + ", payload=" + payload + "]";
  }

}

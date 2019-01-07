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

import org.corant.suites.ddd.message.Message;

/**
 * @author bingo 上午11:49:10
 *
 */
public class DefaultTransientMessage implements Message {

  private static final long serialVersionUID = -6542863705459821423L;

  Object payload;

  MessageMetadata metadata;

  public DefaultTransientMessage() {}

  public DefaultTransientMessage(ExchangedMessage message) {
    payload = message.getPayload();
    metadata = message.getMetadata();
  }

  @Override
  public MessageMetadata getMetadata() {
    return metadata;
  }

  @Override
  public Object getPayload() {
    return payload;
  }


}

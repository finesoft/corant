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

import javax.enterprise.context.ApplicationScoped;
import javax.jms.Queue;
import org.corant.asosat.ddd.util.JsonUtils;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.Message.ExchangedMessage;

/**
 * @author bingo 下午3:42:57
 *
 */
@InfrastructureServices
@ApplicationScoped
public abstract class AbstractBaseJmsMessageSender extends AbstractJmsMessageSender {

  public AbstractBaseJmsMessageSender() {}

  @Override
  protected boolean convertAndSend(Queue queue, ExchangedMessage message) {
    getProducer().send(queue, JsonUtils.toJsonStr(message));
    return true;
  }

}

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

import javax.jms.JMSException;
import javax.jms.Message;
import org.corant.asosat.ddd.util.JsonUtils;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.suites.ddd.message.Message.ExchangedMessage;

/**
 * @author bingo 下午12:02:33
 *
 */
public abstract class AbstractBaseJmsMessageReceiver extends AbstractJmsMessageReceiver {

  public AbstractBaseJmsMessageReceiver() {}

  @Override
  protected ExchangedMessage convert(Message message) {
    try {
      return JsonUtils.fromJsonStr(message.getBody(String.class), BaseExchangedMessage.class);
    } catch (JMSException e) {
      throw new GeneralRuntimeException(e);
    }
  }

}

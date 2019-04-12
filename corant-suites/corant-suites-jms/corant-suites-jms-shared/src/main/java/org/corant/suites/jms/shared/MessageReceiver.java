/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.suites.jms.shared;

import java.util.function.Consumer;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.XAJMSContext;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-suites-jms-shared
 *
 * @author bingo 下午3:19:31
 *
 */
public interface MessageReceiver extends MessageListener {

  /**
   * corant-suites-jms-artemis
   *
   * @author bingo 下午4:26:21
   *
   */
  public static class MessageReceiverImpl implements MessageReceiver {

    private final Consumer<Message> consumer;
    private final JMSContext jmsc;

    /**
     * @param consumer
     */
    protected MessageReceiverImpl(JMSContext jmsc, Consumer<Message> consumer) {
      super();
      this.consumer = consumer;
      this.jmsc = jmsc;
    }

    @Override
    public void onMessage(Message message) {
      int sessionMode = jmsc.getSessionMode();
      try {
        consumer.accept(message);
        if (sessionMode == JMSContext.CLIENT_ACKNOWLEDGE) {
          message.acknowledge();
        } else if (sessionMode == JMSContext.SESSION_TRANSACTED
            && !(jmsc instanceof XAJMSContext)) {
          jmsc.commit();
        }
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }

  }
}

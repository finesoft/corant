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

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.corant.Corant;
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.suites.bundle.GlobalMessageCodes;
import org.corant.suites.ddd.message.Message.ExchangedMessage;
import org.corant.suites.ddd.message.MessageService;

/**
 * @author bingo 下午6:10:11
 *
 */
public abstract class MemonyMessageTesting {

  private static Logger logger = LogManager.getLogger(MemonyMessageTesting.class.getName());

  private static Random rd = new Random();

  public static void test(ExchangedMessage msg) {
    logger.debug(String.format("Send message [%s] [%s] [%s] to globale bus!", msg.queueName(),
        msg.getOriginalMessage().getId(), msg.getPayload().toString()));
    CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(Long.valueOf(rd.nextInt(1000) % 501 + 50));
      } catch (InterruptedException e) {
      }
    }).whenCompleteAsync((r, e) -> {
      if (e != null) {
        throw new GeneralRuntimeException(e, GlobalMessageCodes.ERR_SYS);
      }
      logger.debug(String.format("Receive message [%s] [%s] [%s] from globale bus!",
          msg.queueName(), msg.getOriginalMessage().getId(), msg.getPayload().toString()));
      Corant.resolveManageable(MessageService.class).receive(msg);
    });
  }

}

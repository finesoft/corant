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
package org.corant.modules.ddd.shared.unitwork;

import java.util.LinkedList;
import jakarta.transaction.Transaction;
import org.corant.modules.ddd.Message;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * The JPA unit of work based on JTA XA resource transaction boundaries, generally used for
 * single/multi database and/or use XA message queue scenarios. Through JTA transaction management,
 * the state of the business entity are persisted and the messages are sent before the transaction
 * is committed.<br>
 * This solution can theoretically guarantee full consistency.
 * </p>
 *
 * @author bingo 上午11:38:39
 *
 */
public class JTAXAJPAUnitOfWork extends AbstractJTAJPAUnitOfWork {

  protected JTAXAJPAUnitOfWork(JTAXAJPAUnitOfWorksManager manager, Transaction transaction) {
    super(manager, transaction);
    logger.fine(() -> String.format("Begin unit of work [%s].", transaction.toString()));
  }

  @Override
  protected JTAXAJPAUnitOfWorksManager getManager() {
    return (JTAXAJPAUnitOfWorksManager) super.getManager();
  }

  @Override
  protected void handleMessage() {
    logger.fine(() -> String.format(
        "Sorted the flushed messages and store them if necessary, dispatch them to the message dispatcher, before %s completion.",
        transaction.toString()));
    LinkedList<WrappedMessage> messages = new LinkedList<>();
    extractMessages(messages);
    int cycles = 128;
    WrappedMessage wm;
    while ((wm = messages.poll()) != null) {
      messageDispatcher.accept(new Message[] {wm.delegate});
      if (extractMessages(messages) && --cycles < 0) {
        throw new CorantRuntimeException("Can not handle messages!");
      }
    }
  }

}

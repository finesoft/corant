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

import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.LinkedList;
import javax.transaction.Transaction;
import org.corant.modules.ddd.Message;
import org.corant.modules.ddd.MessageStorage;
import org.corant.modules.ddd.SagaService;
import org.corant.shared.exception.CorantRuntimeException;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * The JPA unit of work based on JTA local resource transaction (non-XA) boundaries, generally used
 * for single database and/or use non-XA message queue scenarios. Through JTA transaction
 * management, the state of the business entity and messages are persisted before the transaction is
 * committed (in general, the message also uses the same database as the business entities use, so
 * as to keep the message from being lost), and the messages are sent(generally use async sending)
 * after the transaction is successfully. If the message sending is unsuccessful, some retry
 * mechanism is required, and after the message is successfully sent, it needs to be cleaned up.<br>
 * This solution cannot guarantee full consistency, usually this solution is used with SAGA /LRA and
 * other finally consistent components.
 * </p>
 *
 * @author bingo 上午11:38:39
 *
 */
public class JTARLJPAUnitOfWork extends AbstractJTAJPAUnitOfWork {

  protected final MessageStorage messageStorage;
  protected final SagaService sagaService; // FIXME Is it right to do so?
  protected final LinkedList<Message> storedMessages = new LinkedList<>();

  protected JTARLJPAUnitOfWork(JTARLJPAUnitOfWorksManager manager, Transaction transaction) {
    super(manager, transaction);
    messageStorage = manager.getMessageStorage();
    sagaService = defaultObject(manager.getSagaService(), SagaService::empty);
    messageDispatcher.prepare();
    messageStorage.prepare();
    sagaService.prepare();
    logger.fine(() -> String.format("Begin unit of work [%s].", transaction.toString()));
  }

  @Override
  public void complete(boolean success) {
    if (success) {
      int messageSize = sizeOf(storedMessages);
      messageDispatcher.accept(storedMessages.toArray(new Message[messageSize]));
    }
    super.complete(success);
  }

  @Override
  protected void clear() {
    try {
      storedMessages.clear();
    } finally {
      super.clear();
    }
  }

  @Override
  protected JTARLJPAUnitOfWorksManager getManager() {
    return (JTARLJPAUnitOfWorksManager) super.getManager();
  }

  @Override
  protected void handleMessage() {
    logger.fine(() -> String.format(
        "Sorted the flushed messages and trigger them if necessary, store them to the message storage, before %s completion.",
        transaction.toString()));
    LinkedList<WrappedMessage> messages = new LinkedList<>();
    extractMessages(messages);
    int cycles = 128;
    WrappedMessage wm;
    while ((wm = messages.poll()) != null) {
      final Message msg = wm.delegate;
      storedMessages.add(messageStorage.apply(msg));
      sagaService.trigger(msg);// FIXME Is it right to do so?
      if (extractMessages(messages) && --cycles < 0) {
        throw new CorantRuntimeException("Can not handle messages!");
      }
    }
  }

}

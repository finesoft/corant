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
package org.corant.suites.ddd.unitwork;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.transaction.Synchronization;
import org.corant.suites.ddd.message.Message;
import org.corant.suites.ddd.message.MessageService;
import org.corant.suites.ddd.message.MessageService.MessageConvertor;
import org.corant.suites.ddd.model.Entity.EntityManagerProvider;
import org.corant.suites.ddd.saga.SagaService;
import org.corant.suites.ddd.unitwork.UnitOfWorksManager.UnitOfWorksHandler;
import org.corant.suites.ddd.unitwork.UnitOfWorksManager.UnitOfWorksListener;

/**
 * @author bingo 下午7:13:58
 */
public abstract class AbstractUnitOfWork
    implements UnitOfWork, Synchronization, EntityManagerProvider {

  protected final transient Logger logger = Logger.getLogger(this.getClass().toString());
  protected final List<Message> message = new LinkedList<>();
  protected final AbstractUnitOfWorksManager manager;
  protected final Stream<UnitOfWorksHandler> handlers;
  protected final Stream<UnitOfWorksListener> listeners;
  protected final MessageService messageService;
  protected final SagaService sagaService;
  protected final MessageConvertor messageConvertor;
  protected volatile boolean activated = false;


  protected AbstractUnitOfWork(AbstractUnitOfWorksManager manager) {
    this.manager = manager;
    handlers = manager.getHandlers();
    listeners = manager.getListeners();
    messageService = manager.getMessageService();
    messageConvertor = manager.getMessageService().getConvertor();
    sagaService = manager.getSagaService();
    activated = true;
  }

  @Override
  public void complete(boolean success) {
    activated = false;
    if (success && !message.isEmpty()) {
      message.stream().sorted(Message::compare).map(messageConvertor::to).filter(Objects::nonNull)
          .forEachOrdered(messageService::send);
    }
  }

  protected void clear() {
    message.clear();
  }

  protected UnitOfWorksManager getManager() {
    return manager;
  }

  protected List<Message> getMessage() {
    return message;
  }

  protected void handlePostCompleted(final Object registration, final boolean success) {
    manager.getListeners().forEach(listener -> {
      try {
        listener.onCompleted(registration, success);
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex, () -> "Handle UOW post-completed occurred error!");
      }
    });
  }

  protected void handlePreComplete() {
    manager.getHandlers().forEach(handler -> {
      try {
        handler.onPreComplete(this);
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex, () -> "Handle UOW pre-complete occurred error!");
      }
    });
  }
}

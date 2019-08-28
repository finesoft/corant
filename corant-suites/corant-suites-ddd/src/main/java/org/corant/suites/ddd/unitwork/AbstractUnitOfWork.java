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
package org.corant.suites.ddd.unitwork;

import static org.corant.shared.util.ObjectUtils.defaultObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.corant.kernel.api.PersistenceService;
import org.corant.suites.ddd.message.MessageDispatcher;
import org.corant.suites.ddd.message.MessageStorage;
import org.corant.suites.ddd.saga.SagaService;

/**
 * @author bingo 下午7:13:58
 */
public abstract class AbstractUnitOfWork implements UnitOfWork {

  protected final transient Logger logger = Logger.getLogger(this.getClass().toString());
  protected final AbstractUnitOfWorksManager manager;
  protected final PersistenceService persistenceService;
  protected final MessageDispatcher messageDispatcher;
  protected final MessageStorage messageStorage;
  protected final SagaService sagaService; // FIXME Is it right to do so?
  protected volatile boolean activated = false;

  protected AbstractUnitOfWork(AbstractUnitOfWorksManager manager) {
    this.manager = manager;
    messageDispatcher = manager.getMessageDispatcher();
    messageStorage = manager.getMessageStorage();
    persistenceService = manager.getPersistenceService();
    sagaService = defaultObject(manager.getSagaService(), SagaService.empty());
    messageDispatcher.prepare();
    messageStorage.prepare();
    sagaService.prepare();
    activated = true;
  }

  @Override
  public void complete(boolean success) {
    activated = false;
  }

  protected UnitOfWorksManager getManager() {
    return manager;
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

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

import static org.corant.context.Beans.find;
import static org.corant.shared.util.Empties.isNotEmpty;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transaction;
import org.corant.context.ContainerEvents.PostContainerStartedEvent;
import org.corant.modules.ddd.Message;
import org.corant.modules.ddd.MessageDispatcher;
import org.corant.modules.ddd.MessageStorage;
import org.corant.modules.ddd.SagaService;
import org.corant.modules.ddd.annotation.InfrastructureServices;
import org.corant.modules.ddd.shared.annotation.JTARL;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-modules-ddd-shared
 *
 * <p>
 * The JTA JPA unit of works manager, use for create and destroy the {@link JTARLJPAUnitOfWork}
 * provide the necessary message storage service and message dispatch service for the unit of work.
 * </p>
 *
 * @author bingo 下午2:14:21
 *
 */
@JTARL
@ApplicationScoped
@InfrastructureServices
public class JTARLJPAUnitOfWorksManager extends AbstractJTAJPAUnitOfWorksManager {

  protected final ExecutorService dispatcher = Executors.newSingleThreadExecutor();

  @Inject
  protected UnitOfWorks unitOfWorks;

  @Inject
  @Any
  protected Instance<MessageStorage> messageStorage;

  @Inject
  @Any
  protected Instance<SagaService> sagaService;

  @Inject
  @Any
  protected Instance<Supplier<List<Message>>> lastUndispatchMessages;

  @Inject
  @ConfigProperty(name = "corant.ddd.unitofwork.use-rl.termination-timeout", defaultValue = "PT5S")
  protected Duration terminationTimeout;

  @Override
  public MessageDispatcher getMessageDispatcher() {
    return msgs -> dispatcher
        .submit(() -> find(MessageDispatcher.class).orElse(MessageDispatcher.empty()).accept(msgs));
  }

  public MessageStorage getMessageStorage() {
    return messageStorage.isResolvable() ? messageStorage.get() : MessageStorage.empty();
  }

  public SagaService getSagaService() {
    return sagaService.isResolvable() ? sagaService.get() : SagaService.empty();
  }

  @Override
  protected JTARLJPAUnitOfWork buildUnitOfWork(Transaction transaction) {
    return new JTARLJPAUnitOfWork(this, transaction);
  }

  protected void onPostContainReady(@Observes PostContainerStartedEvent e) {
    if (!unitOfWorks.isUseJtaXa() && lastUndispatchMessages.isResolvable()) {
      List<Message> messages = lastUndispatchMessages.get().get();
      if (isNotEmpty(messages)) {
        dispatcher.execute(() -> find(MessageDispatcher.class).orElse(MessageDispatcher.empty())
            .accept(messages.toArray(new Message[messages.size()])));
      }
    }
  }

  @PreDestroy
  protected void onPreDestroy() {
    final long ms = terminationTimeout.toMillis();
    try {
      dispatcher.shutdown();
      if (!dispatcher.awaitTermination(ms, TimeUnit.MILLISECONDS)) {
        logger.log(Level.WARNING,
            () -> "Terminate JTA RL JPA unit of work manager dispatcher timeout!");
      }
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, e,
          () -> "Can not terminate JTA RL JPA unit of work manager dispatcher.");
      Thread.currentThread().interrupt();
    }
  }

}

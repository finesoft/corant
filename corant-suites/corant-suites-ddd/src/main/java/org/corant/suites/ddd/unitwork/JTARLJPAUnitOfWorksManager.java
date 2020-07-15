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

import static org.corant.suites.cdi.Instances.find;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.transaction.Transaction;
import org.corant.suites.ddd.annotation.qualifier.JTARL;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.MessageDispatcher;
import org.corant.suites.ddd.message.MessageStorage;
import org.corant.suites.ddd.saga.SagaService;

/**
 * corant-suites-ddd
 *
 * <p>
 * The JTA JPA unit of works manager, use for create and destroy the {@link JTARLJPAUnitOfWork}
 * provide the necessary message stroage service and message dispatch service for the unit of work.
 * </p>
 *
 * @author bingo 下午2:14:21
 *
 */
@JTARL
@ApplicationScoped
@InfrastructureServices
public class JTARLJPAUnitOfWorksManager extends AbstractJTAJPAUnitOfWorksManager {

  protected final ExecutorService es = Executors.newSingleThreadExecutor();

  @Inject
  @Any
  protected Instance<MessageStorage> messageStorage;

  @Inject
  @Any
  protected Instance<SagaService> sagaService;

  @Override
  public MessageDispatcher getMessageDispatcher() {
    return (msgs) -> {
      es.submit(() -> {
        find(MessageDispatcher.class).orElse(MessageDispatcher.empty()).accept(msgs);
      });
    };
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

}

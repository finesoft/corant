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
package org.corant.asosat.ddd.unitwork;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.corant.Corant;
import org.corant.asosat.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.asosat.ddd.message.MessageService;
import org.corant.asosat.ddd.saga.SagaService;
import org.corant.shared.exception.GeneralRuntimeException;

/**
 * corant-asosat-ddd
 *
 * @author bingo 上午11:39:15
 *
 */
@ApplicationScoped
@InfrastructureServices
public abstract class AbstractUnitOfWorksManager implements UnitOfWorksManager {

  protected ThreadLocal<AbstractUnitOfWork> UOWS;

  @Inject
  TransactionManager transactionManager;

  @Inject
  TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  public static DefaultUnitOfWork currentUnitOfWork() {
    Corant.cdi().select(AbstractUnitOfWorksManager.class).get();
    return AbstractUnitOfWorksManager.currentUnitOfWork();
  }

  @Override
  public UnitOfWork getCurrentUnitOfWorks() {
    return null;
  }

  @Override
  public Stream<UnitOfWorksHandler> getHandlers() {
    return null;
  }

  @Override
  public Stream<UnitOfWorksListener> getListeners() {
    return null;
  }

  @Override
  public MessageService getMessageService() {
    return null;
  }

  @Override
  public SagaService getSagaService() {
    return null;
  }

  public TransactionManager getTransactionManager() {
    return transactionManager;
  }

  public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
    return transactionSynchronizationRegistry;
  }

  protected AbstractUnitOfWork buildUnitOfWork(AbstractUnitOfWorksManager unitOfWorksManager,
      EntityManager entityManager, Transaction transaction) {
    return new DefaultUnitOfWork(unitOfWorksManager, entityManager, transaction);
  }

  protected abstract EntityManagerFactory getEntityManagerFactory();

  protected Map<?, ?> getEntityManagerProperties() {
    return Collections.emptyMap();
  }

  void clearCurrentUnitOfWorks(Object key) {
    UOWS.remove();
  }

  @PreDestroy
  void destroy() {
    clearCurrentUnitOfWorks(null);
  }

  @PostConstruct
  void init() {
    UOWS = ThreadLocal.withInitial(() -> {
      try {
        final Transaction tx = getTransactionManager().getTransaction();
        final EntityManager em = getEntityManagerFactory()
            .createEntityManager(SynchronizationType.SYNCHRONIZED, getEntityManagerProperties());
        AbstractUnitOfWork uow = buildUnitOfWork(this, em, tx);
        getTransactionSynchronizationRegistry().registerInterposedSynchronization(uow);
        return uow;
      } catch (SystemException e) {
        throw new GeneralRuntimeException(e, "");
      }
    });
  }


}

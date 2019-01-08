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

import java.util.Collections;
import java.util.Map;
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
import org.corant.kernel.exception.GeneralRuntimeException;
import org.corant.suites.ddd.annotation.qualifier.JPA;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.model.Entity.EntityManagerProvider;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午2:14:21
 *
 */
@JPA
@ApplicationScoped
@InfrastructureServices
public abstract class JtaJpaUnitOfWorksManager extends AbstractUnitOfWorksManager
    implements EntityManagerProvider {

  protected ThreadLocal<JtaJpaUnitOfWork> UOWS;

  @Inject
  TransactionManager transactionManager;

  @Inject
  TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  public static JtaJpaUnitOfWork currentUnitOfWork() {
    return Corant.cdi().select(JtaJpaUnitOfWorksManager.class, JPA.INST).get()
        .getCurrentUnitOfWorks();
  }

  @Override
  public JtaJpaUnitOfWork getCurrentUnitOfWorks() {
    return UOWS.get();
  }

  @Override
  public EntityManager getEntityManager() {
    return getCurrentUnitOfWorks().getEntityManager();
  }

  public TransactionManager getTransactionManager() {
    return transactionManager;
  }

  public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
    return transactionSynchronizationRegistry;
  }

  protected JtaJpaUnitOfWork buildUnitOfWork(EntityManager entityManager, Transaction transaction) {
    return new JtaJpaUnitOfWork(this, entityManager, transaction);
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
        JtaJpaUnitOfWork uow = buildUnitOfWork(em, tx);
        getTransactionSynchronizationRegistry().registerInterposedSynchronization(uow);
        return uow;
      } catch (SystemException e) {
        throw new GeneralRuntimeException(e, PkgMsgCds.ERR_UOW_CREATE);
      }
    });
  }

}

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
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SynchronizationType;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.ddd.annotation.stereotype.InfrastructureServices;
import org.corant.suites.ddd.message.MessageService;
import org.corant.suites.ddd.saga.SagaService;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午2:14:21
 *
 */
@ApplicationScoped
@InfrastructureServices
public class JpaUnitOfWorksManager extends AbstractUnitOfWorksManager {

  protected final Map<Object, Map<Annotation, JpaUnitOfWork>> UOWS = new ConcurrentHashMap<>();

  @Inject
  TransactionManager transactionManager;

  @Inject
  TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  @Inject
  Instance<EntityManagerFactory> entityManagerFactories;

  @Override
  public JpaUnitOfWork getCurrentUnitOfWork(Annotation qualifier) {
    try {
      final Annotation qf = defaultObject(qualifier, Default.Literal.INSTANCE);
      final Transaction curtx = getTransactionManager().getTransaction();
      return UOWS.computeIfAbsent(wrapUintOfWorksKey(curtx), (key) -> new ConcurrentHashMap<>())
          .computeIfAbsent(qf, (q) -> {
            logger.fine(() -> "Register an new unit of work with the current transacion context.");
            JpaUnitOfWork uow = buildUnitOfWork(buildEntityManager(q), unwrapUnifOfWorksKey(curtx));
            getTransactionSynchronizationRegistry().registerInterposedSynchronization(uow);
            return uow;
          });
    } catch (SystemException e) {
      throw new CorantRuntimeException(e, PkgMsgCds.ERR_UOW_CREATE);
    }
  }

  public Instance<EntityManagerFactory> getEntityManagerFactories() {
    return entityManagerFactories;
  }

  @Override
  public MessageService getMessageService() {
    return MessageService.empty();
  }

  @Override
  public SagaService getSagaService() {
    return SagaService.empty();
  }

  public TransactionManager getTransactionManager() {
    return transactionManager;
  }

  public TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
    return transactionSynchronizationRegistry;
  }

  protected EntityManager buildEntityManager(Annotation qualifier) {
    return getEntityManagerFactory(qualifier).createEntityManager(SynchronizationType.SYNCHRONIZED,
        getEntityManagerProperties());
  }

  protected JpaUnitOfWork buildUnitOfWork(EntityManager entityManager, Transaction transaction) {
    return new JpaUnitOfWork(this, entityManager, transaction);
  }

  protected EntityManagerFactory getEntityManagerFactory(Annotation qualifier) {
    if (qualifier == null) {
      return entityManagerFactories.get();
    } else {
      return entityManagerFactories.select(qualifier).get();
    }
  }

  protected Map<?, ?> getEntityManagerProperties() {
    return Collections.emptyMap();
  }

  protected Transaction unwrapUnifOfWorksKey(Object object) {
    return object == null ? null : (Transaction) object;
  }

  protected Object wrapUintOfWorksKey(Transaction transaction) {
    return transaction;// if use narayana arjuna then their TransactionImple hc/eq use Uid.
  }

  void clearCurrentUnitOfWorks(Object key) {
    logger.fine(() -> "Deregister the unit of work with the current transacion context.");
    UOWS.remove(key).clear();
  }

  @PreDestroy
  void destroy() {
    UOWS.clear();
  }
}

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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.corant.shared.exception.CorantRuntimeException;
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

  protected final Map<Object, JtaJpaUnitOfWork> UOWS = new ConcurrentHashMap<>();

  @Inject
  TransactionManager transactionManager;

  @Inject
  TransactionSynchronizationRegistry transactionSynchronizationRegistry;

  public static JtaJpaUnitOfWork currentUnitOfWork() {
    return Corant.instance().select(JtaJpaUnitOfWorksManager.class, JPA.INST).get()
        .getCurrentUnitOfWorks();
  }

  @Override
  public JtaJpaUnitOfWork getCurrentUnitOfWorks() {
    try {
      final Transaction curtx = getTransactionManager().getTransaction();
      return UOWS.computeIfAbsent(wrapUintOfWorksKey(curtx), (key) -> {
        logger.fine(() -> "Register an new unit of work with the current transacion context.");
        JtaJpaUnitOfWork uow = buildUnitOfWork(buildEntityManager(), unwrapUnifOfWorksKey(key));
        getTransactionSynchronizationRegistry().registerInterposedSynchronization(uow);
        return uow;
      });
    } catch (SystemException e) {
      throw new CorantRuntimeException(e, PkgMsgCds.ERR_UOW_CREATE);
    }
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

  protected Transaction unwrapUnifOfWorksKey(Object object) {
    return object == null ? null : (Transaction) object;
  }

  protected Object wrapUintOfWorksKey(Transaction transaction) {
    return transaction;// if use narayana arjuna then their TransactionImple hc/eq use Uid.
  }

  void clearCurrentUnitOfWorks(Object key) {
    logger.fine(() -> "Deregister the unit of work with the current transacion context.");
    UOWS.remove(key);
  }

  @PreDestroy
  void destroy() {
    UOWS.clear();
  }

  private EntityManager buildEntityManager() {
    return getEntityManagerFactory().createEntityManager(SynchronizationType.SYNCHRONIZED,
        getEntityManagerProperties());
  }

}

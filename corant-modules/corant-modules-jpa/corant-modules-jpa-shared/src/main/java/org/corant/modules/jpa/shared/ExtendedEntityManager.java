/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.jpa.shared;

import static java.lang.String.format;
import static org.corant.shared.util.Assertions.shouldNotNull;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;
import org.corant.modules.jta.shared.TransactionService;

/**
 * corant-modules-jpa-shared
 *
 * @author bingo 下午5:45:13
 */
public class ExtendedEntityManager implements EntityManager {

  static Logger logger = Logger.getLogger(ExtendedEntityManager.class.getName());

  final boolean transaction;
  volatile EntityManager delegate;
  volatile Supplier<EntityManager> delegateSupplier;

  protected ExtendedEntityManager(Supplier<EntityManager> delegateSupplier, boolean transaction) {
    this.delegateSupplier = shouldNotNull(delegateSupplier, "EntityManager supplier can't null");
    this.transaction = transaction;
  }

  @Override
  public void clear() {
    resolveDelegate().clear();
  }

  @Override
  public void close() {
    throw new IllegalStateException("Can't close managed entity manager");
  }

  @Override
  public boolean contains(Object entity) {
    return resolveDelegate().contains(entity);
  }

  @Override
  public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
    return resolveDelegate().createEntityGraph(rootType);
  }

  @Override
  public EntityGraph<?> createEntityGraph(String graphName) {
    return resolveDelegate().createEntityGraph(graphName);
  }

  @Override
  public Query createNamedQuery(String name) {
    return resolveDelegate().createNamedQuery(name);
  }

  @Override
  public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
    return resolveDelegate().createNamedQuery(name, resultClass);
  }

  @Override
  public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
    return resolveDelegate().createNamedStoredProcedureQuery(name);
  }

  @Override
  public Query createNativeQuery(String sqlString) {
    return resolveDelegate().createNativeQuery(sqlString);
  }

  @Override
  public Query createNativeQuery(String sqlString,
      @SuppressWarnings("rawtypes") Class resultClass) {
    return resolveDelegate().createNativeQuery(sqlString, resultClass);
  }

  @Override
  public Query createNativeQuery(String sqlString, String resultSetMapping) {
    return resolveDelegate().createNativeQuery(sqlString, resultSetMapping);
  }

  @Override
  public Query createQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery) {
    return resolveDelegate().createQuery(deleteQuery);
  }

  @Override
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
    return resolveDelegate().createQuery(criteriaQuery);
  }

  @Override
  public Query createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery) {
    return resolveDelegate().createQuery(updateQuery);
  }

  @Override
  public Query createQuery(String qlString) {
    return resolveDelegate().createQuery(qlString);
  }

  @Override
  public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
    return resolveDelegate().createQuery(qlString, resultClass);
  }

  @Override
  public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
    return resolveDelegate().createStoredProcedureQuery(procedureName);
  }

  @Override
  public StoredProcedureQuery createStoredProcedureQuery(String procedureName,
      @SuppressWarnings("rawtypes") Class... resultClasses) {
    return resolveDelegate().createStoredProcedureQuery(procedureName, resultClasses);
  }

  @Override
  public StoredProcedureQuery createStoredProcedureQuery(String procedureName,
      String... resultSetMappings) {
    return resolveDelegate().createStoredProcedureQuery(procedureName, resultSetMappings);
  }

  /**
   * TODO FIXME:
   *
   * After the JTA transaction has completed (either by transaction commit or rollback), the
   * container closes the entity manager by calling EntityManager.close. [88] Note that the JTA
   * transaction may rollback in a background thread (e.g., as a result of transaction timeout), in
   * which case the container should arrange for the entity manager to be closed but the Entity-
   * Manager.close method should not be concurrently invoked while the application is in an
   * EntityManager invocation.
   *
   * destroy
   */
  public synchronized void destroy() {
    if (delegate != null && delegate.isOpen()) {
      logger.fine(() -> format("Destroy entity manager %s", delegate));
      delegate.close();
    }
    delegate = null;
  }

  @Override
  public void detach(Object entity) {
    resolveDelegate().detach(entity);
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey) {
    return resolveDelegate().find(entityClass, primaryKey);
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
    return resolveDelegate().find(entityClass, primaryKey, lockMode);
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode,
      Map<String, Object> properties) {
    return resolveDelegate().find(entityClass, primaryKey, lockMode, properties);
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
    return resolveDelegate().find(entityClass, primaryKey, properties);
  }

  @Override
  public void flush() {
    resolveDelegate().flush();
  }

  @Override
  public CriteriaBuilder getCriteriaBuilder() {
    return resolveDelegate().getCriteriaBuilder();
  }

  @Override
  public Object getDelegate() {
    return resolveDelegate().getDelegate();
  }

  @Override
  public EntityGraph<?> getEntityGraph(String graphName) {
    return resolveDelegate().getEntityGraph(graphName);
  }

  @Override
  public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
    return resolveDelegate().getEntityGraphs(entityClass);
  }

  @Override
  public EntityManagerFactory getEntityManagerFactory() {
    return resolveDelegate().getEntityManagerFactory();
  }

  @Override
  public FlushModeType getFlushMode() {
    return resolveDelegate().getFlushMode();
  }

  @Override
  public LockModeType getLockMode(Object entity) {
    return resolveDelegate().getLockMode(entity);
  }

  @Override
  public Metamodel getMetamodel() {
    return resolveDelegate().getMetamodel();
  }

  @Override
  public Map<String, Object> getProperties() {
    return resolveDelegate().getProperties();
  }

  @Override
  public <T> T getReference(Class<T> entityClass, Object primaryKey) {
    return resolveDelegate().getReference(entityClass, primaryKey);
  }

  @Override
  public EntityTransaction getTransaction() {
    if (transaction) {
      throw new IllegalStateException(
          "A JTA EntityManager can not use the EntityTransaction API. See JPA 1.0 section 5.5");
    }
    return resolveDelegate().getTransaction();
  }

  @Override
  public boolean isJoinedToTransaction() {
    return resolveDelegate().isJoinedToTransaction();
  }

  @Override
  public boolean isOpen() {
    return resolveDelegate().isOpen();
  }

  @Override
  public void joinTransaction() {
    resolveDelegate().joinTransaction();
  }

  @Override
  public void lock(Object entity, LockModeType lockMode) {
    resolveDelegate().lock(entity, lockMode);
  }

  @Override
  public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
    resolveDelegate().lock(entity, lockMode, properties);
  }

  @Override
  public <T> T merge(T entity) {
    return resolveDelegate().merge(entity);
  }

  @Override
  public void persist(Object entity) {
    resolveDelegate().persist(entity);
  }

  @Override
  public void refresh(Object entity) {
    resolveDelegate().refresh(entity);
  }

  @Override
  public void refresh(Object entity, LockModeType lockMode) {
    resolveDelegate().refresh(entity, lockMode);
  }

  @Override
  public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
    resolveDelegate().refresh(entity, lockMode, properties);
  }

  @Override
  public void refresh(Object entity, Map<String, Object> properties) {
    resolveDelegate().refresh(entity, properties);
  }

  @Override
  public void remove(Object entity) {
    resolveDelegate().remove(entity);
  }

  @Override
  public void setFlushMode(FlushModeType flushMode) {
    resolveDelegate().setFlushMode(flushMode);
  }

  @Override
  public void setProperty(String propertyName, Object value) {
    resolveDelegate().setProperty(propertyName, value);
  }

  @Override
  public <T> T unwrap(Class<T> cls) {
    return resolveDelegate().unwrap(cls);
  }

  protected void checkTransaction() {
    if (transaction && !TransactionService.isCurrentTransactionActive()) {
      throw new TransactionRequiredException(
          "The transaction is required but is not active! See JPA 2.0 section 7.9.1");
    }
  }

  protected EntityTransaction obtainTransaction() {
    return resolveDelegate().getTransaction();
  }

  protected EntityManager resolveDelegate() {
    checkTransaction();
    if (delegate == null) {
      synchronized (this) {
        if (delegate == null) {
          delegate = shouldNotNull(delegateSupplier.get(), "EntityManager can't null");
          logger.fine(() -> format("Resolve entity manager %s", delegate));
        }
      }
    }
    return delegate;
  }
}

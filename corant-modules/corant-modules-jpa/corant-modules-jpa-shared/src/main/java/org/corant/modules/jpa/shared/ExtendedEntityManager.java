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

import java.util.List;
import java.util.Map;
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
 *
 */
public class ExtendedEntityManager implements EntityManager {

  final EntityManager delegate;
  final boolean transaction;

  protected ExtendedEntityManager(EntityManager delegate, boolean transaction) {
    this.delegate = delegate;
    this.transaction = transaction;
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public void close() {
    throw new IllegalStateException("Can't close managed entity manager");
  }

  @Override
  public boolean contains(Object entity) {
    return delegate.contains(entity);
  }

  @Override
  public <T> EntityGraph<T> createEntityGraph(Class<T> rootType) {
    return delegate.createEntityGraph(rootType);
  }

  @Override
  public EntityGraph<?> createEntityGraph(String graphName) {
    return delegate.createEntityGraph(graphName);
  }

  @Override
  public Query createNamedQuery(String name) {
    return delegate.createNamedQuery(name);
  }

  @Override
  public <T> TypedQuery<T> createNamedQuery(String name, Class<T> resultClass) {
    return delegate.createNamedQuery(name, resultClass);
  }

  @Override
  public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
    return delegate.createNamedStoredProcedureQuery(name);
  }

  @Override
  public Query createNativeQuery(String sqlString) {
    return delegate.createNativeQuery(sqlString);
  }

  @Override
  public Query createNativeQuery(String sqlString,
      @SuppressWarnings("rawtypes") Class resultClass) {
    return delegate.createNativeQuery(sqlString, resultClass);
  }

  @Override
  public Query createNativeQuery(String sqlString, String resultSetMapping) {
    return delegate.createNativeQuery(sqlString, resultSetMapping);
  }

  @Override
  public Query createQuery(@SuppressWarnings("rawtypes") CriteriaDelete deleteQuery) {
    return delegate.createQuery(deleteQuery);
  }

  @Override
  public <T> TypedQuery<T> createQuery(CriteriaQuery<T> criteriaQuery) {
    return delegate.createQuery(criteriaQuery);
  }

  @Override
  public Query createQuery(@SuppressWarnings("rawtypes") CriteriaUpdate updateQuery) {
    return delegate.createQuery(updateQuery);
  }

  @Override
  public Query createQuery(String qlString) {
    return delegate.createQuery(qlString);
  }

  @Override
  public <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
    return delegate.createQuery(qlString, resultClass);
  }

  @Override
  public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
    return delegate.createStoredProcedureQuery(procedureName);
  }

  @Override
  public StoredProcedureQuery createStoredProcedureQuery(String procedureName,
      @SuppressWarnings("rawtypes") Class... resultClasses) {
    return delegate.createStoredProcedureQuery(procedureName, resultClasses);
  }

  @Override
  public StoredProcedureQuery createStoredProcedureQuery(String procedureName,
      String... resultSetMappings) {
    return delegate.createStoredProcedureQuery(procedureName, resultSetMappings);
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
    if (delegate.isOpen()) {
      delegate.close();
    }
  }

  @Override
  public void detach(Object entity) {
    delegate.detach(entity);
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey) {
    return delegate.find(entityClass, primaryKey);
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
    return delegate.find(entityClass, primaryKey, lockMode);
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode,
      Map<String, Object> properties) {
    return delegate.find(entityClass, primaryKey, lockMode, properties);
  }

  @Override
  public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
    return delegate.find(entityClass, primaryKey, properties);
  }

  @Override
  public void flush() {
    delegate.flush();
  }

  @Override
  public CriteriaBuilder getCriteriaBuilder() {
    return delegate.getCriteriaBuilder();
  }

  @Override
  public Object getDelegate() {
    return delegate.getDelegate();
  }

  @Override
  public EntityGraph<?> getEntityGraph(String graphName) {
    return delegate.getEntityGraph(graphName);
  }

  @Override
  public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
    return delegate.getEntityGraphs(entityClass);
  }

  @Override
  public EntityManagerFactory getEntityManagerFactory() {
    return delegate.getEntityManagerFactory();
  }

  @Override
  public FlushModeType getFlushMode() {
    return delegate.getFlushMode();
  }

  @Override
  public LockModeType getLockMode(Object entity) {
    return delegate.getLockMode(entity);
  }

  @Override
  public Metamodel getMetamodel() {
    return delegate.getMetamodel();
  }

  @Override
  public Map<String, Object> getProperties() {
    return delegate.getProperties();
  }

  @Override
  public <T> T getReference(Class<T> entityClass, Object primaryKey) {
    return delegate.getReference(entityClass, primaryKey);
  }

  @Override
  public EntityTransaction getTransaction() {
    if (transaction) {
      throw new IllegalStateException(
          "A JTA EntityManager can not use the EntityTransaction API. See JPA 1.0 section 5.5");
    }
    return delegate.getTransaction();
  }

  @Override
  public boolean isJoinedToTransaction() {
    return delegate.isJoinedToTransaction();
  }

  @Override
  public boolean isOpen() {
    return delegate.isOpen();
  }

  @Override
  public void joinTransaction() {
    delegate.joinTransaction();
  }

  @Override
  public void lock(Object entity, LockModeType lockMode) {
    delegate.lock(entity, lockMode);
  }

  @Override
  public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
    delegate.lock(entity, lockMode, properties);
  }

  @Override
  public <T> T merge(T entity) {
    checkTransaction();
    return delegate.merge(entity);
  }

  @Override
  public void persist(Object entity) {
    checkTransaction();
    delegate.persist(entity);
  }

  @Override
  public void refresh(Object entity) {
    checkTransaction();
    delegate.refresh(entity);
  }

  @Override
  public void refresh(Object entity, LockModeType lockMode) {
    checkTransaction();
    delegate.refresh(entity, lockMode);
  }

  @Override
  public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
    checkTransaction();
    delegate.refresh(entity, lockMode, properties);
  }

  @Override
  public void refresh(Object entity, Map<String, Object> properties) {
    checkTransaction();
    delegate.refresh(entity, properties);
  }

  @Override
  public void remove(Object entity) {
    checkTransaction();
    delegate.remove(entity);
  }

  @Override
  public void setFlushMode(FlushModeType flushMode) {
    delegate.setFlushMode(flushMode);
  }

  @Override
  public void setProperty(String propertyName, Object value) {
    delegate.setProperty(propertyName, value);
  }

  @Override
  public String toString() {
    return "ExtendedEntityManager [delegate=" + delegate + ", transaction=" + transaction + "]";
  }
 
  @Override
  public <T> T unwrap(Class<T> cls) {
    return delegate.unwrap(cls);
  }

  protected void checkTransaction() {
    if (transaction && !TransactionService.isCurrentTransactionActive()) {
      throw new TransactionRequiredException(
          "The transaction is required but is not active! See JPA 2.0 section 7.9.1");
    }
  }

  protected EntityTransaction obtainTransaction() {
    return delegate.getTransaction();
  }
}

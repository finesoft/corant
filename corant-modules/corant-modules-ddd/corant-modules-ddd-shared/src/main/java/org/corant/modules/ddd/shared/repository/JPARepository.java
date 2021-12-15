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
package org.corant.modules.ddd.shared.repository;

import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Sets.linkedHashSetOf;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import org.corant.modules.ddd.Aggregate;
import org.corant.modules.ddd.Aggregate.AggregateIdentifier;
import org.corant.modules.ddd.Entity;
import org.corant.modules.ddd.Repository;
import org.corant.modules.jpa.shared.JPAQueries;
import org.corant.modules.jpa.shared.JPAQueries.AdvancedJPAQuery;
import org.corant.modules.jpa.shared.JPAQueries.JPAQuery;
import org.corant.modules.jpa.shared.JPAQueries.TypedJPAQuery;
import org.corant.shared.util.Objects;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午9:39:59
 *
 */
public interface JPARepository extends Repository<Query> {

  Logger logger = Logger.getLogger(JPARepository.class.getName());

  /**
   * {@link EntityManager#clear()}
   */
  default void clear() {
    getEntityManager().clear();
  }

  /**
   * {@link EntityManager#detach(Object)}
   *
   * @param entity detach
   */
  default void detach(Object entity) {
    getEntityManager().detach(entity);
  }

  /**
   * Remove the data for entities of the specified class (and its subclasses) from the cache.
   * {@link Cache#evict(Class)}
   *
   * @param entityClass evictCache
   */
  default void evictCache(Class<?> entityClass) {
    Cache cache = getEntityManagerFactory().getCache();
    if (cache != null) {
      cache.evict(entityClass);
    } else {
      logger.warning(() -> "There is not cache mechanism!");
    }
  }

  /**
   * Remove the data for the given entity from the cache. {@link Cache#evict(Class, Object)}
   *
   * @param entityClass
   * @param id evictCache
   */
  default void evictCache(Class<?> entityClass, Serializable id) {
    Cache cache = getEntityManagerFactory().getCache();
    if (cache != null) {
      cache.evict(entityClass, id);
    } else {
      logger.warning(() -> "There is not cache mechanism!");
    }
  }

  /**
   * Remove the data for the given entity from the cache.
   *
   * @param entity evictCache
   */
  default void evictCache(Entity entity) {
    if (entity == null || entity.getId() == null) {
      return;
    }
    this.evictCache(entity.getClass(), entity.getId());
  }

  /**
   * {@link EntityManager#flush()}
   */
  default void flush() {
    getEntityManager().flush();
  }

  /**
   * Retrieves entity by the given identifier. Search for an entity by the type of the given
   * identifier and the id of the given identifier .If the entity instance is contained in the
   * persistence context,it is returned from there.
   *
   * @param <T> the aggregate instance
   * @param identifier the aggregate id
   * @return an aggregate
   */
  @SuppressWarnings("unchecked")
  default <T> T get(AggregateIdentifier identifier) {
    if (identifier != null) {
      Class<?> cls = tryAsClass(identifier.getType());
      return (T) getEntityManager().find(cls, identifier.getId());
    }
    return null;
  }

  /**
   * {@link EntityManager#find(Class, Object)}
   */
  @Override
  default <T> T get(Class<T> entityClass, Serializable id) {
    return id != null ? getEntityManager().find(entityClass, id) : null;
  }

  /**
   * {@link EntityManager#find(Class, Object, LockModeType)}
   */
  default <T> T get(Class<T> entityClass, Serializable id, LockModeType lockMode) {
    return id != null ? getEntityManager().find(entityClass, id, lockMode) : null;
  }

  /**
   * {@link EntityManager#find(Class, Object, LockModeType, Map)}
   */
  default <T> T get(Class<T> entityClass, Serializable id, LockModeType lockMode,
      Map<String, Object> properties) {
    return id != null ? getEntityManager().find(entityClass, id, lockMode, properties) : null;
  }

  /**
   * Retrieves the aggregate by primary key and version number.Search for an aggregate of the
   * specified class and primary key and version number. If the aggregate instance is contained in
   * the persistence context,it is returned from there.
   *
   * @param <T> the aggregate type
   * @param entityClass the aggregate class
   * @param id the aggregate id
   * @param vn the aggregate version
   */
  default <T extends Aggregate> T get(Class<T> entityClass, Serializable id, long vn) {
    T entity = this.get(entityClass, id);
    return entity != null && entity.getVn().longValue() == vn ? entity : null;
  }

  /**
   * {@link EntityManager#find(Class, Object, Map)}
   */
  default <T> T get(Class<T> entityClass, Serializable id, Map<String, Object> properties) {
    return getEntityManager().find(entityClass, id, properties);
  }

  /**
   * Retrieves and object by query
   *
   * @param <T> the result type
   * @param query the JPAQuery
   * @return the result
   */
  default <T> T get(Query query) {
    query.setMaxResults(1);
    return forceCast(query.getSingleResult());
  }

  /**
   * Returns the JPA entity manager used for this repository
   */
  EntityManager getEntityManager();

  /**
   * Returns the JPA entity manager factory used for this repository
   */
  default EntityManagerFactory getEntityManagerFactory() {
    return getEntityManager().getEntityManagerFactory();
  }

  /**
   * {@link javax.persistence.PersistenceUnitUtil#isLoaded(Object)}
   */
  default boolean isLoaded(Object object) {
    return object != null && getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(object);
  }

  /**
   * {@link EntityManager#lock(Object, LockModeType)}
   */
  default void lock(Object object, LockModeType lockModeType) {
    if (getEntityManager().contains(object)) {
      getEntityManager().lock(object, lockModeType);
    }
  }

  /**
   * {@link EntityManager#lock(Object, LockModeType, Map)}
   */
  default void lock(Object object, LockModeType lockModeType, Map<String, Object> properties) {
    if (getEntityManager().contains(object)) {
      getEntityManager().lock(object, lockModeType, properties);
    }
  }

  @Override
  default <T> T merge(T entity) {
    return getEntityManager().merge(entity);
  }

  /**
   * Create name query
   *
   * @param name the named query name
   * @return a JPAQuery
   */
  default AdvancedJPAQuery namedQuery(final String name) {
    return JPAQueries.namedQuery(name).entityManager(this::getEntityManager);
  }

  /**
   * {@link JPAQueries#namedQuery(String, Class)}
   *
   * @param <T> the result type
   * @param name the name of a query defined in metadata
   * @param type the type of the query result
   */
  default <T> TypedJPAQuery<T> namedQuery(final String name, final Class<T> type) {
    return JPAQueries.namedQuery(name, type).entityManager(this::getEntityManager);
  }

  /**
   * {@link JPAQueries#namedStoredProcedureQuery(String)}
   *
   * @param name name assigned to the stored procedure query in metadata
   */
  default JPAQuery namedStoredProcedureQuery(final String name) {
    return JPAQueries.namedStoredProcedureQuery(name).entityManager(this::getEntityManager);
  }

  /**
   * {@link JPAQueries#nativeQuery(String)}
   *
   * @param sqlString a native SQL query string
   */
  default AdvancedJPAQuery nativeQuery(final String sqlString) {
    return JPAQueries.nativeQuery(sqlString).entityManager(this::getEntityManager);
  }

  /**
   * {@link JPAQueries#nativeQuery(String, Class)}
   *
   * @param <T> the result type
   * @param sqlString a native SQL query string
   * @param type the class of the resulting instance(s)
   */
  default <T> TypedJPAQuery<T> nativeQuery(final String sqlString, final Class<T> type) {
    return JPAQueries.nativeQuery(sqlString, type).entityManager(this::getEntityManager);
  }

  /**
   * {@link JPAQueries#nativeQuery(String, String)}
   *
   * @param sqlString a native SQL query string
   * @param resultSetMapping the name of the result set mapping
   */
  default AdvancedJPAQuery nativeQuery(final String sqlString, final String resultSetMapping) {
    return JPAQueries.nativeQuery(sqlString, resultSetMapping)
        .entityManager(this::getEntityManager);
  }

  @Override
  default <T> boolean persist(T entity) {
    getEntityManager().persist(entity);
    return true;
  }

  /**
   * {@link JPAQueries#query(CriteriaQuery)}
   *
   * @param <T> the result type
   * @param criteriaQuery a criteria query object
   */
  default <T> TypedJPAQuery<T> query(CriteriaQuery<T> criteriaQuery) {
    return JPAQueries.query(criteriaQuery).entityManager(this::getEntityManager);
  }

  /**
   * {@link JPAQueries#query(String)}
   *
   * @param qlString a Jakarta Persistence query string
   */
  default AdvancedJPAQuery query(final String qlString) {
    return JPAQueries.query(qlString).entityManager(this::getEntityManager);
  }

  /**
   * {@link JPAQueries#query(String, Class)}
   *
   * @param <T>
   * @param qlString a Jakarta Persistence query string
   * @param type the type of the query result
   * @return TypedJPAQuery
   */
  default <T> TypedJPAQuery<T> query(final String qlString, final Class<T> type) {
    return JPAQueries.query(qlString, type).entityManager(this::getEntityManager);
  }

  @Override
  default <T> boolean remove(T obj) {
    if (obj != null) {
      getEntityManager().remove(obj);
      return true;
    }
    return false;
  }

  /**
   * Retrieves the entities by primary keys
   *
   * @param <T> the result type
   * @param entityClass the entity class
   * @param ids the entity primary keys
   * @return entity list
   */
  default <T> List<T> select(Class<T> entityClass, Serializable... ids) {
    if (isEmpty(ids)) {
      return new ArrayList<>();
    } else {
      return linkedHashSetOf(ids).stream().map(i -> get(entityClass, i)).filter(Objects::isNotNull)
          .collect(Collectors.toList());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  default <T> List<T> select(Query query) {
    return defaultObject(query.getResultList(), ArrayList::new);
  }

  default JPAQuery storedProcedureQuery(final String procedureName) {
    return JPAQueries.storedProcedureQuery(procedureName).entityManager(this::getEntityManager);
  }

  default JPAQuery storedProcedureQuery(final String procedureName, final Class<?>... type) {
    return JPAQueries.storedProcedureQuery(procedureName, type)
        .entityManager(this::getEntityManager);
  }

  default JPAQuery storedProcedureQuery(final String procedureName,
      final String... resultSetMappings) {
    return JPAQueries.storedProcedureQuery(procedureName, resultSetMappings)
        .entityManager(this::getEntityManager);
  }
}

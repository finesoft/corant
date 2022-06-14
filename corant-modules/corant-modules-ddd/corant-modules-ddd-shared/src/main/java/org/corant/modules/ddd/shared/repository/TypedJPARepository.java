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
package org.corant.modules.ddd.shared.repository;

import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import org.corant.modules.ddd.Entity;
import org.corant.modules.ddd.TypedRepository;
import org.corant.modules.jpa.shared.JPAQueries;
import org.corant.modules.jpa.shared.JPAQueries.TypedJPAQuery;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午6:15:02
 *
 */
public interface TypedJPARepository<T extends Entity> extends TypedRepository<T, Query> {

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
  default T detach(T entity) {
    getEntityManager().detach(entity);
    return entity;
  }

  /**
   * Remove the data for entities of the specified class (and its subclasses) from the cache.
   * {@link Cache#evict(Class)}
   *
   * @param entityClass evictCache
   */
  void evictCache();

  /**
   * Remove the data for the given entity from the cache.
   *
   * @param entity the given entity from the cache to be removed
   */
  default void evictCache(Entity entity) {
    if (entity == null || entity.getId() == null) {
      return;
    }
    this.evictCache(entity.getId());
  }

  /**
   * Remove the data for the given entity from the cache. {@link Cache#evict(Class, Object)}
   *
   * @param id the given entity id from the cache to be removed
   */
  void evictCache(Serializable id);

  /**
   * {@link EntityManager#flush()}
   */
  default void flush() {
    getEntityManager().flush();
  }

  /**
   * Retrieve object from repository by id and object class
   *
   * @param id the entity i
   * @return the entity
   */
  @Override
  T get(Serializable id);

  /**
   * {@link EntityManager#find(Class, Object, LockModeType)}
   */
  T get(Serializable id, LockModeType lockMode);

  /**
   * {@link EntityManager#find(Class, Object, LockModeType, Map)}
   */
  T get(Serializable id, LockModeType lockMode, Map<String, Object> properties);

  /**
   * {@link EntityManager#find(Class, Object, Map)}
   */
  T get(Serializable id, Map<String, Object> properties);

  /**
   * Returns the JPA entity manager used for this repository
   */
  EntityManager getEntityManager();

  /**
   * {@link javax.persistence.PersistenceUnitUtil#isLoaded(Object)}
   */
  default boolean isLoaded(Object object) {
    return object != null
        && getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(object);
  }

  /**
   * {@link EntityManager#lock(Object, LockModeType)}
   */
  default T lock(T object, LockModeType lockModeType) {
    getEntityManager().lock(object, lockModeType);
    return object;
  }

  /**
   * {@link EntityManager#lock(Object, LockModeType, Map)}
   */
  default T lock(T object, LockModeType lockModeType, Map<String, Object> properties) {
    getEntityManager().lock(object, lockModeType, properties);
    return object;
  }

  /**
   * Merge the state of the given object into repository.
   *
   *
   * @param obj the entity instance
   * @return the merged entity
   */
  @Override
  default T merge(T obj) {
    return getEntityManager().merge(obj);
  }

  /**
   * Merge the state of the given objects into repository.
   *
   * @param objs the objects to be saved
   * @param preHandler merge operation preprocessor, used to configure the entity manager, such as
   *        setting batch size, etc. This is related to the Hints provided by the JPA provider.
   * @param postHandler merge operation post-processor, use to configure the entity manager, such as
   *        flushing to underlying data base,etc.
   */
  default void mergeAll(Iterable<T> objs, UnaryOperator<EntityManager> preHandler,
      Consumer<EntityManager> postHandler) {
    if (objs != null) {
      final EntityManager em =
          preHandler != null ? preHandler.apply(getEntityManager()) : getEntityManager();
      try {
        for (T obj : objs) {
          em.merge(obj);
        }
      } finally {
        if (postHandler != null) {
          postHandler.accept(em);
        }
      }
    }
  }

  /**
   * Returns a typed JPA query with generic type Argument.
   *
   * @param name the name of a query defined in metadata
   */
  TypedJPAQuery<T> namedQuery(final String name);

  /**
   * {@link JPAQueries#namedQuery(String, Class)}
   *
   * @param name the name of a query defined in metadata
   * @param type the result record type
   */
  <X> TypedJPAQuery<X> namedQuery(final String name, Class<X> type);

  /**
   * Returns a typed JPA query with generic type Argument.
   *
   * @param sqlString the native SQL query statement
   * @return TypedJPAQuery
   */
  TypedJPAQuery<T> nativeQuery(final String sqlString);

  /**
   * {@link JPAQueries#query(String, Class)}
   *
   * @param sqlString a native SQL query string
   * @param type the type of the query result
   * @return TypedJPAQuery
   */
  <X> TypedJPAQuery<X> nativeQuery(final String sqlString, Class<X> type);

  /**
   * Save the state of the given object into repository
   *
   *
   * @param obj the entity instance
   */
  @Override
  default T persist(T obj) {
    getEntityManager().persist(obj);
    return obj;
  }

  /**
   * Save the state of the given objects into repository.
   *
   * @param objs the objects to be saved
   * @param preHandler persistence operation preprocessor, used to configure the entity manager,
   *        such as setting batch size, etc. This is related to the Hints provided by the JPA
   *        provider.
   * @param postHandler persistence operation post-processor, use to configure the entity manager,
   *        such as flushing to underlying data base,etc.
   */
  default void persistAll(Iterable<T> objs, UnaryOperator<EntityManager> preHandler,
      Consumer<EntityManager> postHandler) {
    if (objs != null) {
      final EntityManager em =
          preHandler != null ? preHandler.apply(getEntityManager()) : getEntityManager();
      try {
        for (T obj : objs) {
          em.persist(obj);
        }
      } finally {
        if (postHandler != null) {
          postHandler.accept(em);
        }
      }
    }
  }

  /**
   * {@link JPAQueries#query(CriteriaQuery)}
   *
   * @param criteriaQuery a criteria query object
   */
  default TypedJPAQuery<T> query(CriteriaQuery<T> criteriaQuery) {
    return JPAQueries.query(criteriaQuery).entityManager(this::getEntityManager);
  }

  /**
   * Returns a typed JPA query with generic type Argument.
   *
   * @param qlString the Jakarta Persistence query string
   * @return TypedJPAQuery
   */
  TypedJPAQuery<T> query(final String qlString);

  /**
   * {@link JPAQueries#query(String, Class)}
   *
   * @param qlString a Jakarta Persistence query string
   * @param type the type of the query result
   * @return TypedJPAQuery
   */
  <X> TypedJPAQuery<X> query(final String qlString, Class<X> type);

  /**
   * Refresh the state of the instance from the database,overwriting changes made to the entity, if
   * any. {@link EntityManager#refresh(Object)}
   *
   * @param entity entity instance
   * @return refreshed entity
   */
  default T refresh(T entity) {
    getEntityManager().refresh(entity);
    return entity;
  }

  /**
   * Refresh the state of the instance from the database,overwriting changes made to the entity, if
   * any, and lock it with respect to given lock mode type.
   *
   * {@link EntityManager#refresh(Object, LockModeType)}
   *
   * @param entity entity instance
   * @param lockMode lock mode
   * @return refreshed entity
   */
  default T refresh(T entity, LockModeType lockMode) {
    getEntityManager().refresh(entity, lockMode);
    return entity;
  }

  /**
   * Refresh the state of the instance from the database, overwriting changes made to the entity, if
   * any, and lock it with respect to given lock mode type and with specified properties.
   *
   * {@link EntityManager#refresh(Object, LockModeType, Map)}
   *
   * @param entity entity instance
   * @param lockMode lock mode
   * @param properties standard and vendor-specific properties and hints
   * @return refreshed entity
   */
  default T refresh(T entity, LockModeType lockMode, Map<String, Object> properties) {
    getEntityManager().refresh(entity, lockMode, properties);
    return entity;
  }

  /**
   * Refresh the state of the instance from the database, usingthe specified properties, and
   * overwriting changes made to the entity, if any.
   *
   * {@link EntityManager#refresh(Object, Map)}
   *
   * @param entity entity instance
   * @param properties standard and vendor-specific properties and hints
   * @return refreshed entity
   */
  default T refresh(T entity, Map<String, Object> properties) {
    getEntityManager().refresh(entity, properties);
    return entity;
  }

  /**
   * Remove the entity from repository
   *
   *
   * @param obj the entity instance
   * @return true means successfully
   */
  @Override
  default boolean remove(T obj) {
    if (obj != null) {
      getEntityManager().remove(obj);
      return true;
    }
    return false;
  }

  /**
   * Remove the entity by id
   *
   * @param id the entity id
   * @return true means successfully
   */
  default boolean removeById(Serializable id) {
    return remove(get(id));
  }

  /**
   * Retrieve objects from repository by query object
   *
   *
   * @param q the query object
   * @return object list
   */
  @SuppressWarnings("unchecked")
  @Override
  default List<T> select(Query q) {
    return defaultObject(q.getResultList(), ArrayList::new);
  }

}

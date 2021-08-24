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

import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Objects.defaultObject;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import org.corant.context.qualifier.AutoCreated;
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
  default void detach(T entity) {
    getEntityManager().detach(entity);
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
   * @param entity evictCache
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
   * @param id evictCache
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
  default void lock(T object, LockModeType lockModeType) {
    if (getEntityManager().contains(object)) {
      getEntityManager().lock(object, lockModeType);
    }
  }

  /**
   * {@link EntityManager#lock(Object, LockModeType, Map)}
   */
  default void lock(T object, LockModeType lockModeType, Map<String, Object> properties) {
    if (getEntityManager().contains(object)) {
      getEntityManager().lock(object, lockModeType, properties);
    }
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
  default boolean persist(T obj) {
    getEntityManager().persist(obj);
    return true;
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

  @ApplicationScoped
  class TypedJPARepositoryProducer {

    @SuppressWarnings("unchecked")
    @Produces
    @Dependent
    @AutoCreated
    <T extends Entity> TypedJPARepository<T> produceTypedJPARepository(InjectionPoint ip) {
      final Type type = ip.getType();
      final ParameterizedType parameterizedType = (ParameterizedType) type;
      final Type argType = parameterizedType.getActualTypeArguments()[0];
      final Class<T> entityClass = (Class<T>) argType;
      return new TypedJPARepositoryTemplate<T>(entityClass);
    }
  }

  /**
   * corant-modules-ddd-shared
   *
   * @author bingo 下午9:15:00
   *
   */
  public static class TypedJPARepositoryTemplate<T extends Entity>
      implements TypedJPARepository<T> {

    private final Class<T> entityClass;
    private final PersistenceContext context;

    protected TypedJPARepositoryTemplate(Class<T> entityClass) {
      this.entityClass = entityClass;
      this.context = resolve(EntityManagers.class).getPersistenceContext(entityClass);
    }

    @Override
    public void evictCache() {
      Cache cache = getEntityManager().getEntityManagerFactory().getCache();
      if (cache != null) {
        cache.evict(entityClass);
      }
    }

    @Override
    public void evictCache(Serializable id) {
      Cache cache = getEntityManager().getEntityManagerFactory().getCache();
      if (cache != null) {
        cache.evict(entityClass, id);
      }
    }

    @Override
    public T get(Serializable id) {
      return id != null ? getEntityManager().find(entityClass, id) : null;
    }

    @Override
    public T get(Serializable id, LockModeType lockMode) {
      return id != null ? getEntityManager().find(entityClass, id, lockMode) : null;
    }

    @Override
    public T get(Serializable id, LockModeType lockMode, Map<String, Object> properties) {
      return id != null ? getEntityManager().find(entityClass, id, lockMode, properties) : null;
    }

    @Override
    public T get(Serializable id, Map<String, Object> properties) {
      return id != null ? getEntityManager().find(entityClass, id, properties) : null;
    }

    @Override
    public EntityManager getEntityManager() {
      return resolve(EntityManagers.class).getEntityManager(context);
    }

    @Override
    public TypedJPAQuery<T> namedQuery(String name) {
      return JPAQueries.namedQuery(name, entityClass).entityManager(this::getEntityManager);
    }

    @Override
    public <X> TypedJPAQuery<X> namedQuery(String name, Class<X> type) {
      return JPAQueries.namedQuery(name, type).entityManager(this::getEntityManager);
    }

    @Override
    public TypedJPAQuery<T> nativeQuery(String sqlString) {
      return JPAQueries.nativeQuery(sqlString, entityClass).entityManager(this::getEntityManager);
    }

    @Override
    public <X> TypedJPAQuery<X> nativeQuery(String sqlString, Class<X> type) {
      return JPAQueries.nativeQuery(sqlString, type).entityManager(this::getEntityManager);
    }

    @Override
    public TypedJPAQuery<T> query(String qlString) {
      return JPAQueries.query(qlString, entityClass).entityManager(this::getEntityManager);
    }

    @Override
    public <X> TypedJPAQuery<X> query(String qlString, Class<X> type) {
      return JPAQueries.query(qlString, type).entityManager(this::getEntityManager);
    }
  }
}

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
package org.corant.suites.ddd.repository;

import static org.corant.shared.util.Classes.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Sets.linkedHashSetOf;
import static org.corant.suites.ddd.repository.JPAQueryBuilder.namedQuery;
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
import org.corant.shared.util.Objects;
import org.corant.suites.ddd.model.Aggregate;
import org.corant.suites.ddd.model.Aggregate.AggregateIdentifier;
import org.corant.suites.ddd.model.Entity;

/**
 * corant-suites-ddd
 *
 * @author bingo 下午9:39:59
 *
 */
public interface JPARepository extends Repository<Query> {

  Logger logger = Logger.getLogger(JPARepository.class.getName());

  default void clear() {
    getEntityManager().clear();
  }

  default void detach(Object entity) {
    getEntityManager().detach(entity);
  }

  default void evictCache(Class<?> entityClass) {
    Cache cache = getEntityManagerFactory().getCache();
    if (cache != null) {
      cache.evict(entityClass);
    } else {
      logger.warning(() -> "There is not cache mechanism!");
    }
  }

  default void evictCache(Class<?> entityClass, Serializable id) {
    Cache cache = getEntityManagerFactory().getCache();
    if (cache != null) {
      cache.evict(entityClass, id);
    } else {
      logger.warning(() -> "There is not cache mechanism!");
    }
  }

  default void evictCache(Entity entity) {
    if (entity == null || entity.getId() == null) {
      return;
    }
    this.evictCache(entity.getClass(), entity.getId());
  }

  default void flush() {
    getEntityManager().flush();
  }

  @SuppressWarnings("unchecked")
  default <T> T get(AggregateIdentifier identifier) {
    if (identifier != null) {
      Class<?> cls = tryAsClass(identifier.getType());
      return (T) getEntityManager().find(cls, identifier.getId());
    }
    return null;
  }

  @Override
  default <T> T get(Class<T> entityClass, Serializable id) {
    return id != null ? getEntityManager().find(entityClass, id) : null;
  }

  default <T> T get(Class<T> entityClass, Serializable id, LockModeType lockMode) {
    return getEntityManager().find(entityClass, id, lockMode);
  }

  default <T> T get(Class<T> entityClass, Serializable id, LockModeType lockMode,
      Map<String, Object> properties) {
    return getEntityManager().find(entityClass, id, lockMode, properties);
  }

  default <T extends Aggregate> T get(Class<T> entityClass, Serializable id, long vn) {
    T entity = this.get(entityClass, id);
    return entity != null && entity.getVn().longValue() == vn ? entity : null;
  }

  default <T> T get(Class<T> entityClass, Serializable id, Map<String, Object> properties) {
    return getEntityManager().find(entityClass, id, properties);
  }

  default <T> T get(Query query) {
    List<T> result = this.select(query);
    if (!isEmpty(result)) {
      if (result.size() > 1) {
        logger.warning(() -> String.format(
            "The query ['%s'] result set record number > 1, may be breach intentions.", query));
      }
      return result.get(0);
    }
    return null;
  }

  default <T> T get(String queryName, Map<?, ?> param) {
    return this.get(namedQuery(queryName).parameters(param).build(getEntityManager()));
  }

  default <T> T get(String queryName, Object... param) {
    return this.get(namedQuery(queryName).parameters(param).build(getEntityManager()));
  }

  EntityManager getEntityManager();

  default EntityManagerFactory getEntityManagerFactory() {
    return getEntityManager().getEntityManagerFactory();
  }

  default boolean isLoaded(Object object) {
    return object != null && getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(object);
  }

  default void lock(Object object, LockModeType lockModeType) {
    if (getEntityManager().contains(object)) {
      getEntityManager().lock(object, lockModeType);
    }
  }

  default void lock(Object object, LockModeType lockModeType, Map<String, Object> properties) {
    if (getEntityManager().contains(object)) {
      getEntityManager().lock(object, lockModeType, properties);
    }
  }

  @Override
  default <T> T merge(T entity) {
    return getEntityManager().merge(entity);
  }

  @Override
  default <T> boolean persist(T entity) {
    getEntityManager().persist(entity);
    return true;
  }

  @Override
  default <T> boolean remove(T obj) {
    if (obj != null) {
      getEntityManager().remove(obj);
      return true;
    }
    return false;
  }

  default <T> List<T> select(Class<T> entityClass, Serializable... ids) {
    if (isEmpty(ids)) {
      return new ArrayList<>();
    } else {
      return linkedHashSetOf(ids).stream().map(i -> get(entityClass, i))
          .filter(Objects::isNotNull).collect(Collectors.toList());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  default <T> List<T> select(Query query) {
    List<T> resultList = query.getResultList();
    if (resultList == null) {
      resultList = new ArrayList<>();
    }
    return resultList;
  }

  default <T> List<T> select(String queryName, Map<?, ?> param) {
    return this.select(namedQuery(queryName).parameters(param).build(getEntityManager()));
  }

  default <T> List<T> select(String queryName, Object... param) {
    return this.select(namedQuery(queryName).parameters(param).build(getEntityManager()));
  }

}

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

import static org.corant.shared.util.ClassUtils.tryAsClass;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.suites.ddd.repository.JpaQueryBuilder.namedQuery;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import org.corant.suites.ddd.annotation.stereotype.Repositories;
import org.corant.suites.ddd.model.Aggregate;
import org.corant.suites.ddd.model.Aggregate.AggregateIdentifier;
import org.corant.suites.ddd.model.Entity;
import org.corant.suites.ddd.model.Entity.EntityManagerProvider;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午9:54:26
 *
 */
@Repositories
public abstract class AbstractJpaRepository implements JpaRepository {

  @Inject
  protected Logger logger;

  @Override
  public void clear() {
    getEntityManager().clear();
  }

  @Override
  public void detach(Object entity) {
    getEntityManager().detach(entity);
  }

  @Override
  public void evictCache(Class<?> entityClass) {
    Cache cache = getEntityManagerFactory().getCache();
    if (cache != null) {
      cache.evict(entityClass);
    } else {
      logger.warning(() -> "There is not cache mechanism!");
    }
  }

  @Override
  public void evictCache(Class<?> entityClass, Serializable id) {
    Cache cache = getEntityManagerFactory().getCache();
    if (cache != null) {
      cache.evict(entityClass, id);
    } else {
      logger.warning(() -> "There is not cache mechanism!");
    }
  }

  @Override
  public void evictCache(Entity entity) {
    if (entity == null || entity.getId() == null) {
      return;
    }
    this.evictCache(entity.getClass(), entity.getId());
  }

  @Override
  public void flush() {
    getEntityManager().flush();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(AggregateIdentifier identifier) {
    if (identifier != null) {
      Class<?> cls = tryAsClass(identifier.getType());
      return (T) getEntityManager().find(cls, identifier.getId());
    }
    return null;
  }

  @Override
  public <T> T get(Class<T> entityClass, Serializable id) {
    return id != null ? getEntityManager().find(entityClass, id) : null;
  }

  public <T> T get(Class<T> entityClass, Serializable id, LockModeType lockMode) {
    return getEntityManager().find(entityClass, id, lockMode);
  }

  public <T> T get(Class<T> entityClass, Serializable id, LockModeType lockMode,
      Map<String, Object> properties) {
    return getEntityManager().find(entityClass, id, lockMode, properties);
  }

  public <T extends Aggregate> T get(Class<T> entityClass, Serializable id, long vn) {
    T entity = this.get(entityClass, id);
    return entity != null && entity.getVn().longValue() == vn ? entity : null;
  }

  public <T> T get(Class<T> entityClass, Serializable id, Map<String, Object> properties) {
    return getEntityManager().find(entityClass, id, properties);
  }

  public <T> T get(Query query) {
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

  @Override
  public <T> T get(String queryName, Map<?, ?> param) {
    return this.get(namedQuery(queryName).parameters(param).build(getEntityManager()));
  }

  @Override
  public <T> T get(String queryName, Object... param) {
    return this.get(namedQuery(queryName).parameters(param).build(getEntityManager()));
  }

  /**
   * One transaction one entity manager
   */
  @Override
  public EntityManager getEntityManager() {
    return getEntityManagerProvider().getEntityManager();
  }

  public EntityManagerFactory getEntityManagerFactory() {
    return getEntityManager().getEntityManagerFactory();
  }

  public <T> T getForUpdate(Class<T> entityClass, Serializable id) {
    return this.get(entityClass, id, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
  }

  public boolean isLoaded(Object object) {
    return object != null && getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(object);
  }

  public void lock(Object object, LockModeType lockModeType) {
    if (getEntityManager().contains(object)) {
      getEntityManager().lock(object, lockModeType);
    }
  }

  public void lock(Object object, LockModeType lockModeType, Map<String, Object> properties) {
    if (getEntityManager().contains(object)) {
      getEntityManager().lock(object, lockModeType, properties);
    }
  }

  @Override
  public <T> T merge(T entity) {
    return getEntityManager().merge(entity);
  }

  @Override
  public <T> boolean persist(T entity) {
    getEntityManager().persist(entity);
    return true;
  }

  @Override
  public <T> boolean remove(T obj) {
    if (obj != null) {
      getEntityManager().remove(obj);
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> select(Query query) {
    List<T> resultList = query.getResultList();
    if (resultList == null) {
      resultList = new ArrayList<>();
    }
    return resultList;
  }

  @Override
  public <T> List<T> select(String queryName, Map<?, ?> param) {
    return this.select(namedQuery(queryName).parameters(param).build(getEntityManager()));
  }

  @Override
  public <T> List<T> select(String queryName, Object... param) {
    return this.select(namedQuery(queryName).parameters(param).build(getEntityManager()));
  }

  protected abstract EntityManagerProvider getEntityManagerProvider();

}

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
import static org.corant.modules.bundle.GlobalMessageCodes.ERR_OBJ_CONSTRUCT;
import static org.corant.shared.util.Preconditions.requireNotNull;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import org.corant.modules.ddd.Entity;
import org.corant.modules.jpa.shared.JPAQueries;
import org.corant.modules.jpa.shared.JPAQueries.TypedJPAQuery;

/**
 * corant-modules-ddd-shared
 *
 * @author bingo 下午3:59:21
 *
 */
public abstract class AbstractTypedJPARepository<T extends Entity>
    implements TypedJPARepository<T> {

  protected volatile Class<T> entityClass;

  protected AbstractTypedJPARepository() {}

  protected AbstractTypedJPARepository(Class<T> entityClass) {
    this.entityClass = requireNotNull(entityClass, ERR_OBJ_CONSTRUCT);
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
    return resolve(EntityManagers.class).getEntityManager(entityClass);
  }

  @Override
  public T getReference(Serializable id) {
    return getEntityManager().getReference(entityClass, id);
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

  @SuppressWarnings("unchecked")
  @PostConstruct
  protected void onPostConstruct() {
    if (this.entityClass == null) {
      synchronized (this) {
        if (this.entityClass == null) {
          Class<?> repoClass = this.getClass();
          do {
            if (repoClass.getGenericSuperclass() instanceof ParameterizedType) {
              entityClass = (Class<T>) ((ParameterizedType) repoClass.getGenericSuperclass())
                  .getActualTypeArguments()[0];
              break;
            } else {
              Type[] genericInterfaces = repoClass.getGenericInterfaces();
              for (Type type : genericInterfaces) {
                if (type instanceof ParameterizedType) {
                  ParameterizedType parameterizedType = (ParameterizedType) type;
                  if (AbstractTypedJPARepository.class
                      .isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
                    entityClass = (Class<T>) parameterizedType.getActualTypeArguments()[0];
                    break;
                  }
                }
              }
            }
          } while (entityClass == null && (repoClass = repoClass.getSuperclass()) != null);
          requireNotNull(this.entityClass, ERR_OBJ_CONSTRUCT);
        }
      }
    }
  }

  /**
   * corant-modules-ddd-shared
   *
   * @author bingo 下午9:15:00
   *
   */
  public static class TypedJPARepositoryTemplate<T extends Entity>
      extends AbstractTypedJPARepository<T> {

    public TypedJPARepositoryTemplate(Class<T> entityClass) {
      super(entityClass);
    }

  }
}

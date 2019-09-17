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
package org.corant.suites.query.jpql;

import static org.corant.shared.util.CollectionUtils.getSize;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getMapBoolean;
import static org.corant.shared.util.MapUtils.getMapEnum;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.suites.query.jpql.JpqlHelper.getCountJpql;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.query.jpql.JpqlNamedQueryResolver.JpqlQuerier;
import org.corant.suites.query.shared.AbstractNamedQueryService;
import org.corant.suites.query.shared.Querier;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.mapping.FetchQuery;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:33:21
 *
 */
@ApplicationScoped
public abstract class AbstractJpqlNamedQueryService extends AbstractNamedQueryService {

  public static final String PRO_KEY_FLUSH_MODE_TYPE = "jpa.query.flushModeType";
  public static final String PRO_KEY_LOCK_MODE_TYPE = "jpa.query.lockModeType";
  public static final String PRO_KEY_HINT_PREFIX = "jpa.query.hint";
  public static final int PRO_KEY_HINT_PREFIX_LEN = PRO_KEY_HINT_PREFIX.length();
  public static final String PRO_KEY_NATIVE_QUERY = "jpa.query.isNative";

  @Inject
  protected JpqlNamedQueryResolver<String, Object> resolver;

  @SuppressWarnings("unchecked")
  @Override
  public <T> ForwardList<T> forward(String queryName, Object parameter) {
    JpqlQuerier querier = getResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] scriptParameter = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    String ql = querier.getScript();
    int offset = querier.getQueryParameter().getOffset();
    int limit = querier.getQueryParameter().getLimit();
    log(queryName, scriptParameter, ql);
    EntityManager em = getEntityManager();
    try {
      ForwardList<T> result = ForwardList.inst();
      Query query = createQuery(em, ql, properties, resultClass, scriptParameter);
      query.setFirstResult(offset).setMaxResults(limit + 1);
      List<T> list = defaultObject(query.getResultList(), new ArrayList<>());
      int size = getSize(list);
      if (size > 0) {
        if (size > limit) {
          list.remove(size - 1);
          result.withHasNext(true);
        }
      }
      return result.withResults(list);
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T get(String queryName, Object parameter) {
    JpqlQuerier querier = getResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] scriptParameter = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    String ql = querier.getScript();
    log(queryName, scriptParameter, ql);
    T result = null;
    EntityManager em = getEntityManager();
    try {
      List<T> list = createQuery(em, ql, properties, resultClass, scriptParameter).getResultList();
      if (!isEmpty(list)) {
        result = list.get(0);
      }
      return result;
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> PagedList<T> page(String queryName, Object parameter) {
    JpqlQuerier querier = getResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] scriptParameter = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    String ql = querier.getScript();
    int offset = querier.getQueryParameter().getOffset();
    int limit = querier.getQueryParameter().getLimit();
    log(queryName, scriptParameter, ql);
    EntityManager em = getEntityManager();
    try {
      Query query = createQuery(em, ql, properties, resultClass, scriptParameter);
      query.setFirstResult(offset).setMaxResults(limit);
      List<T> list = defaultObject(query.getResultList(), new ArrayList<>());
      PagedList<T> result = PagedList.of(offset, limit);
      int size = getSize(list);
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          String totalSql = getCountJpql(ql);
          log("total-> " + queryName, scriptParameter, totalSql);
          result.withTotal(
              ((Number) createQuery(em, totalSql, properties, resultClass, scriptParameter)
                  .getSingleResult()).intValue());
        }
      }
      return result.withResults(list);
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> select(String queryName, Object parameter) {
    JpqlQuerier querier = getResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] queryParam = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    int maxSelectSize = getMaxSelectSize(querier);
    String ql = querier.getScript();
    log(queryName, queryParam, ql);
    EntityManager em = getEntityManager();
    try {
      Query query =
          createQuery(em, ql, properties, resultClass, queryParam).setMaxResults(maxSelectSize + 1);
      List<T> result = query.getResultList();
      if (getSize(result) > maxSelectSize) {
        throw new QueryRuntimeException(
            "[%s] Result record number overflow, the allowable range is %s.", queryName,
            maxSelectSize);
      }
      return result;
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  @SuppressWarnings({"unchecked", "restriction"})
  @Override
  public <T> Stream<T> stream(String queryName, Object parameter) {
    JpqlQuerier querier = getResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] scriptParameter = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    String ql = querier.getScript();
    final EntityManager em = getEntityManager(); // FIXME close
    Stream<T> stream =
        createQuery(em, ql, properties, resultClass, scriptParameter).getResultStream();
    sun.misc.Cleaner.create(stream, em::close);
    return stream;
  }

  protected Query createQuery(EntityManager em, String ql, Map<String, String> properties,
      Class<?> cls, Object... args) {
    boolean isNative = getMapBoolean(properties, PRO_KEY_NATIVE_QUERY);
    Query query = null;
    if (isNative) {
      query = em.createNativeQuery(ql, cls);
    } else {
      query = em.createQuery(ql);
    }
    FlushModeType fmt = getMapEnum(properties, PRO_KEY_FLUSH_MODE_TYPE, FlushModeType.class);
    if (fmt != null) {
      query.setFlushMode(fmt);
    }
    LockModeType lmt = getMapEnum(properties, PRO_KEY_LOCK_MODE_TYPE, LockModeType.class);
    if (lmt != null) {
      query.setLockMode(lmt);
    }
    if (properties != null) {
      Map<String, String> hints = new LinkedHashMap<>();
      properties.forEach((k, v) -> {
        if (k != null && k.startsWith(PRO_KEY_HINT_PREFIX)) {
          hints.put(k.substring(PRO_KEY_HINT_PREFIX_LEN + 1), v);
        }
      });
      if (!hints.isEmpty()) {
        hints.forEach(query::setHint);
      }
    }
    int counter = 0;
    for (Object parameter : args) {
      query.setParameter(counter++, parameter);
    }
    handleQuery(query, cls, properties);
    return query;
  }

  @Override
  protected <T> void fetch(T obj, FetchQuery fetchQuery, Querier parentQuerier) {
    throw new NotSupportedException();
  }

  protected EntityManager getEntityManager() {
    return getEntityManagerFactory().createEntityManager(SynchronizationType.UNSYNCHRONIZED);
  }

  protected abstract EntityManagerFactory getEntityManagerFactory();

  protected JpqlNamedQueryResolver<String, Object> getResolver() {
    return resolver;
  }

  protected void handleQuery(Query query, Class<?> cls, Map<String, String> properties) {}

}

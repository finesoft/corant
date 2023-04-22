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
package org.corant.modules.query.jpql;

import static org.corant.modules.jpa.shared.JPQLHelper.getTotalQL;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Maps.getMapBoolean;
import static org.corant.shared.util.Maps.getMapEnum;
import static org.corant.shared.util.Objects.defaultObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import org.corant.modules.query.Querier;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.AbstractNamedQueryService;
import org.corant.shared.exception.NotSupportedException;

/**
 * corant-modules-query-jpql
 *
 * @author bingo 下午5:33:21
 *
 */
public abstract class AbstractJpqlNamedQueryService extends AbstractNamedQueryService {

  public static final String PRO_KEY_FLUSH_MODE_TYPE = "jpa.query.flushModeType";
  public static final String PRO_KEY_LOCK_MODE_TYPE = "jpa.query.lockModeType";
  public static final String PRO_KEY_HINT_PREFIX = "jpa.query.hint";
  public static final int PRO_KEY_HINT_PREFIX_LEN = PRO_KEY_HINT_PREFIX.length();
  public static final String PRO_KEY_NATIVE_QUERY = "jpa.query.isNative";

  @Override
  public FetchResult fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier) {
    throw new NotSupportedException();
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public <T> Stream<T> stream(String queryName, Object parameter) {
    JpqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] scriptParameter = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    Duration timeout = querier.resolveTimeout();
    String ql = querier.getScript();
    log("stream-> " + queryName, scriptParameter, ql);
    final EntityManager em = getEntityManager(); // FIXME close
    Stream<T> stream =
        createQuery(em, ql, properties, resultClass, timeout, scriptParameter).getResultStream();
    return stream.onClose(em::close);
  }

  protected Query createQuery(EntityManager em, String ql, Map<String, String> properties,
      Class<?> cls, Duration timeout, Object... args) {
    boolean isNative = getMapBoolean(properties, PRO_KEY_NATIVE_QUERY);
    Query query;
    if (isNative) {
      query = em.createNativeQuery(ql, cls);
    } else {
      query = em.createQuery(ql);
    }
    if (timeout != null) {
      query.setHint("javax.persistence.query.timeout", (int) timeout.toSeconds());
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

  @SuppressWarnings("unchecked")
  @Override
  protected <T> Forwarding<T> doForward(String queryName, Object parameter) throws Exception {
    JpqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] scriptParameter = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    String ql = querier.getScript();
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Duration timeout = querier.resolveTimeout();
    log(queryName, scriptParameter, ql);
    EntityManager em = getEntityManager();
    try {
      Forwarding<T> result = Forwarding.inst();
      Query query = createQuery(em, ql, properties, resultClass, timeout, scriptParameter);
      query.setFirstResult(offset).setMaxResults(limit + 1);
      List<T> list = defaultObject(query.getResultList(), ArrayList::new);
      int size = list.size();
      if (size > 0 && size > limit) {
        list.remove(limit);
        result.withHasNext(true);
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
  protected <T> T doGet(String queryName, Object parameter) throws Exception {
    JpqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] scriptParameter = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    String ql = querier.getScript();
    Duration timeout = querier.resolveTimeout();
    log(queryName, scriptParameter, ql);
    T result = null;
    EntityManager em = getEntityManager();
    try {
      List<T> list =
          createQuery(em, ql, properties, resultClass, timeout, scriptParameter).getResultList();
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
  protected <T> Paging<T> doPage(String queryName, Object parameter) throws Exception {
    JpqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] scriptParameter = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    String ql = querier.getScript();
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Duration timeout = querier.resolveTimeout();
    log(queryName, scriptParameter, ql);
    EntityManager em = getEntityManager();
    try {
      Query query = createQuery(em, ql, properties, resultClass, timeout, scriptParameter);
      query.setFirstResult(offset).setMaxResults(limit);
      List<T> list = defaultObject(query.getResultList(), ArrayList::new);
      Paging<T> result = Paging.of(offset, limit);
      int size = list.size();
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          String totalSql = getTotalQL(ql);
          log("total-> " + queryName, scriptParameter, totalSql);
          result.withTotal(
              ((Number) createQuery(em, totalSql, properties, resultClass, timeout, scriptParameter)
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
  protected <T> List<T> doSelect(String queryName, Object parameter) throws Exception {
    JpqlNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    Class<T> resultClass = (Class<T>) querier.getQuery().getResultClass();
    Object[] queryParam = querier.getScriptParameter();
    Map<String, String> properties = querier.getQuery().getProperties();
    int maxSelectSize = querier.resolveSelectSize();
    Duration timeout = querier.resolveTimeout();
    String ql = querier.getScript();
    log(queryName, queryParam, ql);
    EntityManager em = getEntityManager();
    try {
      Query query = createQuery(em, ql, properties, resultClass, timeout, queryParam)
          .setMaxResults(maxSelectSize + 1);
      List<T> result = query.getResultList();
      querier.handleResultSize(result);
      return result;
    } finally {
      if (em.isOpen()) {
        em.close();
      }
    }
  }

  protected EntityManager getEntityManager() {
    return getEntityManagerFactory().createEntityManager(SynchronizationType.UNSYNCHRONIZED);
  }

  protected abstract EntityManagerFactory getEntityManagerFactory();

  @Override
  protected abstract AbstractNamedQuerierResolver<JpqlNamedQuerier> getQuerierResolver();

  protected void handleQuery(Query query, Class<?> cls, Map<String, String> properties) {}

}

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
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.suites.query.jpql.JpqlHelper.getCountJpql;
import static org.corant.suites.query.shared.QueryUtils.getLimit;
import static org.corant.suites.query.shared.QueryUtils.getOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.exception.NotSupportedException;
import org.corant.suites.query.jpql.JpqlNamedQueryResolver.Querier;
import org.corant.suites.query.shared.NamedQuery;
import org.corant.suites.query.shared.QueryUtils;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.spi.ResultHintHandler;

/**
 * corant-suites-query
 *
 * @author bingo 下午5:33:21
 *
 */
@ApplicationScoped
public abstract class AbstractJpqlNamedQuery implements NamedQuery {

  public static final String PRO_KEY_FLUSH_MODE_TYPE = "jpa.query.flushModeType";
  public static final String PRO_KEY_LOCK_MODE_TYPE = "jpa.query.lockModeType";
  public static final String PRO_KEY_HINT_PREFIX = "jpa.query.hint";
  public static final int PRO_KEY_HINT_PREFIX_LEN = PRO_KEY_HINT_PREFIX.length();
  public static final String PRO_KEY_NATIVE_QUERY = "jpa.query.isNative";

  @Inject
  Logger logger;

  @Inject
  JpqlNamedQueryResolver<String, Map<String, Object>, String, Object[], QueryHint> resolver;

  @Inject
  @Any
  Instance<ResultHintHandler> resultHintHandlers;

  public Object adaptiveSelect(String q, Map<String, Object> param) {
    if (param != null && param.containsKey(QueryUtils.OFFSET_PARAM_NME)) {
      if (param.containsKey(QueryUtils.LIMIT_PARAM_NME)) {
        return this.page(q, param);
      } else {
        return this.forward(q, param);
      }
    } else {
      return this.select(q, param);
    }
  }

  @Override
  public <T> ForwardList<T> forward(String q, Map<String, Object> param) {
    Querier<String, Object[], QueryHint> querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<QueryHint> hints = querier.getHints();
    Map<String, String> properties = querier.getProperties();
    String ql = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    log(q, queryParam, ql);
    ForwardList<T> result = ForwardList.inst();
    Query query = createQuery(ql, properties, resultClass, queryParam);
    query.setFirstResult(offset).setMaxResults(limit + 1);
    @SuppressWarnings("unchecked")
    List<T> list = defaultObject(query.getResultList(), new ArrayList<>());
    int size = getSize(list);
    if (size > 0) {
      if (size > limit) {
        list.remove(size - 1);
        result.withHasNext(true);
      }
      handleResultHints(resultClass, hints, param, list);
    }
    return result.withResults(list);
  }

  @Override
  public <T> T get(String q, Map<String, Object> param) {
    Querier<String, Object[], QueryHint> querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<QueryHint> hints = querier.getHints();
    Map<String, String> properties = querier.getProperties();
    String ql = querier.getScript();
    log(q, queryParam, ql);
    T result = null;
    @SuppressWarnings("unchecked")
    List<T> list = createQuery(ql, properties, resultClass, queryParam).getResultList();
    if (isEmpty(list)) {
      result = list.get(0);
    }
    handleResultHints(resultClass, hints, param, result);
    return result;
  }

  @Override
  public <T> PagedList<T> page(String q, Map<String, Object> param) {
    Querier<String, Object[], QueryHint> querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<QueryHint> hints = querier.getHints();
    Map<String, String> properties = querier.getProperties();
    String ql = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    log(q, queryParam, ql);
    Query query = createQuery(ql, properties, resultClass, queryParam);
    query.setFirstResult(offset).setMaxResults(limit);
    @SuppressWarnings("unchecked")
    List<T> list = defaultObject(query.getResultList(), new ArrayList<>());
    PagedList<T> result = PagedList.of(offset, limit);
    int size = getSize(list);
    if (size > 0) {
      if (size < limit) {
        result.withTotal(offset + size);
      } else {
        String totalSql = getCountJpql(ql);
        log("total-> " + q, queryParam, totalSql);
        result.withTotal(
            ((Number) createQuery(totalSql, properties, resultClass, queryParam).getSingleResult())
                .intValue());
      }
      handleResultHints(resultClass, hints, param, list);
    }
    return result.withResults(list);
  }

  @Override
  public <T> List<T> select(String q, Map<String, Object> param) {
    Querier<String, Object[], QueryHint> querier = getResolver().resolve(q, param);
    Class<T> rcls = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<QueryHint> hints = querier.getHints();
    Map<String, String> properties = querier.getProperties();
    String ql = querier.getScript();
    log(q, queryParam, ql);
    @SuppressWarnings("unchecked")
    List<T> result = createQuery(ql, properties, rcls, queryParam).getResultList();
    int size = getSize(result);
    if (size > 0) {
      handleResultHints(rcls, hints, param, result);
    }
    return result;
  }

  @Override
  public <T> Stream<T> stream(String q, Map<String, Object> param) {
    throw new NotSupportedException();
  }

  protected Query createQuery(String ql, Map<String, String> properties, Class<?> cls,
      Object... args) {
    boolean isNative = getMapBoolean(properties, PRO_KEY_NATIVE_QUERY);
    EntityManager em =
        getEntityManagerFactory().createEntityManager(SynchronizationType.UNSYNCHRONIZED);
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

  protected abstract EntityManagerFactory getEntityManagerFactory();

  protected JpqlNamedQueryResolver<String, Map<String, Object>, String, Object[], QueryHint> getResolver() {
    return resolver;
  }

  protected void handleQuery(Query query, Class<?> cls, Map<String, String> properties) {}

  protected void handleResultHints(Class<?> resultClass, List<QueryHint> hints, Object param,
      Object result) {
    if (result != null && !resultHintHandlers.isUnsatisfied()) {
      hints.forEach(qh -> {
        AtomicBoolean exclusive = new AtomicBoolean(false);
        resultHintHandlers.stream().filter(h -> h.canHandle(resultClass, qh))
            .sorted(ResultHintHandler::compare).forEachOrdered(h -> {
              if (!exclusive.get()) {
                try {
                  h.handle(qh, param, result);
                } catch (Exception e) {
                  throw new CorantRuntimeException(e);
                } finally {
                  if (h.exclusive()) {
                    exclusive.set(true);
                  }
                }
              }
            });
      });
    }
  }

  protected void log(String name, Object[] param, String... sql) {
    logger.fine(
        () -> String.format("%n[Query name]: %s; %n[Query parameters]: [%s]; %n[Query sql]: %s",
            name, String.join(",", asStrings(param)), String.join("; ", sql)));
  }
}

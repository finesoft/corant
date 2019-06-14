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
import static org.corant.shared.util.ObjectUtils.asString;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.suites.query.jpql.JpqlHelper.getCountJpql;
import static org.corant.suites.query.shared.QueryUtils.getLimit;
import static org.corant.suites.query.shared.QueryUtils.getOffset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
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
    String ql = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    log(q, queryParam, ql);
    ForwardList<T> result = ForwardList.inst();
    Query query = createQuery(false, ql, resultClass, queryParam);
    query.setFirstResult(offset).setMaxResults(limit + 1);
    @SuppressWarnings("unchecked")
    List<T> list = query.getResultList();
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
    String ql = querier.getScript();
    log(q, queryParam, ql);
    T result = forceCast(createQuery(false, ql, resultClass, queryParam).getSingleResult());
    handleResultHints(resultClass, hints, param, result);
    return result;
  }

  @Override
  public <T> PagedList<T> page(String q, Map<String, Object> param) {
    Querier<String, Object[], QueryHint> querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    Object[] queryParam = querier.getConvertedParameters();
    List<QueryHint> hints = querier.getHints();
    String ql = querier.getScript();
    int offset = getOffset(param);
    int limit = getLimit(param);
    log(q, queryParam, ql);
    Query query = createQuery(false, ql, resultClass, queryParam);
    query.setFirstResult(offset).setMaxResults(limit);
    @SuppressWarnings("unchecked")
    List<T> list = query.getResultList();
    PagedList<T> result = PagedList.of(offset, limit);
    int size = getSize(list);
    if (size > 0) {
      if (size < limit) {
        result.withTotal(offset + size);
      } else {
        String totalSql = getCountJpql(ql);
        log("total-> " + q, queryParam, totalSql);
        result.withTotal(
            ((Number) createQuery(false, totalSql, resultClass, queryParam).getSingleResult())
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
    String ql = querier.getScript();
    log(q, queryParam, ql);
    @SuppressWarnings("unchecked")
    List<T> result = createQuery(false, ql, rcls, queryParam).getResultList();
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

  protected Query createQuery(boolean nt, String ql, Class<?> rcls, Object... args) {
    EntityManager em =
        getEntityManagerFactory().createEntityManager(SynchronizationType.UNSYNCHRONIZED);
    Query query = nt ? em.createNativeQuery(ql) : em.createQuery(ql);
    int counter = 0;
    for (Object parameter : args) {
      query.setParameter(counter++, parameter);
    }
    return query;
  }

  protected Query createQuery(boolean nt, String ql, Map<?, ?> args) {
    EntityManager em =
        getEntityManagerFactory().createEntityManager(SynchronizationType.UNSYNCHRONIZED);
    Query query = nt ? em.createNativeQuery(ql) : em.createQuery(ql);
    if (args != null) {
      for (Entry<?, ?> entry : args.entrySet()) {
        query.setParameter(asString(entry.getKey()), entry.getValue());
      }
    }
    return query;
  }

  protected abstract EntityManagerFactory getEntityManagerFactory();

  protected JpqlNamedQueryResolver<String, Map<String, Object>, String, Object[], QueryHint> getResolver() {
    return resolver;
  }

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

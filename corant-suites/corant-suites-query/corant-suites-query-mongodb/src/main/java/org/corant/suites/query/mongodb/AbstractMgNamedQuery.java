package org.corant.suites.query.mongodb;
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

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.CollectionUtils.getSize;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getOpt;
import static org.corant.shared.util.ObjectUtils.asStrings;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.suites.query.shared.QueryUtils.convert;
import static org.corant.suites.query.shared.QueryUtils.getLimit;
import static org.corant.suites.query.shared.QueryUtils.getOffset;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.MgOperator;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.Querier;
import org.corant.suites.query.shared.NamedQuery;
import org.corant.suites.query.shared.QueryUtils;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.QueryHint;
import org.corant.suites.query.shared.spi.ResultHintHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:20:43
 *
 */
@ApplicationScoped
public abstract class AbstractMgNamedQuery implements NamedQuery {

  @Inject
  Logger logger;

  @Inject
  MgInLineNamedQueryResolver<String, Map<String, Object>, EnumMap<MgOperator, Bson>, FetchQuery, QueryHint> resolver;

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
    int offset = getOffset(param);
    int limit = getLimit(param);
    Querier<EnumMap<MgOperator, Bson>, FetchQuery, QueryHint> querier =
        getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    EnumMap<MgOperator, Bson> script = querier.getScript();
    ForwardList<T> result = ForwardList.inst();
    FindIterable<Document> fi =
        query(resolveCollectionName(q), script).skip(offset).limit(limit + 1);
    List<Map<String, Object>> list =
        streamOf(fi).map(r -> (Map<String, Object>) r).collect(Collectors.toList());
    int size = getSize(list);
    if (size > 0) {
      this.fetch(list, fetchQueries, param);
      if (size > limit) {
        list.remove(size - 1);
        result.withHasNext(true);
      }
      handleResultHints(resultClass, hints, param, list);
    }
    return result.withResults(convert(list, resultClass));
  }

  @Override
  public <T> T get(String q, Map<String, Object> param) {
    Querier<EnumMap<MgOperator, Bson>, FetchQuery, QueryHint> querier =
        getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    EnumMap<MgOperator, Bson> script = querier.getScript();
    FindIterable<Document> fi = query(resolveCollectionName(q), script).limit(1);
    Map<String, Object> result = fi.first();
    this.fetch(result, fetchQueries, param);
    handleResultHints(resultClass, hints, param, result);
    return convert(result, resultClass);
  }

  @Override
  public <T> PagedList<T> page(String q, Map<String, Object> param) {
    int offset = getOffset(param);
    int limit = getLimit(param);
    Querier<EnumMap<MgOperator, Bson>, FetchQuery, QueryHint> querier =
        getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    EnumMap<MgOperator, Bson> script = querier.getScript();
    PagedList<T> result = PagedList.of(offset, limit);
    FindIterable<Document> fi =
        query(resolveCollectionName(q), script).skip(offset).limit(limit + 1);
    List<Map<String, Object>> list =
        streamOf(fi).map(r -> (Map<String, Object>) r).collect(Collectors.toList());
    int size = getSize(list);
    if (size > 0) {
      if (size < limit) {
        result.withTotal(offset + size);
      } else {
        result.withTotal(Long.valueOf(queryCount(resolveCollectionName(q), script)).intValue());
      }
      this.fetch(list, fetchQueries, param);
      handleResultHints(resultClass, hints, param, list);
    }
    return result.withResults(convert(list, resultClass));
  }

  @Override
  public <T> List<T> select(String q, Map<String, Object> param) {
    Querier<EnumMap<MgOperator, Bson>, FetchQuery, QueryHint> querier =
        getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    EnumMap<MgOperator, Bson> script = querier.getScript();
    FindIterable<Document> fi = query(resolveCollectionName(q), script).limit(128);
    List<Map<String, Object>> result =
        streamOf(fi).map(r -> (Map<String, Object>) r).collect(Collectors.toList());
    int size = getSize(result);
    if (size > 0) {
      this.fetch(result, fetchQueries, param);
      handleResultHints(resultClass, hints, param, result);
    }
    return convert(result, resultClass);
  }

  @Override
  public <T> Stream<T> stream(String q, Map<String, Object> param) {
    return Stream.empty();
  }

  protected <T> void fetch(List<T> list, List<FetchQuery> fetchQueries, Map<String, Object> param) {
    if (!isEmpty(list) && !isEmpty(fetchQueries)) {
      list.forEach(e -> fetchQueries.stream().forEach(f -> fetch(e, f, new HashMap<>(param))));
    }
  }

  protected <T> void fetch(T obj, FetchQuery fetchQuery, Map<String, Object> param) {
    if (null == obj || fetchQuery == null) {
      return;
    }
    Map<String, Object> fetchParam = QueryUtils.resolveFetchParam(obj, fetchQuery, param);
    if (!QueryUtils.decideFetch(obj, fetchQuery, fetchParam)) {
      return;
    }
    boolean multiRecords = fetchQuery.isMultiRecords();
    String injectProName = fetchQuery.getInjectPropertyName();
    String refQueryName = fetchQuery.getVersionedReferenceQueryName();
    Querier<EnumMap<MgOperator, Bson>, FetchQuery, QueryHint> querier =
        resolver.resolve(refQueryName, fetchParam);
    EnumMap<MgOperator, Bson> script = querier.getScript();
    // Class<?> rcls = defaultObject(fetchQuery.getResultClass(), querier.getResultClass());
    List<QueryHint> hints = querier.getHints();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    FindIterable<Document> fi = query(resolveCollectionName(refQueryName), script).limit(128);
    List<Map<String, Object>> fetchedList =
        streamOf(fi).map(r -> (Map<String, Object>) r).collect(Collectors.toList());
    Object fetchedResult = null;
    if (multiRecords) {
      fetchedResult = fetchedList;
    } else if (!isEmpty(fetchedList)) {
      fetchedResult = fetchedList.get(0);
      fetchedList = fetchedList.subList(0, 1);
    }
    QueryUtils.resolveFetchResult(obj, fetchedResult, injectProName);
    this.fetch(fetchedList, fetchQueries, fetchParam);
    handleResultHints(Map.class, hints, fetchParam, fetchedList);
  }

  protected <T> void fetch(T obj, List<FetchQuery> fetchQueries, Map<String, Object> param) {
    if (obj != null && !isEmpty(fetchQueries)) {
      fetchQueries.stream().forEach(f -> this.fetch(obj, f, new HashMap<>(param)));
    }
  }

  protected abstract MongoDatabase getDataBase();

  protected ObjectMapper getObjectMapper() {
    return QueryUtils.ESJOM;
  }

  protected MgInLineNamedQueryResolver<String, Map<String, Object>, EnumMap<MgOperator, Bson>, FetchQuery, QueryHint> getResolver() {
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

  protected void log(String name, Map<String, Object> param, String... script) {
    logger.fine(
        () -> String.format("%n[Query name]: %s; %n[Query parameters]: [%s]; %n[Query script]: %s",
            name, String.join(",", asStrings(param)), String.join("; ", script)));
  }

  protected FindIterable<Document> query(String collection, EnumMap<MgOperator, Bson> script) {
    FindIterable<Document> it = getDataBase().getCollection(collection).find();
    for (MgOperator op : MgOperator.values()) {
      switch (op) {
        case FILTER:
          getOpt(script, op).ifPresent(it::filter);
          break;
        case PROJECTION:
          getOpt(script, op).ifPresent(it::projection);
          break;
        case MIN:
          getOpt(script, op).ifPresent(it::min);
          break;
        case MAX:
          getOpt(script, op).ifPresent(it::max);
          break;
        case HINT:
          getOpt(script, op).ifPresent(it::hint);
          break;
        case SORT:
          getOpt(script, op).ifPresent(it::sort);
          break;
        default:
          break;
      }
    }
    return it;
  }

  protected long queryCount(String collection, EnumMap<MgOperator, Bson> script) {
    Bson bson = script.getOrDefault(MgOperator.FILTER, new BasicDBObject());
    return getDataBase().getCollection(collection).countDocuments(bson);
  }

  protected String resolveCollectionName(String q) {
    int pos = 0;
    shouldBeTrue(isNotBlank(q) && (pos = q.indexOf('.')) != -1);
    return q.substring(0, pos);
  }
}

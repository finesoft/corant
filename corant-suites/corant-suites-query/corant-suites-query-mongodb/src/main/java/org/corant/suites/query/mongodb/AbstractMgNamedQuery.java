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
import static org.corant.shared.util.ConversionUtils.toBoolean;
import static org.corant.shared.util.ConversionUtils.toEnum;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.getMapEnum;
import static org.corant.shared.util.MapUtils.getOpt;
import static org.corant.shared.util.MapUtils.getOptMapObject;
import static org.corant.shared.util.ObjectUtils.max;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.suites.query.shared.QueryUtils.convert;
import static org.corant.suites.query.shared.QueryUtils.getLimit;
import static org.corant.suites.query.shared.QueryUtils.getOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.corant.shared.util.ConversionUtils;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.MgOperator;
import org.corant.suites.query.mongodb.MgInLineNamedQueryResolver.MgQuerier;
import org.corant.suites.query.shared.AbstractNamedQuery;
import org.corant.suites.query.shared.QueryUtils;
import org.corant.suites.query.shared.mapping.FetchQuery;
import org.corant.suites.query.shared.mapping.QueryHint;
import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.CountOptions;

/**
 * corant-suites-query
 *
 * @author bingo 下午8:20:43
 *
 */
@ApplicationScoped
public abstract class AbstractMgNamedQuery extends AbstractNamedQuery {

  public static final String PRO_KEY_MAX_TIMEMS = "mg.maxTimeMs";
  public static final String PRO_KEY_MAX_AWAIT_TIMEMS = "mg.maxAwaitTimeMs";
  public static final String PRO_KEY_NO_CURSOR_TIMEOUT = "mg.noCursorTimeout";
  public static final String PRO_KEY_OPLOG_REPLAY = "mg.oplogReplay";
  public static final String PRO_KEY_PARTIAL = "mg.partial";
  public static final String PRO_KEY_CURSOR_TYPE = "mg.cursorType";
  public static final String PRO_KEY_BATCH_SIZE = "mg.batchSize";
  public static final String PRO_KEY_RETURN_KEY = "mg.returnKey";
  public static final String PRO_KEY_COMMENT = "mg.comment";
  public static final String PRO_KEY_SHOW_RECORDID = "mg.showRecordId";

  public static final String PRO_KEY_CO = "mg.count-options";
  public static final String PRO_KEY_CO_LIMIT = PRO_KEY_CO + ".limit";
  public static final String PRO_KEY_CO_SKIP = PRO_KEY_CO + ".skip";
  public static final String PRO_KEY_CO_MAX_TIMEMS = PRO_KEY_CO + ".maxTimeMS";

  public static final String PRO_KEY_CO_COLA = PRO_KEY_CO + ".collation";
  public static final String PRO_KEY_CO_COLA_LOCALE = PRO_KEY_CO_COLA + ".locale";
  public static final String PRO_KEY_CO_COLA_CASE_LEVEL = PRO_KEY_CO_COLA + ".caseLevel";
  public static final String PRO_KEY_CO_COLA_CASE_FIRST = PRO_KEY_CO_COLA + ".caseFirst";
  public static final String PRO_KEY_CO_COLA_STRENGTH = PRO_KEY_CO_COLA + ".strength";
  public static final String PRO_KEY_CO_COLA_NUMORD = PRO_KEY_CO_COLA + ".numericOrdering";
  public static final String PRO_KEY_CO_COLA_ALTERNATE = PRO_KEY_CO_COLA + ".alternate";
  public static final String PRO_KEY_CO_COLA_MAXVAR = PRO_KEY_CO_COLA + ".maxVariable";
  public static final String PRO_KEY_CO_COLA_NORMA = PRO_KEY_CO_COLA + ".normalization";
  public static final String PRO_KEY_CO_COLA_BACKWORDS = PRO_KEY_CO_COLA + ".backwards";

  @Inject
  protected MgInLineNamedQueryResolver<String, Map<String, Object>> resolver;

  @Override
  public <T> ForwardList<T> forward(String q, Map<String, Object> param) {
    int offset = getOffset(param);
    int limit = getLimit(param);
    MgQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    log(q, param, querier.getOriginalScript());
    ForwardList<T> result = ForwardList.inst();
    FindIterable<Document> fi = query(querier).skip(offset).limit(limit + 1);
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
    MgQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    log(q, param, querier.getOriginalScript());
    FindIterable<Document> fi = query(querier).limit(1);
    Map<String, Object> result = fi.iterator().tryNext();
    this.fetch(result, fetchQueries, param);
    handleResultHints(resultClass, hints, param, result);
    return convert(result, resultClass);
  }

  @Override
  public <T> PagedList<T> page(String q, Map<String, Object> param) {
    int offset = getOffset(param);
    int limit = getLimit(param);
    MgQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    PagedList<T> result = PagedList.of(offset, limit);
    log(q, param, querier.getOriginalScript());
    FindIterable<Document> fi = query(querier).skip(offset).limit(limit);
    List<Map<String, Object>> list =
        streamOf(fi).map(r -> (Map<String, Object>) r).collect(Collectors.toList());
    int size = getSize(list);
    if (size > 0) {
      if (size < limit) {
        result.withTotal(offset + size);
      } else {
        result.withTotal(Long.valueOf(queryCount(querier)).intValue());
      }
      this.fetch(list, fetchQueries, param);
      handleResultHints(resultClass, hints, param, list);
    }
    return result.withResults(convert(list, resultClass));
  }

  @Override
  public <T> List<T> select(String q, Map<String, Object> param) {
    MgQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    log(q, param, querier.getOriginalScript());
    FindIterable<Document> fi = query(querier).limit(getMaxSelectSize(querier));
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
    MgQuerier querier = getResolver().resolve(q, param);
    Class<T> resultClass = querier.getResultClass();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    List<QueryHint> hints = querier.getHints();
    log(q, param, querier.getOriginalScript());
    return streamOf(query(querier)).map(result -> {
      this.fetch(result, fetchQueries, param);
      handleResultHints(resultClass, hints, param, result);
      return convert(result, resultClass);
    });
  }

  @Override
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
    MgQuerier querier = resolver.resolve(refQueryName, fetchParam);
    List<QueryHint> hints = querier.getHints();
    List<FetchQuery> fetchQueries = querier.getFetchQueries();
    log(refQueryName, param, querier.getOriginalScript());
    FindIterable<Document> fi = query(querier).limit(128);
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

  protected abstract MongoDatabase getDataBase();

  protected MgInLineNamedQueryResolver<String, Map<String, Object>> getResolver() {
    return resolver;
  }

  protected FindIterable<Document> query(MgQuerier querier) {
    FindIterable<Document> fi =
        getDataBase().getCollection(resolveCollectionName(querier.getName())).find();
    EnumMap<MgOperator, Bson> script = querier.getScript();
    for (MgOperator op : MgOperator.values()) {
      switch (op) {
        case FILTER:
          getOpt(script, op).ifPresent(fi::filter);
          break;
        case PROJECTION:
          getOpt(script, op).ifPresent(fi::projection);
          break;
        case MIN:
          getOpt(script, op).ifPresent(fi::min);
          break;
        case MAX:
          getOpt(script, op).ifPresent(fi::max);
          break;
        case HINT:
          getOpt(script, op).ifPresent(fi::hint);
          break;
        case SORT:
          getOpt(script, op).ifPresent(fi::sort);
          break;
        default:
          break;
      }
    }
    Map<String, String> pros = querier.getProperties();
    // handle properties
    getOptMapObject(pros, PRO_KEY_BATCH_SIZE, ConversionUtils::toInteger).ifPresent(fi::batchSize);
    getOptMapObject(pros, PRO_KEY_COMMENT, ConversionUtils::toString).ifPresent(fi::comment);
    CursorType ct = getMapEnum(pros, PRO_KEY_CURSOR_TYPE, CursorType.class);
    if (ct != null) {
      fi.cursorType(ct);
    }
    getOptMapObject(pros, PRO_KEY_MAX_AWAIT_TIMEMS, ConversionUtils::toLong)
        .ifPresent(t -> fi.maxAwaitTime(t, TimeUnit.MILLISECONDS));
    getOptMapObject(pros, PRO_KEY_MAX_TIMEMS, ConversionUtils::toLong)
        .ifPresent(t -> fi.maxTime(t, TimeUnit.MILLISECONDS));
    getOptMapObject(pros, PRO_KEY_NO_CURSOR_TIMEOUT, ConversionUtils::toBoolean)
        .ifPresent(fi::noCursorTimeout);
    getOptMapObject(pros, PRO_KEY_OPLOG_REPLAY, ConversionUtils::toBoolean)
        .ifPresent(fi::oplogReplay);
    getOptMapObject(pros, PRO_KEY_PARTIAL, ConversionUtils::toBoolean).ifPresent(fi::partial);
    getOptMapObject(pros, PRO_KEY_RETURN_KEY, ConversionUtils::toBoolean).ifPresent(fi::returnKey);
    getOptMapObject(pros, PRO_KEY_SHOW_RECORDID, ConversionUtils::toBoolean)
        .ifPresent(fi::showRecordId);
    resovleCollation(querier).ifPresent(fi::collation);
    return fi;
  }

  protected long queryCount(MgQuerier querier) {
    CountOptions co = new CountOptions();
    if (querier.getScript().get(MgOperator.HINT) != null) {
      co.hint(querier.getScript().get(MgOperator.HINT));
    }
    Map<String, String> pros = querier.getProperties();
    getOptMapObject(pros, PRO_KEY_CO_LIMIT, ConversionUtils::toInteger).ifPresent(co::limit);
    getOptMapObject(pros, PRO_KEY_CO_MAX_TIMEMS, ConversionUtils::toLong)
        .ifPresent(t -> co.maxTime(t, TimeUnit.MILLISECONDS));
    getOptMapObject(pros, PRO_KEY_CO_SKIP, ConversionUtils::toInteger).ifPresent(co::skip);
    resovleCollation(querier).ifPresent(co::collation);
    if (co.getLimit() <= 0) {
      co.limit(max(resolveCountOptionsLimit(), 1));
    }
    Bson bson = querier.getScript().getOrDefault(MgOperator.FILTER, new BasicDBObject());
    return getDataBase().getCollection(resolveCollectionName(querier.getName()))
        .countDocuments(bson, co);
  }

  protected String resolveCollectionName(String q) {
    int pos = 0;
    shouldBeTrue(isNotBlank(q) && (pos = q.indexOf('.')) != -1);
    return q.substring(0, pos);
  }

  protected int resolveCountOptionsLimit() {
    return 1024;
  }

  protected Optional<Collation> resovleCollation(MgQuerier querier) {
    Map<String, String> pros = querier.getProperties();
    if (pros.keySet().stream().anyMatch(t -> t.startsWith(PRO_KEY_CO_COLA))) {
      Collation.Builder b = Collation.builder();
      getOptMapObject(pros, PRO_KEY_CO_COLA_ALTERNATE, t -> toEnum(t, CollationAlternate.class))
          .ifPresent(b::collationAlternate);
      getOptMapObject(pros, PRO_KEY_CO_COLA_BACKWORDS, t -> toBoolean(t)).ifPresent(b::backwards);
      getOptMapObject(pros, PRO_KEY_CO_COLA_CASE_FIRST, t -> toEnum(t, CollationCaseFirst.class))
          .ifPresent(b::collationCaseFirst);
      getOptMapObject(pros, PRO_KEY_CO_COLA_CASE_LEVEL, t -> toBoolean(t)).ifPresent(b::caseLevel);
      getOptMapObject(pros, PRO_KEY_CO_COLA_LOCALE, t -> ConversionUtils.toString(t))
          .ifPresent(b::locale);
      getOptMapObject(pros, PRO_KEY_CO_COLA_MAXVAR, t -> toEnum(t, CollationMaxVariable.class))
          .ifPresent(b::collationMaxVariable);
      getOptMapObject(pros, PRO_KEY_CO_COLA_NORMA, t -> toBoolean(t)).ifPresent(b::normalization);
      getOptMapObject(pros, PRO_KEY_CO_COLA_NUMORD, t -> toBoolean(t))
          .ifPresent(b::numericOrdering);
      getOptMapObject(pros, PRO_KEY_CO_COLA_STRENGTH, t -> toEnum(t, CollationStrength.class))
          .ifPresent(b::collationStrength);
      return Optional.of(b.build());
    }

    return Optional.empty();
  }
}
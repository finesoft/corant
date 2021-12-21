package org.corant.modules.query.mongodb;
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

import static java.util.stream.Collectors.toList;
import static org.corant.shared.util.Conversions.toEnum;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Maps.getMapEnum;
import static org.corant.shared.util.Maps.getOptMapObject;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Objects.max;
import static org.corant.shared.util.Objects.min;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.corant.modules.query.Querier;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryParameter.StreamQueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.mapping.FetchQuery;
import org.corant.modules.query.mongodb.MgNamedQuerier.MgOperator;
import org.corant.modules.query.shared.AbstractNamedQuerierResolver;
import org.corant.modules.query.shared.AbstractNamedQueryService;
import org.corant.shared.util.Conversions;
import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.CountOptions;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午8:20:43
 */
public abstract class AbstractMgNamedQueryService extends AbstractNamedQueryService
    implements MgNamedQueryService {

  public static final String PRO_KEY_COLLECTION_NAME = ".collection-name";

  public static final String PRO_KEY_AUTO_SET_ID_FIELD = ".mg.auto-set-id-field";

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
  public static final String PRO_KEY_CO_COLA_BACKWARDS = PRO_KEY_CO_COLA + ".backwards";

  @Override
  public <T> List<T> aggregate(String queryName, Object parameter) {
    try {
      MgNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
      log(queryName, querier.getQueryParameter(), querier.getOriginalScript());
      List<Bson> pipeline = forceCast(querier.getScript().get(MgOperator.AGGREGATE));
      AggregateIterable<Document> ai =
          getDataBase().getCollection(resolveCollectionName(querier)).aggregate(pipeline);
      Map<String, String> pros = querier.getQuery().getProperties();
      getOptMapObject(pros, PRO_KEY_BATCH_SIZE, Conversions::toInteger).ifPresent(ai::batchSize);

      Optional<Long> maxTimeMs = getOptMapObject(pros, PRO_KEY_MAX_TIMEMS, Conversions::toLong);
      if (maxTimeMs.isPresent()) {
        ai.maxTime(maxTimeMs.get(), TimeUnit.MILLISECONDS);
      } else if (querier.resolveTimeout() != null) {
        ai.maxTime(querier.resolveTimeout().toMillis(), TimeUnit.MILLISECONDS);
      }
      getOptMapObject(pros, PRO_KEY_MAX_AWAIT_TIMEMS, Conversions::toLong)
          .ifPresent(t -> ai.maxAwaitTime(t, TimeUnit.MILLISECONDS));
      Optional<Bson> bson =
          Optional.ofNullable(forceCast(querier.getScript().get(MgOperator.HINT)));
      bson.ifPresent(ai::hint);
      resovleCollation(querier).ifPresent(ai::collation);
      List<Document> docList = null;
      try (MongoCursor<Document> cursor = ai.iterator()) {
        docList = listOf(cursor);
      }
      List<Map<String, Object>> list = new ArrayList<>();
      if (docList != null) {
        final boolean setId = isAutoSetIdField(querier);
        list = docList.stream().map(r -> convertDocument(r, querier, setId)).collect(toList());
        this.fetch(list, querier);
      }
      return querier.handleResults(list);
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the aggregate query [%s], exception [%s].", queryName,
          e.getMessage());
    }
  }

  @Override
  public FetchResult fetch(Object result, FetchQuery fetchQuery, Querier parentQuerier) {
    try {
      QueryParameter fetchParam = parentQuerier.resolveFetchQueryParameter(result, fetchQuery);
      String refQueryName = fetchQuery.getReferenceQuery().getVersionedName();
      MgNamedQuerier querier = getQuerierResolver().resolve(refQueryName, fetchParam);
      int maxFetchSize = querier.resolveMaxFetchSize(result, fetchQuery);
      log(refQueryName, querier.getQueryParameter(), querier.getOriginalScript());
      FindIterable<Document> fi = query(querier);
      if (maxFetchSize > 0) {
        fi.limit(maxFetchSize);
      }
      if (!querier.getQuery().getProperties().containsKey(PRO_KEY_BATCH_SIZE)) {
        fi.batchSize(min(maxFetchSize, 128));
      }
      List<Map<String, Object>> fetchedList = null;
      try (MongoCursor<Document> cursor = fi.iterator()) {
        fetchedList = listOf(cursor);// streamOf(fi).collect(Collectors.toList());
      }
      return new FetchResult(fetchQuery, querier, fetchedList);
    } catch (Exception e) {
      throw new QueryRuntimeException(e,
          "An error occurred while executing the fetch query [%s], exception [%s].",
          fetchQuery.getReferenceQuery().getVersionedName(), e.getMessage());
    }
  }

  protected List<Document> collect(FindIterable<Document> fi) {
    List<Document> docList = null;
    try (MongoCursor<Document> cursor = fi.iterator()) {
      docList = listOf(cursor);
    }
    return docList;
  }

  protected Map<String, Object> convertDocument(Document doc, MgNamedQuerier querier,
      boolean autoSetIdField) {
    return doc;
  }

  @Override
  protected <T> Forwarding<T> doForward(String queryName, Object parameter) throws Exception {
    MgNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    int fetchLimit = limit + 1;
    log(queryName, querier.getQueryParameter(), querier.getOriginalScript());
    Forwarding<T> result = Forwarding.inst();
    FindIterable<Document> fi = query(querier).batchSize(fetchLimit).skip(offset).limit(fetchLimit);
    List<Document> docList = collect(fi);
    List<Map<String, Object>> list = new ArrayList<>();
    if (docList != null) {
      final boolean setId = isAutoSetIdField(querier);
      list = docList.stream().map(r -> convertDocument(r, querier, setId)).collect(toList());
      docList.clear();
      if (list.size() > 0) {
        if (list.size() > limit) {
          list.remove(limit);
          result.withHasNext(true);
        }
        this.fetch(list, querier);
      }
    }
    return result.withResults(querier.handleResults(list));
  }

  @Override
  protected <T> T doGet(String queryName, Object parameter) throws Exception {
    MgNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    log(queryName, querier.getQueryParameter(), querier.getOriginalScript());
    FindIterable<Document> fi = query(querier).limit(1);
    Document result = null;
    try (MongoCursor<Document> cursor = fi.iterator()) {
      result = cursor.tryNext();
    }
    Map<String, Object> theResult = null;
    if (result != null) {
      theResult = convertDocument(result, querier, isAutoSetIdField(querier));
      this.fetch(theResult, querier);
    }
    return querier.handleResult(theResult);
  }

  @Override
  protected <T> Paging<T> doPage(String queryName, Object parameter) throws Exception {
    MgNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    int offset = querier.resolveOffset();
    int limit = querier.resolveLimit();
    Paging<T> result = Paging.of(offset, limit);
    log(queryName, querier.getQueryParameter(), querier.getOriginalScript());
    FindIterable<Document> fi = query(querier).batchSize(limit).skip(offset).limit(limit);
    List<Document> docList = collect(fi);
    List<Map<String, Object>> list = new ArrayList<>();
    if (docList != null) {
      final boolean setId = isAutoSetIdField(querier);
      list = docList.stream().map(r -> convertDocument(r, querier, setId)).collect(toList());
      docList.clear();
      if (list.size() > 0) {
        if (list.size() < limit) {
          result.withTotal(offset + list.size());
        } else {
          result.withTotal((int) queryCount(querier));
        }
        this.fetch(list, querier);
      }
    }
    return result.withResults(querier.handleResults(list));
  }

  @Override
  protected <T> List<T> doSelect(String queryName, Object parameter) throws Exception {
    MgNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    log(queryName, querier.getQueryParameter(), querier.getOriginalScript());
    int maxSelectSize = querier.resolveMaxSelectSize();
    FindIterable<Document> fi = query(querier).limit(maxSelectSize + 1);
    List<Document> docList = collect(fi);
    List<Map<String, Object>> list = new ArrayList<>();
    if (docList != null) {
      final boolean setId = isAutoSetIdField(querier);
      list = docList.stream().map(r -> convertDocument(r, querier, setId)).collect(toList());
      docList.clear();
      if (querier.handleResultSize(list) > 0) {
        this.fetch(list, querier);
      }
    }
    return querier.handleResults(list);
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   * Note: Be careful about {@link FindIterable#noCursorTimeout(boolean)}
   *
   * @see #query(MgNamedQuerier)
   * @see #PRO_KEY_NO_CURSOR_TIMEOUT
   */
  @Override
  protected <T> Stream<T> doStream(String queryName, StreamQueryParameter parameter) {
    if (parameter.getEnhancer() != null) {
      return super.doStream(queryName, parameter);
    }
    final MgNamedQuerier querier = getQuerierResolver().resolve(queryName, parameter);
    log("stream->" + queryName, querier.getQueryParameter(), querier.getOriginalScript());
    final MongoCursor<Document> cursor = query(querier).batchSize(parameter.getLimit()).iterator();
    final Iterator<T> iterator = new Iterator<>() {
      int counter = 0;
      Forwarding<T> buffer = null;
      T next = null;

      @Override
      public boolean hasNext() {
        initialize();
        boolean more = false;
        if (!parameter.terminateIf(counter, next)) {
          if (!buffer.hasResults()) {
            if (buffer.hasNext()) {
              buffer.with(doForward(cursor));
              more = buffer.hasResults();
            }
          } else {
            more = true;
          }
        }
        if (!more && cursor != null) {
          cursor.close();
        }
        return more;
      }

      @Override
      public T next() {
        initialize();
        if (!buffer.hasResults()) {
          throw new NoSuchElementException();
        }
        counter++;
        next = buffer.getResults().remove(0);
        return next;
      }

      private Forwarding<T> doForward(MongoCursor<Document> it) {
        int size = parameter.getLimit();
        List<Object> list = new ArrayList<>(size);
        while (it.hasNext() && --size >= 0) {
          list.add(it.next());
        }
        fetch(list, querier);
        return Forwarding.of(querier.handleResults(list), it.hasNext());
      }

      private void initialize() {
        if (buffer == null) {
          buffer = defaultObject(doForward(cursor), Forwarding::inst);
          counter = buffer.hasResults() ? 1 : 0;
        }
      }
    };
    Stream<T> stream = streamOf(iterator).onClose(cursor::close);
    Cleaner.create().register(iterator, () -> {
      if (cursor != null) {
        cursor.close();
      }
    });// JDK9+
    return stream;
  }

  protected abstract MongoDatabase getDataBase();

  @Override
  protected abstract AbstractNamedQuerierResolver<MgNamedQuerier> getQuerierResolver();

  protected boolean isAutoSetIdField(MgNamedQuerier querier) {
    return querier.resolveProperty(PRO_KEY_AUTO_SET_ID_FIELD, Boolean.class, Boolean.TRUE);
  }

  protected FindIterable<Document> query(MgNamedQuerier querier) {
    FindIterable<Document> fi = getDataBase().getCollection(resolveCollectionName(querier)).find();
    EnumMap<MgOperator, Object> script = querier.getScript();
    for (MgOperator op : MgOperator.values()) {
      Optional<Bson> bson = Optional.ofNullable(forceCast(script.get(op)));
      switch (op) {
        case FILTER:
          bson.ifPresent(fi::filter);
          break;
        case PROJECTION:
          bson.ifPresent(fi::projection);
          break;
        case MIN:
          bson.ifPresent(fi::min);
          break;
        case MAX:
          bson.ifPresent(fi::max);
          break;
        case HINT:
          bson.ifPresent(fi::hint);
          break;
        case SORT:
          bson.ifPresent(fi::sort);
          break;
        default:
          break;
      }
    }
    Map<String, String> pros = querier.getQuery().getProperties();
    // handle properties
    fi.batchSize(querier.resolveLimit());
    int offset = querier.resolveOffset();
    if (offset > 0) {
      fi.skip(offset);
    }
    getOptMapObject(pros, PRO_KEY_BATCH_SIZE, Conversions::toInteger).ifPresent(fi::batchSize);
    getOptMapObject(pros, PRO_KEY_COMMENT, Conversions::toString).ifPresent(fi::comment);
    CursorType ct = getMapEnum(pros, PRO_KEY_CURSOR_TYPE, CursorType.class);
    if (ct != null) {
      fi.cursorType(ct);
    }
    getOptMapObject(pros, PRO_KEY_MAX_AWAIT_TIMEMS, Conversions::toLong)
        .ifPresent(t -> fi.maxAwaitTime(t, TimeUnit.MILLISECONDS));

    Optional<Long> maxTimeMs = getOptMapObject(pros, PRO_KEY_MAX_TIMEMS, Conversions::toLong);
    if (maxTimeMs.isPresent()) {
      fi.maxTime(maxTimeMs.get(), TimeUnit.MILLISECONDS);
    } else if (querier.resolveTimeout() != null) {
      fi.maxTime(querier.resolveTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
    getOptMapObject(pros, PRO_KEY_NO_CURSOR_TIMEOUT, Conversions::toBoolean)
        .ifPresent(fi::noCursorTimeout);
    getOptMapObject(pros, PRO_KEY_OPLOG_REPLAY, Conversions::toBoolean).ifPresent(fi::oplogReplay);
    getOptMapObject(pros, PRO_KEY_PARTIAL, Conversions::toBoolean).ifPresent(fi::partial);
    getOptMapObject(pros, PRO_KEY_RETURN_KEY, Conversions::toBoolean).ifPresent(fi::returnKey);
    getOptMapObject(pros, PRO_KEY_SHOW_RECORDID, Conversions::toBoolean)
        .ifPresent(fi::showRecordId);
    resovleCollation(querier).ifPresent(fi::collation);
    return fi;
  }

  protected long queryCount(MgNamedQuerier querier) {
    CountOptions co = new CountOptions();
    if (querier.getScript().get(MgOperator.HINT) != null) {
      co.hint((Bson) querier.getScript(null).get(MgOperator.HINT));
    }
    Map<String, String> pros = querier.getQuery().getProperties();
    getOptMapObject(pros, PRO_KEY_CO_LIMIT, Conversions::toInteger).ifPresent(co::limit);

    Optional<Long> maxTimeMs = getOptMapObject(pros, PRO_KEY_CO_MAX_TIMEMS, Conversions::toLong);
    if (maxTimeMs.isPresent()) {
      co.maxTime(maxTimeMs.get(), TimeUnit.MILLISECONDS);
    } else if (querier.resolveTimeout() != null) {
      co.maxTime(querier.resolveTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }

    getOptMapObject(pros, PRO_KEY_CO_SKIP, Conversions::toInteger).ifPresent(co::skip);
    resovleCollation(querier).ifPresent(co::collation);
    if (co.getLimit() <= 0) {
      co.limit(max(resolveCountOptionsLimit(), 1));
    }
    Bson bson =
        forceCast(querier.getScript(null).getOrDefault(MgOperator.FILTER, new BasicDBObject()));
    return getDataBase().getCollection(resolveCollectionName(querier)).countDocuments(bson, co);
  }

  protected String resolveCollectionName(MgNamedQuerier querier) {
    String colName = querier.resolveProperty(PRO_KEY_COLLECTION_NAME, String.class, null);
    return isNotBlank(colName) ? colName : querier.getCollectionName(); // FIXME
  }

  protected int resolveCountOptionsLimit() {
    return 1024;
  }

  protected Optional<Collation> resovleCollation(MgNamedQuerier querier) {
    Map<String, String> pros = querier.getQuery().getProperties();
    if (pros.keySet().stream().anyMatch(t -> t.startsWith(PRO_KEY_CO_COLA))) {
      Collation.Builder b = Collation.builder();
      getOptMapObject(pros, PRO_KEY_CO_COLA_ALTERNATE, t -> toEnum(t, CollationAlternate.class))
          .ifPresent(b::collationAlternate);
      getOptMapObject(pros, PRO_KEY_CO_COLA_BACKWARDS, Conversions::toBoolean)
          .ifPresent(b::backwards);
      getOptMapObject(pros, PRO_KEY_CO_COLA_CASE_FIRST, t -> toEnum(t, CollationCaseFirst.class))
          .ifPresent(b::collationCaseFirst);
      getOptMapObject(pros, PRO_KEY_CO_COLA_CASE_LEVEL, Conversions::toBoolean)
          .ifPresent(b::caseLevel);
      getOptMapObject(pros, PRO_KEY_CO_COLA_LOCALE, Conversions::toString).ifPresent(b::locale);
      getOptMapObject(pros, PRO_KEY_CO_COLA_MAXVAR, t -> toEnum(t, CollationMaxVariable.class))
          .ifPresent(b::collationMaxVariable);
      getOptMapObject(pros, PRO_KEY_CO_COLA_NORMA, Conversions::toBoolean)
          .ifPresent(b::normalization);
      getOptMapObject(pros, PRO_KEY_CO_COLA_NUMORD, Conversions::toBoolean)
          .ifPresent(b::numericOrdering);
      getOptMapObject(pros, PRO_KEY_CO_COLA_STRENGTH, t -> toEnum(t, CollationStrength.class))
          .ifPresent(b::collationStrength);
      return Optional.of(b.build());
    }

    return Optional.empty();
  }
}

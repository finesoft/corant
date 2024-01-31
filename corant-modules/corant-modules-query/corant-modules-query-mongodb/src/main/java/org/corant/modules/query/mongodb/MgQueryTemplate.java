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
package org.corant.modules.query.mongodb;

import static org.corant.context.Beans.findNamed;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.corant.modules.mongodb.MongoExtendedJsons;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryParameter;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.CursorType;
import com.mongodb.ExplainVerbosity;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import net.jcip.annotations.NotThreadSafe;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午4:04:25
 */
@NotThreadSafe
public class MgQueryTemplate {

  protected final MongoDatabase database;
  protected String collection;
  protected Bson filter;
  protected Bson sort;
  protected Bson projection;
  protected Bson max;
  protected Bson min;
  protected Bson hint;
  protected List<Bson> aggregatePipeline;
  protected int limit = 1;
  protected int offset = 0;
  protected boolean autoSetIdField = true;

  protected Boolean bypassDocumentValidation;
  protected Collation collation;
  protected Duration maxTime;
  protected Duration maxAwaitTime;
  protected Boolean partial;
  protected CursorType cursorType;
  protected Integer batchSize;
  protected BsonValue comment;
  protected Bson variables;
  protected Boolean returnKey;
  protected Boolean showRecordId;
  protected Boolean allowDiskUse;
  protected Boolean noCursorTimeout;
  protected ExplainVerbosity explainVerbosity;
  protected Class<?> explainResultClass;

  private MgQueryTemplate(MongoDatabase database) {
    this.database = shouldNotNull(database);
  }

  private MgQueryTemplate(String namedDatabase) {
    database = findNamed(MongoDatabase.class, shouldNotNull(namedDatabase))
        .orElseThrow(QueryRuntimeException::new);
  }

  public static MgQueryTemplate database(MongoClient client, String database) {
    return new MgQueryTemplate(shouldNotNull(client).getDatabase(database));
  }

  public static MgQueryTemplate database(MongoDatabase database) {
    return new MgQueryTemplate(database);
  }

  public static MgQueryTemplate database(String database) {
    return new MgQueryTemplate(MongoDatabases.resolveDatabase(database));
  }

  public List<Map<?, ?>> aggregate() {
    AggregateIterable<Document> ai =
        database.getCollection(collection).aggregate(aggregatePipeline);
    if (collation != null) {
      ai.collation(collation);
    }
    if (maxTime != null) {
      ai.maxTime(maxTime.toMillis(), TimeUnit.MILLISECONDS);
    }
    if (maxAwaitTime != null) {
      ai.maxAwaitTime(maxAwaitTime.toMillis(), TimeUnit.MILLISECONDS);
    }
    if (batchSize != null) {
      ai.batchSize(batchSize);
    }
    if (comment != null) {
      ai.comment(comment);
    }
    if (variables != null) {
      ai.let(variables);
    }
    if (hint != null) {
      ai.hint(hint);
    }
    if (bypassDocumentValidation != null) {
      ai.bypassDocumentValidation(bypassDocumentValidation);
    }
    if (allowDiskUse != null) {
      ai.allowDiskUse(allowDiskUse);
    }
    if (explainVerbosity != null) {
      ai.explain(explainVerbosity);
    }
    if (explainResultClass != null) {
      ai.explain(explainResultClass);
    }
    try (MongoCursor<Document> cursor = ai.iterator()) {
      return streamOf(cursor).onClose(cursor::close).map(this::convert)
          .collect(Collectors.toList());
    }
  }

  public MgQueryTemplate aggregatePipeline(Bson... pipeline) {
    aggregatePipeline = shouldNotEmpty(listOf(pipeline));
    return this;
  }

  public MgQueryTemplate aggregatePipeline(List<Map<?, ?>> pipeline) {
    aggregatePipeline = shouldNotEmpty(pipeline).stream().map(a -> {
      try {
        return MongoExtendedJsons.toBson(a, true);
      } catch (JsonProcessingException e) {
        throw new QueryRuntimeException(e);
      }
    }).collect(Collectors.toList());
    return this;
  }

  public MgQueryTemplate allowDiskUse(Boolean allowDiskUse) {
    this.allowDiskUse = allowDiskUse;
    return this;
  }

  public MgQueryTemplate autoSetIdField(boolean autoSetIdField) {
    this.autoSetIdField = autoSetIdField;
    return this;
  }

  public MgQueryTemplate batchSize(Integer batchSize) {
    this.batchSize = batchSize;
    return this;
  }

  /**
   * @see AggregateIterable#bypassDocumentValidation(Boolean)
   */
  public MgQueryTemplate bypassDocumentValidation(Boolean bypassDocumentValidation) {
    this.bypassDocumentValidation = bypassDocumentValidation;
    return this;
  }

  public MgQueryTemplate collation(Collation collation) {
    this.collation = collation;
    return this;
  }

  public MgQueryTemplate collection(String collection) {
    this.collection = shouldNotNull(collection);
    return this;
  }

  public MgQueryTemplate comment(BsonValue comment) {
    this.comment = comment;
    return this;
  }

  public long count() {
    MongoCollection<Document> mc = database.getCollection(collection);
    if (isNotEmpty(filter)) {
      return mc.countDocuments(filter);
    }
    return mc.countDocuments();
  }

  public MgQueryTemplate cursorType(CursorType cursorType) {
    this.cursorType = cursorType;
    return this;
  }

  public MgQueryTemplate explainResultClass(Class<?> explainResultClass) {
    this.explainResultClass = explainResultClass;
    return this;
  }

  public MgQueryTemplate explainVerbosity(ExplainVerbosity explainVerbosity) {
    this.explainVerbosity = explainVerbosity;
    return this;
  }

  public MgQueryTemplate filter(Bson filter) {
    this.filter = filter;
    return this;
  }

  @Experimental
  public MgQueryTemplate filter(Map<?, ?> filter) {
    return filter(parse(filter, true));
  }

  public Forwarding<Map<?, ?>> forward() {
    Forwarding<Map<?, ?>> result = Forwarding.inst();
    FindIterable<Document> fi = query().skip(offset).limit(limit + 1);
    try (MongoCursor<Document> cursor = fi.iterator()) {
      List<Map<?, ?>> list = streamOf(cursor).map(this::convert).collect(Collectors.toList());
      if (sizeOf(list) > limit) {
        list.remove(limit);
        result.withHasNext(true);
      }
      return result.withResults(list);
    }
  }

  public <T> Forwarding<T> forwardAs(final Class<T> clazz) {
    return forwardAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> Forwarding<T> forwardAs(final Function<Object, T> converter) {
    Forwarding<T> result = Forwarding.inst();
    FindIterable<Document> fi = query().skip(offset).limit(limit + 1);
    try (MongoCursor<Document> cursor = fi.iterator()) {
      List<Map<?, ?>> list = streamOf(cursor).map(this::convert).collect(Collectors.toList());
      if (sizeOf(list) > limit) {
        list.remove(limit);
        result.withHasNext(true);
      }
      return result.withResults(list.stream().map(converter).collect(Collectors.toList()));
    }
  }

  public Map<?, ?> get() {
    try (MongoCursor<Document> it = query().limit(1).iterator()) {
      return it.hasNext() ? convert(it.next()) : null;
    }
  }

  public <T> T getAs(final Class<T> clazz) {
    return getAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> T getAs(final Function<Object, T> converter) {
    Map<?, ?> document = get();
    return document != null ? converter.apply(document) : null;
  }

  public MgQueryTemplate hint(Bson hint) {
    this.hint = hint;
    return this;
  }

  public MgQueryTemplate hint(Map<?, ?> hint) {
    return hint(parse(hint, false));
  }

  /**
   * The expected number of query result set or the expected size of the result set of each
   * iteration of the streaming query
   *
   * @see QueryParameter#getLimit()
   */
  public MgQueryTemplate limit(int limit) {
    this.limit = Objects.max(limit, 1);
    return this;
  }

  public MgQueryTemplate max(Bson max) {
    this.max = max;
    return this;
  }

  public MgQueryTemplate max(Map<?, ?> max) {
    return max(parse(max, false));
  }

  public MgQueryTemplate maxAwaitTime(Duration maxAwaitTime) {
    this.maxAwaitTime = maxAwaitTime;
    return this;
  }

  public MgQueryTemplate maxTime(Duration maxTime) {
    this.maxTime = maxTime;
    return this;
  }

  public MgQueryTemplate min(Bson min) {
    this.min = min;
    return this;
  }

  public MgQueryTemplate min(Map<?, ?> min) {
    return min(parse(min, false));
  }

  public MgQueryTemplate offset(int offset) {
    this.offset = Objects.max(offset, 0);
    return this;
  }

  public Paging<Map<?, ?>> page() {
    Paging<Map<?, ?>> result = Paging.of(offset, limit);
    FindIterable<Document> fi = query();
    try (MongoCursor<Document> cursor = fi.iterator()) {
      List<Map<?, ?>> list = streamOf(cursor).map(this::convert).collect(Collectors.toList());
      int size = list.size();
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          result.withTotal((int) count());
        }
      }
      return result.withResults(list);
    }
  }

  public <T> Paging<T> pageAs(final Class<T> clazz) {
    return pageAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> Paging<T> pageAs(final Function<Object, T> converter) {
    Paging<T> result = Paging.of(offset, limit);
    FindIterable<Document> fi = query();
    try (MongoCursor<Document> cursor = fi.iterator()) {
      List<Map<?, ?>> list = streamOf(cursor).map(this::convert).collect(Collectors.toList());
      int size = list.size();
      if (size > 0) {
        if (size < limit) {
          result.withTotal(offset + size);
        } else {
          result.withTotal((int) count());
        }
      }
      return result.withResults(list.stream().map(converter).collect(Collectors.toList()));
    }
  }

  public MgQueryTemplate partial(Boolean partial) {
    this.partial = partial;
    return this;
  }

  public MgQueryTemplate projection(boolean include, String... propertyNames) {
    if (include) {
      return projection(propertyNames, Strings.EMPTY_ARRAY);
    } else {
      return projection(Strings.EMPTY_ARRAY, propertyNames);
    }
  }

  public MgQueryTemplate projection(Bson projection) {
    this.projection = projection;
    return this;
  }

  public MgQueryTemplate projection(Map<?, ?> projection) {
    return projection(parse(projection, false));
  }

  public MgQueryTemplate projection(String[] includePropertyNames, String[] excludePropertyNames) {
    Map<String, Boolean> projections = new HashMap<>();
    if (includePropertyNames != null) {
      for (String proNme : includePropertyNames) {
        if (isNotBlank(proNme)) {
          projections.put(proNme, true);
        }
      }
    }
    if (excludePropertyNames != null) {
      for (String proNme : excludePropertyNames) {
        if (isNotBlank(proNme)) {
          projections.put(proNme, false);
        }
      }
    }
    if (!projections.isEmpty()) {
      return projection(projections);
    }
    return this;
  }

  public MgQueryTemplate returnKey(Boolean returnKey) {
    this.returnKey = returnKey;
    return this;
  }

  public List<Map<?, ?>> select() {
    try (MongoCursor<Document> it = query().iterator()) {
      return streamOf(it).map(this::convert).collect(Collectors.toList());
    }
  }

  public <T> List<T> selectAs(final Class<T> clazz) {
    return selectAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> List<T> selectAs(final Function<Object, T> converter) {
    try (MongoCursor<Document> it = query().iterator()) {
      return streamOf(it).map(this::convert).map(converter).collect(Collectors.toList());
    }
  }

  public MgQueryTemplate showRecordId(Boolean showRecordId) {
    this.showRecordId = showRecordId;
    return this;
  }

  public <T> T single(final Class<T> clazz) {
    List<Map<?, ?>> result = select();
    if (isEmpty(result)) {
      return null;
    } else {
      shouldBeTrue(result.size() == 1 && result.get(0).size() == 1, () -> new QueryRuntimeException(
          "The size %s of query result set must not greater than one and the result record must have only one field.",
          result.size()));
      return toObject(result.get(0).entrySet().iterator().next().getValue(), clazz);
    }
  }

  public <T> List<T> singles(final Class<T> clazz) {
    List<Map<?, ?>> result = select();
    if (isEmpty(result)) {
      return null;
    } else {
      return result.stream().map(m -> m.entrySet().iterator().next().getValue())
          .map(r -> toObject(r, clazz)).toList();
    }
  }

  public MgQueryTemplate sort(Bson sort) {
    this.sort = sort;
    return this;
  }

  public MgQueryTemplate sort(Map<?, ?> sort) {
    return sort(parse(sort, false));
  }

  @SuppressWarnings("rawtypes")
  public Stream<Map<?, ?>> stream() {
    limit = 0; // NO LIMIT
    final MongoCursor<Document> it = query().iterator();
    return streamOf(it).onClose(it::close).map(this::convert).map(r -> (Map) r);
  }

  public <T> Stream<T> streamAs(final Class<T> clazz) {
    return streamAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> Stream<T> streamAs(final Function<Object, T> converter) {
    limit = 0; // NO LIMIT
    final MongoCursor<Document> it = query().iterator();
    return streamOf(it).onClose(it::close).map(this::convert).map(converter);
  }

  public MgQueryTemplate variables(Bson variables) {
    this.variables = variables;
    return this;
  }

  public MgQueryTemplate variables(Map<?, ?> variables) {
    return variables(parse(variables, false));
  }

  protected Map<?, ?> convert(Document doc) {
    if (doc != null && autoSetIdField && !doc.containsKey("id") && doc.containsKey("_id")) {
      doc.put("id", doc.get("_id"));
    }
    return doc;
  }

  protected Bson parse(Object object, boolean extended) {
    try {
      return MongoExtendedJsons.toBson(object, extended);
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  protected FindIterable<Document> query() {
    MongoCollection<Document> mc = database.getCollection(collection);
    FindIterable<Document> fi = mc.find();
    if (filter != null) {
      fi.filter(filter);
    }
    if (min != null) {
      fi.min(min);
    }
    if (max != null) {
      fi.max(max);
    }
    if (hint != null) {
      fi.hint(hint);
    }
    if (sort != null) {
      fi.sort(sort);
    }
    if (projection != null) {
      fi.projection(projection);
    }
    if (limit > 0) {
      fi.limit(limit);
    }
    if (offset > 0) {
      fi.skip(offset);
    }
    if (collation != null) {
      fi.collation(collation);
    }
    if (maxTime != null) {
      fi.maxTime(maxTime.toMillis(), TimeUnit.MILLISECONDS);
    }
    if (maxAwaitTime != null) {
      fi.maxAwaitTime(maxAwaitTime.toMillis(), TimeUnit.MILLISECONDS);
    }
    if (partial != null) {
      fi.partial(partial);
    }
    if (cursorType != null) {
      fi.cursorType(cursorType);
    }
    if (batchSize != null) {
      fi.batchSize(batchSize);
    }
    if (comment != null) {
      fi.comment(comment);
    }
    if (variables != null) {
      fi.let(variables);
    }
    if (returnKey != null) {
      fi.returnKey(returnKey);
    }
    if (showRecordId != null) {
      fi.showRecordId(showRecordId);
    }
    if (allowDiskUse != null) {
      fi.allowDiskUse(allowDiskUse);
    }
    if (noCursorTimeout != null) {
      fi.noCursorTimeout(noCursorTimeout);
    }
    if (explainVerbosity != null) {
      fi.explain(explainVerbosity);
    }
    if (explainResultClass != null) {
      fi.explain(explainResultClass);
    }
    return fi;
  }
}

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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.corant.modules.mongodb.MongoExtendedJsons;
import org.corant.modules.mongodb.MongoTemplate.TerminableMongoCursor;
import org.corant.modules.query.QueryObjectMapper;
import org.corant.modules.query.QueryRuntimeException;
import org.corant.modules.query.QueryService.Forwarding;
import org.corant.modules.query.QueryService.Paging;
import org.corant.shared.ubiquity.Experimental;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.BasicDBObject;
import com.mongodb.CursorType;
import com.mongodb.ExplainVerbosity;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
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

  /**
   * Aggregates documents according to the specified aggregation pipeline and the specified
   * {@link AggregateIterable} arguments
   */
  public List<Map<String, Object>> aggregate() {
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

  /**
   * @see MongoCollection#aggregate(List)
   */
  public MgQueryTemplate aggregatePipeline(Bson... pipeline) {
    aggregatePipeline = shouldNotEmpty(listOf(pipeline));
    return this;
  }

  /**
   * @see MongoCollection#aggregate(List)
   * @see MongoExtendedJsons#toBsons(java.util.Collection, boolean)
   */
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

  /**
   * @see FindIterable#allowDiskUse(Boolean)
   * @see AggregateIterable#allowDiskUse(Boolean)
   */
  public MgQueryTemplate allowDiskUse(Boolean allowDiskUse) {
    this.allowDiskUse = allowDiskUse;
    return this;
  }

  /**
   * Whether to append 'id' property whose value is derived from the existing "_id" property.
   *
   * @param autoSetIdField whether to append
   */
  public MgQueryTemplate autoSetIdField(boolean autoSetIdField) {
    this.autoSetIdField = autoSetIdField;
    return this;
  }

  /**
   * @see FindIterable#batchSize(int)
   * @see AggregateIterable#batchSize(int)
   */
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

  /**
   * @see FindIterable#collation(Collation)
   * @see AggregateIterable#collation(Collation)
   */
  public MgQueryTemplate collation(Collation collation) {
    this.collation = collation;
    return this;
  }

  /**
   * Set the name of the collection which will be queried.
   *
   * @param collection the collection name
   */
  public MgQueryTemplate collection(String collection) {
    this.collection = shouldNotNull(collection);
    return this;
  }

  /**
   * @see FindIterable#comment(BsonValue)
   * @see AggregateIterable#comment(BsonValue)
   */
  public MgQueryTemplate comment(BsonValue comment) {
    this.comment = comment;
    return this;
  }

  /**
   * Counts the number of documents in the collection according to the given options.
   */
  public long count() {
    MongoCollection<Document> mc = database.getCollection(collection);
    if (isNotEmpty(filter)) {
      return mc.countDocuments(filter);
    }
    return mc.countDocuments();
  }

  /**
   * @see FindIterable#cursorType(CursorType)
   */
  public MgQueryTemplate cursorType(CursorType cursorType) {
    this.cursorType = cursorType;
    return this;
  }

  /**
   * @see FindIterable#explain(Class)
   * @see AggregateIterable#explain(Class)
   */
  public MgQueryTemplate explainResultClass(Class<?> explainResultClass) {
    this.explainResultClass = explainResultClass;
    return this;
  }

  /**
   * @see FindIterable#explain(ExplainVerbosity)
   * @see AggregateIterable#explain(ExplainVerbosity)
   */
  public MgQueryTemplate explainVerbosity(ExplainVerbosity explainVerbosity) {
    this.explainVerbosity = explainVerbosity;
    return this;
  }

  /**
   * @see FindIterable#filter(Bson)
   */
  public MgQueryTemplate filter(Bson filter) {
    this.filter = filter;
    return this;
  }

  /**
   * @see FindIterable#filter(Bson)
   * @see MongoExtendedJsons#toBson(Object, boolean)
   */
  @Experimental
  public MgQueryTemplate filter(Map<?, ?> filter) {
    return filter(parse(filter, true));
  }

  /**
   * Step forward query, do not pagination, do not calculate the total number of records, can be
   * used for mass data streaming processing. Returns a list of documents and a flag indicating
   * whether there is more documents in the collection according to the given options.
   */
  public Forwarding<Map<String, Object>> forward() {
    Forwarding<Map<String, Object>> result = Forwarding.inst();
    FindIterable<Document> fi = query().skip(offset).limit(limit + 1);
    try (MongoCursor<Document> cursor = fi.iterator()) {
      List<Map<String, Object>> list =
          streamOf(cursor).map(this::convert).collect(Collectors.toList());
      if (sizeOf(list) > limit) {
        list.remove(limit);
        result.withHasNext(true);
      }
      return result.withResults(list);
    }
  }

  /**
   * Step forward query, do not pagination, do not calculate the total number of records, can be
   * used for mass data streaming processing. Returns a list of typed objects and a flag indicating
   * whether there is more documents in the collection according to the given options.
   *
   * @param <T> the type to which the document is to be converted
   * @param clazz the class to which the document is to be converted
   */
  public <T> Forwarding<T> forwardAs(final Class<T> clazz) {
    return forwardAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  /**
   * Step forward query, do not pagination, do not calculate the total number of records, can be
   * used for mass data streaming processing. Returns a list of typed objects and a flag indicating
   * whether there is more documents in the collection according to the given options.
   *
   * @param <T> the type to which the document is to be converted
   * @param converter the converter use for document conversion
   */
  public <T> Forwarding<T> forwardAs(final Function<Object, T> converter) {
    Forwarding<T> result = Forwarding.inst();
    FindIterable<Document> fi = query().skip(offset).limit(limit + 1);
    try (MongoCursor<Document> cursor = fi.iterator()) {
      List<Map<String, Object>> list =
          streamOf(cursor).map(this::convert).collect(Collectors.toList());
      if (sizeOf(list) > limit) {
        list.remove(limit);
        result.withHasNext(true);
      }
      return result.withResults(list.stream().map(converter).collect(Collectors.toList()));
    }
  }

  /**
   * Return the document in the collection according to the given options.
   */
  public Map<String, Object> get() {
    try (MongoCursor<Document> it = query().limit(1).iterator()) {
      return it.hasNext() ? convert(it.next()) : null;
    }
  }

  /**
   * Return the typed object in the collection according to the given options.
   *
   * @param <T> the type to which the document is to be converted
   * @param clazz the class to which the document is to be converted
   */
  public <T> T getAs(final Class<T> clazz) {
    return getAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  /**
   * Return the typed object in the collection according to the given options.
   *
   * @param <T> the type to which the document is to be converted
   * @param converter the converter use for document conversion
   */
  public <T> T getAs(final Function<Object, T> converter) {
    Map<?, ?> document = get();
    return document != null ? converter.apply(document) : null;
  }

  /**
   * @see FindIterable#hint(Bson)
   * @see AggregateIterable#hint(Bson)
   */
  public MgQueryTemplate hint(Bson hint) {
    this.hint = hint;
    return this;
  }

  /**
   * @see FindIterable#hint(Bson)
   * @see AggregateIterable#hint(Bson)
   * @see BasicDBObject#parse(String)
   */
  public MgQueryTemplate hint(Map<?, ?> hint) {
    return hint(parse(hint, false));
  }

  /**
   * The expected number of query result set.
   *
   * @see FindIterable#limit(int)
   */
  public MgQueryTemplate limit(int limit) {
    this.limit = Objects.max(limit, 0);
    return this;
  }

  /**
   * @see FindIterable#max(Bson)
   */
  public MgQueryTemplate max(Bson max) {
    this.max = max;
    return this;
  }

  /**
   * @see FindIterable#max(Bson)
   * @see BasicDBObject#parse(String)
   */
  public MgQueryTemplate max(Map<?, ?> max) {
    return max(parse(max, false));
  }

  /**
   * @see FindIterable#maxAwaitTime(long, TimeUnit)
   * @see AggregateIterable#maxAwaitTime(long, TimeUnit)
   */
  public MgQueryTemplate maxAwaitTime(Duration maxAwaitTime) {
    this.maxAwaitTime = maxAwaitTime;
    return this;
  }

  /**
   * @see FindIterable#maxTime(long, TimeUnit)
   * @see AggregateIterable#maxTime(long, TimeUnit)
   */
  public MgQueryTemplate maxTime(Duration maxTime) {
    this.maxTime = maxTime;
    return this;
  }

  /**
   * @see FindIterable#min(Bson)
   */
  public MgQueryTemplate min(Bson min) {
    this.min = min;
    return this;
  }

  /**
   * @see FindIterable#min(Bson)
   * @see BasicDBObject#parse(String)
   */
  public MgQueryTemplate min(Map<?, ?> min) {
    return min(parse(min, false));
  }

  /**
   * @see FindIterable#skip(int)
   */
  public MgQueryTemplate offset(int offset) {
    this.offset = Objects.max(offset, 0);
    return this;
  }

  /**
   * Paging query, returns the documents and total documents counts in the collection according to
   * the given options.
   */
  public Paging<Map<String, Object>> page() {
    Paging<Map<String, Object>> result = Paging.of(offset, limit);
    FindIterable<Document> fi = query();
    try (MongoCursor<Document> cursor = fi.iterator()) {
      List<Map<String, Object>> list =
          streamOf(cursor).map(this::convert).collect(Collectors.toList());
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

  /**
   * Paging query, converts and returns the documents and total documents counts in the collection
   * according to the given options.
   *
   * @param <T> the type to which the document is to be converted
   * @param clazz the class to which the document is to be converted
   */
  public <T> Paging<T> pageAs(final Class<T> clazz) {
    return pageAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  /**
   * Paging query, converts and returns the documents and total documents counts in the collection
   * according to the given options.
   *
   * @param <T> the type to which the document is to be converted
   * @param converter the converter use for document conversion
   */
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

  /**
   * @see FindIterable#partial(boolean)
   */
  public MgQueryTemplate partial(Boolean partial) {
    this.partial = partial;
    return this;
  }

  /**
   * Set the projection
   *
   * @param include whether to project or not
   * @param propertyNames the property names
   * @see FindIterable#projection(Bson)
   */
  public MgQueryTemplate projection(boolean include, String... propertyNames) {
    if (include) {
      return projection(propertyNames, Strings.EMPTY_ARRAY);
    } else {
      return projection(Strings.EMPTY_ARRAY, propertyNames);
    }
  }

  /**
   * @see FindIterable#projection(Bson)
   */
  public MgQueryTemplate projection(Bson projection) {
    this.projection = projection;
    return this;
  }

  /**
   * @see FindIterable#projection(Bson)
   * @see BasicDBObject#parse(String)
   */
  public MgQueryTemplate projection(Map<?, ?> projection) {
    return projection(parse(projection, false));
  }

  /**
   * Set the projections
   *
   * @param includePropertyNames the include property names
   * @param excludePropertyNames the exclude property names
   */
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

  /**
   * @see FindIterable#returnKey(boolean)
   */
  public MgQueryTemplate returnKey(Boolean returnKey) {
    this.returnKey = returnKey;
    return this;
  }

  /**
   * Return a list of documents in the collection according to the given options.
   */
  public List<Map<String, Object>> select() {
    try (MongoCursor<Document> it = query().iterator()) {
      return streamOf(it).map(this::convert).collect(Collectors.toList());
    }
  }

  /**
   * Return a list of objects in the collection according to the given options.
   *
   * @param <T> the type to which the document is to be converted
   * @param clazz the class to which the document is to be converted
   */
  public <T> List<T> selectAs(final Class<T> clazz) {
    return selectAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  /**
   * Return a list of objects in the collection according to the given options.
   *
   * @param <T> the object type
   * @param converter the converter use for document conversion
   */
  public <T> List<T> selectAs(final Function<Object, T> converter) {
    try (MongoCursor<Document> it = query().iterator()) {
      return streamOf(it).map(this::convert).map(converter).collect(Collectors.toList());
    }
  }

  /**
   * Returns the distinct values list of the specified field name and field type.
   *
   * @param <T> the type to cast any distinct items into.
   * @param fieldName the field name
   * @param fieldType the class to cast any distinct items into.
   */
  public <T> List<T> selectDistinctly(String fieldName, Class<T> fieldType) {
    try (MongoCursor<T> it = queryDistinct(fieldName, fieldType).iterator()) {
      return streamOf(it).collect(Collectors.toList());
    }
  }

  /**
   * @see FindIterable#showRecordId(boolean)
   */
  public MgQueryTemplate showRecordId(Boolean showRecordId) {
    this.showRecordId = showRecordId;
    return this;
  }

  /**
   * Returns a single document property value in the collection according to the given options.
   *
   * @param <T> the type to which the property of document is to be converted
   * @param clazz the class to which the property of document is to be converted
   */
  public <T> T single(final Class<T> clazz) {
    List<Map<String, Object>> result = select();
    if (isEmpty(result)) {
      return null;
    } else {
      shouldBeTrue(result.size() == 1 && result.get(0).size() == 1, () -> new QueryRuntimeException(
          "The size %s of query result set must not greater than one and the result record must have only one field.",
          result.size()));
      return toObject(result.get(0).entrySet().iterator().next().getValue(), clazz);
    }
  }

  /**
   * Returns a single document property value list in the collection according to the given options.
   *
   * @param <T> the type to which the property of document is to be converted
   * @param clazz the class to which the property of document is to be converted
   */
  public <T> List<T> singles(final Class<T> clazz) {
    List<Map<String, Object>> result = select();
    if (isEmpty(result)) {
      return null;
    } else {
      return result.stream().map(m -> m.entrySet().iterator().next().getValue())
          .map(r -> toObject(r, clazz)).toList();
    }
  }

  /**
   * @see FindIterable#sort(Bson)
   */
  public MgQueryTemplate sort(Bson sort) {
    this.sort = sort;
    return this;
  }

  /**
   * @see FindIterable#sort(Bson)
   * @see BasicDBObject#parse(String)
   */
  public MgQueryTemplate sort(Map<?, ?> sort) {
    return sort(parse(sort, false));
  }

  /**
   * Return a stream of documents in the collection according to the given options.
   * <p>
   * Note: the result set size is limited by {@link #limit(int)}
   */
  public Stream<Map<String, Object>> stream() {
    final MongoCursor<Document> it = query().iterator();
    return streamOf(it).onClose(it::close).map(this::convert);
  }

  /**
   * Return a stream of documents in the collection according to the given options, the stream may
   * be terminated by the given terminator when met the conditions.
   * <p>
   * Note: <b> The size of the result set is not limited by {@link #limit(int)} but by the given
   * {@code terminator}. </b>
   *
   * @param terminator the terminator use to terminate the stream when meet the conditions.
   */
  public Stream<Map<String, Object>> stream(
      final BiFunction<Long, Map<String, Object>, Boolean> terminator) {
    limit = -1; // NO LIMIT
    if (terminator == null) {
      return stream();
    }
    final BiFunction<Long, Document, Boolean> ut = terminator::apply;
    final MongoCursor<Document> it = new TerminableMongoCursor<>(query().iterator(), ut);
    return streamOf(it).onClose(it::close).map(this::convert);
  }

  /**
   * Converts and returns a collection of documents that satisfy the conditions based on the given
   * options and the given class.
   * <p>
   * Note: the result set size is limited by {@link #limit(int)}
   *
   * @param <T> the type to which the document is to be converted
   * @param clazz the class to which the document is to be converted
   */
  public <T> Stream<T> streamAs(final Class<T> clazz) {
    return streamAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  /**
   * Converts and returns a collection of documents that satisfy the conditions based on the given
   * options and the given converter.
   * <p>
   * Note: the result set size is limited by {@link #limit(int)}
   *
   * @param <T> the type to which the document is to be converted
   * @param converter the converter use for document conversion
   */
  public <T> Stream<T> streamAs(final Function<Object, T> converter) {
    final MongoCursor<Document> it = query().iterator();
    return streamOf(it).onClose(it::close).map(this::convert).map(converter);
  }

  /**
   * Returns the distinct values stream of the specified field name and field type.
   *
   * @param <T> the type to cast any distinct items into.
   * @param fieldName the field name
   * @param fieldType the class to cast any distinct items into.
   */
  public <T> Stream<T> streamDistinctly(String fieldName, Class<T> fieldType) {
    MongoCursor<T> it = queryDistinct(fieldName, fieldType).iterator();
    return streamOf(it).onClose(it::close);
  }

  /**
   * Returns the distinct values stream of the specified field name and field type.
   *
   * @param <T> the type to cast any distinct items into.
   * @param fieldName the field name
   * @param fieldType the class to cast any distinct items into.
   * @param terminator the terminator use to terminate the stream when meet the conditions.
   */
  public <T> Stream<T> streamDistinctly(String fieldName, Class<T> fieldType,
      BiFunction<Long, T, Boolean> terminator) {
    if (terminator == null) {
      return streamDistinctly(fieldName, fieldType);
    }
    MongoCursor<T> it =
        new TerminableMongoCursor<>(queryDistinct(fieldName, fieldType).iterator(), terminator);
    return streamOf(it).onClose(it::close);
  }

  /**
   * @see AggregateIterable#let(Bson)
   * @see FindIterable#let(Bson)
   */
  public MgQueryTemplate variables(Bson variables) {
    this.variables = variables;
    return this;
  }

  /**
   * @see AggregateIterable#let(Bson)
   * @see FindIterable#let(Bson)
   * @see BasicDBObject#parse(String)
   */
  public MgQueryTemplate variables(Map<?, ?> variables) {
    return variables(parse(variables, false));
  }

  protected Map<String, Object> convert(Document doc) {
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
    if (limit >= 0) {
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

  protected <T> DistinctIterable<T> queryDistinct(String fieldName, Class<T> fieldType) {
    MongoCollection<Document> mc = database.getCollection(collection);
    DistinctIterable<T> di = mc.distinct(fieldName, fieldType);
    if (filter != null) {
      di.filter(filter);
    }
    if (collation != null) {
      di.collation(collation);
    }
    if (maxTime != null) {
      di.maxTime(maxTime.toMillis(), TimeUnit.MILLISECONDS);
    }
    if (batchSize != null) {
      di.batchSize(batchSize);
    }
    if (comment != null) {
      di.comment(comment);
    }
    return di;
  }
}

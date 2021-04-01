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

import static org.corant.context.Instances.findNamed;
import static org.corant.context.Instances.resolve;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Conversions.toObject;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Empties.sizeOf;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.corant.modules.query.mongodb.converter.Bsons;
import org.corant.modules.query.shared.QueryObjectMapper;
import org.corant.modules.query.shared.QueryParameter;
import org.corant.modules.query.shared.QueryRuntimeException;
import org.corant.modules.query.shared.QueryService.Forwarding;
import org.corant.modules.query.shared.QueryService.Paging;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 下午4:04:25
 *
 */
public class MgQueryTemplate {

  protected final MongoDatabase database;
  protected String collection;
  protected Bson filter;
  protected Bson sort;
  protected Bson projection;
  protected Bson max;
  protected Bson min;
  protected Bson hint;
  protected List<Bson> aggregate;
  protected int limit = 1;
  protected int offset = 0;
  protected boolean autoSetIdField = true;

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
    return new MgQueryTemplate(database);
  }

  public static MgQueryTemplate database(String mongoClientUri, String queryDatabase) {
    MongoClient mc = new MongoClient(new MongoClientURI(shouldNotBlank(mongoClientUri)));
    return new MgQueryTemplate(shouldNotNull(mc).getDatabase(shouldNotBlank(queryDatabase)));
  }

  public List<Map<?, ?>> aggregate() {
    AggregateIterable<Document> ai = database.getCollection(collection).aggregate(aggregate);
    return streamOf(ai).map(this::convert).collect(Collectors.toList());
  }

  public MgQueryTemplate aggregate(Bson... pipeline) {
    aggregate = shouldNotEmpty(listOf(pipeline));
    return this;
  }

  public MgQueryTemplate aggregate(List<Map<?, ?>> aggregate) {
    this.aggregate = shouldNotEmpty(aggregate).stream().map(a -> {
      try {
        return Bsons.toBson(a);
      } catch (JsonProcessingException e) {
        throw new QueryRuntimeException(e);
      }
    }).collect(Collectors.toList());
    return this;
  }

  public MgQueryTemplate autoSetIdField(boolean autoSetIdField) {
    this.autoSetIdField = autoSetIdField;
    return this;
  }

  public MgQueryTemplate collection(String collection) {
    this.collection = shouldNotNull(collection);
    return this;
  }

  public long count() {
    MongoCollection<Document> mc = database.getCollection(collection);
    if (isNotEmpty(filter)) {
      return mc.countDocuments(filter);
    }
    return mc.countDocuments();
  }

  public MgQueryTemplate filter(Bson filter) {
    this.filter = shouldNotNull(filter);
    return this;
  }

  public MgQueryTemplate filter(Map<?, ?> filter) {
    return filter(parse(defaultObject(filter, HashMap::new)));
  }

  public Forwarding<Map<?, ?>> forward() {
    Forwarding<Map<?, ?>> result = Forwarding.inst();
    FindIterable<Document> fi = query().skip(offset).limit(limit + 1);
    List<Map<?, ?>> list = streamOf(fi).map(this::convert).collect(Collectors.toList());
    if (sizeOf(list) > limit) {
      list.remove(limit);
      result.withHasNext(true);
    }
    return result.withResults(list);
  }

  public <T> Forwarding<T> forwardAs(final Class<T> clazz) {
    return forwardAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> Forwarding<T> forwardAs(final Function<Object, T> converter) {
    Forwarding<T> result = Forwarding.inst();
    FindIterable<Document> fi = query().skip(offset).limit(limit + 1);
    List<Map<?, ?>> list = streamOf(fi).map(this::convert).collect(Collectors.toList());
    if (sizeOf(list) > limit) {
      list.remove(limit);
      result.withHasNext(true);
    }
    return result.withResults(list.stream().map(converter).collect(Collectors.toList()));
  }

  public Map<?, ?> get() {
    Iterator<Document> it = query().limit(1).iterator();
    return it.hasNext() ? convert(it.next()) : null;
  }

  public <T> T getAs(final Class<T> clazz) {
    return getAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> T getAs(final Function<Object, T> converter) {
    Map<?, ?> document = get();
    return document != null ? converter.apply(document) : null;
  }

  public MgQueryTemplate hint(Bson hint) {
    this.hint = shouldNotNull(hint);
    return this;
  }

  public MgQueryTemplate hint(Map<?, ?> hint) {
    return hint(parse(defaultObject(hint, HashMap::new)));
  }

  /**
   * The expected number of query result set or the expected size of the result set of each
   * iteration of the streaming query
   *
   * @see QueryParameter#getLimit()
   *
   * @param limit
   * @return limit
   */
  public MgQueryTemplate limit(int limit) {
    this.limit = Objects.max(limit, 1);
    return this;
  }

  public MgQueryTemplate max(Bson max) {
    this.max = shouldNotNull(max);
    return this;
  }

  public MgQueryTemplate max(Map<?, ?> max) {
    return max(parse(defaultObject(max, HashMap::new)));
  }

  public MgQueryTemplate min(Bson min) {
    this.min = shouldNotNull(min);
    return this;
  }

  public MgQueryTemplate min(Map<?, ?> min) {
    return min(parse(defaultObject(min, HashMap::new)));
  }

  public MgQueryTemplate offset(int offset) {
    this.offset = Objects.max(offset, 0);
    return this;
  }

  public Paging<Map<?, ?>> page() {
    Paging<Map<?, ?>> result = Paging.of(offset, limit);
    FindIterable<Document> fi = query();
    List<Map<?, ?>> list = streamOf(fi).map(this::convert).collect(Collectors.toList());
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

  public <T> Paging<T> pageAs(final Class<T> clazz) {
    return pageAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> Paging<T> pageAs(final Function<Object, T> converter) {
    Paging<T> result = Paging.of(offset, limit);
    FindIterable<Document> fi = query();
    List<Map<?, ?>> list = streamOf(fi).map(this::convert).collect(Collectors.toList());
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

  public MgQueryTemplate projection(boolean include, String... propertyNames) {
    if (include) {
      return projection(propertyNames, Strings.EMPTY_ARRAY);
    } else {
      return projection(Strings.EMPTY_ARRAY, propertyNames);
    }
  }

  public MgQueryTemplate projection(Bson projection) {
    this.projection = shouldNotNull(projection);
    return this;
  }

  public MgQueryTemplate projection(Map<?, ?> projection) {
    return projection(parse(defaultObject(projection, HashMap::new)));
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

  public List<Map<?, ?>> select() {
    return streamOf(query()).map(this::convert).collect(Collectors.toList());
  }

  public <T> List<T> selectAs(final Class<T> clazz) {
    return selectAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> List<T> selectAs(final Function<Object, T> converter) {
    return streamOf(query()).map(this::convert).map(converter).collect(Collectors.toList());
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

  public MgQueryTemplate sort(Bson sort) {
    this.sort = shouldNotNull(sort);
    return this;
  }

  public MgQueryTemplate sort(Map<?, ?> sort) {
    return sort(parse(defaultObject(sort, HashMap::new)));
  }

  @SuppressWarnings("rawtypes")
  public Stream<Map<?, ?>> stream() {
    limit = 0;// NO LIMIT
    return streamOf(query()).map(this::convert).map(r -> (Map) r);
  }

  public <T> Stream<T> streamAs(final Class<T> clazz) {
    return streamAs(r -> resolve(QueryObjectMapper.class).toObject(r, clazz));
  }

  public <T> Stream<T> streamAs(final Function<Object, T> converter) {
    limit = 0;// NO LIMIT
    return streamOf(query()).map(this::convert).map(converter);
  }

  protected Map<?, ?> convert(Document doc) {
    if (doc != null && autoSetIdField && !doc.containsKey("id") && doc.containsKey("_id")) {
      doc.put("id", doc.get("_id"));
    }
    return doc;
  }

  protected Bson parse(Object object) {
    try {
      return Bsons.toBson(object);
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  protected FindIterable<Document> query() {
    MongoCollection<Document> mc = database.getCollection(collection);
    FindIterable<Document> fi = mc.find();
    if (isNotEmpty(filter)) {
      fi.filter(filter);
    }
    if (isNotEmpty(min)) {
      fi.min(min);
    }
    if (isNotEmpty(max)) {
      fi.max(max);
    }
    if (isNotEmpty(hint)) {
      fi.hint(hint);
    }
    if (isNotEmpty(sort)) {
      fi.sort(sort);
    }
    if (isNotEmpty(projection)) {
      fi.projection(projection);
    }
    if (limit > 0) {
      fi.limit(limit);
    }
    if (offset > 0) {
      fi.skip(offset);
    }
    return fi;
  }
}

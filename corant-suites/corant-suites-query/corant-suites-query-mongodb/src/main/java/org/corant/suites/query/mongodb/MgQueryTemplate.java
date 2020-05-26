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
package org.corant.suites.query.mongodb;

import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.CollectionUtils.listOf;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.shared.util.StringUtils.isNotBlank;
import static org.corant.suites.cdi.Instances.findNamed;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.corant.shared.util.ObjectUtils;
import org.corant.suites.query.shared.QueryObjectMapper;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.QueryService.Forwarding;
import org.corant.suites.query.shared.QueryService.Paging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * corant-suites-query-mongodb
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

  @SuppressWarnings("resource")
  private MgQueryTemplate(String database) {
    shouldNotBlank(database, () -> new QueryRuntimeException("Data base can't not empty"));
    if (database.startsWith("mongodb+srv://") || database.startsWith("mongodb://")) {
      String db = database.substring(database.lastIndexOf('/'));
      this.database = new MongoClient(new MongoClientURI(database)).getDatabase(db);
    } else {
      this.database = findNamed(MongoDatabase.class, shouldNotNull(database))
          .orElseThrow(QueryRuntimeException::new);
    }
  }

  public static MgQueryTemplate database(String database) {
    return new MgQueryTemplate(database);
  }

  public List<Map<?, ?>> aggregate() {
    AggregateIterable<Document> ai = database.getCollection(collection).aggregate(aggregate);
    return streamOf(ai).map(this::convert).collect(Collectors.toList());
  }

  public MgQueryTemplate aggregate(Bson... pipeline) {
    aggregate = shouldNotEmpty(listOf(pipeline));
    return this;
  }

  @SuppressWarnings("unchecked")
  public MgQueryTemplate aggregate(List<Map<?, ?>> aggregate) {
    try {
      this.aggregate = (List<Bson>) DefaultMgNamedQuerier.resolve(shouldNotEmpty(aggregate));
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
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
    return filter((Bson) parse(defaultObject(filter, HashMap::new)));
  }

  public Forwarding<Map<?, ?>> forward() {
    Forwarding<Map<?, ?>> result = Forwarding.inst();
    FindIterable<Document> fi = query().skip(offset).limit(limit + 1);
    List<Map<?, ?>> list = streamOf(fi).map(this::convert).collect(Collectors.toList());
    int size = list.size();
    if (size > 0 && size > limit) {
      list.remove(limit);
      result.withHasNext(true);
    }
    return result.withResults(list);
  }

  public <T> Forwarding<T> forward(Class<T> clazz) {
    Forwarding<T> result = Forwarding.inst();
    FindIterable<Document> fi = query().skip(offset).limit(limit + 1);
    List<Map<?, ?>> list = streamOf(fi).map(this::convert).collect(Collectors.toList());
    int size = list.size();
    if (size > 0 && size > limit) {
      list.remove(limit);
      result.withHasNext(true);
    }
    return result.withResults(list.stream().map(r -> QueryObjectMapper.OM.convertValue(r, clazz))
        .collect(Collectors.toList()));
  }

  public Map<?, ?> get() {
    Iterator<Document> it = query().limit(1).iterator();
    return it.hasNext() ? convert(it.next()) : null;
  }

  public <T> T get(Class<T> clazz) {
    Map<?, ?> document = get();
    return document != null ? QueryObjectMapper.OM.convertValue(document, clazz) : null;
  }

  public MgQueryTemplate hint(Bson hint) {
    this.hint = shouldNotNull(hint);
    return this;
  }

  public MgQueryTemplate hint(Map<?, ?> hint) {
    return hint((Bson) parse(defaultObject(hint, HashMap::new)));
  }

  public MgQueryTemplate limit(int limit) {
    this.limit = ObjectUtils.max(limit, 1);
    return this;
  }

  public MgQueryTemplate max(Bson max) {
    this.max = shouldNotNull(max);
    return this;
  }

  public MgQueryTemplate max(Map<?, ?> max) {
    return max((Bson) parse(defaultObject(max, HashMap::new)));
  }

  public MgQueryTemplate min(Bson min) {
    this.min = shouldNotNull(min);
    return this;
  }

  public MgQueryTemplate min(Map<?, ?> min) {
    return min((Bson) parse(defaultObject(min, HashMap::new)));
  }

  public MgQueryTemplate offset(int offset) {
    this.offset = ObjectUtils.max(offset, 0);
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

  public <T> Paging<T> page(Class<T> clazz) {
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
    return result.withResults(list.stream().map(r -> QueryObjectMapper.OM.convertValue(r, clazz))
        .collect(Collectors.toList()));
  }

  public MgQueryTemplate projection(boolean include, String... propertyNames) {
    if (include) {
      return projection(propertyNames, new String[0]);
    } else {
      return projection(new String[0], propertyNames);
    }
  }

  public MgQueryTemplate projection(Bson projection) {
    this.projection = shouldNotNull(projection);
    return this;
  }

  public MgQueryTemplate projection(Map<?, ?> projection) {
    return projection((Bson) parse(defaultObject(projection, HashMap::new)));
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

  public <T> List<T> select(Class<T> clazz) {
    return streamOf(query()).map(this::convert)
        .map(r -> QueryObjectMapper.OM.convertValue(r, clazz)).collect(Collectors.toList());
  }

  public MgQueryTemplate sort(Bson sort) {
    this.sort = shouldNotNull(sort);
    return this;
  }

  public MgQueryTemplate sort(Map<?, ?> sort) {
    return sort((Bson) parse(defaultObject(sort, HashMap::new)));
  }

  @SuppressWarnings("rawtypes")
  public Stream<Map<?, ?>> stream() {
    limit = 0;// NO LIMIT
    return streamOf(query()).map(this::convert).map(r -> (Map) r);
  }

  public <T> Stream<T> stream(Class<T> clazz) {
    limit = 0;// NO LIMIT
    return streamOf(query()).map(this::convert)
        .map(r -> QueryObjectMapper.OM.convertValue(r, clazz));
  }

  protected Map<?, ?> convert(Document doc) {
    if (autoSetIdField && !doc.containsKey("id") && doc.containsKey("_id")) {
      doc.put("id", doc.get("_id"));
    }
    return doc;
  }

  protected Object parse(Object object) {
    try {
      return DefaultMgNamedQuerier.resolve(object);
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

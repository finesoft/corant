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

import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StreamUtils.streamOf;
import static org.corant.suites.cdi.Instances.findNamed;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.Document;
import org.corant.shared.util.ObjectUtils;
import org.corant.suites.query.shared.QueryObjectMapper;
import org.corant.suites.query.shared.QueryRuntimeException;
import org.corant.suites.query.shared.QueryService.Forwarding;
import org.corant.suites.query.shared.QueryService.Paging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.BasicDBObject;
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

  private final MongoDatabase database;
  private String collection;
  private BasicDBObject filter;
  private BasicDBObject sort;
  private BasicDBObject projection;
  private BasicDBObject max;
  private BasicDBObject min;
  private BasicDBObject hint;
  private List<BasicDBObject> aggregate;
  private int limit = -1;
  private int offset = 0;

  private MgQueryTemplate(String database) {
    this.database = findNamed(MongoDatabase.class, shouldNotNull(database))
        .orElseThrow(QueryRuntimeException::new);
  }

  public static MgQueryTemplate database(String database) {
    return new MgQueryTemplate(database);
  }

  public List<Map<?, ?>> aggregate() {
    AggregateIterable<Document> ai = database.getCollection(collection).aggregate(aggregate);
    return streamOf(ai).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  public MgQueryTemplate aggregate(List<Map<?, ?>> aggregate) {
    try {
      this.aggregate = (List<BasicDBObject>) DefaultMgNamedQuerier
          .resolve(defaultObject(shouldNotEmpty(aggregate), ArrayList::new));
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
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

  public MgQueryTemplate filter(Map<?, ?> filter) {
    this.filter = (BasicDBObject) parse(defaultObject(filter, HashMap::new));
    return this;
  }

  public Forwarding<Map<?, ?>> forward() {
    Forwarding<Map<?, ?>> result = Forwarding.inst();
    FindIterable<Document> fi = query().skip(offset).limit(limit + 1);
    List<Map<?, ?>> list = streamOf(fi).collect(Collectors.toList());
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
    List<Map<String, Object>> list = streamOf(fi).collect(Collectors.toList());
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
    return it.hasNext() ? it.next() : null;
  }

  public MgQueryTemplate hint(Map<?, ?> hint) {
    this.hint = (BasicDBObject) parse(defaultObject(hint, HashMap::new));
    return this;
  }

  public MgQueryTemplate limit(int limit) {
    this.limit = ObjectUtils.max(limit, 1);
    return this;
  }

  public MgQueryTemplate max(Map<?, ?> max) {
    this.max = (BasicDBObject) parse(defaultObject(max, HashMap::new));
    return this;
  }

  public MgQueryTemplate min(Map<?, ?> min) {
    this.min = (BasicDBObject) parse(defaultObject(min, HashMap::new));
    return this;
  }

  public MgQueryTemplate offset(int offset) {
    this.offset = ObjectUtils.max(offset, 0);
    return this;
  }

  public Paging<Map<?, ?>> page() {
    Paging<Map<?, ?>> result = Paging.of(offset, limit);
    FindIterable<Document> fi = query();
    List<Map<?, ?>> list = streamOf(fi).collect(Collectors.toList());
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
    List<Map<?, ?>> list = streamOf(fi).collect(Collectors.toList());
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

  public MgQueryTemplate projection(Map<?, ?> projection) {
    this.projection = (BasicDBObject) parse(defaultObject(projection, HashMap::new));
    return this;
  }

  public List<Map<?, ?>> select() {
    return streamOf(query()).collect(Collectors.toList());
  }

  public <T> List<T> select(Class<T> clazz) {
    return streamOf(query()).map(r -> QueryObjectMapper.OM.convertValue(r, clazz))
        .collect(Collectors.toList());
  }

  public MgQueryTemplate sort(Map<?, ?> sort) {
    this.sort = (BasicDBObject) parse(defaultObject(sort, HashMap::new));
    return this;
  }

  @SuppressWarnings("rawtypes")
  public Stream<Map<?, ?>> stream() {
    limit = -1;// NO LIMIT
    return streamOf(query()).map(r -> (Map) r);
  }

  public <T> Stream<T> stream(Class<T> clazz) {
    limit = -1;// NO LIMIT
    return streamOf(query()).map(r -> QueryObjectMapper.OM.convertValue(r, clazz));
  }

  Object parse(Object object) {
    try {
      return DefaultMgNamedQuerier.resolve(object);
    } catch (JsonProcessingException e) {
      throw new QueryRuntimeException(e);
    }
  }

  FindIterable<Document> query() {
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
      fi.min(projection);
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

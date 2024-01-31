/*
 * Copyright (c) 2013-2024, Bingo.Chen (finesoft@gmail.com).
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
package org.corant.modules.mongodb;

import static java.util.Collections.singletonMap;
import static org.corant.shared.ubiquity.Throwing.uncheckedFunction;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
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
import org.corant.modules.bson.Bsons;
import org.corant.modules.json.Jsons;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.ubiquity.TypeLiteral;
import org.corant.shared.util.Objects;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.CursorType;
import com.mongodb.ExplainVerbosity;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.annotations.NotThreadSafe;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.DropCollectionOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

/**
 * corant-modules-mongodb
 *
 * @author bingo 18:29:22
 * @since 2.0
 */
@NotThreadSafe
public class MongoTemplate {

  protected final MongoDatabase database;
  protected final WriteConcern writeConcern;
  protected final ReadPreference readPreference;
  protected final ReadConcern readConcern;
  protected final ClientSession session;

  public MongoTemplate(MongoClient mongoClient, String databaseName) {
    this(mongoClient.getDatabase(databaseName));
  }

  public MongoTemplate(MongoDatabase database) {
    this(database, null, null, null, null);
  }

  public MongoTemplate(MongoDatabase database, ClientSession session) {
    this(database, null, session);
  }

  public MongoTemplate(MongoDatabase database, ReadPreference readPreference,
      ReadConcern readConcern) {
    this(database, readPreference, readConcern, null);
  }

  public MongoTemplate(MongoDatabase database, ReadPreference readPreference,
      ReadConcern readConcern, ClientSession session) {
    this(database, null, readPreference, readConcern, session);
  }

  public MongoTemplate(MongoDatabase database, WriteConcern writeConcern) {
    this(database, writeConcern, null);
  }

  public MongoTemplate(MongoDatabase database, WriteConcern writeConcern, ClientSession session) {
    this(database, writeConcern, null, null, session);
  }

  public MongoTemplate(MongoDatabase database, WriteConcern writeConcern,
      ReadPreference readPreference, ReadConcern readConcern, ClientSession session) {
    this.database = database;
    this.writeConcern = writeConcern;
    this.readPreference = readPreference;
    this.readConcern = readConcern;
    this.session = session;
  }

  protected static Bson parse(Object object, boolean extended) {
    try {
      return MongoExtendedJsons.toBson(object, extended);
    } catch (JsonProcessingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  public MongoAggregator aggregator() {
    return new MongoAggregator(this);
  }

  /**
   * Check to see if a collection with a given name exists.
   *
   * @param collectionName name of the collection. Must not be null
   * @return true if a collection with the given name is found, false otherwise.
   */
  public boolean collectionExists(String collectionName) {
    if (isNotBlank(collectionName)) {
      for (String name : database.listCollectionNames()) {
        if (collectionName.equals(name)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Create a collection with the provided name and options.
   *
   * @param collectionName name of the collection. Must not be {@literal null} nor empty.
   * @param options options to use when creating the collection.
   * @return the created collection.
   */
  public MongoCollection<Document> createCollection(String collectionName,
      CreateCollectionOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    if (options != null) {
      database.createCollection(collectionName, options);
    } else {
      database.createCollection(collectionName);
    }
    return database.getCollection(collectionName);
  }

  /**
   * Removes all documents from the given collection that match the given query filter. If no
   * documents match, the collection is not modified.
   *
   * @param collectionName the collection name
   * @param filter the query filter to apply the delete operation
   * @param options the options to apply to the delete operation
   * @return the result of the remove operation
   */
  public DeleteResult delete(String collectionName, Bson filter, DeleteOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(filter, "Filter must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    if (options != null) {
      if (session != null) {
        return collection.deleteMany(session, filter, options);
      } else {
        return collection.deleteMany(filter, options);
      }
    } else if (session != null) {
      return collection.deleteMany(session, filter);
    } else {
      return collection.deleteMany(filter);
    }
  }

  /**
   * Removes all documents from the given collection that match the given query filter. If no
   * documents match, the collection is not modified.
   *
   * @param collectionName the collection name
   * @param filter the query filter to apply the delete operation, supports extended JSON
   * @return the result of the remove operation
   */
  public DeleteResult delete(String collectionName, Map<?, ?> filter) {
    return delete(collectionName, filter, null);
  }

  /**
   * Removes all documents from the given collection that match the given query filter. If no
   * documents match, the collection is not modified.
   *
   * @param collectionName the collection name
   * @param filter the query filter to apply the delete operation, supports extended JSON
   * @param options the options to apply to the delete operation
   * @return the result of the remove operation
   */
  public DeleteResult delete(String collectionName, Map<?, ?> filter, DeleteOptions options) {
    try {
      return delete(collectionName, MongoExtendedJsons.toBson(filter, true), options);
    } catch (JsonProcessingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Drop the collection with the given name.
   *
   * @param collectionName name of the collection to drop/delete.
   * @param options various options for dropping the collection
   */
  public void dropCollection(String collectionName, DropCollectionOptions options) {
    shouldNotBlank(collectionName, "CollectionName must not be null");
    MongoCollection<?> collection = database.getCollection(collectionName);
    if (options != null) {
      collection.drop(options);
    } else {
      collection.drop();
    }
  }

  /**
   * Execute a MongoDB command
   *
   * @param command a MongoDB command.
   * @return a result object returned by the action.
   */
  public Document executeCommand(Document command) {
    return executeCommand(command, null);
  }

  /**
   * Execute a MongoDB command
   *
   * @param command a MongoDB command.
   * @param readPreference read preferences to use, can be {@literal null}.
   * @return a result object returned by the action.
   */
  public Document executeCommand(Document command, ReadPreference readPreference) {
    shouldNotNull(command, "Command must not be null");
    return readPreference != null ? database.runCommand(command, readPreference, Document.class)
        : database.runCommand(command, Document.class);
  }

  /**
   * Execute a MongoDB command expressed as a JSON string. Parsing is delegated to
   * {@link Document#parse(String)} to obtain the {@link Document} holding the actual command.
   *
   * @param jsonCommand a MongoDB command expressed as a JSON string
   * @return a result object returned by the action.
   */
  public Document executeCommand(String jsonCommand) {
    shouldNotBlank(jsonCommand, "JsonCommand must not be null nor empty");
    return executeCommand(Document.parse(jsonCommand));
  }

  public MongoDatabase getDatabase() {
    return database;
  }

  public ReadConcern getReadConcern() {
    return readConcern;
  }

  public ReadPreference getReadPreference() {
    return readPreference;
  }

  public ClientSession getSession() {
    return session;
  }

  public WriteConcern getWriteConcern() {
    return writeConcern;
  }

  /**
   * Insert the object into the specified collection. If the given object is not {@link Document}
   * instance, it will be converted to Map and then constructed into a {@link Document}, if the
   * given object has a property named 'id', the value of the property will be used as the primary
   * key(named '_id') of the {@link Document}.
   *
   * @param collectionName name of the collection to store the object in. Must not be {@literal
   *     null}
   * @param object the object to store in the collection. Must not be {@literal null}.
   * @return the insert result
   */
  public InsertOneResult insert(String collectionName, Object object) {
    return insert(collectionName, object, null, null);
  }

  /**
   * Insert the object into the specified collection. If the given object is not {@link Document}
   * instance, it will be converted to Map and then constructed into a {@link Document}, if the
   * given object has a property named 'id', the value of the property will be used as the primary
   * key(named '_id') of the {@link Document}.
   *
   * @param collectionName name of the collection to store the object in. Must not be {@literal
   *     null}
   * @param object the object to store in the collection. Must not be {@literal null}.
   * @param options the options to apply to the operation
   * @return the insert result
   */
  public InsertOneResult insert(String collectionName, Object object, InsertOneOptions options) {
    return insert(collectionName, object, options, null);
  }

  /**
   * Insert the object into the specified collection
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be {@literal
   *     null}.
   * @param object the object to store in the collection. Must not be {@literal null}.
   * @param options the options to apply to the operation
   * @param handler the handler which use to convert the given object to {@link Document} and
   *        process the primary key of the {@link Document}, can be null. If the given handler is
   *        null, if the given object is not {@link Document} instance, it will be converted to Map
   *        and then constructed into a {@link Document}, if the given object has a property named
   *        'id', the value of the property will be used as the primary key(named '_id') of the
   *        {@link Document}.
   * @return the insert result
   */
  public <T> InsertOneResult insert(String collectionName, T object, InsertOneOptions options,
      Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(object, "Object must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    Document doc;
    if (handler == null) {
      doc = handle(object);
    } else {
      doc = handler.apply(object);
    }
    if (options != null) {
      if (session != null) {
        return collection.insertOne(session, doc, options);
      } else {
        return collection.insertOne(doc, options);
      }
    } else if (session != null) {
      return collection.insertOne(session, doc);
    } else {
      return collection.insertOne(doc);
    }
  }

  /**
   * Insert a list of objects into a collection in a single batch write to the database. If the
   * element of the given list is not {@link Document} instance, it will be converted to Map and
   * then constructed into a {@link Document}, if the object has a property named 'id', the value of
   * the property will be used as the primary key(named '_id') of the {@link Document}.
   *
   * @param <T> the object type
   * @param collectionName collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param objects the list of objects to store in the collection. Must not be {@literal null}.
   * @return the insert result
   */
  public <T> InsertManyResult insertMany(String collectionName, List<T> objects) {
    return insertMany(collectionName, objects, null, null);
  }

  /**
   * Insert a list of objects into a collection in a single batch write to the database. If the
   * element of the given list is not {@link Document} instance, it will be converted to Map and
   * then constructed into a {@link Document}, if the object has a property named 'id', the value of
   * the property will be used as the primary key(named '_id') of the {@link Document}.
   *
   * @param <T> the object type
   * @param collectionName collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param objects the list of objects to store in the collection. Must not be {@literal null}.
   * @param options the options to apply to the operation
   * @return the insert result
   */
  public <T> InsertManyResult insertMany(String collectionName, List<T> objects,
      InsertManyOptions options) {
    return insertMany(collectionName, objects, options, null);
  }

  /**
   * Insert a list of objects into a collection in a single batch write to the database.
   *
   * @param <T> the object type
   * @param collectionName collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param objects the list of objects to store in the collection. Must not be {@literal null}.
   * @param options the options to apply to the operation
   * @param handler the handler which use to convert the element of the given list to
   *        {@link Document} and process the primary key of the {@link Document}, can be null. If
   *        the given handler is null, if the element of the given list is not {@link Document}
   *        instance, it will be converted to Map and then constructed into a {@link Document}, if
   *        the object has a property named 'id', the value of the property will be used as the
   *        primary key(named '_id') of the {@link Document}.
   * @return the insert result
   */
  public <T> InsertManyResult insertMany(String collectionName, List<T> objects,
      InsertManyOptions options, Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(objects, "Object must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    Function<T, Document> useHandler = defaultObject(handler, this::handle);
    List<Document> docs = objects.stream().map(useHandler).toList();
    if (options != null) {
      if (session != null) {
        return collection.insertMany(session, docs, options);
      } else {
        return collection.insertMany(docs, options);
      }
    } else if (session != null) {
      return collection.insertMany(session, docs);
    } else {
      return collection.insertMany(docs);
    }
  }

  /**
   * Returns a new {@link MongoQuery} instance.
   */
  public MongoQuery query() {
    return new MongoQuery(this);
  }

  /**
   * Save or replace an object in the given collection according to the specified arguments. If the
   * given object is not {@link Document} instance, it will be converted to Map and then constructed
   * into a {@link Document}, if the given object has a property named 'id', the value of the
   * property will be used as the primary key(named '_id') of the {@link Document}.
   *
   * @param collectionName the collection name
   * @param object the object to be saved or replaced
   * @return the save or replace result
   */
  public UpdateResult save(String collectionName, Object object) {
    return save(collectionName, object, null, null);
  }

  /**
   * Save or replace an object in the given collection according to the specified arguments. If the
   * given object is not {@link Document} instance, it will be converted to Map and then constructed
   * into a {@link Document}, if the given object has a property named 'id', the value of the
   * property will be used as the primary key(named '_id') of the {@link Document}.
   *
   * @param collectionName the collection name
   * @param object the object to be saved or replaced
   * @param options the options to apply to the save operation
   * @return the save or replace result
   */
  public UpdateResult save(String collectionName, Object object, ReplaceOptions options) {
    return save(collectionName, object, options, null);
  }

  /**
   * Save or replace an object in the given collection according to the specified arguments.
   *
   * @param <T> the object type
   * @param collectionName the collection name
   * @param object the object to be saved or replaced
   * @param options the options to apply to the save operation
   * @param handler the handler which use to convert the given object to {@link Document} and
   *        process the primary key of the {@link Document}, can be null. If the given handler is
   *        null, if the given object is not {@link Document} instance, it will be converted to Map
   *        and then constructed into a {@link Document}, if the given object has a property named
   *        'id', the value of the property will be used as the primary key(named '_id') of the
   *        {@link Document}.
   * @return the save or replace result
   */
  public <T> UpdateResult save(String collectionName, T object, ReplaceOptions options,
      Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(object, "Object must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    Document doc;
    if (handler == null) {
      doc = handle(object);
    } else {
      doc = handler.apply(object);
    }
    ReplaceOptions ro = defaultObject(options, ReplaceOptions::new).upsert(true);
    if (session != null) {
      return collection.replaceOne(session,
          new Document(singletonMap("_id", Bsons.toSimpleBsonValue(doc.get("_id")))), doc, ro);
    } else {
      return collection.replaceOne(
          new Document(singletonMap("_id", Bsons.toSimpleBsonValue(doc.get("_id")))), doc, ro);
    }
  }

  /**
   * Save or replace a list of objects in the given collection according to the specified arguments.
   * If the element of the given list is not {@link Document} instance, it will be converted to Map
   * and then constructed into a {@link Document}, if the object has a property named 'id', the
   * value of the property will be used as the primary key(named '_id') of the {@link Document}.
   *
   * @param collectionName the collection name
   * @param objects a list of object to be saved or replaced
   * @return a list of save or replace result
   */
  public List<UpdateResult> saveMany(String collectionName, List<?> objects) {
    return saveMany(collectionName, objects, null, null);
  }

  /**
   * Save or replace a list of objects in the given collection according to the specified arguments.
   * If the element of the given list is not {@link Document} instance, it will be converted to Map
   * and then constructed into a {@link Document}, if the object has a property named 'id', the
   * value of the property will be used as the primary key(named '_id') of the {@link Document}.
   *
   * @param collectionName the collection name
   * @param objects a list of object to be saved or replaced
   * @param options the options to apply to the save operation
   * @return a list of save or replace result
   */
  public List<UpdateResult> saveMany(String collectionName, List<?> objects,
      ReplaceOptions options) {
    return saveMany(collectionName, objects, options, null);
  }

  /**
   * Save or replace a list of objects in the given collection according to the specified arguments.
   *
   * @param <T> the object type
   * @param collectionName the collection name
   * @param objects a list of object to be saved or replaced
   * @param options the options to apply to the save operation
   * @param handler the handler which use to convert the given object to {@link Document} and
   *        process the primary key of the {@link Document}, can be null. If the given handler is
   *        null, if the given object is not {@link Document} instance, it will be converted to Map
   *        and then constructed into a {@link Document}, if the given object has a property named
   *        'id', the value of the property will be used as the primary key(named '_id') of the
   *        {@link Document}.
   * @return a list of save or replace result
   */
  public <T> List<UpdateResult> saveMany(String collectionName, List<T> objects,
      ReplaceOptions options, Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(objects, "Objects must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    Function<T, Document> useHandler = defaultObject(handler, this::handle);
    ReplaceOptions ro = defaultObject(options, ReplaceOptions::new).upsert(true);
    if (session != null) {
      return objects.stream().map(useHandler)
          .map(doc -> collection.replaceOne(session,
              new Document(singletonMap("_id", Bsons.toSimpleBsonValue(doc.get("_id")))), doc, ro))
          .toList();
    } else {
      return objects.stream().map(useHandler)
          .map(doc -> collection.replaceOne(
              new Document(singletonMap("_id", Bsons.toSimpleBsonValue(doc.get("_id")))), doc, ro))
          .toList();
    }
  }

  /**
   * Save or replace an objects stream in the given collection according to the specified arguments.
   * If the element of the given stream is not {@link Document} instance, it will be converted to
   * Map and then constructed into a {@link Document}, if the object has a property named 'id', the
   * value of the property will be used as the primary key(named '_id') of the {@link Document}.
   *
   * @param <T> the object type
   * @param collectionName the collection name
   * @param stream the objects stream to be saved
   * @return the save result stream
   */
  public <T> Stream<UpdateResult> streamSave(String collectionName, Stream<T> stream) {
    return streamSave(collectionName, stream, null, null);
  }

  /**
   * Save or replace an objects stream in the given collection according to the specified arguments.
   * If the element of the given stream is not {@link Document} instance, it will be converted to
   * Map and then constructed into a {@link Document}, if the object has a property named 'id', the
   * value of the property will be used as the primary key(named '_id') of the {@link Document}.
   *
   * @param <T> the object type
   * @param collectionName the collection name
   * @param stream the objects stream to be saved
   * @param options the options to apply to the save operation
   * @return the save result stream
   */
  public <T> Stream<UpdateResult> streamSave(String collectionName, Stream<T> stream,
      ReplaceOptions options) {
    return streamSave(collectionName, stream, options, null);
  }

  /**
   * Save or replace an objects stream in the given collection according to the specified arguments.
   *
   * @param <T> the object type
   * @param collectionName the collection name
   * @param stream the objects stream to be saved
   * @param options the options to apply to the save operation
   * @param handler the handler which use to convert the given object to {@link Document} and
   *        process the primary key of the {@link Document}, can be null. If the given handler is
   *        null, if the given object is not {@link Document} instance, it will be converted to Map
   *        and then constructed into a {@link Document}, if the given object has a property named
   *        'id', the value of the property will be used as the primary key(named '_id') of the
   *        {@link Document}.
   * @return the save result stream
   */
  public <T> Stream<UpdateResult> streamSave(String collectionName, Stream<T> stream,
      ReplaceOptions options, Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(stream, "Objects must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    Function<T, Document> useHandler = defaultObject(handler, this::handle);
    ReplaceOptions ro = defaultObject(options, ReplaceOptions::new).upsert(true);
    if (session != null) {
      return stream.map(useHandler).map(doc -> collection.replaceOne(session,
          new Document(singletonMap("_id", Bsons.toSimpleBsonValue(doc.get("_id")))), doc, ro));
    } else {
      return stream.map(useHandler).map(doc -> collection.replaceOne(
          new Document(singletonMap("_id", Bsons.toSimpleBsonValue(doc.get("_id")))), doc, ro));
    }
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName the collection name
   * @param filter a document describing the query filter, which may not be null.
   * @param update a document describing the update, which may not be null. The update to apply must
   *        include only update operators.
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Bson filter, Bson update,
      UpdateOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(filter, "Filter must not be null");
    shouldNotNull(update, "Update must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    if (options != null) {
      if (session != null) {
        return collection.updateMany(session, filter, update, options);
      } else {
        return collection.updateMany(filter, update, options);
      }
    } else if (session != null) {
      return collection.updateMany(session, filter, update);
    } else {
      return collection.updateMany(filter, update);
    }
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName the collection name
   * @param filter a document describing the query filter, which may not be null.
   * @param pipeline a pipeline describing the update, which may not be null.
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Bson filter, List<Bson> pipeline,
      UpdateOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(filter, "Filter must not be null");
    shouldNotNull(pipeline, "Update must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    if (options != null) {
      if (session != null) {
        return collection.updateMany(session, filter, pipeline, options);
      } else {
        return collection.updateMany(filter, pipeline, options);
      }
    } else if (session != null) {
      return collection.updateMany(session, filter, pipeline);
    } else {
      return collection.updateMany(filter, pipeline);
    }
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName the collection name
   * @param filter a document describing the query filter, which may not be null, supports extended
   *        JSON
   * @param pipeline a pipeline describing the update, which may not be null, supports extended JSON
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Map<?, ?> filter, List<Map<?, ?>> pipeline) {
    return update(collectionName, filter, pipeline, null);
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName the collection name
   * @param filter a document describing the query filter, which may not be null, supports extended
   *        JSON
   * @param pipeline a pipeline describing the update, which may not be null, supports extended JSON
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Map<?, ?> filter, List<Map<?, ?>> pipeline,
      UpdateOptions options) {
    try {
      return update(collectionName, MongoExtendedJsons.toBson(filter, true), pipeline.stream()
          .map(uncheckedFunction(u -> MongoExtendedJsons.toBson(u, true))).toList(), options);
    } catch (JsonProcessingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName the collection name
   * @param filter a document describing the query filter, which may not be null, support extended
   *        JSON
   * @param update a document describing the update, which may not be null. The update to apply must
   *        include only update operators, support extended JSON
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Map<?, ?> filter, Map<?, ?> update) {
    return update(collectionName, filter, update, null);
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName the collection name
   * @param filter a document describing the query filter, which may not be null, support extended
   *        JSON
   * @param update a document describing the update, which may not be null. The update to apply must
   *        include only update operators, support extended JSON
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Map<?, ?> filter, Map<?, ?> update,
      UpdateOptions options) {
    try {
      return update(collectionName, MongoExtendedJsons.toBson(filter, true),
          MongoExtendedJsons.toBson(update, true), options);
    } catch (JsonProcessingException e) {
      throw new CorantRuntimeException(e);
    }
  }

  protected <T> Document handle(T object) {
    if (object instanceof Document doc) {
      return doc;
    } else {
      Map<String, Object> map;
      if (object instanceof Map m) {
        map = forceCast(m); // FIXME force cast
      } else {
        map = Jsons.convert(object, new TypeLiteral<Map<String, Object>>() {});
      }
      if (map.containsKey("id") && !map.containsKey("_id")) {
        map.put("_id", map.remove("id"));
      }
      return new Document(map);
    }
  }

  protected MongoCollection<Document> obtainCollection(String collectionName) {
    MongoCollection<Document> collection = database.getCollection(collectionName);
    if (writeConcern != null) {
      collection.withWriteConcern(writeConcern);
    }
    if (readPreference != null) {
      collection.withReadPreference(readPreference);
    }
    if (readConcern != null) {
      collection.withReadConcern(readConcern);
    }
    return collection;
  }

  /**
   * corant-modules-mongodb
   *
   * @author bingo 18:34:06
   */
  public static class MongoAggregator {
    protected final MongoTemplate tpl;
    protected List<Bson> pipeline;
    protected String collectionName;
    protected Bson hint;
    protected Collation collation;
    protected Duration maxTime;
    protected Duration maxAwaitTime;
    protected Integer batchSize;
    protected Boolean bypassDocumentValidation;
    protected BsonValue comment;
    protected Bson variables;
    protected Boolean allowDiskUse;
    protected ExplainVerbosity explainVerbosity;
    protected Class<?> explainResultClass;

    MongoAggregator(MongoTemplate tpl) {
      this.tpl = tpl;
    }

    public List<Map<?, ?>> aggregate() {
      ClientSession session = obtainSession();
      AggregateIterable<Document> ai = session == null ? obtainCollection().aggregate(pipeline)
          : obtainCollection().aggregate(session, pipeline);
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
        return streamOf(cursor).onClose(cursor::close).collect(Collectors.toList());
      }
    }

    public MongoAggregator allowDiskUse(Boolean allowDiskUse) {
      this.allowDiskUse = allowDiskUse;
      return this;
    }

    public MongoAggregator batchSize(Integer batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public MongoAggregator bypassDocumentValidation(Boolean bypassDocumentValidation) {
      this.bypassDocumentValidation = bypassDocumentValidation;
      return this;
    }

    public MongoAggregator collation(Collation collation) {
      this.collation = collation;
      return this;
    }

    public MongoAggregator collectionName(String collectionName) {
      this.collectionName = shouldNotNull(collectionName);
      return this;
    }

    public MongoAggregator comment(BsonValue comment) {
      this.comment = comment;
      return this;
    }

    public MongoAggregator explainResultClass(Class<?> explainResultClass) {
      this.explainResultClass = explainResultClass;
      return this;
    }

    public MongoAggregator explainVerbosity(ExplainVerbosity explainVerbosity) {
      this.explainVerbosity = explainVerbosity;
      return this;
    }

    public MongoAggregator hint(Bson hint) {
      this.hint = shouldNotNull(hint);
      return this;
    }

    public MongoAggregator hint(Map<?, ?> hint) {
      return hint(parse(hint, false));
    }

    public MongoAggregator maxAwaitTime(Duration maxAwaitTime) {
      this.maxAwaitTime = maxAwaitTime;
      return this;
    }

    public MongoAggregator maxTime(Duration maxTime) {
      this.maxTime = maxTime;
      return this;
    }

    public MongoAggregator pipeline(Bson... pipeline) {
      this.pipeline = shouldNotEmpty(listOf(pipeline));
      return this;
    }

    public MongoAggregator pipeline(List<Map<?, ?>> pipeline) {
      this.pipeline = shouldNotEmpty(pipeline).stream().map(a -> {
        try {
          return MongoExtendedJsons.toBson(a, true);
        } catch (JsonProcessingException e) {
          throw new CorantRuntimeException(e);
        }
      }).collect(Collectors.toList());
      return this;
    }

    public MongoAggregator variables(Bson variables) {
      this.variables = variables;
      return this;
    }

    public MongoAggregator variables(Map<?, ?> variables) {
      return variables(parse(variables, false));
    }

    protected MongoCollection<Document> obtainCollection() {
      return tpl.obtainCollection(collectionName);
    }

    protected ClientSession obtainSession() {
      return tpl.getSession();
    }
  }

  /**
   * corant-modules-mongodb
   *
   * @author bingo 18:34:06
   */
  public static class MongoQuery {
    protected final MongoTemplate tpl;
    protected String collectionName;
    protected Bson filter;
    protected Bson sort;
    protected Bson projection;
    protected Bson max;
    protected Bson min;
    protected Bson hint;
    protected int limit = 1;
    protected int offset = 0;
    protected boolean autoSetIdField = true;
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

    protected MongoQuery(MongoTemplate tpl) {
      this.tpl = tpl;
    }

    public MongoQuery allowDiskUse(Boolean allowDiskUse) {
      this.allowDiskUse = allowDiskUse;
      return this;
    }

    public MongoQuery autoSetIdField(boolean autoSetIdField) {
      this.autoSetIdField = autoSetIdField;
      return this;
    }

    public MongoQuery batchSize(Integer batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public MongoQuery collation(Collation collation) {
      this.collation = collation;
      return this;
    }

    public MongoQuery collectionName(String collectionName) {
      this.collectionName = shouldNotNull(collectionName);
      return this;
    }

    public MongoQuery comment(BsonValue comment) {
      this.comment = comment;
      return this;
    }

    public long countDocuments() {
      MongoCollection<Document> mc = obtainCollection();
      if (isNotEmpty(filter)) {
        return mc.countDocuments(filter);
      }
      return mc.countDocuments();
    }

    public MongoQuery cursorType(CursorType cursorType) {
      this.cursorType = cursorType;
      return this;
    }

    public MongoQuery explainResultClass(Class<?> explainResultClass) {
      this.explainResultClass = explainResultClass;
      return this;
    }

    public MongoQuery explainVerbosity(ExplainVerbosity explainVerbosity) {
      this.explainVerbosity = explainVerbosity;
      return this;
    }

    public MongoQuery filter(Bson filter) {
      this.filter = shouldNotNull(filter);
      return this;
    }

    public MongoQuery filter(Map<?, ?> filter) {
      if (filter != null) {
        this.filter = parse(filter, true);
      }
      return this;
    }

    public List<Map<?, ?>> find() {
      try (MongoCursor<Document> it = query().iterator()) {
        return streamOf(it).map(this::convert).collect(Collectors.toList());
      }
    }

    public <T> List<T> findAs(final Class<T> clazz) {
      return findAs(t -> Jsons.convert(t, clazz));
    }

    public <T> List<T> findAs(final Function<Object, T> converter) {
      try (MongoCursor<Document> it = query().iterator()) {
        return streamOf(it).map(this::convert).map(converter).collect(Collectors.toList());
      }
    }

    public Map<String, Object> findOne() {
      try (MongoCursor<Document> it = query().limit(1).iterator()) {
        return it.hasNext() ? it.next() : null;
      }
    }

    public <T> T findOne(Class<T> type) {
      try (MongoCursor<Document> it = query().limit(1).iterator()) {
        return it.hasNext() ? Jsons.convert(it.next(), type) : null;
      }
    }

    public MongoQuery hint(Bson hint) {
      this.hint = hint;
      return this;
    }

    public MongoQuery hint(Map<?, ?> hint) {
      return hint(parse(hint, false));
    }

    public MongoQuery limit(int limit) {
      this.limit = Objects.max(limit, 1);
      return this;
    }

    public MongoQuery max(Bson max) {
      this.max = max;
      return this;
    }

    public MongoQuery max(Map<?, ?> max) {
      return max(parse(max, false));
    }

    public MongoQuery maxAwaitTime(Duration maxAwaitTime) {
      this.maxAwaitTime = maxAwaitTime;
      return this;
    }

    public MongoQuery maxTime(Duration maxTime) {
      this.maxTime = maxTime;
      return this;
    }

    public MongoQuery min(Bson min) {
      this.min = min;
      return this;
    }

    public MongoQuery min(Map<?, ?> min) {
      return min(parse(min, false));
    }

    public MongoQuery noCursorTimeout(Boolean noCursorTimeout) {
      this.noCursorTimeout = noCursorTimeout;
      return this;
    }

    public MongoQuery offset(int offset) {
      this.offset = Objects.max(offset, 0);
      return this;
    }

    public MongoQuery partial(Boolean partial) {
      this.partial = partial;
      return this;
    }

    public MongoQuery projection(Bson projection) {
      this.projection = projection;
      return this;
    }

    public MongoQuery projection(Map<?, ?> projection) {
      return projection(parse(projection, false));
    }

    public MongoQuery projection(String[] includePropertyNames, String[] excludePropertyNames) {
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

    public MongoQuery returnKey(Boolean returnKey) {
      this.returnKey = returnKey;
      return this;
    }

    public MongoQuery showRecordId(Boolean showRecordId) {
      this.showRecordId = showRecordId;
      return this;
    }

    public MongoQuery simpleComment(Object comment) {
      this.comment = Bsons.toSimpleBsonValue(comment);
      return this;
    }

    public MongoQuery sort(Bson sort) {
      this.sort = shouldNotNull(sort);
      return this;
    }

    public MongoQuery sort(Map<?, ?> sort) {
      return sort(parse(sort, false));
    }

    @SuppressWarnings("rawtypes")
    public Stream<Map<?, ?>> stream() {
      limit = 0; // NO LIMIT
      MongoCursor<Document> it = query().iterator();
      return streamOf(it).onClose(it::close).map(this::convert).map(r -> (Map) r);
    }

    public <T> Stream<T> streamAs(final Class<T> clazz) {
      return streamAs(r -> Jsons.convert(r, clazz));
    }

    public <T> Stream<T> streamAs(final Function<Object, T> converter) {
      limit = 0; // NO LIMIT
      MongoCursor<Document> it = query().iterator();
      return streamOf(it).onClose(it::close).map(this::convert).map(converter);
    }

    public MongoQuery variables(Bson variables) {
      this.variables = variables;
      return this;
    }

    public MongoQuery variables(Map<?, ?> variables) {
      return variables(parse(variables, false));
    }

    protected MongoCollection<Document> obtainCollection() {
      return tpl.obtainCollection(collectionName);
    }

    protected ClientSession obtainSession() {
      return tpl.getSession();
    }

    Map<?, ?> convert(Document doc) {
      if (doc != null && autoSetIdField && !doc.containsKey("id") && doc.containsKey("_id")) {
        doc.put("id", doc.get("_id"));
      }
      return doc;
    }

    FindIterable<Document> query() {
      MongoCollection<Document> mc = obtainCollection();
      ClientSession session = obtainSession();
      FindIterable<Document> fi = session != null ? mc.find(session) : mc.find();
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
}

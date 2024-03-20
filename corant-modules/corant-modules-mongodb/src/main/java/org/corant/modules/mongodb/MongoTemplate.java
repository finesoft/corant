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

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.corant.modules.mongodb.Mongos.DOC_ID_FIELD_NAME;
import static org.corant.modules.mongodb.Mongos.ENTITY_ID_FIELD_NAME;
import static org.corant.shared.ubiquity.Throwing.uncheckedFunction;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import static org.corant.shared.util.Assertions.shouldNotEmpty;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Lists.listOf;
import static org.corant.shared.util.Lists.transform;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.corant.modules.bson.Bsons;
import org.corant.modules.bson.ExtendedCodecProvider;
import org.corant.modules.json.ObjectMappers;
import org.corant.shared.util.Objects;
import org.corant.shared.util.Strings;
import com.mongodb.CursorType;
import com.mongodb.ExplainVerbosity;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.ServerCursor;
import com.mongodb.WriteConcern;
import com.mongodb.annotations.NotThreadSafe;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.DropCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
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

  public static final CodecRegistry DEFAULT_CODEC_REGISTRY =
      fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
          fromProviders(Bsons.DOCUMENT_CODEC_PROVIDER, new ExtendedCodecProvider()));
  protected static final Function<Document, Object> DOC_ID_GETTER = d -> d.get(DOC_ID_FIELD_NAME);

  protected final MongoDatabase database;
  protected WriteConcern writeConcern;
  protected ReadPreference readPreference;
  protected ReadConcern readConcern;
  protected ClientSession session;
  protected CodecRegistry codecRegistry = DEFAULT_CODEC_REGISTRY;

  public MongoTemplate(MongoClient mongoClient, String databaseName) {
    this(mongoClient.getDatabase(databaseName));
  }

  public MongoTemplate(MongoDatabase database) {
    this(database, null, null, null, null, null);
  }

  public MongoTemplate(MongoDatabase database, ClientSession session) {
    this(database, null, session);
  }

  public MongoTemplate(MongoDatabase database, ReadPreference readPreference,
      ReadConcern readConcern) {
    this(database, null, readPreference, readConcern, null, null);
  }

  public MongoTemplate(MongoDatabase database, ReadPreference readPreference,
      ReadConcern readConcern, ClientSession session) {
    this(database, null, readPreference, readConcern, null, session);
  }

  public MongoTemplate(MongoDatabase database, WriteConcern writeConcern) {
    this(database, writeConcern, null);
  }

  public MongoTemplate(MongoDatabase database, WriteConcern writeConcern, ClientSession session) {
    this(database, writeConcern, null, null, null, session);
  }

  public MongoTemplate(MongoDatabase database, WriteConcern writeConcern,
      ReadPreference readPreference, ReadConcern readConcern, CodecRegistry codecRegistry,
      ClientSession session) {
    this.database = database;
    this.writeConcern = writeConcern;
    this.readPreference = readPreference;
    this.readConcern = readConcern;
    this.session = session;
    if (codecRegistry != null) {
      this.codecRegistry = codecRegistry;
    }
  }

  public MongoTemplate(String mongoURL) {
    this(Mongos.resolveDatabase(mongoURL));
  }

  public MongoAggregate aggregate() {
    return new MongoAggregate(this);
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
   * Removes one documents from the given collection that match the given id. If no document
   * match,the collection is not modified.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param id the document id
   * @return the result of the remove operation
   */
  public DeleteResult delete(String collectionName, Object id) {
    return executeDelete(collectionName, id, null);
  }

  /**
   * Removes all documents from the given collection that match the given query filter. If no
   * documents match, the collection is not modified.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter the query filter to apply the delete operation
   * @return the result of the remove operation
   */
  public DeleteResult deleteMany(String collectionName, Map<String, ?> filter) {
    return deleteMany(collectionName, filter, null);
  }

  /**
   * Removes all documents from the given collection that match the given query filter. If no
   * documents match, the collection is not modified.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter the query filter to apply the delete operation
   * @param options the options to apply to the delete operation
   * @return the result of the remove operation
   */
  public DeleteResult deleteMany(String collectionName, Map<String, ?> filter,
      DeleteOptions options) {
    return executeDeleteMany(collectionName,
        Mongos.parse(shouldNotNull(filter, "Filter can't null")), options);
  }

  /**
   * Returns a new {@link MongoDistinction} instance.
   */
  public MongoDistinction distinction() {
    return new MongoDistinction(this);
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

  /**
   * Removes one documents from the given collection that match the given id. If no document match,
   * the collection is not modified.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param id the document id
   * @param options the options to apply to the delete operation
   * @return the result of the delete operation
   */
  public DeleteResult executeDelete(String collectionName, Object id, DeleteOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(id, "Id must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    Bson filter = Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(id));
    if (options != null) {
      if (session != null) {
        return collection.deleteOne(session, filter, options);
      } else {
        return collection.deleteOne(filter, options);
      }
    } else if (session != null) {
      return collection.deleteOne(session, filter);
    } else {
      return collection.deleteOne(filter);
    }
  }

  /**
   * Removes all documents from the given collection that match the given query filter. If no
   * documents match, the collection is not modified.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter the query filter to apply the delete operation
   * @param options the options to apply to the delete operation
   * @return the result of the remove operation
   */
  public DeleteResult executeDeleteMany(String collectionName, Bson filter, DeleteOptions options) {
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
   * Insert an object into the specified collection according to the specified codec-registry and
   * options.
   *
   * @param <T> the type of the class to use instead of Document
   * @param collectionName name of the collection to store the object in. Must not be {@literal
   *     null}
   * @param object the object to store in the collection. Must not be {@literal null}.
   * @param codecRegistry the codec-registry apply to the operation
   * @param options the options to apply to the operation
   * @return the insert result
   */
  public <T> InsertOneResult executeInsert(String collectionName, T object,
      CodecRegistry codecRegistry, InsertOneOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(object, "Object must not be null");
    @SuppressWarnings("unchecked")
    Class<T> klass = (Class<T>) object.getClass();
    MongoCollection<T> collection = obtainCollection(collectionName, klass);
    if (codecRegistry != null) {
      collection = collection.withCodecRegistry(codecRegistry);
    }
    if (options != null) {
      if (session != null) {
        return collection.insertOne(session, object, options);
      } else {
        return collection.insertOne(object, options);
      }
    } else if (session != null) {
      return collection.insertOne(session, object);
    } else {
      return collection.insertOne(object);
    }
  }

  /**
   * Insert a list of objects into a collection in a single batch write to the database according to
   * the specified codec-registry and options.
   *
   * @param <T> the type of the class to use instead of Document
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param klass the object class
   * @param objects the list of objects to store in the collection. Must not be {@literal null}.
   * @param codecRegistry the codec-registry to apply to the operation
   * @param options the options to apply to the operation
   * @return the insert result
   */
  public <T> InsertManyResult executeInsertMany(String collectionName, Class<T> klass,
      List<T> objects, CodecRegistry codecRegistry, InsertManyOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(klass, "Object class must not be null");
    shouldNotNull(objects, "Objects list must not be null");
    MongoCollection<T> collection = obtainCollection(collectionName, klass);
    if (codecRegistry != null) {
      collection = collection.withCodecRegistry(codecRegistry);
    }
    if (options != null) {
      if (session != null) {
        return collection.insertMany(session, objects, options);
      } else {
        return collection.insertMany(objects, options);
      }
    } else if (session != null) {
      return collection.insertMany(session, objects);
    } else {
      return collection.insertMany(objects);
    }
  }

  /**
   * Save or replace an object in the given collection according to the specified codec-registry and
   * options.
   *
   * @param <T> the type of the class to use instead of Document
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param object the object to be saved or replaced
   * @param codecRegistry the codec-registry to apply to the save operation
   * @param idGetter the id extractor function
   * @param options the options to apply to the save operation
   * @return the save or replace result
   */
  public <T> UpdateResult executeSave(String collectionName, T object, CodecRegistry codecRegistry,
      Function<T, Object> idGetter, ReplaceOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(object, "Object must not be null");
    shouldNotNull(idGetter, "The id getter must not be null");
    @SuppressWarnings("unchecked")
    Class<T> klass = (Class<T>) object.getClass();
    MongoCollection<T> collection = obtainCollection(collectionName, klass);
    if (codecRegistry != null) {
      collection = collection.withCodecRegistry(codecRegistry);
    }
    ReplaceOptions ro = defaultObject(options, ReplaceOptions::new).upsert(true);
    Bson filter = Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(idGetter.apply(object)));
    if (session != null) {
      return collection.replaceOne(session, filter, object, ro);
    } else {
      return collection.replaceOne(filter, object, ro);
    }
  }

  /**
   * Save or replace an object in the given collection according to the specified codec-registry and
   * options.
   *
   * @param <T> the type of the class to use instead of Document
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param klass the class of list element
   * @param objects a list of object to be saved or replaced
   * @param codecRegistry the codec-registry to apply to the save operation
   * @param idGetter the id extractor function
   * @param options the options to apply to the save operation
   * @return a list of save or replace result
   */
  public <T> BulkWriteResult executeSaveMany(String collectionName, Class<T> klass, List<T> objects,
      CodecRegistry codecRegistry, Function<T, Object> idGetter, ReplaceOptions options,
      BulkWriteOptions bulkWriteOptions) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(klass, "Object klass must not be null");
    shouldNotNull(objects, "Objects list must not be null");
    shouldNotNull(idGetter, "The Id getter must not be null");
    MongoCollection<T> collection = obtainCollection(collectionName, klass);
    if (codecRegistry != null) {
      collection = collection.withCodecRegistry(codecRegistry);
    }
    ReplaceOptions ro = defaultObject(options, ReplaceOptions::new).upsert(true);

    List<WriteModel<T>> docs = new ArrayList<>(objects.size());
    for (T object : objects) {
      docs.add(new ReplaceOneModel<>(
          Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(idGetter.apply(object))), object, ro));
    }
    if (session != null) {
      if (bulkWriteOptions != null) {
        return collection.bulkWrite(session, docs, bulkWriteOptions);
      } else {
        return collection.bulkWrite(session, docs);
      }
    } else if (bulkWriteOptions != null) {
      return collection.bulkWrite(docs, bulkWriteOptions);
    } else {
      return collection.bulkWrite(docs);
    }
  }

  /**
   * Save or replace an object in the given collection according to the specified codec-registry and
   * options.
   *
   * @param <T> the type of the class to use instead of Document
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param klass the class of list element
   * @param stream the objects stream to be saved or replaced
   * @param codecRegistry the codec-registry to apply to the save operation
   * @param idGetter the id extractor function
   * @param options the options to apply to the save operation
   * @return the save result stream
   */
  public <T> Stream<UpdateResult> executeStreamSave(String collectionName, Class<T> klass,
      Stream<T> stream, CodecRegistry codecRegistry, Function<T, Object> idGetter,
      ReplaceOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(klass, "Object klass must not be null");
    shouldNotNull(stream, "Stream must not be null");
    shouldNotNull(idGetter, "The Id getter must not be null");
    MongoCollection<T> collection = obtainCollection(collectionName, klass);
    if (codecRegistry != null) {
      collection = collection.withCodecRegistry(codecRegistry);
    }
    final MongoCollection<T> useCollection = collection;
    ReplaceOptions ro = defaultObject(options, ReplaceOptions::new).upsert(true);
    if (session != null) {
      return stream.map(doc -> useCollection.replaceOne(session,
          Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(idGetter.apply(doc))), doc, ro));
    } else {
      return stream.map(doc -> useCollection
          .replaceOne(Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(idGetter.apply(doc))), doc, ro));
    }
  }

  /**
   * Update documents in the given collection according to the specified options.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param many update all documents or single document
   * @param filter a document describing the query filter, which may not be null.
   * @param update a document describing the update, which may not be null. The update to apply must
   *        include only update operators
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult executeUpdates(String collectionName, boolean many, Bson filter, Bson update,
      UpdateOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(filter, "Filter must not be null");
    shouldNotNull(update, "Update must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    if (options != null) {
      if (session != null) {
        if (many) {
          return collection.updateMany(session, filter, update, options);
        } else {
          return collection.updateOne(session, filter, update, options);
        }
      } else if (many) {
        return collection.updateMany(filter, update, options);
      } else {
        return collection.updateOne(filter, update, options);
      }
    } else if (session != null) {
      if (many) {
        return collection.updateMany(session, filter, update);
      } else {
        return collection.updateOne(session, filter, update);
      }
    } else if (many) {
      return collection.updateMany(filter, update);
    } else {
      return collection.updateOne(filter, update);
    }
  }

  /**
   * Update documents in the given collection according to the specified options.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param many update all documents or single document
   * @param filter a document describing the query filter, which may not be null.
   * @param pipeline a pipeline describing the update, which may not be null
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult executeUpdates(String collectionName, boolean many, Bson filter,
      List<? extends Bson> pipeline, UpdateOptions options) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(filter, "Filter must not be null");
    shouldNotNull(pipeline, "Update must not be null");
    MongoCollection<Document> collection = obtainCollection(collectionName);
    if (options != null) {
      if (session != null) {
        if (many) {
          return collection.updateMany(session, filter, pipeline, options);
        } else {
          return collection.updateOne(session, filter, pipeline, options);
        }
      } else if (many) {
        return collection.updateMany(filter, pipeline, options);
      } else {
        return collection.updateOne(filter, pipeline, options);
      }
    } else if (session != null) {
      if (many) {
        return collection.updateMany(session, filter, pipeline);
      } else {
        return collection.updateOne(session, filter, pipeline);
      }
    } else if (many) {
      return collection.updateMany(filter, pipeline);
    } else {
      return collection.updateOne(filter, pipeline);
    }
  }

  /**
   * Returns whether the file associated with the given id and collection is existing.
   *
   * @param collectionName the collection name
   * @param id the document id
   */
  public boolean exists(String collectionName, Object id) {
    try (MongoCursor<Document> it = obtainCollection(collectionName)
        .find(Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(id))).iterator()) {
      return it.hasNext();
    }
  }

  public CodecRegistry getCodecRegistry() {
    return codecRegistry;
  }

  public MongoDatabase getDatabase() {
    return database;
  }

  /**
   * Returns a {@link Document} with the given id and collection
   *
   * @param collectionName the collection name
   * @param id the document id
   */
  public Document getDocument(String collectionName, Object id) {
    try (MongoCursor<Document> it = obtainCollection(collectionName)
        .find(Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(id))).iterator()) {
      if (it.hasNext()) {
        return it.next();
      }
    }
    return null;
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
   * Insert an object into the specified collection. If the given object is not a {@link Document}
   * instance, it will be converted to a Map and then constructed into a {@link Document}. If the
   * given object has a property named 'id', the value of the property will be used as the primary
   * key(named '_id') of the {@link Document} and the converted document doesn't retain the id
   * property.
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
   * Insert an object into the specified collection according to the specified options. If the given
   * object is not a {@link Document} instance, it will be converted to a Map and then constructed
   * into a {@link Document}. If the given object has a property named 'id', the value of the
   * property will be used as the primary key(named '_id') of the {@link Document} and the converted
   * document doesn't retain the id property.
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
   * Insert an object into the specified collection according to the specified options and document
   * handler.
   * <p>
   * If the given handler is null and the given object is not a {@link Document} instance, it will
   * be converted to Map and then constructed into a {@link Document}. If the given object has a
   * property named 'id', the value of the property will be used as the primary key(named '_id') of
   * the {@link Document} and the converted document doesn't retain the id property.
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be {@literal
   *     null}.
   * @param object the object to store in the collection. Must not be {@literal null}.
   * @param options the options to apply to the operation
   * @param handler the handler which use to convert the given object to a {@link Document} and
   *        process the primary key of the {@link Document}, can be null.
   * @return the insert result
   */
  public <T> InsertOneResult insert(String collectionName, T object, InsertOneOptions options,
      Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(object, "Object must not be null");
    Document doc;
    if (handler == null) {
      doc = resolveDocument(object);
    } else {
      doc = handler.apply(object);
    }
    return executeInsert(collectionName, doc, null, options);
  }

  /**
   * Insert a list of objects into a collection in a single batch write to the database.
   * <p>
   * If the given handler is null and the given object is not a {@link Document} instance, it will
   * be converted to Map and then constructed into a {@link Document}. If the given object has a
   * property named 'id', the value of the property will be used as the primary key(named '_id') of
   * the {@link Document} and the converted document doesn't retain the id property.
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param objects the list of objects to store in the collection. Must not be {@literal null}.
   * @return the insert result
   */
  public <T> InsertManyResult insertMany(String collectionName, List<T> objects) {
    return insertMany(collectionName, objects, null, null);
  }

  /**
   * Insert a list of objects into a collection in a single batch write to the database according to
   * the specified options.
   * <p>
   * If the given handler is null and the given object is not a {@link Document} instance, it will
   * be converted to Map and then constructed into a {@link Document}. If the given object has a
   * property named 'id', the value of the property will be used as the primary key(named '_id') of
   * the {@link Document} and the converted document doesn't retain the id property.
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be
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
   * Insert a list of objects into a collection in a single batch write to the database according to
   * the specified options and document handler.
   * <p>
   * If the given handler is null and the given element of list is not a {@link Document} instance,
   * it will be converted to Map and then constructed into a {@link Document}. If the element has a
   * property named 'id', the value of the property will be used as the primary key(named '_id') of
   * the {@link Document} and the converted document doesn't retain the id property.
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param objects the list of objects to store in the collection. Must not be {@literal null}.
   * @param options the options to apply to the operation
   * @param handler the handler which use to convert the element of the given list to
   *        {@link Document} and process the primary key of the {@link Document}, can be null.
   * @return the insert result
   */
  public <T> InsertManyResult insertMany(String collectionName, List<T> objects,
      InsertManyOptions options, Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(objects, "Objects list must not be null");
    Function<T, Document> useHandler = defaultObject(handler, this::resolveDocument);
    List<Document> docs = objects.stream().map(useHandler).toList();
    return executeInsertMany(collectionName, Document.class, docs, null, options);
  }

  /**
   * Returns a new {@link MongoQuery} instance.
   */
  public MongoQuery query() {
    return new MongoQuery(this);
  }

  /**
   * Save or replace an object in the given collection according to the specified arguments. If the
   * given object is not a {@link Document} instance, it will be converted to Map and then
   * constructed into a {@link Document}. If the given object has a property named 'id', the value
   * of the property will be used as the primary key(named '_id') of the {@link Document} and the
   * converted document doesn't retain the id property.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param object the object to be saved or replaced
   * @return the save or replace result
   */
  public UpdateResult save(String collectionName, Object object) {
    return save(collectionName, object, null, null);
  }

  /**
   * Save or replace an object in the given collection according to the specified options. If the
   * given object is not a {@link Document} instance, it will be converted to Map and then
   * constructed into a {@link Document}. If the given object has a property named 'id', the value
   * of the property will be used as the primary key(named '_id') of the {@link Document} and the
   * converted document doesn't retain the id property.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param object the object to be saved or replaced
   * @param options the options to apply to the save operation
   * @return the save or replace result
   */
  public UpdateResult save(String collectionName, Object object, ReplaceOptions options) {
    return save(collectionName, object, options, null);
  }

  /**
   * Save or replace an object in the given collection according to the specified options and
   * document handler.
   * <p>
   * If the given handler is null and the given object is not a {@link Document} instance, it will
   * be converted to Map and then constructed into a {@link Document}. If the object has a property
   * named 'id', the value of the property will be used as the primary key(named '_id') of the
   * {@link Document} and the converted document doesn't retain the id property.
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param object the object to be saved or replaced
   * @param options the options to apply to the save operation
   * @param handler the handler which use to convert the given object to {@link Document} and
   *        process the primary key of the {@link Document}, can be null.
   * @return the save or replace result
   */
  public <T> UpdateResult save(String collectionName, T object, ReplaceOptions options,
      Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(object, "Object must not be null");
    Document doc;
    if (handler == null) {
      doc = resolveDocument(object);
    } else {
      doc = handler.apply(object);
    }
    return executeSave(collectionName, doc, null, DOC_ID_GETTER, options);
  }

  /**
   * Save or replace a list of objects in the given collection.
   * <p>
   * If the given element of list is not a {@link Document} instance, it will be converted to Map
   * and then constructed into a {@link Document}. If the element has a property named 'id', the
   * value of the property will be used as the primary key(named '_id') of the {@link Document} and
   * the converted document doesn't retain the id property.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param objects a list of object to be saved or replaced
   * @return a list of save or replace result
   */
  public BulkWriteResult saveMany(String collectionName, List<?> objects) {
    return saveMany(collectionName, objects, null, null);
  }

  /**
   * Save or replace a list of objects in the given collection according to the specified options.
   * <p>
   * If the given element of list is not a {@link Document} instance, it will be converted to Map
   * and then constructed into a {@link Document}. If the element has a property named 'id', the
   * value of the property will be used as the primary key(named '_id') of the {@link Document} and
   * the converted document doesn't retain the id property.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param objects a list of object to be saved or replaced
   * @param options the options to apply to the save operation
   * @return a list of save or replace result
   */
  public BulkWriteResult saveMany(String collectionName, List<?> objects, ReplaceOptions options) {
    return saveMany(collectionName, objects, options, null);
  }

  /**
   * Save or replace a list of objects in the given collection according to the specified options
   * and document handler.
   * <p>
   * If the given element of list is not a {@link Document} instance, it will be converted to Map
   * and then constructed into a {@link Document}. If the element has a property named 'id', the
   * value of the property will be used as the primary key(named '_id') of the {@link Document} and
   * the converted document doesn't retain the id property.
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param objects a list of object to be saved or replaced
   * @param options the options to apply to the save operation
   * @param handler the handler which use to convert the given object to {@link Document} and
   *        process the primary key of the {@link Document}, can be null.
   * @return a list of save or replace result
   */
  public <T> BulkWriteResult saveMany(String collectionName, List<T> objects,
      ReplaceOptions options, Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(objects, "Objects list must not be null");
    return executeSaveMany(collectionName, Document.class,
        transform(objects, defaultObject(handler, this::resolveDocument)), null, DOC_ID_GETTER,
        options, null);
  }

  /**
   * Save or replace an objects stream in the given collection.
   * <p>
   * If the given element of list is not a {@link Document} instance, it will be converted to Map
   * and then constructed into a {@link Document}. If the element has a property named 'id', the
   * value of the property will be used as the primary key(named '_id') of the {@link Document} and
   * the converted document doesn't retain the id property.
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param stream the objects stream to be saved or replaced
   * @return the save result stream
   */
  public <T> Stream<UpdateResult> streamSave(String collectionName, Stream<T> stream) {
    return streamSave(collectionName, stream, null, null);
  }

  /**
   * Save or replace an objects stream in the given collection according to the specified options.
   * <p>
   * If the given element of list is not a {@link Document} instance, it will be converted to Map
   * and then constructed into a {@link Document}. If the element has a property named 'id', the
   * value of the property will be used as the primary key(named '_id') of the {@link Document} and
   * the converted document doesn't retain the id property.
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param stream the objects stream to be saved
   * @param options the options to apply to the save operation
   * @return the save result stream
   */
  public <T> Stream<UpdateResult> streamSave(String collectionName, Stream<T> stream,
      ReplaceOptions options) {
    return streamSave(collectionName, stream, options, null);
  }

  /**
   * Save or replace an objects stream in the given collection according to the specified options
   * and document handler.
   * <p>
   * If the document handler the given element of list is not a {@link Document} instance, it will
   * be converted to Map and then constructed into a {@link Document}. If the element has a property
   * named 'id', the value of the property will be used as the primary key(named '_id') of the
   * {@link Document} and the converted document doesn't retain the id property.
   *
   * @param <T> the object type
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param stream the objects stream to be saved
   * @param options the options to apply to the save operation
   * @param handler the handler which use to convert the given object to {@link Document} and
   *        process the primary key of the {@link Document}, can be null.
   * @return the save result stream
   */
  public <T> Stream<UpdateResult> streamSave(String collectionName, Stream<T> stream,
      ReplaceOptions options, Function<T, Document> handler) {
    shouldNotNull(collectionName, "CollectionName must not be null");
    shouldNotNull(stream, "Objects must not be null");
    return executeStreamSave(collectionName, Document.class,
        stream.map(defaultObject(handler, this::resolveDocument)), null, DOC_ID_GETTER, options);
  }

  /**
   * Update a single documents in the given collection according to the specified arguments.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter a document describing the query filter, which may not be null
   * @param pipeline a pipeline describing the update, which may not be null
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Map<String, ?> filter,
      List<Map<String, ?>> pipeline) {
    return update(collectionName, filter, pipeline, null);
  }

  /**
   * Update a single documents in the given collection according to the specified arguments.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter a document describing the query filter, which may not be null
   * @param pipeline a pipeline describing the update, which may not be null
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Map<String, ?> filter,
      List<Map<String, ?>> pipeline, UpdateOptions options) {
    return executeUpdates(collectionName, false, Mongos.parse(filter),
        pipeline.stream().map(uncheckedFunction(Mongos::parse)).toList(), options);
  }

  /**
   * Update a single documents in the given collection according to the specified arguments.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter a document describing the query filter, which may not be null
   * @param update a document describing the update, which may not be null. The update to apply must
   *        include only update operators
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Map<String, ?> filter, Map<String, ?> update) {
    return update(collectionName, filter, update, null);
  }

  /**
   * Update a single documents in the given collection according to the specified arguments.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter a document describing the query filter, which may not be null
   * @param update a document describing the update, which may not be null. The update to apply must
   *        include only update operators
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult update(String collectionName, Map<String, ?> filter, Map<String, ?> update,
      UpdateOptions options) {
    return executeUpdates(collectionName, false, Mongos.parse(filter), Mongos.parse(update),
        options);
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter a document describing the query filter, which may not be null
   * @param pipeline a pipeline describing the update, which may not be null
   * @return the result of the update many operation
   */
  public UpdateResult updateMany(String collectionName, Map<String, ?> filter,
      List<Map<String, ?>> pipeline) {
    return updateMany(collectionName, filter, pipeline, null);
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter a document describing the query filter, which may not be null
   * @param pipeline a pipeline describing the update, which may not be null
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult updateMany(String collectionName, Map<String, ?> filter,
      List<Map<String, ?>> pipeline, UpdateOptions options) {
    return executeUpdates(collectionName, true, Mongos.parse(filter),
        pipeline.stream().map(uncheckedFunction(Mongos::parse)).toList(), options);
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter a document describing the query filter, which may not be null
   * @param update a document describing the update, which may not be null. The update to apply must
   *        include only update operators
   * @return the result of the update many operation
   */
  public UpdateResult updateMany(String collectionName, Map<String, ?> filter,
      Map<String, ?> update) {
    return updateMany(collectionName, filter, update, null);
  }

  /**
   * Update all documents in the given collection according to the specified arguments.
   *
   * @param collectionName name of the collection to store the object in. Must not be
   *        {@literal null}.
   * @param filter a document describing the query filter, which may not be null
   * @param update a document describing the update, which may not be null. The update to apply must
   *        include only update operators
   * @param options the options to apply to the update operation
   * @return the result of the update many operation
   */
  public UpdateResult updateMany(String collectionName, Map<String, ?> filter,
      Map<String, ?> update, UpdateOptions options) {
    return executeUpdates(collectionName, true, Mongos.parse(filter), Mongos.parse(update),
        options);
  }

  protected MongoCollection<Document> obtainCollection(String collectionName) {
    return obtainCollection(collectionName, Document.class);
  }

  protected <T> MongoCollection<T> obtainCollection(String collectionName, Class<T> klass) {
    MongoCollection<T> collection = database.getCollection(collectionName, klass);
    if (writeConcern != null) {
      collection = collection.withWriteConcern(writeConcern);
    }
    if (readPreference != null) {
      collection = collection.withReadPreference(readPreference);
    }
    if (readConcern != null) {
      collection = collection.withReadConcern(readConcern);
    }
    if (codecRegistry != null) {
      collection = collection.withCodecRegistry(codecRegistry);
    }
    return collection;
  }

  /**
   * Returns a {@link Document} with primary key(named '_id') from the given object for persisting.
   * <p>
   * Note: If the given object is a Map instance, we assume it is a {@code Map<String, Object>}. If
   * the given object has a property named 'id', the value of the property will be used as the
   * primary key(named '_id') of the {@link Document} and the resolved document doesn't retain the
   * id property.
   *
   * @param <T> the object type
   * @param object the object to be resolved
   * @return a document
   */
  protected <T> Document resolveDocument(T object) {
    Document document = null;
    if (object instanceof Document doc) {
      document = doc;
    } else if (object != null) {
      Map<String, Object> map;
      if (object instanceof Map m) {
        // FIXME force cast, we assume the object is Map<String,Object>
        map = forceCast(m);
      } else {
        // a common POJO
        map = ObjectMappers.toDocMap(object);
      }
      document = new Document(map);
    }
    if (document != null && document.containsKey(ENTITY_ID_FIELD_NAME)
        && !document.containsKey(DOC_ID_FIELD_NAME)) {
      document.put(DOC_ID_FIELD_NAME, document.remove(ENTITY_ID_FIELD_NAME));
    }
    return document;
  }

  /**
   * corant-modules-mongodb
   *
   * @author bingo 18:34:06
   */
  public static class MongoAggregate {
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
    protected CodecRegistry codecRegistry;

    protected MongoAggregate(MongoTemplate tpl) {
      this.tpl = tpl;
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline and the specified
     * {@link AggregateIterable} arguments
     */
    public List<Document> aggregate() {
      try (MongoCursor<Document> cursor = doAggregate().iterator()) {
        return streamOf(cursor).onClose(cursor::close).collect(Collectors.toList());
      }
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline and the specified
     * {@link AggregateIterable} arguments
     *
     * @param <T> the result type
     * @param klass the result class
     */
    public <T> List<T> aggregateAs(Class<T> klass) {
      try (MongoCursor<Document> cursor = doAggregate().iterator()) {
        return streamOf(cursor).map(a -> ObjectMappers.fromMap(a, klass)).onClose(cursor::close)
            .toList();
      }
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline and the specified
     * {@link AggregateIterable} arguments
     */
    public Stream<Document> aggregateStream() {
      MongoCursor<Document> cursor = doAggregate().iterator();
      return streamOf(cursor).onClose(cursor::close);
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline and the specified
     * {@link AggregateIterable} arguments
     *
     * @param <T> the result type
     * @param klass the result class
     */
    public <T> Stream<T> aggregateStreamAs(Class<T> klass) {
      MongoCursor<Document> cursor = doAggregate().iterator();
      return streamOf(cursor).map(a -> ObjectMappers.fromMap(a, klass)).onClose(cursor::close);
    }

    /**
     * @see AggregateIterable#allowDiskUse(Boolean)
     */
    public MongoAggregate allowDiskUse(Boolean allowDiskUse) {
      this.allowDiskUse = allowDiskUse;
      return this;
    }

    /**
     * @see AggregateIterable#batchSize(int)
     */
    public MongoAggregate batchSize(Integer batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    /**
     * @see AggregateIterable#bypassDocumentValidation(Boolean)
     */
    public MongoAggregate bypassDocumentValidation(Boolean bypassDocumentValidation) {
      this.bypassDocumentValidation = bypassDocumentValidation;
      return this;
    }

    /**
     * @see MongoCollection#withCodecRegistry(CodecRegistry)
     */
    public MongoAggregate codecRegistry(CodecRegistry codecRegistry) {
      this.codecRegistry = codecRegistry;
      return this;
    }

    /**
     * @see AggregateIterable#collation(Collation)
     */
    public MongoAggregate collation(Collation collation) {
      this.collation = collation;
      return this;
    }

    /**
     * Set the collection name
     *
     * @param collectionName the collection name
     */
    public MongoAggregate collectionName(String collectionName) {
      this.collectionName = shouldNotNull(collectionName);
      return this;
    }

    /**
     * @see AggregateIterable#comment(BsonValue)
     */
    public MongoAggregate comment(BsonValue comment) {
      this.comment = comment;
      return this;
    }

    /**
     * @see AggregateIterable#explain(Class)
     */
    public MongoAggregate explainResultClass(Class<?> explainResultClass) {
      this.explainResultClass = explainResultClass;
      return this;
    }

    /**
     * @see AggregateIterable#explain(Class, ExplainVerbosity)
     */
    public MongoAggregate explainVerbosity(ExplainVerbosity explainVerbosity) {
      this.explainVerbosity = explainVerbosity;
      return this;
    }

    /**
     * @see AggregateIterable#hint(Bson)
     */
    public MongoAggregate hint(Bson hint) {
      this.hint = hint;
      return this;
    }

    /**
     * @see AggregateIterable#hint(Bson)
     */
    public MongoAggregate hintMap(Map<String, ?> hint) {
      return hint(Mongos.parse(hint));
    }

    /**
     * @see AggregateIterable#maxAwaitTime(long, TimeUnit)
     */
    public MongoAggregate maxAwaitTime(Duration maxAwaitTime) {
      this.maxAwaitTime = maxAwaitTime;
      return this;
    }

    /**
     * @see AggregateIterable#maxTime(long, TimeUnit)
     */
    public MongoAggregate maxTime(Duration maxTime) {
      this.maxTime = maxTime;
      return this;
    }

    /**
     * @see MongoCollection#aggregate(List)
     */
    public MongoAggregate pipeline(Bson... pipeline) {
      this.pipeline = shouldNotEmpty(listOf(pipeline));
      return this;
    }

    /**
     * @see MongoCollection#aggregate(List)
     */
    public MongoAggregate pipeline(List<Map<String, ?>> pipeline) {
      this.pipeline =
          shouldNotEmpty(pipeline).stream().map(Mongos::parse).collect(Collectors.toList());
      return this;
    }

    /**
     * @see AggregateIterable#let(Bson)
     */
    public MongoAggregate variables(Bson variables) {
      this.variables = variables;
      return this;
    }

    /**
     * @see AggregateIterable#let(Bson)
     */
    public MongoAggregate variablesMap(Map<String, ?> variables) {
      return variables(Mongos.parse(variables));
    }

    protected AggregateIterable<Document> doAggregate() {
      ClientSession session = obtainSession();
      MongoCollection<Document> mc = obtainCollection();
      if (codecRegistry != null) {
        mc = mc.withCodecRegistry(codecRegistry);
      }
      AggregateIterable<Document> ai =
          session == null ? mc.aggregate(pipeline) : mc.aggregate(session, pipeline);
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
      return ai;
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
   * @author bingo 16:23:15
   */
  public static class MongoDistinction {
    protected final MongoTemplate tpl;
    protected String collectionName;
    protected Bson filter;
    protected Duration maxTime;
    protected Collation collation;
    protected BsonValue comment;
    protected Integer batchSize;
    protected CodecRegistry codecRegistry;

    protected MongoDistinction(MongoTemplate tpl) {
      this.tpl = tpl;
    }

    /**
     * @see DistinctIterable#batchSize(int)
     */
    public MongoDistinction batchSize(Integer batchSize) {
      this.batchSize = batchSize;
      if (this.batchSize != null && this.batchSize < 1) {
        this.batchSize = 1;
      }
      return this;
    }

    /**
     * @see MongoCollection#withCodecRegistry(CodecRegistry)
     */
    public MongoDistinction codecRegistry(CodecRegistry codecRegistry) {
      this.codecRegistry = codecRegistry;
      return this;
    }

    /**
     * @see DistinctIterable#collation(Collation)
     */
    public MongoDistinction collation(Collation collation) {
      this.collation = collation;
      return this;
    }

    /**
     * Set the name of the collection which will be queried.
     *
     * @param collectionName the collection name
     */
    public MongoDistinction collectionName(String collectionName) {
      this.collectionName = shouldNotNull(collectionName);
      return this;
    }

    /**
     * @see DistinctIterable#comment(BsonValue)
     */
    public MongoDistinction comment(BsonValue comment) {
      this.comment = comment;
      return this;
    }

    /**
     * @see DistinctIterable#comment(String)
     */
    public MongoDistinction comment(String comment) {
      this.comment = comment == null ? null : new BsonString(comment);
      return this;
    }

    /**
     * Returns the distinct values stream of the specified field name and field type.
     *
     * @param <T> the type to cast any distinct items into.
     * @param fieldName the field name
     * @param fieldType the class to cast any distinct items into.
     */
    public <T> Stream<T> distinctSteam(String fieldName, Class<T> fieldType) {
      MongoCursor<T> it = find(fieldName, fieldType).iterator();
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
    public <T> Stream<T> distinctSteam(String fieldName, Class<T> fieldType,
        BiFunction<Long, T, Boolean> terminator) {
      MongoCursor<T> it = terminator == null ? find(fieldName, fieldType).iterator()
          : new TerminableMongoCursor<>(find(fieldName, fieldType).iterator(), terminator);
      return streamOf(it).onClose(it::close);
    }

    /**
     * @see DistinctIterable#filter(Bson)
     */
    public MongoDistinction filter(Bson filter) {
      this.filter = filter;
      return this;
    }

    /**
     * @see DistinctIterable#filter(Bson)
     */
    public MongoDistinction filterMap(Map<String, ?> filter) {
      if (filter != null) {
        this.filter = Mongos.parse(filter);
      }
      return this;
    }

    /**
     * Returns the distinct values list of the specified field name and field type.
     *
     * @param <T> the type to cast any distinct items into.
     * @param fieldName the field name
     * @param fieldType the class to cast any distinct items into.
     */
    public <T> List<T> findDistinct(String fieldName, Class<T> fieldType) {
      try (MongoCursor<T> it = find(fieldName, fieldType).iterator()) {
        return streamOf(it).collect(Collectors.toList());
      }
    }

    /**
     * @see DistinctIterable#maxTime(long, TimeUnit)
     */
    public MongoDistinction maxTime(Duration maxTime) {
      this.maxTime = maxTime;
      return this;
    }

    protected MongoCollection<Document> obtainCollection() {
      return tpl.obtainCollection(collectionName);
    }

    protected ClientSession obtainSession() {
      return tpl.getSession();
    }

    <T> DistinctIterable<T> find(String fieldName, Class<T> fieldType) {
      MongoCollection<Document> mc = obtainCollection();
      if (codecRegistry != null) {
        mc = mc.withCodecRegistry(codecRegistry);
      }
      ClientSession session = obtainSession();
      DistinctIterable<T> di = session != null ? mc.distinct(session, fieldName, fieldType)
          : mc.distinct(fieldName, fieldType);
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
    protected int limit = -1;
    protected int skip = 0;
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
    protected CodecRegistry codecRegistry;

    protected MongoQuery(MongoTemplate tpl) {
      this.tpl = tpl;
    }

    /**
     * @see FindIterable#allowDiskUse(Boolean)
     */
    public MongoQuery allowDiskUse(Boolean allowDiskUse) {
      this.allowDiskUse = allowDiskUse;
      return this;
    }

    /**
     * Whether to append 'id' property whose value is derived from the existing "_id" property.
     *
     * @param autoSetIdField whether to append
     */
    public MongoQuery autoSetIdField(boolean autoSetIdField) {
      this.autoSetIdField = autoSetIdField;
      return this;
    }

    /**
     * @see FindIterable#batchSize(int)
     */
    public MongoQuery batchSize(Integer batchSize) {
      this.batchSize = batchSize;
      if (this.batchSize != null && this.batchSize < 1) {
        this.batchSize = 1;
      }
      return this;
    }

    /**
     * @see MongoCollection#withCodecRegistry(CodecRegistry)
     */
    public MongoQuery codecRegistry(CodecRegistry codecRegistry) {
      this.codecRegistry = codecRegistry;
      return this;
    }

    /**
     * @see FindIterable#collation(Collation)
     */
    public MongoQuery collation(Collation collation) {
      this.collation = collation;
      return this;
    }

    /**
     * Set the name of the collection which will be queried.
     *
     * @param collectionName the collection name
     */
    public MongoQuery collectionName(String collectionName) {
      this.collectionName = shouldNotNull(collectionName);
      return this;
    }

    /**
     * @see FindIterable#comment(BsonValue)
     */
    public MongoQuery comment(BsonValue comment) {
      this.comment = comment;
      return this;
    }

    /**
     * @see FindIterable#comment(String)
     */
    public MongoQuery comment(String comment) {
      this.comment = comment == null ? null : new BsonString(comment);
      return this;
    }

    /**
     * Counts the number of documents in the collection.
     */
    public long countDocuments() {
      return countDocuments(null);
    }

    /**
     * Counts the number of documents in the collection according to the given options.
     */
    public long countDocuments(CountOptions options) {
      MongoCollection<Document> mc = obtainCollection();
      return mc.countDocuments(defaultObject(filter, Document::new),
          defaultObject(options, CountOptions::new));
    }

    /**
     * @see FindIterable#cursorType(CursorType)
     */
    public MongoQuery cursorType(CursorType cursorType) {
      this.cursorType = cursorType;
      return this;
    }

    /**
     * @see FindIterable#explain(Class)
     */
    public MongoQuery explainResultClass(Class<?> explainResultClass) {
      this.explainResultClass = explainResultClass;
      return this;
    }

    /**
     * @see FindIterable#explain(ExplainVerbosity)
     */
    public MongoQuery explainVerbosity(ExplainVerbosity explainVerbosity) {
      this.explainVerbosity = explainVerbosity;
      return this;
    }

    /**
     * @see FindIterable#filter(Bson)
     */
    public MongoQuery filter(Bson filter) {
      this.filter = filter;
      return this;
    }

    /**
     * @see FindIterable#filter(Bson)
     */
    public MongoQuery filterMap(Map<String, ?> filter) {
      if (filter != null) {
        this.filter = Mongos.parse(filter);
      }
      return this;
    }

    /**
     * Return a list of documents in the collection according to the given options.
     */
    public List<Map<String, Object>> find() {
      try (MongoCursor<Document> it = query(Document.class).iterator()) {
        return streamOf(it).map(this::convert).collect(Collectors.toList());
      }
    }

    /**
     * Return a list of objects in the collection according to the given options.
     *
     * @param <T> the type to which the document is to be converted
     * @param clazz the document clazz
     *
     * @see MongoDatabase#getCollection(String, Class)
     * @see MongoCollection#withCodecRegistry(CodecRegistry)
     */
    public <T> List<T> find(final Class<T> clazz) {
      try (MongoCursor<T> it = query(clazz).iterator()) {
        return streamOf(it).collect(Collectors.toList());
      }
    }

    /**
     * Return a list of objects in the collection according to the given options.
     *
     * @param <T> the object type
     * @param clazz the clazz which the document will be converted to
     */
    public <T> List<T> findAs(Class<T> clazz) {
      try (MongoCursor<Document> it = query(Document.class).iterator()) {
        return streamOf(it).map(this::convert).map(x -> ObjectMappers.fromMap(x, clazz))
            .collect(Collectors.toList());
      }
    }

    /**
     * Return a list of objects in the collection according to the given options.
     *
     * @param <T> the object type
     * @param converter the converter use for document conversion
     */
    public <T> List<T> findAs(final Function<Document, T> converter) {
      try (MongoCursor<Document> it = query(Document.class).iterator()) {
        return streamOf(it).map(this::convert).map(converter).collect(Collectors.toList());
      }
    }

    /**
     * Return the document in the collection according to the given options.
     */
    public Map<String, Object> findOne() {
      try (MongoCursor<Document> it = query(Document.class).limit(1).iterator()) {
        return it.hasNext() ? it.next() : null;
      }
    }

    /**
     * Return the object in the collection according to the given options.
     *
     * @param <T> the type to which the document is to be converted
     * @param type the document class
     *
     * @see MongoDatabase#getCollection(String, Class)
     * @see MongoCollection#withCodecRegistry(CodecRegistry)
     */
    public <T> T findOne(Class<T> type) {
      try (MongoCursor<T> it = query(type).limit(1).iterator()) {
        return it.hasNext() ? it.next() : null;
      }
    }

    /**
     * Return the object in the collection according to the given options.
     *
     * @param <T> the type to which the document is to be converted
     * @param type the class to which the document is to be converted
     */
    public <T> T findOneAs(Class<T> type) {
      try (MongoCursor<Document> it = query(Document.class).limit(1).iterator()) {
        return it.hasNext() ? ObjectMappers.fromMap(convert(it.next()), type) : null;
      }
    }

    /**
     * @see FindIterable#hint(Bson)
     */
    public MongoQuery hint(Bson hint) {
      this.hint = hint;
      return this;
    }

    /**
     * @see FindIterable#hint(Bson)
     */
    public MongoQuery hintMap(Map<String, ?> hint) {
      return hint(Mongos.parse(hint));
    }

    /**
     * Limit the result size, default is 1. If the given limit <= 0 means don't limit the result
     * size.
     *
     * @param limit the result size.
     */
    public MongoQuery limit(int limit) {
      this.limit = limit;
      return this;
    }

    /**
     * @see FindIterable#max(Bson)
     */
    public MongoQuery max(Bson max) {
      this.max = max;
      return this;
    }

    /**
     * @see FindIterable#maxAwaitTime(long, TimeUnit)
     */
    public MongoQuery maxAwaitTime(Duration maxAwaitTime) {
      this.maxAwaitTime = maxAwaitTime;
      return this;
    }

    /**
     * @see FindIterable#max(Bson)
     */
    public MongoQuery maxMap(Map<String, ?> max) {
      return max(Mongos.parse(max));
    }

    /**
     * @see FindIterable#maxTime(long, TimeUnit)
     */
    public MongoQuery maxTime(Duration maxTime) {
      this.maxTime = maxTime;
      return this;
    }

    /**
     * @see FindIterable#min(Bson)
     */
    public MongoQuery min(Bson min) {
      this.min = min;
      return this;
    }

    /**
     * @see FindIterable#min(Bson)
     */
    public MongoQuery minMap(Map<String, ?> min) {
      return min(Mongos.parse(min));
    }

    /**
     * @see FindIterable#noCursorTimeout(boolean)
     */
    public MongoQuery noCursorTimeout(Boolean noCursorTimeout) {
      this.noCursorTimeout = noCursorTimeout;
      return this;
    }

    /**
     * @see FindIterable#partial(boolean)
     */
    public MongoQuery partial(Boolean partial) {
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
    public MongoQuery projection(boolean include, String... propertyNames) {
      if (include) {
        return projection(propertyNames, Strings.EMPTY_ARRAY);
      } else {
        return projection(Strings.EMPTY_ARRAY, propertyNames);
      }
    }

    /**
     * @see FindIterable#projection(Bson)
     */
    public MongoQuery projection(Bson projection) {
      this.projection = projection;
      return this;
    }

    /**
     * Set the projections
     *
     * @param includePropertyNames the include property names
     * @param excludePropertyNames the exclude property names
     */
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
        return projectionMap(projections);
      }
      return this;
    }

    /**
     * @see FindIterable#projection(Bson)
     */
    public MongoQuery projectionMap(Map<String, ?> projection) {
      return projection(Mongos.parse(projection));
    }

    /**
     * @see FindIterable#returnKey(boolean)
     */
    public MongoQuery returnKey(Boolean returnKey) {
      this.returnKey = returnKey;
      return this;
    }

    /**
     * @see FindIterable#showRecordId(boolean)
     */
    public MongoQuery showRecordId(Boolean showRecordId) {
      this.showRecordId = showRecordId;
      return this;
    }

    /**
     * @see FindIterable#comment(BsonValue)
     */
    public MongoQuery simpleComment(Object comment) {
      this.comment = Bsons.toSimpleBsonValue(comment);
      return this;
    }

    /**
     * @see FindIterable#skip(int)
     */
    public MongoQuery skip(int skip) {
      this.skip = Objects.max(skip, 0);
      return this;
    }

    /**
     * @see FindIterable#sort(Bson)
     */
    public MongoQuery sort(Bson sort) {
      this.sort = sort;
      return this;
    }

    /**
     * @see FindIterable#sort(Bson)
     */
    public MongoQuery sortMap(Map<String, ?> sort) {
      return sort(Mongos.parse(sort));
    }

    /**
     * Return a stream of documents in the collection according to the given options.
     * <p>
     * Note: the result set size is limited by {@link #limit(int)}
     */
    public Stream<Map<String, Object>> stream() {
      MongoCursor<Document> it = query(Document.class).iterator();
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
    public Stream<Map<String, Object>> stream(BiFunction<Long, Document, Boolean> terminator) {
      limit = -1; // NO LIMIT
      if (terminator == null) {
        return stream();
      }
      MongoCursor<Document> tit =
          new TerminableMongoCursor<>(query(Document.class).iterator(), terminator);
      return streamOf(tit).onClose(tit::close).map(this::convert);
    }

    /**
     * Converts and returns a collection of documents that satisfy the conditions based on the given
     * options and the given class.
     * <p>
     * Note: the result set size is limited by {@link #limit(int)}
     *
     * @param <T> the type to which the document is to be converted
     * @param clazz the document class
     *
     * @see MongoDatabase#getCollection(String, Class)
     * @see MongoCollection#withCodecRegistry(CodecRegistry)
     */
    public <T> Stream<T> stream(final Class<T> clazz) {
      MongoCursor<T> it = query(clazz).iterator();
      return streamOf(it).onClose(it::close);
    }

    /**
     * Return a stream of documents in the collection according to the given options, the stream may
     * be terminated by the given terminator when met the conditions.
     * <p>
     * Note: <b> The size of the result set is not limited by {@link #limit(int)} but by the given
     * {@code terminator}. </b>
     *
     * @param clazz the document class
     * @param terminator the terminator use to terminate the stream when meet the conditions.
     *
     * @see MongoDatabase#getCollection(String, Class)
     * @see MongoCollection#withCodecRegistry(CodecRegistry)
     */
    public <T> Stream<T> stream(Class<T> clazz, BiFunction<Long, T, Boolean> terminator) {
      limit = -1; // NO LIMIT
      if (terminator == null) {
        return stream(clazz);
      }
      MongoCursor<T> tit = new TerminableMongoCursor<>(query(clazz).iterator(), terminator);
      return streamOf(tit).onClose(tit::close);
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
      return streamAs(x -> ObjectMappers.fromMap(x, clazz));
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
    public <T> Stream<T> streamAs(final Function<Document, T> converter) {
      MongoCursor<Document> it = query(Document.class).iterator();
      return streamOf(it).onClose(it::close).map(this::convert).map(converter);
    }

    /**
     * @see FindIterable#let(Bson)
     */
    public MongoQuery variables(Bson variables) {
      this.variables = variables;
      return this;
    }

    /**
     * @see FindIterable#let(Bson)
     */
    public MongoQuery variablesMap(Map<String, ?> variables) {
      return variables(Mongos.parse(variables));
    }

    protected MongoCollection<Document> obtainCollection() {
      return tpl.obtainCollection(collectionName);
    }

    protected <T> MongoCollection<T> obtainCollection(Class<T> klass) {
      return tpl.obtainCollection(collectionName, klass);
    }

    protected ClientSession obtainSession() {
      return tpl.getSession();
    }

    Document convert(Document doc) {
      if (doc != null && autoSetIdField && !doc.containsKey(ENTITY_ID_FIELD_NAME)
          && doc.containsKey(DOC_ID_FIELD_NAME)) {
        doc.put(ENTITY_ID_FIELD_NAME, doc.get(DOC_ID_FIELD_NAME));
      }
      return doc;
    }

    <T> FindIterable<T> query(Class<T> klass) {
      MongoCollection<T> mc = obtainCollection(klass);
      if (codecRegistry != null) {
        mc = mc.withCodecRegistry(codecRegistry);
      }
      ClientSession session = obtainSession();
      FindIterable<T> fi = session != null ? mc.find(session) : mc.find();
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
      if (skip > 0) {
        fi.skip(skip);
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

  /**
   * corant-modules-mongodb
   *
   * @author bingo 11:47:40
   */
  public static class TerminableMongoCursor<T> implements MongoCursor<T> {

    protected final MongoCursor<T> delegate;
    protected final AtomicLong counter = new AtomicLong(0);
    protected final BiFunction<Long, T, Boolean> terminator;
    volatile T lastDoc;

    public TerminableMongoCursor(MongoCursor<T> delegate, BiFunction<Long, T, Boolean> terminator) {
      this.delegate = delegate;
      this.terminator = defaultObject(terminator, (i, t) -> false);
    }

    @Override
    public int available() {
      return delegate.available();
    }

    @Override
    public void close() {
      delegate.close();
    }

    @Override
    public ServerAddress getServerAddress() {
      return delegate.getServerAddress();
    }

    @Override
    public ServerCursor getServerCursor() {
      return delegate.getServerCursor();
    }

    @Override
    public boolean hasNext() {
      return delegate.hasNext() && !terminator.apply(counter.get(), lastDoc);
    }

    @Override
    public T next() {
      lastDoc = delegate.next();
      counter.incrementAndGet();
      return lastDoc;
    }

    @Override
    public T tryNext() {
      lastDoc = delegate.tryNext();
      counter.incrementAndGet();
      return lastDoc;
    }
  }
}

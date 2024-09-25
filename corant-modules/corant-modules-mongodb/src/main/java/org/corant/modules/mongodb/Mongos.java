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
package org.corant.modules.mongodb;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.corant.context.Beans.findNamed;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Objects.asString;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Streams.batchCollectStream;
import static org.corant.shared.util.Streams.batchStream;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.ref.Cleaner;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jakarta.enterprise.inject.literal.NamedLiteral;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonObjectId;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.corant.modules.bson.Bsons;
import org.corant.modules.bson.ExtendedCodecProvider;
import org.corant.modules.json.ObjectMappers;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.result.UpdateResult;

/**
 * corant-modules-mongodb
 *
 * @author bingo 下午7:18:23
 */
public class Mongos {

  public static final String DOC_ID_FIELD_NAME = "_id";
  public static final String ENTITY_ID_FIELD_NAME = "id";
  public static final String GFS_DOC_COLLECTION_SUFFIX = ".files";
  public static final String GFS_METADATA_PROPERTY_NAME = "metadata";
  public static final UpdateResult EMPTY_UPDATE_RESULT = new EmptyUpdateResult();

  public static final DocumentCodecProvider DEFAULT_DOCUMENT_CODEC_PROVIDER =
      new DocumentCodecProvider(Bsons.DEFAULT_BSON_TYPE_CLASS_MAP, new MongoBsonTransformer());

  public static final CodecRegistry DEFAULT_CODEC_REGISTRY =
      fromRegistries(fromProviders(DEFAULT_DOCUMENT_CODEC_PROVIDER, new ExtendedCodecProvider()),
          MongoClientSettings.getDefaultCodecRegistry());

  static final ObjectMapper WRITE_OBJECT_MAPPER = ObjectMappers.copyForwardingObjectMapper();
  static final ObjectMapper READ_OBJECT_MAPPER = ObjectMappers.copyDefaultObjectMapper();
  static final JavaType WRITE_DOC_MAP_TYPE =
      WRITE_OBJECT_MAPPER.constructType(new TypeReference<Map<String, Object>>() {});
  static {
    WRITE_OBJECT_MAPPER.setVisibility(WRITE_OBJECT_MAPPER.getSerializationConfig()
        .getDefaultVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withSetterVisibility(JsonAutoDetect.Visibility.NONE));

    READ_OBJECT_MAPPER.setVisibility(READ_OBJECT_MAPPER.getDeserializationConfig()
        .getDefaultVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
        .withSetterVisibility(JsonAutoDetect.Visibility.NONE));
  }

  public static BsonValue bsonId(Object id) {
    if (id == null) {
      return null;
    }
    if (id instanceof Long) {
      return new BsonInt64((Long) id);
    } else if (id instanceof Integer) {
      return new BsonInt32((Integer) id);
    } else if (id instanceof BsonObjectId boid) {
      return boid;
    } else if (id instanceof ObjectId oid) {
      return new BsonObjectId(oid);
    } else {
      return new BsonString(asString(id));
    }
  }

  public static void cloneCollection(String srcDatabaseNameSpace, String destDatabaseNameSpace,
      String collectionName) {
    copyCollection(srcDatabaseNameSpace, destDatabaseNameSpace, collectionName, collectionName,
        null, 256, null);
  }

  public static void cloneDatabase(String srcDatabaseNameSpace, String destDatabaseNameSpace,
      int batchSize, BiConsumer<String, List<Document>> consumer) {
    MongoDatabase s = resolve(MongoDatabase.class, NamedLiteral.of(srcDatabaseNameSpace));
    MongoDatabase d = resolve(MongoDatabase.class, NamedLiteral.of(destDatabaseNameSpace));
    final BiConsumer<String, List<Document>> useConsumer = consumer == null ? (cn, docs) -> {
    } : consumer;
    for (String c : s.listCollectionNames()) {
      MongoCollection<Document> dest = d.getCollection(c);
      batchStream(batchSize, s.getCollection(c).find().batchSize(batchSize)).forEach(b -> {
        b.forEach(cs -> cs.append(c, d));
        dest.insertMany(b);
        useConsumer.accept(c, b);
      });
    }
  }

  public static void copyCollection(MongoCollection<Document> srcCollection,
      MongoCollection<Document> destCollection, Supplier<Bson> filter, int batchSize,
      Consumer<Document> consumer) {
    Bson useFilter = filter == null ? null : filter.get();
    if (useFilter == null) {
      batchStream(batchSize, srcCollection.find().batchSize(batchSize)).forEach(b -> {
        if (consumer != null) {
          b.forEach(consumer);
        }
        destCollection.insertMany(b);
      });
    } else {
      batchStream(batchSize, srcCollection.find(useFilter).batchSize(batchSize)).forEach(b -> {
        if (consumer != null) {
          b.forEach(consumer);
        }
        destCollection.insertMany(b);
      });
    }
  }

  public static void copyCollection(String srcDatabaseNameSpace, String destDatabaseNameSpace,
      String srcCollectionName, String destCollectionName, Supplier<Bson> filter, int batchSize,
      Consumer<Document> consumer) {
    MongoDatabase s = resolve(MongoDatabase.class, NamedLiteral.of(srcDatabaseNameSpace));
    MongoDatabase d = resolve(MongoDatabase.class, NamedLiteral.of(destDatabaseNameSpace));
    copyCollection(s.getCollection(srcCollectionName), d.getCollection(destCollectionName), filter,
        batchSize, consumer);
  }

  public static void copyCollection(String srcDatabaseNameSpace, String destDatabaseNameSpace,
      String collectionName, Supplier<Bson> filter, int batchSize, Consumer<Document> consumer) {
    copyCollection(srcDatabaseNameSpace, destDatabaseNameSpace, collectionName, collectionName,
        filter, batchSize, consumer);
  }

  public static void copyDatabase(String srcDatabaseNameSpace, String destDatabaseNameSpace,
      int batchSize, BiConsumer<String, List<Document>> consumer, String... collections) {
    MongoDatabase s = resolve(MongoDatabase.class, NamedLiteral.of(srcDatabaseNameSpace));
    MongoDatabase d = resolve(MongoDatabase.class, NamedLiteral.of(destDatabaseNameSpace));
    final BiConsumer<String, List<Document>> useConsumer = consumer == null ? (cn, docs) -> {
    } : consumer;
    for (String c : setOf(collections)) {
      MongoCollection<Document> dest = d.getCollection(c);
      batchCollectStream(batchSize, streamOf(s.getCollection(c).find().batchSize(batchSize)))
          .forEach(b -> {
            b.forEach(cs -> cs.append(c, d));
            dest.insertMany(b);
            useConsumer.accept(c, b);
          });
    }
  }

  public static void copyGridFSBucket(String srcDatabaseNameSpace, String destDatabaseNameSpace,
      String srcGridFSBucketName, String destGridFSBucketName, Bson filter, int batchSize) {
    MongoDatabase s = resolve(MongoDatabase.class, NamedLiteral.of(srcDatabaseNameSpace));
    MongoDatabase d = resolve(MongoDatabase.class, NamedLiteral.of(destDatabaseNameSpace));
    GridFSBucket sg = GridFSBuckets.create(s, srcGridFSBucketName);
    GridFSBucket dg = GridFSBuckets.create(d, destGridFSBucketName);
    batchStream(batchSize, filter == null ? sg.find() : sg.find(filter))
        .forEach(gfses -> gfses.forEach(gf -> {
          try (GridFSDownloadStream gfos = sg.openDownloadStream(gf.getId())) {
            dg.uploadFromStream(gf.getId(), gf.getFilename(), gfos, new GridFSUploadOptions()
                .metadata(new Document(defaultObject(gf.getMetadata(), Collections::emptyMap))));
          }
        }));
  }

  public static void copyGridFSBucket(String srcDatabaseNameSpace, String destDatabaseNameSpace,
      String srcGridFSBucketName, String destGridFSBucketName, int batchSize) {
    copyGridFSBucket(srcDatabaseNameSpace, destDatabaseNameSpace, srcGridFSBucketName,
        destGridFSBucketName, null, batchSize);
  }

  public static MongoClient resolveClient(String uri) {
    if (isNotBlank(uri) && (uri.startsWith("mongodb+srv://") || uri.startsWith("mongodb://"))) {
      ConnectionString cs = new ConnectionString(uri);
      return MongoClients.create(cs);
    } else {
      return null;
    }
  }

  public static MongoDatabase resolveDatabase(String database) {
    if (isNotBlank(database)
        && (database.startsWith("mongodb+srv://") || database.startsWith("mongodb://"))) {
      ConnectionString cs = new ConnectionString(database);
      if (cs.getDatabase() == null) {
        return null;
      }
      MongoClient client = MongoClients.create(cs);
      MongoDatabase mongoDatabase = client.getDatabase(cs.getDatabase());
      // Do we need to automatically close the mongodb client here?
      Cleaner.create().register(mongoDatabase, client::close);
      return mongoDatabase;
    } else {
      return findNamed(MongoDatabase.class, database).orElse(null);
    }
  }

  /**
   * Returns a {@link Document} with primary key (named '_id') from the given object for persisting.
   * <p>
   * Note: If the given object is a Map instance, we assume it is a {@code Map<String, Object>}. If
   * the given object has a property named 'id', the value of the property will be used as the
   * primary key (named '_id') of the {@link Document} and the resolved document doesn't retain the
   * id property.
   *
   * @param <T> the object type
   * @param object the object to be resolved
   * @return a document
   */
  public static <T> Document resolveDocument(T object) {
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
        map = WRITE_OBJECT_MAPPER.convertValue(object, WRITE_DOC_MAP_TYPE);
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
   * Returns an entity from the given document {@code doc} with the given entity type class.
   * <p>
   * Note: If the given document does not contain a key with the name 'id', but contains a key with
   * the name '_id', then the value of the key with the name 'id' is automatically set to the value
   * of the key with the name '_id'.
   *
   * @param <T> the entity type
   * @param doc the doc to be resolved
   * @param clazz the entity class
   * @return an entity object
   *
   * @see #resolveObject(Document, Class, boolean)
   */
  public static <T> T resolveEntity(Document doc, Class<T> clazz) {
    return resolveObject(doc, clazz, true);
  }

  /**
   * Returns an object from the given document {@code doc} with the given object type class. The
   * entity parameter indicates whether the returned object type is an entity, i.e., contains the id
   * attribute.
   * <p>
   * Note: IF the returned object is entity and the given document does not contain a key with the
   * name 'id', but contains a key with the name '_id', then the value of the key with the name 'id'
   * is automatically set to the value of the key with the name '_id'.
   * <p>
   * Note: In the current implementation, the nanoseconds of the date time may be lost.
   *
   * @param <T> the returned object type
   * @param doc the doc to be resolved
   * @param clazz the returned object class
   * @param entity whether the returned object is an entity
   * @return an object
   */
  @SuppressWarnings("unchecked")
  public static <T> T resolveObject(Document doc, Class<T> clazz, boolean entity) {
    if (clazz == null) {
      throw new IllegalArgumentException("Class can't null");
    } else if (doc == null) {
      return null;
    } else if (clazz.isInstance(doc)) {
      return (T) doc;
    } else if (entity && !doc.containsKey(ENTITY_ID_FIELD_NAME)
        && doc.containsKey(DOC_ID_FIELD_NAME)) {
      Document newDoc = new Document(doc);
      newDoc.put(ENTITY_ID_FIELD_NAME, doc.get(DOC_ID_FIELD_NAME));
      return ObjectMappers.fromMap(newDoc, clazz);
    } else {
      return ObjectMappers.fromMap(doc, clazz);
    }
  }

  static Bson parse(Map<String, ?> object) {
    return object == null ? null : object instanceof Bson ? (Bson) object : new Document(object);
  }

  public static class EmptyUpdateResult extends UpdateResult {

    @Override
    public long getMatchedCount() {
      return 0;
    }

    @Override
    public long getModifiedCount() {
      return 0;
    }

    @Override
    public BsonValue getUpsertedId() {
      return null;
    }

    @Override
    public boolean wasAcknowledged() {
      return true;
    }

  }
}

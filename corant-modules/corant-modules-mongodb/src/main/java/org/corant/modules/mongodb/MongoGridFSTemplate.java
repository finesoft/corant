/*
 * Copyright (c) 2013-2023, Bingo.Chen (finesoft@gmail.com).
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

import static org.corant.modules.mongodb.Mongos.DOC_ID_FIELD_NAME;
import static org.corant.modules.mongodb.Mongos.EMPTY_UPDATE_RESULT;
import static org.corant.modules.mongodb.Mongos.GFS_DOC_COLLECTION_SUFFIX;
import static org.corant.modules.mongodb.Mongos.GFS_METADATA_PROPERTY_NAME;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Streams.streamOf;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.resource.Resource;
import org.corant.shared.util.Objects;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

/**
 * corant-modules-mongodb
 *
 * @author bingo 14:52:13
 */
public class MongoGridFSTemplate {

  protected final MongoDatabase database;
  protected WriteConcern writeConcern;
  protected ReadPreference readPreference;
  protected ReadConcern readConcern;
  protected ClientSession session;

  public MongoGridFSTemplate(MongoClient mongoClient, String databaseName) {
    this(mongoClient.getDatabase(databaseName));
  }

  public MongoGridFSTemplate(MongoDatabase database) {
    this(database, null, null, null, null);
  }

  public MongoGridFSTemplate(MongoDatabase database, ClientSession session) {
    this(database, null, session);
  }

  public MongoGridFSTemplate(MongoDatabase database, ReadPreference readPreference,
      ReadConcern readConcern) {
    this(database, null, readPreference, readConcern, null);
  }

  public MongoGridFSTemplate(MongoDatabase database, ReadPreference readPreference,
      ReadConcern readConcern, ClientSession session) {
    this(database, null, readPreference, readConcern, session);
  }

  public MongoGridFSTemplate(MongoDatabase database, WriteConcern writeConcern) {
    this(database, writeConcern, null);
  }

  public MongoGridFSTemplate(MongoDatabase database, WriteConcern writeConcern,
      ClientSession session) {
    this(database, writeConcern, null, null, session);
  }

  public MongoGridFSTemplate(MongoDatabase database, WriteConcern writeConcern,
      ReadPreference readPreference, ReadConcern readConcern, ClientSession session) {
    this.database = database;
    this.writeConcern = writeConcern;
    this.readPreference = readPreference;
    this.readConcern = readConcern;
    this.session = session;
  }

  public MongoGridFSTemplate(String mongoURL) {
    this(Mongos.resolveDatabase(mongoURL));
  }

  /**
   * Given an id, delete this stored file's files collection document and associated chunks from a
   * GridFS bucket.
   *
   * @param bucketName the bucket name
   * @param id the GridFSFile id
   */
  public void delete(String bucketName, Object id) {
    GridFSBucket bucket = getBucket(bucketName);
    if (session != null) {
      bucket.delete(session, Mongos.bsonId(id));
    } else {
      bucket.delete(Mongos.bsonId(id));
    }
  }

  /**
   * Removes all stored file's files collection documents and associated chunks from the given
   * bucket that match the given query filter. If no files match, the bucket is not modified.
   *
   * @param bucketName name of the collection to store the object in. Must not be {@literal null}.
   * @param filter the query filter to apply the delete operation
   */
  public void deleteMany(String bucketName, Bson filter) {
    final GridFSBucket bucket = getBucket(bucketName);
    if (session != null) {
      new MongoGridFSQuery(this, bucket).filter(filter).find()
          .forEach(f -> bucket.delete(session, f.getId()));
    } else {
      new MongoGridFSQuery(this, bucket).filter(filter).find()
          .forEach(f -> bucket.delete(f.getId()));
    }
  }

  /**
   * Removes all stored file's files collection documents and associated chunks from the given
   * bucket that match the given query filter. If no files match, the bucket is not modified.
   *
   * @param bucketName name of the collection to store the object in. Must not be {@literal null}.
   * @param filter the query filter to apply the delete operation
   */
  public void deleteMany(String bucketName, Map<String, ?> filter) {
    deleteMany(bucketName, Mongos.parse(shouldNotNull(filter, "Filter can't null")));
  }

  /**
   * Drops the data associated with this bucket from the database.
   *
   * @param bucketName the bucket name to be dropped
   */
  public void dropBucket(String bucketName) {
    if (session != null) {
      getBucket(bucketName).drop(session);
    } else {
      getBucket(bucketName).drop();
    }
  }

  /**
   * Update file metadata in a specified bucket.
   *
   * @param bucketName the bucket name
   * @param docCollectionName the file document collection name
   * @param metadataFieldName the file document metadata field name
   * @param id the file id
   * @param updater the updater, the input parameter is original metadata and the output is new
   *        metadata
   */
  public UpdateResult executeUpdateMetadata(String bucketName, String docCollectionName,
      String metadataFieldName, Object id, UnaryOperator<Map<String, Object>> updater) {
    String collectionName =
        defaultObject(docCollectionName, () -> bucketName.concat(GFS_DOC_COLLECTION_SUFFIX));
    String fieldName = defaultObject(metadataFieldName, GFS_METADATA_PROPERTY_NAME);
    Bson filter = Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(id));
    Document doc = obtainCollection(collectionName).find(filter).first();
    if (doc != null) {
      Document olds = (Document) doc.get(fieldName);
      Map<String, Object> news = updater.apply(olds);
      return obtainCollection(collectionName).updateOne(filter, Updates.set(fieldName, news));
    }
    return EMPTY_UPDATE_RESULT;
  }

  /**
   * Returns whether the file associated with the given id and bucket is existing.
   *
   * @param bucketName the bucket name
   * @param id the GridFS id
   */
  public boolean exists(String bucketName, Object id) {
    try (MongoCursor<GridFSFile> it = getBucket(bucketName)
        .find(Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(id))).limit(1).iterator()) {
      return it.hasNext();
    }
  }

  /**
   * Returns a bucket a custom bucket name
   *
   * @param bucketName the bucket to be returned
   */
  public GridFSBucket getBucket(String bucketName) {
    GridFSBucket bucket = bucketName == null ? GridFSBuckets.create(database)
        : GridFSBuckets.create(database, bucketName);
    if (writeConcern != null) {
      bucket = bucket.withWriteConcern(writeConcern);
    }
    if (readPreference != null) {
      bucket = bucket.withReadPreference(readPreference);
    }
    if (readConcern != null) {
      bucket = bucket.withReadConcern(readConcern);
    }
    return bucket;
  }

  public MongoDatabase getDatabase() {
    return database;
  }

  /**
   * Returns a MongoGridFSResource with the given id and bucket
   *
   * @param bucketName the bucket name
   * @param id the GridFS file id
   */
  public MongoGridFSResource getGridFSResource(String bucketName, Object id) {
    final GridFSBucket bucket = getBucket(bucketName);
    try (MongoCursor<GridFSFile> it =
        bucket.find(Filters.eq(DOC_ID_FIELD_NAME, Mongos.bsonId(id))).iterator()) {
      if (it.hasNext()) {
        return new MongoGridFSResource(bucket, it.next());
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
   * Returns a new {@link MongoGridFSQuery} instance.
   */
  public MongoGridFSQuery query(String bucketName) {
    shouldNotNull(bucketName, "Bucket name must not be null");
    return new MongoGridFSQuery(this, bucketName);
  }

  /**
   * Save the contents of the given {@link InputStream} to a GridFS bucket.
   *
   * @param bucketName the name of bucket to save the contents
   * @param is the Stream providing the file data
   * @param id the custom id value of the file
   * @param name the filename for the stream
   * @param chunkSizeBytes the number of bytes per chunk for the stored file, less than equal 0
   *        means use default setting
   * @param metadata a metadata to stored alongside the filename in the files collection
   */
  public void save(String bucketName, InputStream is, Object id, String name, int chunkSizeBytes,
      Map<String, Object> metadata) {
    shouldNotNull(is, "Input stream can't null");
    GridFSBucket bucket = getBucket(bucketName);
    final GridFSUploadOptions options =
        new GridFSUploadOptions().chunkSizeBytes(chunkSizeBytes > 0 ? chunkSizeBytes : null)
            .metadata(metadata == null ? null : new Document(metadata));
    if (session != null) {
      bucket.uploadFromStream(session, Mongos.bsonId(id), name, is, options);
    } else {
      bucket.uploadFromStream(Mongos.bsonId(id), name, is, options);
    }
  }

  /**
   * Save the contents of the given {@link Resource} to a GridFS bucket, use default check size.
   *
   * @param bucketName the name of bucket to save the contents
   * @param resource the Resource providing the file data
   * @param id the custom id value of the file
   */
  public void save(String bucketName, Resource resource, Object id) {
    save(bucketName, resource, id, -1);
  }

  /***
   * Save the contents of the given {@link Resource} to a GridFS bucket.
   *
   * @param bucketName the name of bucket to save the contents
   * @param resource the Resource providing the file data
   * @param id the custom id value of the file
   * @param chunkSizeBytes the number of bytes per chunk for the stored file, less than equal 0
   *        means use default setting
   */
  public void save(String bucketName, Resource resource, Object id, int chunkSizeBytes) {
    shouldNotNull(resource, "Resource can't null");
    try (InputStream is = resource.openInputStream()) {
      save(bucketName, is, id, resource.getName(), chunkSizeBytes, resource.getMetadata());
    } catch (IOException e) {
      throw new CorantRuntimeException(e);
    }
  }

  /**
   * Update file metadata in a specified bucket.
   *
   * @param bucketName the bucket name
   * @param id the file id
   * @param newMetadata the metadata to be set
   */
  public UpdateResult updateMetadata(String bucketName, Object id,
      Map<String, Object> newMetadata) {
    return updateMetadata(bucketName, id, md -> {
      md.clear();
      if (newMetadata != null) {
        md.putAll(newMetadata);
      }
      return md;
    });
  }

  /**
   * Update file metadata in a specified bucket.
   *
   * @param bucketName the bucket name
   * @param id the file id
   * @param updater the updater, the input parameter is original metadata and the output is new
   *        metadata
   */
  public UpdateResult updateMetadata(String bucketName, Object id,
      UnaryOperator<Map<String, Object>> updater) {
    return executeUpdateMetadata(bucketName, null, null, id, updater);
  }

  public MongoGridFSTemplate withReadConcern(ClientSession session) {
    return new MongoGridFSTemplate(database, writeConcern, readPreference, readConcern, session);
  }

  public MongoGridFSTemplate withReadConcern(ReadConcern readConcern) {
    return new MongoGridFSTemplate(database, writeConcern, readPreference, readConcern, session);
  }

  public MongoGridFSTemplate withReadPreference(ReadPreference readPreference) {
    return new MongoGridFSTemplate(database, writeConcern, readPreference, readConcern, session);
  }

  public MongoGridFSTemplate withWriteConcern(WriteConcern writeConcern) {
    return new MongoGridFSTemplate(database, writeConcern, readPreference, readConcern, session);
  }

  protected MongoCollection<Document> obtainCollection(String collectionName) {
    MongoCollection<Document> collection = database.getCollection(collectionName);
    if (writeConcern != null) {
      collection = collection.withWriteConcern(writeConcern);
    }
    if (readPreference != null) {
      collection = collection.withReadPreference(readPreference);
    }
    if (readConcern != null) {
      collection = collection.withReadConcern(readConcern);
    }
    return collection;
  }

  /**
   * corant-modules-mongodb
   *
   * @author bingo 18:58:27
   */
  public static class MongoGridFSQuery {
    protected final MongoGridFSTemplate tpl;
    protected final GridFSBucket bucket;
    protected Bson filter;
    protected int limit = -1;
    protected int skip = 0;
    protected Bson sort;
    protected Boolean noCursorTimeout;
    protected Duration maxTime;
    protected Collation collation;
    protected Integer batchSize;

    public MongoGridFSQuery(MongoGridFSTemplate tpl, GridFSBucket bucket) {
      this.tpl = tpl;
      this.bucket = bucket;
    }

    public MongoGridFSQuery(MongoGridFSTemplate tpl, String bucketName) {
      this(tpl, tpl.getBucket(bucketName));
    }

    /**
     * @see GridFSFindIterable#batchSize(int)
     */
    public MongoGridFSQuery batchSize(Integer batchSize) {
      this.batchSize = batchSize;
      if (this.batchSize != null && this.batchSize < 1) {
        this.batchSize = 1;
      }
      return this;
    }

    /**
     * @see GridFSFindIterable#collation(Collation)
     */
    public MongoGridFSQuery collation(Collation collation) {
      this.collation = collation;
      return this;
    }

    /**
     * @see GridFSFindIterable#filter(Bson)
     */
    public MongoGridFSQuery filter(Bson filter) {
      this.filter = filter;
      return this;
    }

    /**
     * @see GridFSFindIterable#filter(Bson)
     */
    public MongoGridFSQuery filterMap(Map<String, ?> filter) {
      if (filter != null) {
        this.filter = Mongos.parse(filter);
      }
      return this;
    }

    /**
     * Return a list of GridFSFile in the bucket according to the given options.
     */
    public List<GridFSFile> find() {
      try (MongoCursor<GridFSFile> it = query().iterator()) {
        return streamOf(it).collect(Collectors.toList());
      }
    }

    /**
     * Return a list of MongoGridFSResource in the bucket according to the given options.
     */
    public List<MongoGridFSResource> findGridFSResources() {
      try (MongoCursor<GridFSFile> it = query().iterator()) {
        return streamOf(it).map(f -> new MongoGridFSResource(bucket, f))
            .collect(Collectors.toList());
      }
    }

    /**
     * Return the GridFSFile in the bucket according to the given options.
     */
    public GridFSFile findOne() {
      try (MongoCursor<GridFSFile> it = query().limit(1).iterator()) {
        return it.hasNext() ? it.next() : null;
      }
    }

    /**
     * Return the MongoGridFSResource in the bucket according to the given options.
     */
    public MongoGridFSResource findOneGridFSResource() {
      try (MongoCursor<GridFSFile> it = query().limit(1).iterator()) {
        return it.hasNext() ? new MongoGridFSResource(bucket, it.next()) : null;
      }
    }

    /**
     * Limit the result size, default is 1. If the given limit <= 0 means don't limit the result
     * size.
     *
     * @param limit the result size.
     */
    public MongoGridFSQuery limit(int limit) {
      this.limit = limit;
      return this;
    }

    /**
     * @see GridFSFindIterable#maxTime(long, TimeUnit)
     */
    public MongoGridFSQuery maxTime(Duration maxTime) {
      this.maxTime = maxTime;
      return this;
    }

    /**
     * @see GridFSFindIterable#noCursorTimeout(boolean)
     */
    public MongoGridFSQuery noCursorTimeout(Boolean noCursorTimeout) {
      this.noCursorTimeout = noCursorTimeout;
      return this;
    }

    /**
     * @see GridFSFindIterable#skip(int)
     */
    public MongoGridFSQuery skip(int skip) {
      this.skip = Objects.max(skip, 0);
      return this;
    }

    /**
     * @see GridFSFindIterable#sort(Bson)
     */
    public MongoGridFSQuery sort(Bson sort) {
      this.sort = sort;
      return this;
    }

    /**
     * @see GridFSFindIterable#sort(Bson)
     */
    public MongoGridFSQuery sortMap(Map<String, ?> sort) {
      return sort(Mongos.parse(sort));
    }

    GridFSFindIterable query() {
      ClientSession session = tpl.getSession();
      GridFSFindIterable fi = session != null ? bucket.find(session) : bucket.find();
      if (filter != null) {
        fi.filter(filter);
      }
      if (sort != null) {
        fi.sort(sort);
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
      if (batchSize != null) {
        fi.batchSize(batchSize);
      }
      if (noCursorTimeout != null) {
        fi.noCursorTimeout(noCursorTimeout);
      }
      return fi;
    }

  }
}

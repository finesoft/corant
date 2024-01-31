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

import static org.corant.context.Beans.findNamed;
import static org.corant.context.Beans.resolve;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Streams.batchCollectStream;
import static org.corant.shared.util.Streams.batchStream;
import static org.corant.shared.util.Streams.streamOf;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.ref.Cleaner;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jakarta.enterprise.inject.literal.NamedLiteral;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;

/**
 * corant-modules-mongodb
 *
 * @author bingo 下午7:18:23
 */
public class Mongos {

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
      MongoClient client = MongoClients.create(cs);
      MongoDatabase mongoDatabase = client.getDatabase(cs.getDatabase());
      if (mongoDatabase != null) {
        Cleaner.create().register(mongoDatabase, () -> {
          if (client != null) {
            client.close(); // Do we need to automatically close the mongodb client here
          }
        });
      }
      return mongoDatabase;
    } else {
      return findNamed(MongoDatabase.class, database).orElse(null);
    }
  }

}

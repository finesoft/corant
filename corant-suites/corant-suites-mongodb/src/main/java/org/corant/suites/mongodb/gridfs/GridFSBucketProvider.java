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
package org.corant.suites.mongodb.gridfs;

import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Assertions.shouldNotBlank;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.corant.shared.normal.Names;
import org.corant.suites.mongodb.MongoClientExtension;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

/**
 * corant-suites-mongodb
 *
 * @author bingo 下午4:19:59
 *
 */
@ApplicationScoped
public class GridFSBucketProvider {

  protected static final Map<String, GridFSBucket> gridFsBuckets = new ConcurrentHashMap<>();

  @Inject
  MongoClientExtension extension;

  public GridFSBucket getBucket(String key) {
    int pos = shouldNotBlank(key).lastIndexOf(Names.NAME_SPACE_SEPARATOR);
    shouldBeTrue(pos > 0 && key.length() > pos);
    return getBucket(key.substring(0, pos), key.substring(pos));
  }

  public GridFSBucket getBucket(String databaseName, String bucketName) {
    final String key = String.join(".", databaseName, bucketName);
    return gridFsBuckets.computeIfAbsent(key, (k) -> {
      MongoClient mc = extension.getClient(databaseName);
      if (mc != null) {
        MongoDatabase stroage = mc.getDatabase(databaseName);
        return GridFSBuckets.create(stroage, bucketName);
      }
      return null;
    });
  }

}

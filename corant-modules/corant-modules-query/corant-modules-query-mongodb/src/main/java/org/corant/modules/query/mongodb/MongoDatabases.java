/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.ref.Cleaner;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * corant-modules-query-mongodb
 *
 * @author bingo 上午11:22:24
 *
 */
public class MongoDatabases {

  public static MongoDatabase resolveDatabase(String database) {
    if (isNotBlank(database)
        && (database.startsWith("mongodb+srv://") || database.startsWith("mongodb://"))) {
      ConnectionString cs = new ConnectionString(database);
      MongoClient client = MongoClients.create(cs);
      MongoDatabase mongoDatabase = client.getDatabase(cs.getDatabase());
      if (mongoDatabase != null) {
        Cleaner.create().register(mongoDatabase, () -> {
          if (client != null) {
            client.close(); // FIXME Do we need to automatically close the mongodb client here
          }
        });
      }
      return mongoDatabase;
    } else {
      return findNamed(MongoDatabase.class, database).orElse(null);
    }
  }
}

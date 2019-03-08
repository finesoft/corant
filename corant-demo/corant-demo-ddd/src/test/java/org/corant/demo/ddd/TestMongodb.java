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
package org.corant.demo.ddd;

import static org.corant.shared.util.MapUtils.asMap;
import static org.corant.shared.util.MapUtils.getMapInstant;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Named;
import org.bson.Document;
import org.corant.devops.test.unit.CorantJUnit4ClassRunner;
import org.corant.kernel.util.Unnamed;
import org.corant.suites.mongodb.MongoClientExtension;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;

/**
 * corant-demo-ddd
 *
 * @author bingo 下午8:52:43
 *
 */
@RunWith(CorantJUnit4ClassRunner.class)
public class TestMongodb {

  @Inject
  @Unnamed
  MongoClient client;

  @Inject
  @Named("12")
  MongoClient client1;

  @Inject
  @Named("CADB-DATA")
  MongoDatabase db;

  @Inject
  @Named("CADB-FILE.fs")
  GridFSBucket bucket;

  @Test
  public void fake() {

  }

  // @Test
  public void test() {
    MongoCollection<Document> coll = db.getCollection("articlePublishInfo");
    for (Document doc : coll.find()) {
      System.out.println(doc.get("_id"));
    }
  }

  @Test
  public void testFs() throws FileNotFoundException {
    System.out.println(
        db.runCommand(new Document(asMap("serverStatus", 1, "repl", 0, "metrics", 0, "locks", 0)))
            .toJson());

    Instant obj = getMapInstant(
        db.runCommand(new Document(asMap("serverStatus", 1, "repl", 0, "metrics", 0, "locks", 0))),
        "localTime");

    System.out.println(obj.toEpochMilli());

    bucket.downloadToStream(MongoClientExtension.bsonId(302630402751721472L),
        new FileOutputStream(new File("d:/xxxx2.jpg")));
  }
}

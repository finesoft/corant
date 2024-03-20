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

import static org.corant.shared.util.Maps.mapOf;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.corant.shared.resource.FileSystemResource;
import org.corant.shared.util.Streams;
import org.junit.Test;

/**
 * corant-modules-mongodb
 *
 * @author bingo 12:02:53
 */
public class MongoGridFSTemplateTest {

  @Test
  public void testDelete() {
    MongoGridFSTemplate mt =
        new MongoGridFSTemplate(Mongos.resolveClient(MongoTemplateTest.murl).getDatabase("anncy"));
    mt.delete("tpl-bucket", 2);
    mt.deleteMany("tpl-bucket", mapOf("_id", mapOf("$gt", 5)));
  }

  @Test
  public void testExists() {
    MongoGridFSTemplate mt =
        new MongoGridFSTemplate(Mongos.resolveClient(MongoTemplateTest.murl).getDatabase("anncy"));
    System.out.println(mt.exists("tpl-bucket", 2));
    System.out.println(mt.exists("tpl-bucket", "2"));
  }

  @Test
  public void testQuery() throws IOException {
    MongoGridFSTemplate mt =
        new MongoGridFSTemplate(Mongos.resolveClient(MongoTemplateTest.murl).getDatabase("anncy"));
    try (InputStream is = mt.getGridFSResource("tpl-bucket", 5).openInputStream();
        FileOutputStream fos = new FileOutputStream(new File("d:/xasdas.txt"))) {
      Streams.copy(is, fos);
    }
    mt.query("tpl-bucket").filterMap(mapOf("_id", mapOf("$gte", 3))).find().forEach(gf -> {
      System.out.println(gf.getFilename() + "\t" + gf.getLength());
    });

    mt.query("tpl-bucket").filterMap(mapOf("_id", mapOf("$gte", 3))).findGridFSResources()
        .forEach(gf -> {
          try {
            System.out.println(gf.getGridFSFile().getFilename() + "\t" + gf.getBytes().length);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }

  @Test
  public void testSave() {
    MongoGridFSTemplate mt =
        new MongoGridFSTemplate(Mongos.resolveClient(MongoTemplateTest.murl).getDatabase("anncy"));
    mt.save("tpl-bucket", new FileSystemResource(new File("D:\\test\\brd.txt")), 4);
    mt.save("tpl-bucket",
        new FileSystemResource(new File("D:\\test\\brd.txt"), mapOf("a1", "a1", "a2", "a2")), 5);
    mt.save("tpl-bucket",
        new FileSystemResource(new File("D:\\test\\brd.txt"), mapOf("a1", "a1", "a2", "a2")), 6,
        1024000);
  }

  @Test
  public void testUpdateMetadata() {
    MongoGridFSTemplate mt =
        new MongoGridFSTemplate(Mongos.resolveClient(MongoTemplateTest.murl).getDatabase("anncy"));
    mt.updateMetadata("tpl-bucket", 3, mapOf("1a", "1a", "2a", "2a"));
    mt.updateMetadata("tpl-bucket", 5, m -> {
      m.put("adsasd", "adsadasd");
      return m;
    });
  }
}

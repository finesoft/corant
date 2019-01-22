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
package org.corant.suites.elastic;

import static org.corant.shared.util.MapUtils.asMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.corant.Corant;
import org.corant.kernel.logging.LoggerFactory;
import org.corant.suites.elastic.metadata.resolver.ElasticIndexingResolver;

/**
 * corant-suites-elastic
 *
 * @author bingo 上午10:53:48
 *
 */
public class ElasticSchemaUtils {

  public static void stdout(String clusterName, BiConsumer<String, Map<String, Object>> out) {
    prepare(clusterName);
    Corant corant = new Corant(ElasticSchemaUtils.class, "-disable_boost_line");
    corant.start();
    ElasticIndexingResolver indexingResolver =
        Corant.cdi().select(ElasticIndexingResolver.class).get();
    indexingResolver.getIndexings().forEach((n, i) -> {
      out.accept(n, asMap("setting", i.getSetting().getSetting(), "mappings",
          asMap(Elastic6Constants.TYP_NME, i.getSchema())));
    });
  }

  static void prepare(String clusterName) {
    LoggerFactory.disableLogger();
    System.setProperty("corant.temp.webserver.auto-start", "false");
    System.setProperty("corant.temp.elastic." + clusterName + ".auto-update-schame", "false");
  }
}

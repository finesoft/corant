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

import static org.corant.shared.util.Maps.mapOf;
import static org.corant.suites.cdi.Instances.select;
import java.util.Map;
import java.util.function.BiConsumer;
import org.corant.Corant;
import org.corant.config.ConfigUtils;
import org.corant.kernel.logging.LoggerFactory;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.suites.elastic.metadata.resolver.ElasticIndexingResolver;

/**
 * corant-suites-elastic
 *
 * @author bingo 上午10:53:48
 *
 */
public class ElasticDeveloperKits {

  public static void stdout(String clusterName, BiConsumer<String, Map<String, Object>> out) {
    try (Corant corant = prepare(clusterName)) {
      ElasticIndexingResolver indexingResolver = select(ElasticIndexingResolver.class).get();
      indexingResolver.getIndexings().forEach((n, i) -> {
        out.accept(n, mapOf("settings", i.getSetting().getSetting(), "mappings",
            mapOf(Elastic6Constants.TYP_NME, i.getSchema())));
      });
    } catch (Exception e) {
      throw new CorantRuntimeException(e);
    }
  }

  static Corant prepare(String clusterName) {
    LoggerFactory.disableLogger();
    ConfigUtils.adjust("webserver.auto-start", "false",
        "elastic." + clusterName + ".auto-update-schame", "false");
    return Corant.run(ElasticDeveloperKits.class, new String[] {Corant.DISABLE_BOOST_LINE_CMD});
  }
}

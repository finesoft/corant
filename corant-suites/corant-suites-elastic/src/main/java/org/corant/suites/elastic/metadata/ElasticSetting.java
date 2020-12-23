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
package org.corant.suites.elastic.metadata;

import static org.corant.shared.util.Maps.mapOf;
import static org.corant.shared.util.Objects.defaultObject;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.corant.suites.elastic.metadata.annotation.EsDocument;
import org.elasticsearch.common.xcontent.XContentHelper;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午4:15:36
 *
 */
public class ElasticSetting {

  public static final int DFLT_NUM_OF_SHARDS = 1;
  public static final int DFLT_NUM_OF_REPS = 0;

  private final Map<String, Object> setting = new LinkedHashMap<>();

  public ElasticSetting(Map<String, Object> setting) {
    if (setting != null) {
      this.setting.putAll(setting);
    }
  }

  public static ElasticSetting of(Map<String, Object> golbalSetting, EsDocument docAnn) {
    Map<String, Object> map = new HashMap<>();
    if (golbalSetting != null) {
      map.putAll(golbalSetting);
    }
    int numberOfShards = defaultObject(docAnn.number_of_shards(), DFLT_NUM_OF_SHARDS);
    int numberOfReplicas = defaultObject(docAnn.number_of_replicas(), DFLT_NUM_OF_REPS);
    XContentHelper.update(map,
        mapOf("index",
            mapOf("number_of_shards", numberOfShards <= 0 ? DFLT_NUM_OF_SHARDS : numberOfShards,
                "number_of_replicas", numberOfReplicas < 0 ? DFLT_NUM_OF_REPS : numberOfReplicas)),
        true);
    return new ElasticSetting(map);
  }

  /**
   *
   * @return the setting
   */
  public Map<String, Object> getSetting() {
    return setting;
  }

}

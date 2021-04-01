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
package org.corant.modules.elastic.data.metadata;

import static org.corant.shared.util.Objects.areEqual;
import static org.corant.shared.util.Objects.forceCast;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * corant-modules-elastic-data
 *
 * <p>
 * The elastic indexing metadata, include index name and index setting and mapping
 * <p>
 *
 * @author bingo 下午4:13:06
 *
 */
public class ElasticIndexing {

  private final String name;
  private final ElasticSetting setting;
  private final Map<String, Object> schema = new LinkedHashMap<>();
  private final ElasticMapping mapping;

  /**
   * @param name
   * @param setting
   * @param mapping
   */
  public ElasticIndexing(String name, ElasticSetting setting, ElasticMapping mapping,
      Map<String, Object> schema) {
    this.name = name;
    this.setting = setting;
    this.mapping = mapping;
    this.schema.putAll(schema);
  }

  public ElasticMapping getMapping(Class<?> clazz) {
    if (areEqual(mapping.getDocumentClass(), clazz)) {
      return forceCast(mapping);
    } else {
      for (ElasticMapping childMapping : mapping) {
        if (areEqual(childMapping.getDocumentClass(), clazz)) {
          return forceCast(childMapping);
        }
      }
    }
    return null;
  }

  /**
   * The index name
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * The index mapping schema
   *
   * @return the schema
   */
  public Map<String, Object> getSchema() {
    return schema;
  }

  /**
   * The index setting
   *
   * @return the setting
   */
  public ElasticSetting getSetting() {
    return setting;
  }

}

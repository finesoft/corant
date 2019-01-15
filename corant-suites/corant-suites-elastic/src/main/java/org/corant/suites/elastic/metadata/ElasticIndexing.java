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

import static org.corant.shared.util.ObjectUtils.shouldNotNull;
import java.util.Map;
import org.corant.suites.elastic.metadata.annotation.EsDocument;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午4:13:06
 *
 */
public class ElasticIndexing {

  private final String name;

  private final ElasticSetting setting;

  /**
   * @param name
   */
  public ElasticIndexing(String name, ElasticSetting setting) {
    super();
    this.name = shouldNotNull(name);
    this.setting = setting;
  }

  public static ElasticIndexing of(Map<String, Object> golbalSetting, EsDocument docAnn,
      String version) {
    String name =
        String.join(ElasticMapping.VERSION_SEPARATOR, shouldNotNull(docAnn.indexName()), version);
    return new ElasticIndexing(name, ElasticSetting.of(golbalSetting, docAnn));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ElasticIndexing other = (ElasticIndexing) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   *
   * @return the setting
   */
  public ElasticSetting getSetting() {
    return setting;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (name == null ? 0 : name.hashCode());
    return result;
  }

}

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

import java.util.LinkedHashMap;
import java.util.Map;
import org.elasticsearch.index.VersionType;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午4:12:48
 *
 */
public class ElasticMapping {

  public static final String VERSION_SEPARATOR = "_";

  private final ElasticIndexing index;
  private final Class<?> documentClass;
  private final Map<String, Object> schema = new LinkedHashMap<>();
  private final boolean versioned;
  private final String versionPropertyName;
  private final VersionType versionType;
  private final ElasticRelation relation;

  /**
   * @param index
   * @param documentClass
   * @param schema
   * @param relation
   * @param versioned
   * @param versionPropertyName
   * @param versionType
   */
  public ElasticMapping(ElasticIndexing index, Class<?> documentClass, Map<String, Object> schema,
      ElasticRelation relation, boolean versioned, String versionPropertyName,
      VersionType versionType) {
    super();
    this.index = index;
    this.documentClass = documentClass;
    this.relation = relation;
    this.versioned = versioned;
    this.versionPropertyName = versionPropertyName;
    this.versionType = versionType;
    if (schema != null) {
      this.schema.putAll(schema);
    }
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
    ElasticMapping other = (ElasticMapping) obj;
    if (documentClass == null) {
      if (other.documentClass != null) {
        return false;
      }
    } else if (!documentClass.equals(other.documentClass)) {
      return false;
    }
    if (index == null) {
      if (other.index != null) {
        return false;
      }
    } else if (!index.equals(other.index)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @return the documentClass
   */
  public Class<?> getDocumentClass() {
    return documentClass;
  }

  /**
   *
   * @return the index
   */
  public ElasticIndexing getIndex() {
    return index;
  }

  /**
   *
   * @return the relation
   */
  public ElasticRelation getRelation() {
    return relation;
  }

  /**
   *
   * @return the schema
   */
  public Map<String, Object> getSchema() {
    return schema;
  }

  /**
   *
   * @return the versionPropertyName
   */
  public String getVersionPropertyName() {
    return versionPropertyName;
  }

  /**
   *
   * @return the versionType
   */
  public VersionType getVersionType() {
    return versionType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (documentClass == null ? 0 : documentClass.hashCode());
    result = prime * result + (index == null ? 0 : index.hashCode());
    return result;
  }

  /**
   *
   * @return the versioned
   */
  public boolean isVersioned() {
    return versioned;
  }

}

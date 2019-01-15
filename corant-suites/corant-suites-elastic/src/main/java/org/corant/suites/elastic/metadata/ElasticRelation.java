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

import static org.corant.shared.util.MapUtils.asMap;
import static org.corant.shared.util.StreamUtils.asStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.corant.suites.elastic.metadata.annotation.EsRelation;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午3:34:48
 *
 */
public class ElasticRelation {

  private Class<?> parentClass;

  private Set<Class<?>> childrenClasses = new LinkedHashSet<>();

  private String fieldName;

  public ElasticRelation(Class<?> parentClass, EsRelation ann) {
    this.parentClass = parentClass;
    asStream(ann.children()).forEach(childrenClasses::add);
    fieldName = ann.fieldName();
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
    ElasticRelation other = (ElasticRelation) obj;
    if (childrenClasses == null) {
      if (other.childrenClasses != null) {
        return false;
      }
    } else if (!childrenClasses.equals(other.childrenClasses)) {
      return false;
    }
    if (fieldName == null) {
      if (other.fieldName != null) {
        return false;
      }
    } else if (!fieldName.equals(other.fieldName)) {
      return false;
    }
    if (parentClass == null) {
      if (other.parentClass != null) {
        return false;
      }
    } else if (!parentClass.equals(other.parentClass)) {
      return false;
    }
    return true;
  }

  public Map<String, Object> genSchema() {
    String parent = parentClass.getSimpleName();
    Set<String> children =
        childrenClasses.stream().map(Class::getSimpleName).collect(Collectors.toSet());
    return asMap(fieldName, asMap(parent, children));
  }

  /**
   *
   * @return the childrenClasses
   */
  public Set<Class<?>> getChildrenClasses() {
    return childrenClasses;
  }

  /**
   *
   * @return the fieldName
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   *
   * @return the parentClass
   */
  public Class<?> getParentClass() {
    return parentClass;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (childrenClasses == null ? 0 : childrenClasses.hashCode());
    result = prime * result + (fieldName == null ? 0 : fieldName.hashCode());
    result = prime * result + (parentClass == null ? 0 : parentClass.hashCode());
    return result;
  }

}

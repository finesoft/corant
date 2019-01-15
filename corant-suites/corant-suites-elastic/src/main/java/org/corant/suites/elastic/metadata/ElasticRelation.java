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

}

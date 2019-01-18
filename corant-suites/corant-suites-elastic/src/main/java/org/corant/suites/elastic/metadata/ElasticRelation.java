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

import static org.corant.shared.util.StreamUtils.asStream;
import java.util.LinkedHashSet;
import java.util.Set;
import org.corant.suites.elastic.metadata.annotation.EsJoinChild;
import org.corant.suites.elastic.metadata.annotation.EsJoinParent;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午3:34:48
 *
 */
public class ElasticRelation {

  private final Class<?> parentClass;

  private final String parentName;

  private final Set<String> childrenNames = new LinkedHashSet<>();

  private final String fieldName;

  public ElasticRelation(Class<?> parentClass, EsJoinParent parent, EsJoinChild... children) {
    this.parentClass = parentClass;
    fieldName = parent.fieldName();
    parentName = parent.parentName();
    asStream(children).map(EsJoinChild::childName).forEach(childrenNames::add);
  }

  /**
   *
   * @return the childrenNames
   */
  public Set<String> getChildrenNames() {
    return childrenNames;
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

  /**
   *
   * @return the parentName
   */
  public String getParentName() {
    return parentName;
  }

}

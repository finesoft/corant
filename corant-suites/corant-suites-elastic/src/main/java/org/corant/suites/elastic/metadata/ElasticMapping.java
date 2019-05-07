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

import static org.corant.shared.util.Assertions.shouldBeFalse;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.MapUtils.mapOf;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.corant.suites.elastic.metadata.resolver.ResolverUtils;
import org.corant.suites.elastic.model.ElasticDocument;
import org.elasticsearch.index.VersionType;

/**
 * corant-suites-elastic
 *
 * @author bingo 下午2:47:08
 *
 */
public class ElasticMapping implements Iterable<ElasticMapping> {

  private final Class<?> documentClass;
  private final boolean versioned;
  private final VersionType versionType;
  private final Set<ElasticMapping> children = new LinkedHashSet<>();
  private final String name;
  private final String joinFiledName;
  private final boolean root;

  /**
   * @param documentClass
   * @param root
   * @param joinFiledName
   * @param name
   * @param versionType
   */
  public ElasticMapping(Class<?> documentClass, boolean root, String joinFiledName, String name,
      VersionType versionType) {
    super();
    this.documentClass = documentClass;
    this.root = root;
    this.joinFiledName = joinFiledName;
    this.name = name;
    this.versionType = versionType;
    versioned = versionType != VersionType.INTERNAL;
  }

  @SuppressWarnings("unchecked")
  public <T> T fromMap(Map<String, Object> map) {
    return (T) ResolverUtils.toObject(map, documentClass);
  }

  /**
   *
   * @return the children
   */
  public Set<ElasticMapping> getChildren() {
    return children;
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
   * @return the joinFiledName
   */
  public String getJoinFiledName() {
    return joinFiledName;
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
   * @return the versionType
   */
  public VersionType getVersionType() {
    return versionType;
  }

  /**
   *
   * @return the root
   */
  public boolean isRoot() {
    return root;
  }

  /**
   *
   * @return the versioned
   */
  public boolean isVersioned() {
    return versioned;
  }

  @Override
  public Iterator<ElasticMapping> iterator() {
    return children.iterator();
  }

  public Map<String, Object> toMap(ElasticDocument doc) {
    Map<String, Object> convertedMap = ResolverUtils.toMap(doc);
    if (!isEmpty(convertedMap) && getJoinFiledName() != null) {
      shouldBeFalse(convertedMap.containsKey(getJoinFiledName()),
          "Join field name and property name conflicts %s", getJoinFiledName());
      if (isRoot()) {
        convertedMap.put(getJoinFiledName(), mapOf("name", getName()));
      } else {
        String parentId = shouldNotNull(doc.getPId(), "Parent id can not null");
        convertedMap.put(getJoinFiledName(), mapOf("name", getName(), "parent", parentId));
      }
    }
    return convertedMap;
  }

}

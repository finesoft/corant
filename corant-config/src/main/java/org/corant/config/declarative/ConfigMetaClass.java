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
package org.corant.config.declarative;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * corant-config
 *
 * @author bingo 11:32:11
 *
 */
public class ConfigMetaClass {

  private final String keyRoot;
  private final int keyIndex;
  private final Class<?> clazz;
  private final List<ConfigMetaField> fields = new ArrayList<>();
  private final boolean ignoreNoAnnotatedItem;

  /**
   * Returns a configuration metadata object
   *
   * @param keyRoot the configuration key prefix
   * @param keyIndex the configuration property index
   * @param clazz the declarative configuration class
   * @param ignoreNoAnnotatedItem whether to ignore the field that does not have annotaion.
   */
  protected ConfigMetaClass(String keyRoot, int keyIndex, Class<?> clazz,
      boolean ignoreNoAnnotatedItem) {
    this.keyRoot = keyRoot;
    this.keyIndex = keyIndex;
    this.clazz = clazz;
    this.ignoreNoAnnotatedItem = ignoreNoAnnotatedItem;
  }

  public Class<?> getClazz() {
    return clazz;
  }

  public Set<String> getDefaultItemKeys() {
    return fields.stream().map(ConfigMetaField::getDefaultKey).collect(Collectors.toSet());
  }

  public List<ConfigMetaField> getFields() {
    return fields;
  }

  public int getKeyIndex() {
    return keyIndex;
  }

  public String getKeyRoot() {
    return keyRoot;
  }

  public boolean isIgnoreNoAnnotatedItem() {
    return ignoreNoAnnotatedItem;
  }

  @Override
  public String toString() {
    return "ConfigClass [keyRoot=" + keyRoot + ", keyIndex=" + keyIndex + ", clazz=" + clazz
        + ", fields=" + fields + ", ignoreNoAnnotatedItem=" + ignoreNoAnnotatedItem + "]";
  }

  void addField(ConfigMetaField field) {
    fields.add(field);
  }

  void setFields(List<ConfigMetaField> fields) {
    this.fields.clear();
    if (fields != null) {
      this.fields.addAll(fields);
    }
  }

}

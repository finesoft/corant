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

import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Assertions.shouldNotNull;
import static org.corant.shared.util.Fields.traverseFields;
import java.lang.reflect.Modifier;
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
public class ConfigClass<T extends DeclarativeConfig> {

  private final String keyRoot;
  private final int keyIndex;
  private final Class<T> clazz;
  private final List<ConfigField> fields = new ArrayList<>();
  private final boolean ignoreNoAnnotatedItem;

  public ConfigClass(Class<T> clazz) {
    this.clazz = clazz;
    ConfigKeyRoot configKeyRoot = shouldNotNull(findAnnotation(clazz, ConfigKeyRoot.class, true));
    keyRoot = configKeyRoot.value();
    keyIndex = configKeyRoot.keyIndex();
    ignoreNoAnnotatedItem = configKeyRoot.ignoreNoAnnotatedItem();
    traverseFields(clazz, field -> {
      if (!Modifier.isFinal(field.getModifiers())) {
        fields.add(new ConfigField(this, field));
      }
    });
  }

  public Class<T> getClazz() {
    return clazz;
  }

  public Set<String> getDefaultItemKeys() {
    return getFields().stream().map(ConfigField::getDefaultKey).collect(Collectors.toSet());
  }

  public List<ConfigField> getFields() {
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

}

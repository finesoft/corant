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

import java.lang.reflect.Field;

/**
 * corant-config
 *
 * @author bingo 11:32:23
 *
 */
public class ConfigMetaField {

  private final ConfigMetaClass configClass;
  private final Field field;
  private final String keyItem;
  private final ConfigInjector injector;
  private final String defaultValue;
  private final String defaultKey;
  private final String defaultNull;

  protected ConfigMetaField(ConfigMetaClass configClass, Field field, String keyItem,
      ConfigInjector injector, String defaultValue, String defaultKey, String defaultNull) {
    this.configClass = configClass;
    this.field = field;
    this.keyItem = keyItem;
    this.injector = injector;
    this.defaultValue = defaultValue;
    this.defaultKey = defaultKey;
    this.defaultNull = defaultNull;
  }

  public ConfigMetaClass getConfigClass() {
    return configClass;
  }

  public String getDefaultKey() {
    return defaultKey;
  }

  public String getDefaultValue() {
    return defaultValue.equals(defaultNull) ? null : defaultValue;
  }

  public Field getField() {
    return field;
  }

  public ConfigInjector getInjector() {
    return injector;
  }

  public String getKeyItem() {
    return keyItem;
  }

  @Override
  public String toString() {
    return "ConfigField [configClass=" + configClass.getClazz() + ", field=" + field + ", keyItem="
        + keyItem + ", pattern=" + injector + ", defaultValue=" + defaultValue + ", defaultKey="
        + defaultKey + ", defaultNull=" + defaultNull + "]";
  }

}

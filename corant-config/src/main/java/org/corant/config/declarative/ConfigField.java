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

import static org.corant.config.CorantConfigResolver.concatKey;
import static org.corant.shared.util.Strings.isBlank;
import java.lang.reflect.Field;

/**
 * corant-config
 *
 * @author bingo 11:32:23
 *
 */
public class ConfigField {

  private final ConfigClass configClass;
  private final Field field;
  private final String keyItem;
  private final ConfigPropertyInjector injector;
  private final String defaultValue;
  private final String defaultKey;
  private final String defaultNull;

  /**
   * @param configClass
   * @param field
   * @param keyItem
   * @param injector
   * @param defaultValue
   * @param defaultKey
   * @param defaultNull
   */
  protected ConfigField(ConfigClass configClass, Field field, String keyItem,
      ConfigPropertyInjector injector, String defaultValue, String defaultKey, String defaultNull) {
    super();
    this.configClass = configClass;
    this.field = field;
    this.keyItem = keyItem;
    this.injector = injector;
    this.defaultValue = defaultValue;
    this.defaultKey = defaultKey;
    this.defaultNull = defaultNull;
  }

  public ConfigClass getConfigClass() {
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

  public ConfigPropertyInjector getInjector() {
    return injector;
  }

  public String getKey(String infix) {
    if (isBlank(infix)) {
      return defaultKey;
    } else {
      return concatKey(configClass.getKeyRoot(), infix, keyItem);
    }
  }

  public String getKeyItem() {
    return keyItem;
  }

  @Override
  public String toString() {
    return "ConfigField [configClass=" + configClass + ", field=" + field + ", keyItem=" + keyItem
        + ", pattern=" + injector + ", defaultValue=" + defaultValue + ", defaultKey=" + defaultKey
        + ", defaultNull=" + defaultNull + "]";
  }

}

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

import static org.corant.config.ConfigUtils.concatKey;
import static org.corant.config.ConfigUtils.dashify;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isBlank;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

/**
 * corant-config
 *
 * @author bingo 11:32:23
 *
 */
public class ConfigField {

  private final ConfigClass<?> configClass;
  private final Field field;
  private final String keyItem;
  private final DeclarativePattern pattern;
  private final String defaultValue;
  private final String defaultKey;

  ConfigField(ConfigClass<?> configClass, Field field) {
    this.configClass = configClass;
    this.field = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
      field.setAccessible(true);
      return field;
    });
    ConfigKeyItem configKeyItem =
        defaultObject(field.getAnnotation(ConfigKeyItem.class), () -> ConfigKeyItem.EMPTY);
    keyItem = isBlank(configKeyItem.value()) ? dashify(field.getName()) : configKeyItem.value();
    pattern = defaultObject(configKeyItem.pattern(), () -> DeclarativePattern.SUFFIX);
    defaultValue = configKeyItem.defaultValue();
    defaultKey = concatKey(configClass.getKeyRoot(), keyItem);

    if (pattern == DeclarativePattern.PREFIX) {
      Type fieldType = field.getGenericType();
      if (fieldType instanceof ParameterizedType) {
        Type rawType = ((ParameterizedType) fieldType).getRawType();
        shouldBeTrue(rawType.equals(Map.class),
            "We only support Map field type for PREFIX pattern %s %s.",
            configClass.getClazz().getName(), field.getName());
      } else {
        shouldBeTrue(fieldType.equals(Map.class),
            "We only support Map field type for PREFIX pattern %s %s.",
            configClass.getClazz().getName(), field.getName());
      }
    }
  }

  public ConfigClass<?> getConfigClass() {
    return configClass;
  }

  public String getDefaultKey() {
    return defaultKey;
  }

  public String getDefaultValue() {
    return defaultValue.equals(ConfigKeyItem.NO_DFLT_VALUE) ? null : defaultValue;
  }

  public Field getField() {
    return field;
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

  public DeclarativePattern getPattern() {
    return pattern;
  }

  @Override
  public String toString() {
    return "ConfigField [configClass=" + configClass + ", field=" + field + ", keyItem=" + keyItem
        + ", pattern=" + pattern + ", defaultValue=" + defaultValue + ", defaultKey=" + defaultKey
        + "]";
  }

}

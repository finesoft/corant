/*
 * Copyright (c) 2013-2021, Bingo.Chen (finesoft@gmail.com).
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
import static org.corant.config.CorantConfigResolver.dashify;
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Fields.traverseFields;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isBlank;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-config
 *
 * @author bingo 上午11:27:11
 *
 */
public class ConfigMetaResolver {

  /**
   * Create declarative configuration metadata
   *
   * @param clazz
   * @return declarative
   */
  public static ConfigMetaClass declarative(Class<?> clazz) {
    ConfigKeyRoot configKeyRoot = findAnnotation(clazz, ConfigKeyRoot.class, true);
    if (configKeyRoot == null || isBlank(configKeyRoot.value())) {
      return null;
    }
    String keyRoot = configKeyRoot.value();
    int keyIndex = configKeyRoot.keyIndex();
    boolean ignoreNoAnnotatedItem = configKeyRoot.ignoreNoAnnotatedItem();
    final ConfigMetaClass configClass =
        new ConfigMetaClass(keyRoot, keyIndex, clazz, ignoreNoAnnotatedItem);
    traverseFields(clazz, field -> {
      if (!Modifier.isFinal(field.getModifiers())) {
        if (!ignoreNoAnnotatedItem || field.getAnnotation(ConfigKeyItem.class) != null) {
          Field theField = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
            field.setAccessible(true);
            return field;
          });
          ConfigKeyItem configKeyItem =
              defaultObject(field.getAnnotation(ConfigKeyItem.class), () -> ConfigKeyItem.EMPTY);
          String keyItem =
              isBlank(configKeyItem.value()) ? dashify(field.getName()) : configKeyItem.value();
          DeclarativePattern pattern =
              defaultObject(configKeyItem.pattern(), () -> DeclarativePattern.SUFFIX);
          String defaultValue = configKeyItem.defaultValue();
          String defaultKey = concatKey(keyRoot, keyItem);
          String defaultNull = ConfigKeyItem.NO_DFLT_VALUE;
          if (pattern == DeclarativePattern.PREFIX) {
            Type fieldType = field.getGenericType();
            if (fieldType instanceof ParameterizedType) {
              Type rawType = ((ParameterizedType) fieldType).getRawType();
              shouldBeTrue(rawType.equals(Map.class),
                  "We only support Map field type for PREFIX pattern %s %s.", clazz.getName(),
                  field.getName());
            } else {
              shouldBeTrue(fieldType.equals(Map.class),
                  "We only support Map field type for PREFIX pattern %s %s.", clazz.getName(),
                  field.getName());
            }
          }
          configClass.addField(new ConfigMetaField(configClass, theField, keyItem, pattern,
              defaultValue, defaultKey, defaultNull));
        }
      }
    });
    return configClass;
  }

  public static ConfigMetaClass microprofile(Class<?> clazz, String prefix) {
    ConfigProperties configProperties = findAnnotation(clazz, ConfigProperties.class, true);
    if (configProperties == null) {
      return null;
    }
    final ConfigMetaClass configClass =
        new ConfigMetaClass(defaultString(prefix, configProperties.prefix()), 0, clazz, false);
    traverseFields(clazz, field -> {
      if (!Modifier.isFinal(field.getModifiers())) {
        Field theField = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
          field.setAccessible(true);
          return field;
        });
        ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
        String keyItem = configProperty == null || isBlank(configProperty.name()) ? field.getName()
            : configProperty.name();
        String defaultValue = ConfigProperty.UNCONFIGURED_VALUE;
        String defaultKey = concatKey(prefix, keyItem);
        String defaultNull = ConfigProperty.UNCONFIGURED_VALUE;
        configClass.addField(new ConfigMetaField(configClass, theField, keyItem,
            ConfigInjector.DEFAULT_INJECTOR, defaultValue, defaultKey, defaultNull));
      }
    });
    return configClass;
  }
}

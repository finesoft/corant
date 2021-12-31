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
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Fields.traverseFields;
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
   * @param clazz the configuration class
   */
  public static ConfigMetaClass declarative(Class<?> clazz) {
    Class<?> klass = ConfigClasses.resolveClass(clazz);
    ConfigKeyRoot configKeyRoot = ConfigClasses.createRoot(klass);
    if (configKeyRoot == null) {
      return null;
    }
    String root = configKeyRoot.value();
    int index = configKeyRoot.keyIndex();
    boolean ignore = configKeyRoot.ignoreNoAnnotatedItem();
    final ConfigMetaClass configClass = new ConfigMetaClass(root, index, klass, ignore);
    traverseFields(klass, field -> {
      if (!Modifier.isFinal(field.getModifiers())
          && (!ignore || field.getAnnotation(ConfigKeyItem.class) != null)) {
        Field theField = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
          field.setAccessible(true);
          return field;
        });
        ConfigKeyItem cfgKeyItem = ConfigClasses.createItem(theField);
        String keyItem = cfgKeyItem.name();
        DeclarativePattern pattern = cfgKeyItem.pattern();
        String defaultValue = cfgKeyItem.defaultValue();
        String defaultKey = concatKey(root, keyItem);
        String defaultNull = ConfigKeyItem.NO_DFLT_VALUE;
        if (pattern == DeclarativePattern.PREFIX) {
          Type fieldType = theField.getGenericType();
          if (fieldType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) fieldType).getRawType();
            shouldBeTrue(rawType.equals(Map.class),
                "We only support Map field type for PREFIX pattern %s %s.", klass.getName(),
                theField.getName());
          } else {
            shouldBeTrue(fieldType.equals(Map.class),
                "We only support Map field type for PREFIX pattern %s %s.", klass.getName(),
                theField.getName());
          }
        }
        configClass.addField(new ConfigMetaField(configClass, theField, keyItem, pattern,
            defaultValue, defaultKey, defaultNull));
      }
    });
    return configClass;
  }

  /**
   * Create a micro-profile configuration properties instance
   *
   * @param clazz the configuration properties class
   * @param prefix the configuration property name prefix
   */
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
        String defaultValue = configProperty != null ? configProperty.defaultValue()
            : ConfigProperty.UNCONFIGURED_VALUE;
        String defaultKey = concatKey(prefix, keyItem);
        String defaultNull = ConfigProperty.UNCONFIGURED_VALUE;
        configClass.addField(new ConfigMetaField(configClass, theField, keyItem,
            ConfigInjector.DEFAULT_INJECTOR, defaultValue, defaultKey, defaultNull));
      }
    });
    return configClass;
  }

}

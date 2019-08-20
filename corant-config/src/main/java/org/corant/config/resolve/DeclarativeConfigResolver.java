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
package org.corant.config.resolve;

import static org.corant.config.ConfigUtils.getGroupConfigNames;
import static org.corant.shared.util.AnnotationUtils.findAnnotation;
import static org.corant.shared.util.ConversionUtils.toObject;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.FieldUtils.traverseFields;
import static org.corant.shared.util.StringUtils.EMPTY;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import org.corant.config.ConfigUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-config
 *
 * @author bingo 下午7:42:56
 *
 */
public class DeclarativeConfigResolver {

  public static <T extends DeclarativeConfig> Map<String, T> resolve(Class<T> cls)
      throws InstantiationException, IllegalAccessException {
    Map<String, T> configMaps = new HashMap<>();
    ConfigClass<T> configClass = resolveClass(cls);
    if (configClass != null) {
      Config config = ConfigProvider.getConfig();
      Set<String> keys = resolveKeys(configClass, config);
      configMaps.putAll(resolve(config, keys, configClass));
    }
    return configMaps;
  }

  static Set<String> resolveKeys(ConfigClass<?> configClass, Config config) {
    final String prefix = ConfigUtils.regulerKeyPrefix(configClass.keyRoot);
    Set<String> keys = new HashSet<>();
    Set<String> itemKeys = new LinkedHashSet<>();
    for (String itemKey : config.getPropertyNames()) {
      if (itemKey.startsWith(prefix)) {
        itemKeys.add(itemKey);
      }
    }
    Set<String> dfltKeys = new HashSet<>(itemKeys);
    dfltKeys.retainAll(configClass.getDefaultItemKeys());
    if (isNotEmpty(dfltKeys)) {
      keys.add(EMPTY);
    }
    itemKeys.removeAll(dfltKeys);
    if (isNotEmpty(itemKeys)) {
      keys.addAll(getGroupConfigNames(config,
          s -> defaultString(s).startsWith(prefix) && !dfltKeys.contains(s), 1).keySet());
    }
    return keys;
  }

  static <T extends DeclarativeConfig> Map<String, T> resolve(Config config, Set<String> keys,
      ConfigClass<T> configClass) throws InstantiationException, IllegalAccessException {
    Map<String, T> configMaps = new HashMap<>();
    if (isNotEmpty(keys)) {
      for (String key : keys) {
        T configObject = configClass.clazz.newInstance();
        for (ConfigField cf : configClass.fields) {
          handle(config, key, configObject, cf);
        }
        if (configObject.isValid()) {
          configObject.onPostConstruct();
          configMaps.put(key, configObject);
        }
      }
    }
    return configMaps;
  }

  static <T extends DeclarativeConfig> T resolve(Config config, String infix, T configObject,
      ConfigClass<T> configClass) throws IllegalArgumentException, IllegalAccessException {
    for (ConfigField cf : configClass.fields) {
      handle(config, infix, configObject, cf);
    }
    return configObject;
  }

  static <T extends DeclarativeConfig> void handle(Config config, String infix, T configObject,
      ConfigField configField) throws IllegalArgumentException, IllegalAccessException {
    String defaultValue = configField.defaultValue;
    if (defaultValue.equals(ConfigKeyItem.NO_DFLT_VALUE)) {
      defaultValue = null;
    }
    Class<?> filedType = configField.type;
    Field field = configField.field;
    String key = configField.getKey(infix);
    // TODO FIXME Handle pattern
    Optional<?> val = config.getOptionalValue(key, filedType);
    if (val.isPresent()) {
      field.set(configObject, val.get());
    } else if (defaultValue != null) {
      if (filedType.equals(List.class)) {
        Class<?> listType =
            (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        String[] parts = ConfigUtils.splitValue(defaultValue);
        List<Object> list = new ArrayList<>();
        for (String i : parts) {
          list.add(toObject(i, listType));
        }
        field.set(configObject, list);
      } else if (filedType.equals(Set.class)) {
        Class<?> listType =
            (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        String[] parts = ConfigUtils.splitValue(defaultValue);
        Set<Object> list = new HashSet<>();
        for (String i : parts) {
          list.add(toObject(i, listType));
        }
        field.set(configObject, list);
      } else if (field.getType().equals(Optional.class)) {
        Class<?> optionalType =
            (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        field.set(configObject, Optional.of(toObject(defaultValue, optionalType)));
      } else {
        field.set(configObject, toObject(defaultValue, field.getType()));
      }
    } else if (field.getType().equals(Optional.class)) {
      field.set(configObject, Optional.empty());
    } else if (field.getType().equals(OptionalInt.class)) {
      field.set(configObject, OptionalInt.empty());
    } else if (field.getType().equals(OptionalDouble.class)) {
      field.set(configObject, OptionalDouble.empty());
    } else if (field.getType().equals(OptionalLong.class)) {
      field.set(configObject, OptionalLong.empty());
    }
  }

  static <T extends DeclarativeConfig> ConfigClass<T> resolveClass(Class<T> cls) {
    ConfigKeyRoot ckr = findAnnotation(cls, ConfigKeyRoot.class, true);
    if (ckr != null && isNotBlank(ckr.value())) {
      return new ConfigClass<>(cls);
    }
    return null;
  }

  static class ConfigClass<T extends DeclarativeConfig> {
    final String keyRoot;
    final Class<T> clazz;
    final List<ConfigField> fields = new ArrayList<>();

    ConfigClass(Class<T> cls) {
      ConfigKeyRoot ckr = findAnnotation(cls, ConfigKeyRoot.class, true);
      keyRoot = ckr.value();
      clazz = cls;
      traverseFields(cls, (f) -> {
        if (f.isAnnotationPresent(ConfigKeyItem.class)) {
          fields.add(new ConfigField(ckr.value(), f));
        }
      });
    }

    Set<String> getDefaultItemKeys() {
      return fields.stream().map(f -> f.defaultKey).collect(Collectors.toSet());
    }
  }

  static class ConfigField {
    final Field field;
    final String keyRoot;
    final String keyItem;
    final DeclarativePattern pattern;
    final String defaultValue;
    final int keyRootLen;
    final int keyItemLen;
    final String defaultKey;
    final Class<?> type;

    ConfigField(String keyRoot, Field field) {
      this.keyRoot = keyRoot;
      keyRootLen = keyRoot.length();
      this.field = field;
      this.field.setAccessible(true);
      type = field.getType();
      ConfigKeyItem cki = field.getAnnotation(ConfigKeyItem.class);
      if (isBlank(cki.value())) {
        keyItem = ConfigUtils.dashify(field.getName());
      } else {
        keyItem = cki.value();
      }
      keyItemLen = keyItem.length();
      pattern = cki.pattern();
      defaultValue = cki.defaultValue();
      defaultKey = ConfigUtils.concatKey(keyRoot, keyItem);
    }

    String getKey(String infix) {
      if (isBlank(infix)) {
        return defaultKey;
      } else {
        return ConfigUtils.concatKey(keyRoot, infix, keyItem);
      }
    }
  }
}

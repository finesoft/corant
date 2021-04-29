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

import static org.corant.config.CorantConfigResolver.KEY_DELIMITER;
import static org.corant.config.CorantConfigResolver.getGroupConfigKeys;
import static org.corant.config.CorantConfigResolver.regulateKeyPrefix;
import static org.corant.shared.util.Empties.isEmpty;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.defaultString;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.corant.shared.exception.CorantRuntimeException;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-config
 *
 * @author bingo 下午7:42:56
 *
 */
public class ConfigInstances {

  public static <T> T resolveMicroprofile(Class<T> cls, String prefix) {
    Map<String, T> map = new HashMap<>(1);
    ConfigMetaClass configClass = ConfigMetaResolver.microprofile(cls, prefix);
    if (configClass != null) {
      Config config = ConfigProvider.getConfig();
      try {
        map = resolveConfigInstances(config, setOf(EMPTY), configClass); // FIXME EMPTY?
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    return isEmpty(map) ? null : map.values().iterator().next();
  }

  public static <T> Map<String, T> resolveMulti(Class<T> cls) {
    Map<String, T> configMaps = null;
    ConfigMetaClass configClass = ConfigMetaResolver.declarative(cls);
    if (configClass != null) {
      Config config = ConfigProvider.getConfig();
      Set<String> keys = resolveKeys(configClass, config);
      try {
        configMaps = resolveConfigInstances(config, keys, configClass);
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    return defaultObject(configMaps, HashMap::new);
  }

  public static <T> T resolveSingle(Class<T> cls) {
    Map<String, T> map = new HashMap<>(1);
    ConfigMetaClass configClass = ConfigMetaResolver.declarative(cls);
    if (configClass != null) {
      Config config = ConfigProvider.getConfig();
      try {
        map = resolveConfigInstances(config, setOf(EMPTY), configClass); // FIXME EMPTY?
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    return isEmpty(map) ? null : map.values().iterator().next();
  }

  static Class<?> getFieldActualTypeArguments(Field field, int index) {
    return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[index];
  }

  static <T extends DeclarativeConfig> T resolveConfigInstance(Config config, String infix,
      T configObject, ConfigMetaClass configClass) throws Exception {
    for (ConfigMetaField cf : configClass.getFields()) {
      cf.getInjector().inject(config, infix, configObject, cf);
    }
    return configObject;
  }

  @SuppressWarnings("unchecked")
  static <T> Map<String, T> resolveConfigInstances(Config config, Set<String> keys,
      ConfigMetaClass configClass) throws Exception {
    if (isNotEmpty(keys)) {
      Map<String, T> configMaps = new HashMap<>(keys.size());
      for (String key : keys) {
        // T configObject = configClass.getClazz().newInstance();// JDK8
        Object configObject = configClass.getClazz().getDeclaredConstructor().newInstance();// JDK9+
        for (ConfigMetaField cf : configClass.getFields()) {
          cf.getInjector().inject(config, key, configObject, cf);
        }
        if (configObject instanceof DeclarativeConfig) {
          DeclarativeConfig declarativeConfigObject = (DeclarativeConfig) configObject;
          declarativeConfigObject.onPostConstruct(config, key);
          if (declarativeConfigObject.isValid()) {
            configMaps.put(key, (T) declarativeConfigObject);
          }
        } else {
          configMaps.put(key, (T) configObject);
        }
      }
      return configMaps;
    }
    return null;
  }

  static Set<String> resolveKeys(ConfigMetaClass configClass, Config config) {
    final String prefix = regulateKeyPrefix(configClass.getKeyRoot());
    Set<String> keys = new HashSet<>();
    Set<String> itemKeys = new LinkedHashSet<>();
    for (String itemKey : config.getPropertyNames()) {
      if (itemKey.startsWith(prefix)) {
        itemKeys.add(itemKey);
      }
    }
    Set<String> matchedItemKeys = new HashSet<>(itemKeys);
    Set<String> defaultItemKeys = new HashSet<>();
    for (String configDefaultKey : configClass.getDefaultItemKeys()) {
      if (itemKeys.removeIf(ik -> isDefaultKey(configDefaultKey, ik))) {
        defaultItemKeys.add(configDefaultKey);
      }
    }
    if (isNotEmpty(defaultItemKeys)) {
      keys.add(EMPTY);
    }
    if (isNotEmpty(itemKeys) && configClass.getKeyIndex() > 0) {
      keys.addAll(getGroupConfigKeys(matchedItemKeys,
          s -> defaultString(s).startsWith(prefix)
              && defaultItemKeys.stream().noneMatch(d -> isDefaultKey(d, s)),
          configClass.getKeyIndex()).keySet());
    }
    return keys;
  }

  private static boolean isDefaultKey(String defaultKey, String candidateKey) {
    return defaultKey.equals(candidateKey) || candidateKey.startsWith(defaultKey + KEY_DELIMITER);
  }

}

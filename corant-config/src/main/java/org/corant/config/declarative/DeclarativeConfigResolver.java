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

import static org.corant.config.ConfigUtils.getGroupConfigKeys;
import static org.corant.config.ConfigUtils.regulerKeyPrefix;
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Sets.setOf;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isNotBlank;
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
public class DeclarativeConfigResolver {

  public static <T extends DeclarativeConfig> Map<String, T> resolveMulti(Class<T> cls) {
    Map<String, T> configMaps = new HashMap<>();
    ConfigClass<T> configClass = resolveConfigClass(cls);
    if (configClass != null) {
      Config config = ConfigProvider.getConfig();
      Set<String> keys = resolveKeys(configClass, config);
      try {
        configMaps.putAll(resolveConfigInstances(config, keys, configClass));
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    return configMaps;
  }

  public static <T extends DeclarativeConfig> T resolveSingle(Class<T> cls) {
    Map<String, T> map = new HashMap<>();
    ConfigClass<T> configClass = resolveConfigClass(cls);
    if (configClass != null) {
      Config config = ConfigProvider.getConfig();
      try {
        map = resolveConfigInstances(config, setOf(EMPTY), configClass); // FIXME EMPTY?
      } catch (Exception e) {
        throw new CorantRuntimeException(e);
      }
    }
    return map.isEmpty() ? null : map.values().iterator().next();
  }

  static <T extends DeclarativeConfig> ConfigClass<T> resolveConfigClass(Class<T> cls) {
    ConfigKeyRoot ckr = findAnnotation(cls, ConfigKeyRoot.class, true);
    if (ckr != null && isNotBlank(ckr.value())) {
      return new ConfigClass<>(cls);
    }
    return null;
  }

  static <T extends DeclarativeConfig> T resolveConfigInstance(Config config, String infix,
      T configObject, ConfigClass<T> configClass) throws Exception {
    for (ConfigField cf : configClass.getFields()) {
      cf.getPattern().resolve(config, infix, configObject, cf);
    }
    return configObject;
  }

  static <T extends DeclarativeConfig> Map<String, T> resolveConfigInstances(Config config,
      Set<String> keys, ConfigClass<T> configClass) throws Exception {
    Map<String, T> configMaps = new HashMap<>();
    if (isNotEmpty(keys)) {
      for (String key : keys) {
        T configObject = configClass.getClazz().newInstance();
        for (ConfigField cf : configClass.getFields()) {
          cf.getPattern().resolve(config, key, configObject, cf);
        }
        configObject.onPostConstruct(config, key);
        if (configObject.isValid()) {
          configMaps.put(key, configObject);
        }
      }
    }
    return configMaps;
  }

  static Set<String> resolveKeys(ConfigClass<?> configClass, Config config) {
    final String prefix = regulerKeyPrefix(configClass.getKeyRoot());
    Set<String> keys = new HashSet<>();
    Set<String> itemKeys = new LinkedHashSet<>();
    for (String itemKey : config.getPropertyNames()) {
      if (itemKey.startsWith(prefix)) {
        itemKeys.add(itemKey);
      }
    }
    Set<String> dfltKeys = new HashSet<>();
    configClass.getDefaultItemKeys().forEach(dik -> {
      if (itemKeys.removeIf(ik -> ik.startsWith(dik))) {
        dfltKeys.add(dik);
      }
    });
    if (isNotEmpty(dfltKeys)) {
      keys.add(EMPTY);
    }
    itemKeys.removeAll(dfltKeys);
    if (isNotEmpty(itemKeys)) {
      keys.addAll(getGroupConfigKeys(config,
          s -> defaultString(s).startsWith(prefix)
              && dfltKeys.stream().noneMatch(dk -> s.startsWith(dk)),
          configClass.getKeyIndex()).keySet());
    }
    return keys;
  }

  static Class<?> getFieldActualTypeArguments(Field field, int index) {
    return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[index];
  }

}

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
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Strings.EMPTY;
import static org.corant.shared.util.Strings.defaultString;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.corant.config.CorantConfig;
import org.corant.config.declarative.ConfigInjector.InjectStrategy;

/**
 * corant-config
 *
 * <p>
 * A convenient configuration instance resolver.
 *
 * @author bingo 下午7:42:56
 */
public class ConfigInstances {

  public static <T> Map<String, T> resolveConfigInstances(CorantConfig config,
      ConfigMetaClass configClass) throws Exception {
    return resolveConfigInstances(config, resolveKeys(configClass, config), configClass);
  }

  @SuppressWarnings("unchecked")
  public static <T> Map<String, T> resolveConfigInstances(CorantConfig config, Set<String> keys,
      ConfigMetaClass configClass) throws Exception {
    if (isNotEmpty(keys)) {
      Map<String, T> configMaps = new HashMap<>(keys.size());
      for (String key : keys) {
        // T configObject = configClass.getClazz().newInstance();// JDK8
        Object configObject = configClass.getClazz().getDeclaredConstructor().newInstance();// JDK9+
        if (configClass.getInjectStrategy() == InjectStrategy.FIELD) {
          injectFields(config, configClass, key, configObject);
        } else if (configClass.getInjectStrategy() == InjectStrategy.PROPERTY) {
          injectMethods(config, configClass, key, configObject);
        } else if (configClass.getInjectStrategy() == InjectStrategy.PROPERTY_FIELD) {
          injectMethods(config, configClass, key, configObject);
          injectFields(config, configClass, key, configObject);
        } else {
          injectFields(config, configClass, key, configObject);
          injectMethods(config, configClass, key, configObject);
        }
        if (configObject instanceof DeclarativeConfig declarativeConfigObject) {
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

  protected static void injectMethods(CorantConfig config, ConfigMetaClass configClass, String key,
      Object configObject) throws Exception {
    for (ConfigMetaMethod cp : configClass.getMethods()) {
      cp.getInjector().inject(config, key, configObject, cp);
    }
  }

  protected static void injectFields(CorantConfig config, ConfigMetaClass configClass, String key,
      Object configObject) throws Exception {
    for (ConfigMetaField cf : configClass.getFields()) {
      cf.getInjector().inject(config, key, configObject, cf);
    }
  }

  static Set<String> resolveKeys(ConfigMetaClass configClass, CorantConfig config) {
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

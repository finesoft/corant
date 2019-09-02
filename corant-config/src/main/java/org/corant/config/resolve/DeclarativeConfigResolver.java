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

import static org.corant.config.ConfigUtils.concatKey;
import static org.corant.config.ConfigUtils.dashify;
import static org.corant.config.ConfigUtils.getFieldActualTypeArguments;
import static org.corant.config.ConfigUtils.getGroupConfigNames;
import static org.corant.config.ConfigUtils.hanleInfixKey;
import static org.corant.config.ConfigUtils.regulerKeyPrefix;
import static org.corant.shared.util.AnnotationUtils.findAnnotation;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.CollectionUtils.setOf;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.FieldUtils.traverseFields;
import static org.corant.shared.util.ObjectUtils.defaultObject;
import static org.corant.shared.util.StringUtils.EMPTY;
import static org.corant.shared.util.StringUtils.defaultString;
import static org.corant.shared.util.StringUtils.isBlank;
import static org.corant.shared.util.StringUtils.isNotBlank;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.corant.shared.conversion.ConverterRegistry;
import org.corant.shared.exception.CorantRuntimeException;
import org.corant.shared.util.ClassUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * corant-config
 *
 * @author bingo 下午7:42:56
 *
 */
public class DeclarativeConfigResolver {

  static final Set<Class<?>> supportTypes =
      new HashSet<>(ClassUtils.WRAPPER_PRIMITIVE_MAP.keySet());

  static {
    ConverterRegistry.getSupportConverters().keySet().stream()
        .filter(ct -> String.class.equals(ct.getSourceClass())).map(ct -> ct.getSourceClass())
        .forEach(supportTypes::add);
  }

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
        map = resolveConfigInstances(config, setOf(EMPTY), configClass);
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
    Set<String> dfltKeys = new HashSet<>(itemKeys);
    dfltKeys.retainAll(configClass.getDefaultItemKeys());
    if (isNotEmpty(dfltKeys)) {
      keys.add(EMPTY);
    }
    itemKeys.removeAll(dfltKeys);
    if (isNotEmpty(itemKeys)) {
      keys.addAll(getGroupConfigNames(config,
          s -> defaultString(s).startsWith(prefix) && !dfltKeys.contains(s),
          configClass.getKeyIndex()).keySet());
    }
    return keys;
  }

  public static class ConfigClass<T extends DeclarativeConfig> {
    private final String keyRoot;
    private final int keyIndex;
    private final Class<T> clazz;
    private final List<ConfigField> fields = new ArrayList<>();
    private final boolean ignoreNoAnnotatedItem;

    public ConfigClass(Class<T> cls) {
      ConfigKeyRoot ckr = findAnnotation(cls, ConfigKeyRoot.class, true);
      keyRoot = ckr.value();
      clazz = cls;
      keyIndex = ckr.keyIndex();
      ignoreNoAnnotatedItem = ckr.ignoreNoAnnotatedItem();
      traverseFields(cls, (f) -> {
        if (f.isAnnotationPresent(ConfigKeyItem.class)) {
          getFields().add(new ConfigField(this, f));
        } else if (!ignoreNoAnnotatedItem) {
          Class<?> ft = ClassUtils.primitiveToWrapper(f.getType());
          if (Collection.class.isAssignableFrom(ft)) {
            ft = getFieldActualTypeArguments(f, 0);
          }
          if (supportTypes.contains(ft)) {
            getFields().add(new ConfigField(this, f));
          }
        }
      });
    }

    public Class<T> getClazz() {
      return clazz;
    }

    public Set<String> getDefaultItemKeys() {
      return getFields().stream().map(f -> f.getDefaultKey()).collect(Collectors.toSet());
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

  public static class ConfigField {
    private final ConfigClass<?> configClass;
    private final Field field;
    private final String keyItem;
    private final DeclarativePattern pattern;
    private final String defaultValue;
    private final String defaultKey;
    private final Class<?> type;

    ConfigField(ConfigClass<?> configClass, Field field) {
      this.configClass = configClass;
      ConfigKeyItem cki =
          defaultObject(field.getAnnotation(ConfigKeyItem.class), ConfigKeyItem.EMPTY);
      this.field = AccessController.doPrivileged((PrivilegedAction<Field>) () -> {
        field.setAccessible(true);
        return field;
      });
      type = field.getType();
      keyItem = isBlank(cki.value()) ? dashify(field.getName()) : cki.value();
      pattern = defaultObject(cki.pattern(), DeclarativePattern.SUFFIX);
      defaultValue = cki.defaultValue();
      defaultKey = concatKey(configClass.getKeyRoot(), getKeyItem());
      if (pattern == DeclarativePattern.PREFIX) {
        shouldBeTrue(type.equals(Map.class),
            "We only support Map field type for PREFIX pattern %s %s.",
            configClass.getClazz().getName(), field.getName());
        Class<?> mapKeyType = getFieldActualTypeArguments(field, 0);
        Class<?> mapValType = getFieldActualTypeArguments(field, 1);
        shouldBeTrue(
            mapKeyType.equals(String.class)
                && (mapValType.equals(Object.class) || mapValType.equals(String.class)),
            "We only support Map<String,Object> or Map<String,String> field type for PREFIX pattern %s %s.",
            configClass.getClazz().getName(), field.getName());
      }
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
        return getDefaultKey();
      } else {
        return concatKey(configClass.getKeyRoot(), hanleInfixKey(infix), getKeyItem());
      }
    }

    public String getKeyItem() {
      return keyItem;
    }

    public DeclarativePattern getPattern() {
      return pattern;
    }

    public Class<?> getType() {
      return type;
    }

    @Override
    public String toString() {
      return "ConfigField [configClass=" + configClass + ", field=" + field + ", keyItem=" + keyItem
          + ", pattern=" + pattern + ", defaultValue=" + defaultValue + ", defaultKey=" + defaultKey
          + ", type=" + type + "]";
    }
  }
}

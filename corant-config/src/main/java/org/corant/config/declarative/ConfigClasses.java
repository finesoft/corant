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

import static org.corant.config.CorantConfigResolver.dashify;
import static org.corant.config.CorantConfigResolver.splitKey;
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Classes.asClass;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.reflect.Field;
import org.corant.config.Configs;
import org.corant.config.declarative.ConfigKeyItem.ConfigKeyItemLiteral;
import org.corant.config.declarative.ConfigKeyRoot.ConfigKeyRootLiteral;

/**
 * corant-config
 *
 * @author bingo 下午7:00:42
 *
 */
public class ConfigClasses {

  public static final String SPEC = "corant.declarative-config.%s";

  public static final String SPEC_REAL = SPEC + ".specializes";

  public static final String SPEC_KEY_ROOT_FMT = SPEC + ".key-root";
  public static final String SPEC_KEY_ROOT_INDEX_FMT = SPEC + ".key-root-index";
  public static final String SPEC_KEY_ROOT_IG_NOANN_ITEM_FMT = SPEC + ".key-root-ignore";

  public static final String SPEC_KEY_ITEM_FMT = SPEC + ".key-item.%s";
  public static final String SPEC_KEY_ITEM_DV_FMT = SPEC_KEY_ITEM_FMT + ".default-value";
  public static final String SPEC_KEY_ITEM_PTN_FMT = SPEC_KEY_ITEM_FMT + ".pattern";

  public static ConfigKeyItem createItem(Field field) {
    String className = getUserClass(field.getDeclaringClass()).getCanonicalName();
    String fieldName = field.getName();
    String keyItemCfgKey = String.format(SPEC_KEY_ITEM_FMT, className, fieldName);
    String dfltValCfgKey = String.format(SPEC_KEY_ITEM_DV_FMT, className, fieldName);
    String ptnCfgKey = String.format(SPEC_KEY_ITEM_DV_FMT, className, fieldName);
    String keyItem = Configs.getValue(keyItemCfgKey, String.class);
    String defaultValue = Configs.getValue(dfltValCfgKey, String.class);
    DeclarativePattern pattern = Configs.getValue(ptnCfgKey, DeclarativePattern.class);
    ConfigKeyItem ann = field.getAnnotation(ConfigKeyItem.class);
    if (ann != null) {
      if (keyItem == null) {
        keyItem = ann.name();
      }
      if (defaultValue == null) {
        defaultValue = ann.defaultValue();
      }
      if (pattern == null) {
        pattern = ann.pattern();
      }
    }
    return new ConfigKeyItemLiteral(defaultValue, isBlank(keyItem) ? dashify(fieldName) : keyItem,
        defaultObject(pattern, DeclarativePattern.SUFFIX));
  }

  public static ConfigKeyRoot createRoot(Class<?> cls) {
    Class<?> clazz = resolveClass(cls);
    String className = getUserClass(clazz).getCanonicalName();
    String rootCfgKey = String.format(SPEC_KEY_ROOT_FMT, className);
    String indexCfgKey = String.format(SPEC_KEY_ROOT_INDEX_FMT, className);
    String ignoreKey = String.format(SPEC_KEY_ROOT_IG_NOANN_ITEM_FMT, className);
    String keyRoot = Configs.getValue(rootCfgKey, String.class);
    Integer keyIndex = Configs.getValue(indexCfgKey, Integer.class);
    String ignoreNotAnnItem = Configs.getValue(ignoreKey, String.class);
    ConfigKeyRoot ann = findAnnotation(clazz, ConfigKeyRoot.class, true);
    Boolean ignore = isNotBlank(ignoreNotAnnItem) ? toBoolean(ignoreNotAnnItem) : null;
    if (ann != null) {
      if (keyRoot == null) {
        keyRoot = ann.value();
      }
      if (keyIndex != null) {
        keyIndex = ann.keyIndex();
      }
      if (ignore == null) {
        ignore = ann.ignoreNoAnnotatedItem();
      }
    }
    if (keyRoot != null) {
      return new ConfigKeyRootLiteral(defaultObject(ignore, Boolean.FALSE),
          keyIndex == null || keyIndex < 0 ? splitKey(keyRoot).length : keyIndex, keyRoot);
    }
    return null;
  }

  public static Class<?> resolveClass(Class<?> clazz) {
    Class<?> klass = getUserClass(clazz);
    String classNameKey = String.format(SPEC_REAL, klass.getCanonicalName());
    String className = Configs.getValue(classNameKey, String.class);
    if (isNotBlank(className)) {
      Class<?> realClass = asClass(className);
      shouldBeTrue(klass.isAssignableFrom(realClass), "The config class %s must be derived from %s",
          className, klass.getCanonicalName());
      return realClass;
    }
    return klass;
  }
}

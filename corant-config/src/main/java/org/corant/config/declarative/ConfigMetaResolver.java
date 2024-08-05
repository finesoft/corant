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

import static java.lang.String.format;
import static org.corant.config.CorantConfigResolver.concatKey;
import static org.corant.config.CorantConfigResolver.dashify;
import static org.corant.config.CorantConfigResolver.splitKey;
import static org.corant.shared.util.Annotations.findAnnotation;
import static org.corant.shared.util.Assertions.shouldBeTrue;
import static org.corant.shared.util.Classes.asClass;
import static org.corant.shared.util.Classes.getUserClass;
import static org.corant.shared.util.Conversions.toBoolean;
import static org.corant.shared.util.Fields.traverseFields;
import static org.corant.shared.util.Objects.defaultObject;
import static org.corant.shared.util.Strings.defaultString;
import static org.corant.shared.util.Strings.isBlank;
import static org.corant.shared.util.Strings.isNotBlank;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import org.corant.config.Configs;
import org.corant.config.PropertyAccessor;
import org.corant.config.PropertyAccessor.PropertyMetadata;
import org.corant.config.declarative.ConfigInjector.InjectStrategy;
import org.corant.config.declarative.ConfigKeyItem.ConfigKeyItemLiteral;
import org.corant.config.declarative.ConfigKeyRoot.ConfigKeyRootLiteral;
import org.corant.shared.ubiquity.Tuple;
import org.corant.shared.ubiquity.Tuple.Pair;
import org.eclipse.microprofile.config.inject.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * corant-config
 *
 * @author bingo 上午11:27:11
 */
public class ConfigMetaResolver {

  public static final String SPEC = "corant.declarative-config.%s";

  public static final String SPEC_REAL = SPEC + ".specializes";

  public static final String SPEC_INJECT_STRATEGY_FMT = SPEC + ".inject-strategy";
  public static final String SPEC_KEY_ROOT_FMT = SPEC + ".key-root";
  public static final String SPEC_KEY_ROOT_INDEX_FMT = SPEC + ".key-root-index";
  public static final String SPEC_KEY_ROOT_IG_NOANN_ITEM_FMT = SPEC + ".key-root-ignore";

  public static final String SPEC_KEY_ITEM_FMT = SPEC + ".key-item.%s";
  public static final String SPEC_KEY_ITEM_DV_FMT = SPEC_KEY_ITEM_FMT + ".default-value";
  public static final String SPEC_KEY_ITEM_PTN_FMT = SPEC_KEY_ITEM_FMT + ".pattern";

  /**
   * Create declarative configuration metadata
   *
   * @param clazz the configuration class
   */
  public static ConfigMetaClass declarative(Class<?> clazz) {
    Pair<Class<?>, ConfigKeyRoot> resolved = resolveRoot(clazz);
    if (resolved.isEmpty()) {
      return null;
    }
    Class<?> klass = resolved.left();
    ConfigKeyRoot configKeyRoot = resolved.right();
    String root = configKeyRoot.value();
    int index = configKeyRoot.keyIndex();
    boolean ignore = configKeyRoot.ignoreNoAnnotatedItem();
    InjectStrategy injectStrategy = configKeyRoot.injectStrategy();
    final ConfigMetaClass configClass =
        new ConfigMetaClass(root, index, klass, ignore, injectStrategy);

    if (injectStrategy != InjectStrategy.PROPERTY) {
      traverseFields(klass, field -> {
        if (!Modifier.isFinal(field.getModifiers())
            && (!ignore || field.getAnnotation(ConfigKeyItem.class) != null)) {
          configClass.addField(extractedDeclarativeField(klass, root, configClass, field));
        }
      });
    }

    if (injectStrategy != InjectStrategy.FIELD) {
      List<PropertyMetadata> propertyMethods = new PropertyAccessor(klass).getPropertyMethods();
      for (PropertyMetadata pm : propertyMethods) {
        if (pm.getWriteMethod() == null) {
          continue;
        }
        if (!ignore
            || (pm.getReadMethod() != null
                && pm.getReadMethod().getAnnotation(ConfigKeyItem.class) != null)
            || pm.getWriteMethod().getAnnotation(ConfigKeyItem.class) != null) {
          configClass.addMethod(extractedDeclarativeMethod(klass, root, configClass,
              pm.getName(), pm.getReadMethod(), pm.getWriteMethod()));
        }
      }
    }

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
    final ConfigMetaClass configClass = new ConfigMetaClass(
        defaultString(prefix, configProperties.prefix()), 0, clazz, false, null);
    traverseFields(clazz, field -> {
      if (!Modifier.isFinal(field.getModifiers())) {
        configClass.addField(extractedMicroprofileField(prefix, configClass, field));
      }
    });
    return configClass;
  }

  public static ConfigKeyItem resolveItem(Class<?> configClass, String propertyName,
      AnnotatedElement... elements) {
    String className = getUserClass(configClass).getCanonicalName();
    String keyItemCfgKey = format(SPEC_KEY_ITEM_FMT, className, propertyName);
    String dfltValCfgKey = format(SPEC_KEY_ITEM_DV_FMT, className, propertyName);
    String ptnCfgKey = format(SPEC_KEY_ITEM_PTN_FMT, className, propertyName);
    String keyItem = Configs.getValue(keyItemCfgKey, String.class);
    String defaultValue = Configs.getValue(dfltValCfgKey, String.class);
    DeclarativePattern pattern = Configs.getValue(ptnCfgKey, DeclarativePattern.class);
    ConfigKeyItem annotation;
    for (AnnotatedElement element : elements) {
      if (element == null) {
        continue;
      }
      annotation = element.getAnnotation(ConfigKeyItem.class);
      if (annotation != null) {
        if (keyItem == null) {
          keyItem = annotation.name();
        }
        if (defaultValue == null) {
          defaultValue = annotation.defaultValue();
        }
        if (pattern == null) {
          pattern = annotation.pattern();
        }
        break;
      }
    }
    return new ConfigKeyItemLiteral(defaultValue,
        isBlank(keyItem) ? dashify(propertyName) : keyItem, pattern);
  }

  public static Pair<Class<?>, ConfigKeyRoot> resolveRoot(Class<?> cls) {
    Class<?> clazz = resolveClass(cls);
    String className = clazz.getCanonicalName();
    String injectStrategyCfgKey = format(SPEC_INJECT_STRATEGY_FMT, className);
    String rootCfgKey = format(SPEC_KEY_ROOT_FMT, className);
    String indexCfgKey = format(SPEC_KEY_ROOT_INDEX_FMT, className);
    String ignoreKey = format(SPEC_KEY_ROOT_IG_NOANN_ITEM_FMT, className);
    String keyRoot = Configs.getValue(rootCfgKey, String.class);
    Integer keyIndex = Configs.getValue(indexCfgKey, Integer.class);
    String ignoreNotAnnItem = Configs.getValue(ignoreKey, String.class);
    InjectStrategy injectStrategy = Configs.getValue(injectStrategyCfgKey, InjectStrategy.class);
    Boolean ignore = isNotBlank(ignoreNotAnnItem) ? toBoolean(ignoreNotAnnItem) : null;
    ConfigKeyRoot annotation = findAnnotation(clazz, ConfigKeyRoot.class, true);
    if (annotation != null) {
      if (keyRoot == null) {
        keyRoot = annotation.value();
      }
      if (keyIndex == null) {
        keyIndex = annotation.keyIndex();
      }
      if (ignore == null) {
        ignore = annotation.ignoreNoAnnotatedItem();
      }
      if (injectStrategy == null) {
        injectStrategy = annotation.injectStrategy();
      }
    }
    if (keyRoot != null) {
      return Tuple.pairOf(clazz,
          new ConfigKeyRootLiteral(defaultObject(ignore, Boolean.FALSE),
              keyIndex == null || keyIndex < 0 ? splitKey(keyRoot).length : keyIndex, keyRoot,
              injectStrategy));
    }
    return Pair.empty();
  }

  protected static ConfigMetaField extractedDeclarativeField(Class<?> klass, String root,
      final ConfigMetaClass configClass, Field field) {
    field.setAccessible(true);
    ConfigKeyItem cfgKeyItem = resolveItem(klass, field.getName(), field);
    String keyItem = cfgKeyItem.name();
    DeclarativePattern pattern = cfgKeyItem.pattern();
    String defaultValue = cfgKeyItem.defaultValue();
    String defaultKey = concatKey(root, keyItem);
    String defaultNull = ConfigKeyItem.NO_DFLT_VALUE;
    if (pattern == DeclarativePattern.PREFIX) {
      Type fieldType = field.getGenericType();
      if (fieldType instanceof ParameterizedType) {
        Type rawType = ((ParameterizedType) fieldType).getRawType();
        shouldBeTrue(rawType.equals(Map.class),
            "We only support Map field type for PREFIX pattern %s %s.", klass.getName(),
            field.getName());
      } else {
        shouldBeTrue(fieldType.equals(Map.class),
            "We only support Map field type for PREFIX pattern %s %s.", klass.getName(),
            field.getName());
      }
    }
    return new ConfigMetaField(configClass, field, keyItem, pattern, defaultValue, defaultKey,
        defaultNull);
  }

  protected static ConfigMetaMethod extractedDeclarativeMethod(Class<?> klass, String root,
      final ConfigMetaClass configClass, String propertyName, Method readMethod,
      Method writeMethod) {
    ConfigKeyItem cfgKeyItem = resolveItem(klass, propertyName, readMethod, writeMethod);
    String keyItem = cfgKeyItem.name();
    DeclarativePattern pattern = cfgKeyItem.pattern();
    String defaultValue = cfgKeyItem.defaultValue();
    String defaultKey = concatKey(root, keyItem);
    String defaultNull = ConfigKeyItem.NO_DFLT_VALUE;
    writeMethod.setAccessible(true);
    Type parameterType = writeMethod.getGenericParameterTypes()[0];
    // Class<?> parameterClass = setter.getParameterTypes()[0];
    if (pattern == DeclarativePattern.PREFIX) {
      if (parameterType instanceof ParameterizedType) {
        Type rawType = ((ParameterizedType) parameterType).getRawType();
        shouldBeTrue(rawType.equals(Map.class),
            "We only support Map property type for PREFIX pattern %s %s.", klass.getName(),
            propertyName);
      } else {
        shouldBeTrue(parameterType.equals(Map.class),
            "We only support Map property type for PREFIX pattern %s %s.", klass.getName(),
            propertyName);
      }
    }
    return new ConfigMetaMethod(configClass, writeMethod, keyItem, pattern, defaultValue,
        defaultKey, defaultNull);
  }

  protected static ConfigMetaField extractedMicroprofileField(String prefix,
      final ConfigMetaClass configClass, Field field) {
    field.setAccessible(true);
    ConfigProperty configProperty = field.getAnnotation(ConfigProperty.class);
    String keyItem = configProperty == null || isBlank(configProperty.name()) ? field.getName()
        : configProperty.name();
    String defaultValue =
        configProperty != null ? configProperty.defaultValue() : ConfigProperty.UNCONFIGURED_VALUE;
    String defaultKey = concatKey(prefix, keyItem);
    String defaultNull = ConfigProperty.UNCONFIGURED_VALUE;
    return new ConfigMetaField(configClass, field, keyItem, ConfigInjector.DEFAULT_INJECTOR,
        defaultValue, defaultKey, defaultNull);
  }

  static Class<?> resolveClass(Class<?> clazz) {
    Class<?> klass = getUserClass(clazz);
    String classNameKey = format(SPEC_REAL, klass.getCanonicalName());
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

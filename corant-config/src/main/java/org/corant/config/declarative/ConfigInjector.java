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
import static org.corant.shared.util.Strings.isBlank;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import org.corant.config.CorantConfig;

/**
 * corant-config
 *
 * @author bingo 上午11:14:06
 */
public interface ConfigInjector {

  ConfigInjector DEFAULT_INJECTOR = new ConfigInjector() {};

  static String resolveInfixKey(String infix, String keyRoot, String keyItem, String defaultKey) {
    return isBlank(infix) ? defaultKey : concatKey(keyRoot, infix, keyItem);
  }

  static String resolvePrefixKey(String keyRoot, String keyItem) {
    return concatKey(keyRoot, keyItem);
  }

  default void inject(CorantConfig config, String infix, Object configObject,
      ConfigMetaField configField) throws Exception {
    String keyRoot = configField.getKeyRoot();
    String keyItem = configField.getKeyItem();
    String key = resolvePrefixKey(keyRoot, keyItem);
    Field field = configField.getField();
    Type type = field.getGenericType();
    String defaultValue = configField.getDefaultValue();
    Object obj = config.getConvertedValue(key, type, defaultValue);
    if (obj != null) {
      field.set(configObject, obj);
    }
  }

  default void inject(CorantConfig config, String infix, Object configObject,
      ConfigMetaMethod configMethod) throws Exception {
    String keyRoot = configMethod.getKeyRoot();
    String keyItem = configMethod.getKeyItem();
    String key = resolvePrefixKey(keyRoot, keyItem);
    if (!config.getCorantConfigSources().getInitializedPropertyNames().contains(key)) {
      return;
    }
    Method setter = configMethod.getMethod();
    Type type = setter.getGenericParameterTypes()[0];
    String defaultValue = configMethod.getDefaultValue();
    Object obj = config.getConvertedValue(key, type, defaultValue);
    if (obj != null) {
      setter.invoke(configObject, obj);
    }
  }

  enum InjectStrategy {
    FIELD, PROPERTY, PROPERTY_FIELD, FIELD_PROPERTY
  }
}

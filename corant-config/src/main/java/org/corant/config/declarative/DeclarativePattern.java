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
import static org.corant.config.CorantConfigResolver.removeSplitor;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.Streams.streamOf;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import org.corant.config.CorantConfig;
import org.corant.config.CorantConfigConversion;

/**
 * corant-config
 *
 * @author bingo 下午8:05:35
 */
public enum DeclarativePattern implements ConfigInjector {

  SUFFIX() {

    @Override
    public void inject(CorantConfig config, String infix, Object configObject,
        ConfigMetaField configField) throws Exception {
      Field field = configField.getField();
      String keyRoot = configField.getKeyRoot();
      String keyItem = configField.getKeyItem();
      String defaultKey = configField.getDefaultKey();
      String key = ConfigInjector.resolveInfixKey(infix, keyRoot, keyItem, defaultKey);
      Object obj = config.getConvertedValue(key, field.getGenericType(),
          configField.getDefaultValue(), ConfigKeyItem.NO_DFLT_VALUE); // FIXME NO_DFLT_VALUE
      if (obj != null) {
        field.set(configObject, obj);
      }
    }

    @Override
    public void inject(CorantConfig config, String infix, Object configObject,
        ConfigMetaMethod configMethod) throws Exception {

      String keyRoot = configMethod.getKeyRoot();
      String keyItem = configMethod.getKeyItem();
      String defaultKey = configMethod.getDefaultKey();
      String key = ConfigInjector.resolveInfixKey(infix, keyRoot, keyItem, defaultKey);
      if (!config.getCorantConfigSources().getInitializedPropertyNames().contains(key)) {
        return;
      }
      Method setter = configMethod.getMethod();
      Object obj = config.getConvertedValue(key, setter.getGenericParameterTypes()[0],
          configMethod.getDefaultValue(), ConfigKeyItem.NO_DFLT_VALUE); // FIXME NO_DFLT_VALUE
      if (obj != null) {
        setter.invoke(configObject, obj);
      }
    }

  },

  PREFIX() {

    @Override
    public void inject(CorantConfig config, String infix, Object configObject,
        ConfigMetaField configField) throws Exception {

      Map<String, Optional<String>> rawMap = new HashMap<>();

      String keyRoot = configField.getKeyRoot();
      String keyItem = configField.getKeyItem();
      String defaultKey = configField.getDefaultKey();

      String key =
          ConfigInjector.resolveInfixKey(infix, keyRoot, keyItem, defaultKey) + KEY_DELIMITER;
      streamOf(config.getPropertyNames()).filter(p -> p.startsWith(key)).forEach(k -> rawMap
          .put(removeSplitor(k.substring(key.length())), config.getOptionalValue(k, String.class)));

      if (isNotEmpty(rawMap)) {
        Field field = configField.getField();
        Type fieldType = field.getGenericType();
        Class<?> fieldClass = Object.class;
        if (field.getGenericType() instanceof Class) {
          fieldClass = (Class<?>) field.getGenericType();
        } else {
          Object defaultFieldValue = field.get(configObject);
          if (defaultFieldValue != null) {
            fieldClass = defaultFieldValue.getClass();
          }
        }
        // FIXME need merge???
        field.set(configObject, resolveValueMap(config, rawMap, fieldType, fieldClass));
      }
    }

    @Override
    public void inject(CorantConfig config, String infix, Object configObject,
        ConfigMetaMethod configMethod) throws Exception {
      Map<String, Optional<String>> rawMap = new HashMap<>();

      String keyRoot = configMethod.getKeyRoot();
      String keyItem = configMethod.getKeyItem();
      String defaultKey = configMethod.getDefaultKey();

      String key =
          ConfigInjector.resolveInfixKey(infix, keyRoot, keyItem, defaultKey) + KEY_DELIMITER;
      streamOf(config.getPropertyNames()).filter(p -> p.startsWith(key)).forEach(k -> rawMap
          .put(removeSplitor(k.substring(key.length())), config.getOptionalValue(k, String.class)));

      if (isNotEmpty(rawMap)) {
        Method setter = configMethod.getMethod();
        Type parameterType = setter.getGenericParameterTypes()[0];
        Class<?> parameterClass = setter.getParameterTypes()[0];
        // FIXME need merge???
        setter.invoke(configObject, resolveValueMap(config, rawMap, parameterType, parameterClass));
      }
    }

    @SuppressWarnings("rawtypes")
    protected Map resolveValueMap(CorantConfig config, Map<String, Optional<String>> rawMap,
        Type type, Class<?> typeClass) {
      Supplier<Map<?, ?>> factory;
      if (LinkedHashMap.class.isAssignableFrom(typeClass)) {
        factory = LinkedHashMap::new;
      } else if (TreeMap.class.isAssignableFrom(typeClass)) {
        factory = TreeMap::new;
      } else {
        factory = HashMap::new;
      }
      CorantConfigConversion conversion = config.getConversion();
      Type keyType = Object.class;
      Type valueType = Object.class;
      if (type instanceof ParameterizedType parameterizedType) {
        if (!(parameterizedType.getActualTypeArguments()[0] instanceof WildcardType)) {
          keyType = parameterizedType.getActualTypeArguments()[0];
        }
        if (!(parameterizedType.getActualTypeArguments()[1] instanceof WildcardType)) {
          valueType = parameterizedType.getActualTypeArguments()[1];
        }
      }
      return conversion.convertMap(rawMap, factory, keyType, valueType);
    }

  }

}

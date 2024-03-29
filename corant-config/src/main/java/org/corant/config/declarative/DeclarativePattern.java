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
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Streams.streamOf;
import java.lang.reflect.Field;
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
import org.eclipse.microprofile.config.Config;

/**
 * corant-config
 *
 * @author bingo 下午8:05:35
 *
 */
public enum DeclarativePattern implements ConfigInjector {

  SUFFIX() {

    @Override
    public void inject(Config config, String infix, Object configObject,
        ConfigMetaField configField) throws Exception {
      CorantConfig corantConfig = forceCast(config);
      Field field = configField.getField();
      String key = ConfigInjector.resolveInfixKey(infix, configField);
      Object obj = corantConfig.getConvertedValue(key, field.getGenericType(),
          configField.getDefaultValue(), ConfigKeyItem.NO_DFLT_VALUE); // FIXME NO_DFLT_VALUE
      if (obj != null) {
        field.set(configObject, obj);
      }
    }

  },

  PREFIX() {

    @SuppressWarnings("rawtypes")
    @Override
    public void inject(Config config, String infix, Object configObject,
        ConfigMetaField configField) throws Exception {

      Map<String, Optional<String>> rawMap = new HashMap<>();
      String key = ConfigInjector.resolveInfixKey(infix, configField) + KEY_DELIMITER;
      streamOf(config.getPropertyNames()).filter(p -> p.startsWith(key)).forEach(k -> rawMap
          .put(removeSplitor(k.substring(key.length())), config.getOptionalValue(k, String.class)));

      if (isNotEmpty(rawMap)) {
        Field field = configField.getField();
        Type fieldType = field.getGenericType();
        Supplier<Map<?, ?>> factory;
        Object defaultFieldValue = field.get(configObject);
        if (defaultFieldValue instanceof LinkedHashMap) {
          factory = LinkedHashMap::new;
        } else if (defaultFieldValue instanceof TreeMap) {
          factory = TreeMap::new;
        } else {
          factory = HashMap::new;
        }
        CorantConfig corantConfig = forceCast(config);
        CorantConfigConversion conversion = corantConfig.getConversion();
        Map valueMap;
        Type keyType = Object.class;
        Type valueType = Object.class;
        if (fieldType instanceof ParameterizedType) {
          ParameterizedType parameterizedFieldType = (ParameterizedType) fieldType;
          if (!(parameterizedFieldType.getActualTypeArguments()[0] instanceof WildcardType)) {
            keyType = parameterizedFieldType.getActualTypeArguments()[0];
          }
          if (!(parameterizedFieldType.getActualTypeArguments()[1] instanceof WildcardType)) {
            valueType = parameterizedFieldType.getActualTypeArguments()[1];
          }
        }
        valueMap = conversion.convertMap(rawMap, factory, keyType, valueType);
        field.set(configObject, valueMap); // FIXME
                                           // need
                                           // merge???
      }
    }
  }

}

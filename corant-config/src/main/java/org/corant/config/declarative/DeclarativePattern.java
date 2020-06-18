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

import static org.corant.config.ConfigUtils.removeSplitor;
import static org.corant.shared.util.Empties.isNotEmpty;
import static org.corant.shared.util.ObjectUtils.forceCast;
import static org.corant.shared.util.Streams.streamOf;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.corant.config.CorantConfig;
import org.corant.config.declarative.DeclarativeConfigResolver.ConfigField;
import org.eclipse.microprofile.config.Config;

/**
 * corant-config
 *
 * @author bingo 下午8:05:35
 *
 */
public enum DeclarativePattern {

  SUFFIX() {
    @Override
    public <T extends DeclarativeConfig> void resolve(Config config, String infix, T configObject,
        ConfigField configField) throws Exception {
      CorantConfig corantConfig = forceCast(config);
      Field field = configField.getField();
      String key = configField.getKey(infix);
      Object obj = corantConfig.getConvertedValue(key, field.getGenericType(),
          configField.getDefaultValue());
      if (obj != null) {
        field.set(configObject, obj);
      }
    }
  },

  PREFIX() {
    @Override
    public <T extends DeclarativeConfig> void resolve(Config config, String infix, T configObject,
        ConfigField configField) throws Exception {
      String key = configField.getKey(infix);
      int prefixLen = key.length();
      Map<String, Optional<String>> map =
          streamOf(config.getPropertyNames()).filter(p -> p.startsWith(key))
              .collect(Collectors.toMap(k -> k, v -> config.getOptionalValue(v, String.class)));
      if (isNotEmpty(map)) {
        Field field = configField.getField();
        Class<?> filedType = field.getType();
        Map<String, Object> valueMap = new HashMap<>();
        if (filedType.equals(Map.class)) {
          for (Entry<String, Optional<String>> entry : map.entrySet()) {
            entry.getValue().ifPresent(
                v -> valueMap.put(removeSplitor(entry.getKey().substring(prefixLen)), v));
          }
          field.set(configObject, valueMap);
        }
      }
    }
  };

  public abstract <T extends DeclarativeConfig> void resolve(Config config, String infix,
      T configObject, ConfigField configField) throws Exception;

}

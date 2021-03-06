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
import static org.corant.shared.util.Objects.forceCast;
import static org.corant.shared.util.Strings.isBlank;
import java.lang.reflect.Field;
import org.corant.config.CorantConfig;
import org.eclipse.microprofile.config.Config;

/**
 * corant-config
 *
 * @author bingo 上午11:14:06
 *
 */
public interface ConfigInjector {

  ConfigInjector DEFAULT_INJECTOR = new ConfigInjector() {};

  static String resolveInfixKey(String infix, ConfigMetaField field) {
    return isBlank(infix) ? field.getDefaultKey()
        : concatKey(field.getConfigClass().getKeyRoot(), infix, field.getKeyItem());
  }

  static String resolvePrefixKey(ConfigMetaField field) {
    return concatKey(field.getConfigClass().getKeyRoot(), field.getKeyItem());
  }

  default void inject(Config config, String infix, Object configObject, ConfigMetaField configField)
      throws Exception {
    CorantConfig corantConfig = forceCast(config);
    Field field = configField.getField();
    String key = resolvePrefixKey(configField);
    Object obj =
        corantConfig.getConvertedValue(key, field.getGenericType(), configField.getDefaultValue());
    if (obj != null) {
      field.set(configObject, obj);
    }
  }
}
